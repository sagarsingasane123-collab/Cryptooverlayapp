package com.cryptooverlay.app.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.cryptooverlay.app.R;

public class OverlayService extends Service {

    private static final String TAG = "OverlayService";
    private static final String CHANNEL_ID = "CryptoOverlayChannel";

    private WindowManager windowManager;
    private View overlayView;
    private TextView priceDisplay;
    private TextView statusDisplay;
    private MediaPlayer mediaPlayer;

    private BinanceWebSocketService binanceService;
    private boolean isBound = false;

    private float basePrice;
    private float targetPrice;
    private boolean hasTriggeredUpper = false;
    private boolean hasTriggeredLower = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BinanceWebSocketService.LocalBinder binder = (BinanceWebSocketService.LocalBinder) service;
            binanceService = binder.getService();
            isBound = true;

            binanceService.setPriceListener(new BinanceWebSocketService.PriceListener() {
                @Override
                public void onPriceUpdate(double price) {
                    updateOverlay(price);
                }

                @Override
                public void onConnectionStateChanged(boolean connected) {
                    updateConnectionStatus(connected);
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        createOverlayView();
        initializeMediaPlayer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        basePrice = intent.getFloatExtra("basePrice", 0);
        targetPrice = intent.getFloatExtra("targetPrice", 0);

        Log.d(TAG, "OverlayService started - Base: " + basePrice + ", Target: " + targetPrice);

        // Start WebSocket service
        Intent wsIntent = new Intent(this, BinanceWebSocketService.class);
        startService(wsIntent);
        bindService(wsIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        // Show notification
        Notification notification = buildNotification();
        startForeground(1, notification);

        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Crypto Overlay",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Crypto Overlay Running")
                .setContentText("Monitoring BTC/USDT price...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();
    }

    private void createOverlayView() {
        LayoutInflater inflater = LayoutInflater.from(this);
        overlayView = inflater.inflate(R.layout.overlay_layout, null);

        priceDisplay = overlayView.findViewById(R.id.overlayPrice);
        statusDisplay = overlayView.findViewById(R.id.overlayStatus);

        // Set initial text
        priceDisplay.setText("Connecting...");
        statusDisplay.setText("Initializing...");

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.RIGHT;
        params.x = 0;
        params.y = 100;

        // Make draggable
        overlayView.setOnTouchListener(new View.OnTouchListener() {
            private int lastAction;
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        lastAction = MotionEvent.ACTION_DOWN;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(overlayView, params);
                        lastAction = MotionEvent.ACTION_MOVE;
                        return true;

                    case MotionEvent.ACTION_UP:
                        return lastAction == MotionEvent.ACTION_DOWN;
                }
                return false;
            }
        });

        windowManager.addView(overlayView, params);
    }

    private void initializeMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        try {
            // Create a beep sound programmatically
            // For now, you can use a beep sound file in assets
            // mediaPlayer.setDataSource(...);
            // mediaPlayer.prepare();
        } catch (Exception e) {
            Log.e(TAG, "MediaPlayer initialization failed", e);
        }
    }

    private void updateOverlay(double price) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
            if (priceDisplay != null) {
                priceDisplay.setText(String.format("BTC/USDT\n%.2f", price));
            }

            checkAlertConditions(price);
        });
    }

    private void checkAlertConditions(double price) {
        double upperBound = basePrice + targetPrice;
        double lowerBound = basePrice - targetPrice;

        String status = "";

        if (price >= upperBound && !hasTriggeredUpper) {
            playBeepSound();
            status = "🔔 ALERT: +50 Target Hit!";
            hasTriggeredUpper = true;
            hasTriggeredLower = false;
            Log.d(TAG, "Upper target triggered at price: " + price);
        } else if (price <= lowerBound && !hasTriggeredLower) {
            playBeepSound();
            status = "🔔 ALERT: -50 Target Hit!";
            hasTriggeredLower = true;
            hasTriggeredUpper = false;
            Log.d(TAG, "Lower target triggered at price: " + price);
        } else if (price < upperBound && price > lowerBound) {
            hasTriggeredUpper = false;
            hasTriggeredLower = false;
            status = String.format("Range: %.2f - %.2f", lowerBound, upperBound);
        }

        if (statusDisplay != null) {
            statusDisplay.setText(status);
        }
    }

    private void playBeepSound() {
        try {
            // Generate beep using AudioTrack for precise 1ms start
            // For production, use a pre-recorded beep sound file
            Log.d(TAG, "BEEP! Playing alert sound");
            Toast.makeText(this, "🔔 ALERT TRIGGERED!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error playing sound", e);
        }
    }

    private void updateConnectionStatus(boolean connected) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
            if (statusDisplay != null) {
                if (connected) {
                    statusDisplay.setText("Connected to Binance");
                } else {
                    statusDisplay.setText("Reconnecting...");
                }
            }
        });
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (overlayView != null) {
            windowManager.removeView(overlayView);
        }
        if (isBound) {
            unbindService(serviceConnection);
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        Intent wsIntent = new Intent(this, BinanceWebSocketService.class);
        stopService(wsIntent);

        Log.d(TAG, "OverlayService destroyed");
    }
}
