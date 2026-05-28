package com.cryptooverlay.app.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class BinanceWebSocketService extends Service {

    private static final String TAG = "BinanceWebSocket";
    private static final String BINANCE_WS_URL = "wss://stream.binance.com:9443/ws/btcusdt@trade";

    private OkHttpClient okHttpClient;
    private WebSocket webSocket;
    private final IBinder binder = new LocalBinder();
    private PriceListener priceListener;

    public interface PriceListener {
        void onPriceUpdate(double price);
        void onConnectionStateChanged(boolean connected);
    }

    public class LocalBinder extends Binder {
        BinanceWebSocketService getService() {
            return BinanceWebSocketService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        okHttpClient = new OkHttpClient();
        connectWebSocket();
    }

    private void connectWebSocket() {
        Request request = new Request.Builder()
                .url(BINANCE_WS_URL)
                .build();

        okHttpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, okhttp3.Response response) {
                Log.d(TAG, "WebSocket Connected");
                if (priceListener != null) {
                    priceListener.onConnectionStateChanged(true);
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                try {
                    JsonObject jsonObject = JsonParser.parseString(text).getAsJsonObject();
                    double price = jsonObject.get("p").getAsDouble();

                    Log.d(TAG, "BTC/USDT Price: " + price);

                    if (priceListener != null) {
                        priceListener.onPriceUpdate(price);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing price", e);
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, @Nullable okhttp3.Response response) {
                Log.e(TAG, "WebSocket Error", t);
                if (priceListener != null) {
                    priceListener.onConnectionStateChanged(false);
                }
                // Reconnect after 5 seconds
                reconnectWebSocket();
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket Closed: " + reason);
                if (priceListener != null) {
                    priceListener.onConnectionStateChanged(false);
                }
            }
        });
    }

    private void reconnectWebSocket() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Log.e(TAG, "Reconnect interrupted", e);
        }
        connectWebSocket();
    }

    public void setPriceListener(PriceListener listener) {
        this.priceListener = listener;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (webSocket != null) {
            webSocket.close(1000, "Service destroyed");
        }
        okHttpClient.dispatcher().cancelAll();
    }
}
