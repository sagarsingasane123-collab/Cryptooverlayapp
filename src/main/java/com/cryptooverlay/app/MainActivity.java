package com.cryptooverlay.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cryptooverlay.app.services.BinanceWebSocketService;
import com.cryptooverlay.app.services.OverlayService;

public class MainActivity extends AppCompatActivity {

    private TextView statusText;
    private TextView priceText;
    private TextView targetText;
    private Button startButton;
    private Button stopButton;
    private Button resetButton;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "CryptoOverlayPrefs";
    private static final String KEY_BASE_PRICE = "basePrice";
    private static final String KEY_TARGET_PRICE = "targetPrice";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        initializeUI();
        checkOverlayPermission();
        loadSavedPrices();
    }

    private void initializeUI() {
        statusText = findViewById(R.id.statusText);
        priceText = findViewById(R.id.priceText);
        targetText = findViewById(R.id.targetText);
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        resetButton = findViewById(R.id.resetButton);

        startButton.setOnClickListener(v -> startOverlay());
        stopButton.setOnClickListener(v -> stopOverlay());
        resetButton.setOnClickListener(v -> resetApp());
    }

    private void checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Overlay Permission Required")
                        .setMessage("This app needs permission to display overlay. Please enable it in settings.")
                        .setPositiveButton("Go to Settings", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                        .show();
            }
        }
    }

    private void loadSavedPrices() {
        float basePrice = sharedPreferences.getFloat(KEY_BASE_PRICE, 0);
        float targetPrice = sharedPreferences.getFloat(KEY_TARGET_PRICE, 0);

        if (basePrice == 0 || targetPrice == 0) {
            showPriceInputDialog();
        } else {
            updateUI(basePrice, targetPrice);
        }
    }

    private void showPriceInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Prices");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);

        EditText basePriceInput = new EditText(this);
        basePriceInput.setHint("Base Price (e.g., 45000)");
        basePriceInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(basePriceInput);

        EditText targetPriceInput = new EditText(this);
        targetPriceInput.setHint("Target Price (e.g., 46000)");
        targetPriceInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(targetPriceInput);

        builder.setView(layout);

        builder.setPositiveButton("Start", (dialog, which) -> {
            String baseStr = basePriceInput.getText().toString();
            String targetStr = targetPriceInput.getText().toString();

            if (baseStr.isEmpty() || targetStr.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please enter both prices", Toast.LENGTH_SHORT).show();
                return;
            }

            float basePrice = Float.parseFloat(baseStr);
            float targetPrice = Float.parseFloat(targetStr);

            savePrices(basePrice, targetPrice);
            updateUI(basePrice, targetPrice);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
            statusText.setText("Prices not set. Please try again.");
        });

        builder.setCancelable(false);
        builder.show();
    }

    private void savePrices(float basePrice, float targetPrice) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat(KEY_BASE_PRICE, basePrice);
        editor.putFloat(KEY_TARGET_PRICE, targetPrice);
        editor.apply();
    }

    private void updateUI(float basePrice, float targetPrice) {
        float upper = basePrice + targetPrice;
        float lower = basePrice - targetPrice;

        targetText.setText(String.format("Target Range: -%.2f to +%.2f\nLower: %.2f | Upper: %.2f",
                targetPrice, targetPrice, lower, upper));
        statusText.setText("Ready to start monitoring...");
    }

    private void startOverlay() {
        float basePrice = sharedPreferences.getFloat(KEY_BASE_PRICE, 0);
        float targetPrice = sharedPreferences.getFloat(KEY_TARGET_PRICE, 0);

        if (basePrice == 0 || targetPrice == 0) {
            Toast.makeText(this, "Please set prices first", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent serviceIntent = new Intent(this, OverlayService.class);
        serviceIntent.putExtra("basePrice", basePrice);
        serviceIntent.putExtra("targetPrice", targetPrice);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        statusText.setText("Overlay service started!");
        Toast.makeText(this, "Monitoring started", Toast.LENGTH_SHORT).show();
    }

    private void stopOverlay() {
        Intent serviceIntent = new Intent(this, OverlayService.class);
        stopService(serviceIntent);

        Intent wsServiceIntent = new Intent(this, BinanceWebSocketService.class);
        stopService(wsServiceIntent);

        statusText.setText("Service stopped");
        Toast.makeText(this, "Monitoring stopped", Toast.LENGTH_SHORT).show();
    }

    private void resetApp() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset App")
                .setMessage("Do you want to reset prices and start again?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    stopOverlay();
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.remove(KEY_BASE_PRICE);
                    editor.remove(KEY_TARGET_PRICE);
                    editor.apply();

                    statusText.setText("App reset. Setting prices...");
                    showPriceInputDialog();
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopOverlay();
    }
}
