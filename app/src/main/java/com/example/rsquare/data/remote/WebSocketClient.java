package com.example.rsquare.data.remote;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * WebSocket Client for Real-time Price Updates
 * Binance WebSocket을 사용하여 실시간 가격 업데이트
 */
public class WebSocketClient {
    
    private static final String TAG = "WebSocketClient";
    private static final String BINANCE_WS_URL = "wss://stream.binance.com:9443/ws/";
    
    private OkHttpClient client;
    private WebSocket webSocket;
    private List<PriceUpdateListener> listeners = new ArrayList<>();
    private List<KlineUpdateListener> klineListeners = new ArrayList<>();
    private boolean isConnected = false;
    
    // Binance 심볼 매핑 (CoinGecko ID -> Binance Symbol)
    private static final java.util.Map<String, String> SYMBOL_MAP = new java.util.HashMap<String, String>() {{
        put("bitcoin", "btcusdt");
        put("ethereum", "ethusdt");
        put("cardano", "adausdt");
        put("solana", "solusdt");
        put("ripple", "xrpusdt");
        put("polkadot", "dotusdt");
        put("dogecoin", "dogeusdt");
        put("avalanche-2", "avaxusdt");
    }};
    
    public WebSocketClient() {
        client = new OkHttpClient.Builder()
            .pingInterval(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();
    }
    
    /**
     * WebSocket 연결 시작 (ticker만)
     * @param coinIds CoinGecko 코인 ID 목록
     */
    public void connect(List<String> coinIds) {
        connect(coinIds, null);
    }
    
    /**
     * WebSocket 연결 시작 (ticker + kline)
     * @param coinIds CoinGecko 코인 ID 목록
     * @param klineInterval kline 간격 (예: "1m", "5m", "1h") - null이면 ticker만 구독
     */
    public void connect(List<String> coinIds, String klineInterval) {
        if (isConnected) {
            Log.w(TAG, "WebSocket already connected");
            return;
        }
        
        // Binance 심볼로 변환
        List<String> binanceSymbols = new ArrayList<>();
        for (String coinId : coinIds) {
            String symbol = SYMBOL_MAP.get(coinId.toLowerCase());
            if (symbol != null) {
                binanceSymbols.add(symbol.toLowerCase() + "@ticker");
                // kline 스트림도 추가
                if (klineInterval != null && !klineInterval.isEmpty()) {
                    binanceSymbols.add(symbol.toLowerCase() + "@kline_" + klineInterval);
                }
            }
        }
        
        if (binanceSymbols.isEmpty()) {
            Log.w(TAG, "No valid symbols to subscribe");
            return;
        }
        
        Log.d(TAG, "Subscribing to streams: " + binanceSymbols);
        
        // Binance WebSocket: 여러 스트림을 하나의 연결로 구독
        // 형식: wss://stream.binance.com:9443/stream?streams=btcusdt@ticker/btcusdt@kline_1m
        if (binanceSymbols.size() == 1) {
            // 단일 스트림
            connectToStream(binanceSymbols.get(0));
        } else {
            // 여러 스트림 (스트림 이름을 /로 구분)
            String streams = String.join("/", binanceSymbols);
            String wsUrl = "wss://stream.binance.com:9443/stream?streams=" + streams;
            Log.d(TAG, "Connecting to: " + wsUrl);
            connectToCombinedStream(wsUrl);
        }
    }
    
    /**
     * 여러 스트림을 하나의 연결로 구독
     */
    private void connectToCombinedStream(String wsUrl) {
        Request request = new Request.Builder()
            .url(wsUrl)
            .build();
        
        webSocket = client.newWebSocket(request, createWebSocketListener());
    }
    
    /**
     * 단일 스트림에 연결
     */
    private void connectToStream(String stream) {
        String wsUrl = BINANCE_WS_URL + stream;
        
        Request request = new Request.Builder()
            .url(wsUrl)
            .build();
        
        webSocket = client.newWebSocket(request, createWebSocketListener());
    }
    
    /**
     * WebSocket 리스너 생성
     */
    private WebSocketListener createWebSocketListener() {
        return new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.d(TAG, "WebSocket connected");
                isConnected = true;
                notifyConnectionStatus(true);
            }
            
            @Override
            public void onMessage(WebSocket webSocket, String text) {
                handleMessage(text);
            }
            
            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                handleMessage(bytes.utf8());
            }
            
            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket closing: " + reason);
                isConnected = false;
                notifyConnectionStatus(false);
            }
            
            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket closed: " + reason);
                isConnected = false;
                notifyConnectionStatus(false);
            }
            
            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e(TAG, "WebSocket failure", t);
                isConnected = false;
                notifyConnectionStatus(false);
                
                // 재연결은 외부에서 처리
            }
        };
    }
    
    /**
     * 메시지 처리
     */
    private void handleMessage(String message) {
        try {
            // 디버깅: kline 메시지인 경우 로그 출력
            if (message.contains("kline") || message.contains("@kline")) {
                Log.d(TAG, "Received kline message: " + message.substring(0, Math.min(200, message.length())));
            }
            
            JSONObject json = new JSONObject(message);
            
            // Binance combined stream 형식: {"stream":"btcusdt@ticker","data":{...}}
            if (json.has("stream") && json.has("data")) {
                JSONObject data = json.getJSONObject("data");
                String stream = json.getString("stream");
                
                // ticker 스트림인지 확인
                if (stream.contains("@ticker")) {
                    String symbol = data.getString("s");
                    double price = data.getDouble("c");
                    
                    // Binance 심볼을 CoinGecko ID로 변환
                    String coinId = getCoinIdFromSymbol(symbol);
                    if (coinId != null) {
                        notifyPriceUpdate(coinId, price);
                    }
                }
                // kline 스트림인지 확인
                else if (stream.contains("@kline")) {
                    Log.d(TAG, "Received kline stream: " + stream);
                    if (data.has("k")) {
                        JSONObject kline = data.getJSONObject("k");
                        boolean isClosed = kline.getBoolean("x"); // 캔들이 닫혔는지 여부
                        
                        Log.d(TAG, "Kline isClosed: " + isClosed);
                        
                        if (isClosed) {
                            String symbol = kline.getString("s");
                            long openTime = kline.getLong("t");
                            double open = kline.getDouble("o");
                            double high = kline.getDouble("h");
                            double low = kline.getDouble("l");
                            double close = kline.getDouble("c");
                            double volume = kline.getDouble("v");
                            
                            Log.d(TAG, "Kline data: " + symbol + " " + close + " at " + openTime);
                            
                            // Binance 심볼을 CoinGecko ID로 변환
                            String coinId = getCoinIdFromSymbol(symbol);
                            if (coinId != null) {
                                Log.d(TAG, "Notifying kline update for: " + coinId);
                                notifyKlineUpdate(coinId, openTime, open, high, low, close, volume);
                            } else {
                                Log.w(TAG, "Could not convert symbol to coinId: " + symbol);
                            }
                        }
                    } else {
                        Log.w(TAG, "Kline data missing 'k' field");
                    }
                }
            } 
            // 단일 스트림 형식: {"e":"24hrTicker","s":"BTCUSDT","c":"50000.00",...}
            else if (json.has("e") && "24hrTicker".equals(json.getString("e"))) {
                String symbol = json.getString("s");
                double price = json.getDouble("c");
                
                // Binance 심볼을 CoinGecko ID로 변환
                String coinId = getCoinIdFromSymbol(symbol);
                if (coinId != null) {
                    notifyPriceUpdate(coinId, price);
                }
            }
            // 단일 kline 스트림 형식: {"e":"kline","s":"BTCUSDT","k":{...}}
            else if (json.has("e") && "kline".equals(json.getString("e"))) {
                if (json.has("k")) {
                    JSONObject kline = json.getJSONObject("k");
                    boolean isClosed = kline.getBoolean("x");
                    
                    if (isClosed) {
                        String symbol = kline.getString("s");
                        long openTime = kline.getLong("t");
                        double open = kline.getDouble("o");
                        double high = kline.getDouble("h");
                        double low = kline.getDouble("l");
                        double close = kline.getDouble("c");
                        double volume = kline.getDouble("v");
                        
                        String coinId = getCoinIdFromSymbol(symbol);
                        if (coinId != null) {
                            notifyKlineUpdate(coinId, openTime, open, high, low, close, volume);
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing WebSocket message: " + message, e);
        }
    }
    
    /**
     * Binance 심볼을 CoinGecko ID로 변환
     */
    private String getCoinIdFromSymbol(String binanceSymbol) {
        String symbol = binanceSymbol.toLowerCase().replace("usdt", "");
        for (java.util.Map.Entry<String, String> entry : SYMBOL_MAP.entrySet()) {
            if (entry.getValue().replace("usdt", "").equals(symbol)) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    /**
     * 연결 해제
     */
    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "Normal closure");
            webSocket = null;
        }
        isConnected = false;
        notifyConnectionStatus(false);
    }
    
    /**
     * 가격 업데이트 리스너 추가
     */
    public void addPriceUpdateListener(PriceUpdateListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    /**
     * 가격 업데이트 리스너 제거
     */
    public void removePriceUpdateListener(PriceUpdateListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Kline 업데이트 리스너 추가
     */
    public void addKlineUpdateListener(KlineUpdateListener listener) {
        if (!klineListeners.contains(listener)) {
            klineListeners.add(listener);
        }
    }
    
    /**
     * Kline 업데이트 리스너 제거
     */
    public void removeKlineUpdateListener(KlineUpdateListener listener) {
        klineListeners.remove(listener);
    }
    
    /**
     * 가격 업데이트 알림
     */
    private void notifyPriceUpdate(String coinId, double price) {
        for (PriceUpdateListener listener : listeners) {
            listener.onPriceUpdate(coinId, price);
        }
    }
    
    /**
     * Kline 업데이트 알림
     */
    private void notifyKlineUpdate(String coinId, long openTime, double open, double high, double low, double close, double volume) {
        for (KlineUpdateListener listener : klineListeners) {
            listener.onKlineUpdate(coinId, openTime, open, high, low, close, volume);
        }
    }
    
    /**
     * 연결 상태 알림
     */
    private void notifyConnectionStatus(boolean connected) {
        for (PriceUpdateListener listener : listeners) {
            listener.onConnectionStatusChanged(connected);
        }
    }
    
    /**
     * 연결 상태 확인
     */
    public boolean isConnected() {
        return isConnected;
    }
    
    /**
     * 가격 업데이트 리스너 인터페이스
     */
    public interface PriceUpdateListener {
        void onPriceUpdate(String coinId, double price);
        void onConnectionStatusChanged(boolean connected);
    }
    
    /**
     * Kline 업데이트 리스너 인터페이스
     */
    public interface KlineUpdateListener {
        void onKlineUpdate(String coinId, long openTime, double open, double high, double low, double close, double volume);
    }
}

