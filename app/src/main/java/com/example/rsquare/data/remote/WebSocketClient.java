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
    private List<PriceUpdateListener> listeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    private List<KlineUpdateListener> klineListeners = new java.util.concurrent.CopyOnWriteArrayList<>();
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
    
    private android.os.Handler reconnectHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private long reconnectDelay = 1000; // Initial delay 1 second
    private static final long MAX_RECONNECT_DELAY = 30000; // Max delay 30 seconds
    private List<String> lastCoinIds;
    private String lastKlineInterval;
    private boolean isConnecting = false;

    public WebSocketClient() {
        client = new OkHttpClient.Builder()
            .pingInterval(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();
    }
    
    /**
     * WebSocket 연결 시작 (ticker만)
     * @param coinIds CoinGecko 코인 ID 목록 또는 Binance 심볼 목록
     */
    public void connect(List<String> coinIds) {
        connect(coinIds, null);
    }
    
    /**
     * WebSocket 연결 시작 (ticker + kline)
     * @param coinIds CoinGecko 코인 ID 목록 또는 Binance 심볼 목록
     * @param klineInterval kline 간격 (예: "1m", "5m", "1h") - null이면 ticker만 구독
     */
    public void connect(List<String> coinIds, String klineInterval) {
        if (isConnected || isConnecting) {
            Log.w(TAG, "WebSocket already connected or connecting");
            return;
        }
        
        this.lastCoinIds = coinIds;
        this.lastKlineInterval = klineInterval;
        this.isConnecting = true;
        
        // Binance 심볼로 변환
        List<String> binanceSymbols = new ArrayList<>();
        for (String coinId : coinIds) {
            String symbol = SYMBOL_MAP.get(coinId.toLowerCase());
            if (symbol == null) {
                // 매핑에 없으면 입력값을 그대로 사용하되, USDT가 없으면 추가
                symbol = coinId.toLowerCase();
                if (!symbol.endsWith("usdt")) {
                    symbol += "usdt";
                }
            }
            
            if (symbol != null && !symbol.isEmpty()) {
                binanceSymbols.add(symbol + "@ticker");
                // kline 스트림도 추가
                if (klineInterval != null && !klineInterval.isEmpty()) {
                    binanceSymbols.add(symbol + "@kline_" + klineInterval);
                }
            }
        }
        
        if (binanceSymbols.isEmpty()) {
            Log.w(TAG, "No valid symbols to subscribe");
            isConnecting = false;
            return;
        }
        
        Log.d(TAG, "Subscribing to streams: " + binanceSymbols);
        
        try {
            // Binance WebSocket: 여러 스트림을 하나의 연결로 구독
            if (binanceSymbols.size() == 1) {
                connectToStream(binanceSymbols.get(0));
            } else {
                String streams = String.join("/", binanceSymbols);
                String wsUrl = "wss://stream.binance.com:9443/stream?streams=" + streams;
                Log.d(TAG, "Connecting to: " + wsUrl);
                connectToCombinedStream(wsUrl);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initiating connection", e);
            isConnecting = false;
            scheduleReconnect();
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
                isConnecting = false;
                reconnectDelay = 1000; // Reset delay on successful connection
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
                isConnecting = false;
                notifyConnectionStatus(false);
                scheduleReconnect();
            }
            
            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e(TAG, "WebSocket failure", t);
                isConnected = false;
                isConnecting = false;
                notifyConnectionStatus(false);
                scheduleReconnect();
            }
        };
    }
    
    private void scheduleReconnect() {
        if (lastCoinIds == null || isConnected || isConnecting) return;
        
        Log.d(TAG, "Scheduling reconnect in " + reconnectDelay + "ms");
        reconnectHandler.postDelayed(() -> {
            if (!isConnected && !isConnecting) {
                Log.d(TAG, "Attempting reconnect...");
                connect(lastCoinIds, lastKlineInterval);
            }
        }, reconnectDelay);
        
        // Exponential backoff
        reconnectDelay = Math.min(reconnectDelay * 2, MAX_RECONNECT_DELAY);
    }
    
    /**
     * 메시지 처리
     */
    private void handleMessage(String message) {
        try {
            // ... (existing message handling logic) ...
            // To save tokens, I'm not repeating the entire handleMessage body here unless necessary.
            // But since I'm replacing the whole block, I MUST include it.
            
            // 디버깅: kline 메시지인 경우 로그 출력
            if (message.contains("kline") || message.contains("@kline")) {
                // Log.d(TAG, "Received kline message: " + message.substring(0, Math.min(200, message.length())));
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
                    double changePercent = data.optDouble("P", 0.0); // 24시간 변동률
                    
                    String coinId = getCoinIdFromSymbol(symbol);
                    if (coinId == null) coinId = symbol;
                    
                    notifyPriceUpdate(coinId, price, changePercent);
                }
                // kline 스트림인지 확인
                else if (stream.contains("@kline")) {
                    if (data.has("k")) {
                        JSONObject kline = data.getJSONObject("k");
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
                            if (coinId == null) coinId = symbol;
                            
                            notifyKlineUpdate(coinId, openTime, open, high, low, close, volume);
                        }
                    }
                }
            } 
            // 단일 스트림 형식
            else if (json.has("e") && "24hrTicker".equals(json.getString("e"))) {
                String symbol = json.getString("s");
                double price = json.getDouble("c");
                double changePercent = json.optDouble("P", 0.0);
                
                String coinId = getCoinIdFromSymbol(symbol);
                if (coinId == null) coinId = symbol;
                
                notifyPriceUpdate(coinId, price, changePercent);
            }
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
                        if (coinId == null) coinId = symbol;
                        
                        notifyKlineUpdate(coinId, openTime, open, high, low, close, volume);
                    }
                }
            }
        } catch (Exception e) {
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
        // 매핑에 없으면 심볼 그대로 반환 (예: "leo")
        return symbol;
    }
    
    /**
     * 연결 해제
     */
    public void disconnect() {
        // 재연결 예약 취소
        reconnectHandler.removeCallbacksAndMessages(null);
        lastCoinIds = null; // 재연결 방지
        
        if (webSocket != null) {
            try {
                webSocket.close(1000, "Normal closure");
            } catch (Exception e) {
                Log.e(TAG, "Error closing WebSocket", e);
            }
            webSocket = null;
        }
        isConnected = false;
        isConnecting = false;
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
    private void notifyPriceUpdate(String coinId, double price, double changePercent) {
        for (PriceUpdateListener listener : listeners) {
            listener.onPriceUpdate(coinId, price, changePercent);
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
        void onPriceUpdate(String coinId, double price, double changePercent);
        void onConnectionStatusChanged(boolean connected);
    }
    
    /**
     * Kline 업데이트 리스너 인터페이스
     */
    public interface KlineUpdateListener {
        void onKlineUpdate(String coinId, long openTime, double open, double high, double low, double close, double volume);
    }
}

