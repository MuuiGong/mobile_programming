package com.example.rsquare.data.repository;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.rsquare.data.remote.BinanceApiService;
import com.example.rsquare.data.remote.CoinGeckoApiService;
import com.example.rsquare.data.remote.NetworkModule;
import com.example.rsquare.data.remote.WebSocketClient;
import com.example.rsquare.data.remote.model.CoinListItem;
import com.example.rsquare.data.remote.model.CoinMarketChart;
import com.example.rsquare.data.remote.model.CoinPrice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Market Data Repository
 * CoinGecko API를 통한 시장 데이터 조회 및 캐싱
 */
public class MarketDataRepository implements WebSocketClient.PriceUpdateListener {
    
    private static final String TAG = "MarketDataRepository";
    private final CoinGeckoApiService apiService;
    private final BinanceApiService binanceApiService;
    private WebSocketClient webSocketClient;
    
    // Binance 심볼 매핑 (CoinGecko ID -> Binance Symbol)
    private static final java.util.Map<String, String> SYMBOL_MAP = new java.util.HashMap<String, String>() {{
        put("bitcoin", "BTCUSDT");
        put("ethereum", "ETHUSDT");
        put("cardano", "ADAUSDT");
        put("solana", "SOLUSDT");
        put("ripple", "XRPUSDT");
        put("polkadot", "DOTUSDT");
        put("dogecoin", "DOGEUSDT");
        put("avalanche-2", "AVAXUSDT");
    }};
    
    // 간단한 메모리 캐시
    private final Map<String, CoinPrice> priceCache = new HashMap<>();
    private long lastPriceUpdate = 0;
    private static final long CACHE_DURATION = 30 * 1000; // 30초
    
    // 실시간 가격 업데이트 리스너
    private OnPriceUpdateListener priceUpdateListener;
    private OnKlineUpdateListener klineUpdateListener;
    
    public MarketDataRepository() {
        this.apiService = NetworkModule.getApiService();
        this.binanceApiService = NetworkModule.getBinanceApiService();
        this.webSocketClient = new WebSocketClient();
        this.webSocketClient.addPriceUpdateListener(this);
    }
    
    /**
     * 웹소켓 연결 시작 (실시간 가격 업데이트)
     */
    public void startWebSocket(List<String> coinIds) {
        startWebSocket(coinIds, null);
    }
    
    /**
     * 웹소켓 연결 시작 (실시간 가격 + 캔들스틱 업데이트)
     * @param coinIds 코인 ID 목록
     * @param klineInterval kline 간격 (예: "1m", "5m", "1h") - null이면 ticker만
     */
    public void startWebSocket(List<String> coinIds, String klineInterval) {
        if (webSocketClient == null) {
            Log.e(TAG, "WebSocketClient is null");
            return;
        }
        
        if (webSocketClient.isConnected()) {
            Log.d(TAG, "WebSocket already connected, reconnecting...");
            webSocketClient.disconnect();
        }
        
        Log.d(TAG, "Starting WebSocket connection for coins: " + coinIds + ", interval: " + klineInterval);
        webSocketClient.connect(coinIds, klineInterval);
    }
    
    /**
     * 웹소켓 연결 해제
     */
    public void stopWebSocket() {
        if (webSocketClient != null) {
            webSocketClient.disconnect();
        }
    }
    
    /**
     * 실시간 가격 업데이트 리스너 설정
     */
    public void setPriceUpdateListener(OnPriceUpdateListener listener) {
        this.priceUpdateListener = listener;
    }
    
    /**
     * 실시간 Kline 업데이트 리스너 설정
     */
    public void setKlineUpdateListener(OnKlineUpdateListener listener) {
        this.klineUpdateListener = listener;
        if (listener != null && webSocketClient != null) {
            webSocketClient.addKlineUpdateListener(new WebSocketClient.KlineUpdateListener() {
                @Override
                public void onKlineUpdate(String coinId, long openTime, double open, double high, double low, double close, double volume) {
                    if (klineUpdateListener != null) {
                        klineUpdateListener.onKlineUpdate(coinId, openTime, open, high, low, close, volume);
                    }
                }
            });
        }
    }
    
    @Override
    public void onPriceUpdate(String coinId, double price) {
        // 캐시 업데이트
        CoinPrice cachedPrice = priceCache.get(coinId);
        if (cachedPrice != null) {
            // 가격만 업데이트
            cachedPrice.setCurrentPrice(price);
            priceCache.put(coinId, cachedPrice);
        } else {
            // 새로 생성
            CoinPrice newPrice = new CoinPrice();
            newPrice.setId(coinId);
            newPrice.setCurrentPrice(price);
            priceCache.put(coinId, newPrice);
        }
        
        lastPriceUpdate = System.currentTimeMillis();
        
        // 리스너에 알림
        if (priceUpdateListener != null) {
            priceUpdateListener.onPriceUpdate(coinId, price);
        }
    }
    
    @Override
    public void onConnectionStatusChanged(boolean connected) {
        Log.d(TAG, "WebSocket connection status: " + connected);
        if (priceUpdateListener != null) {
            priceUpdateListener.onConnectionStatusChanged(connected);
        }
    }
    
    /**
     * 코인 시장 데이터 조회
     * 
     * @param coinIds 코인 ID 목록 (쉼표로 구분, 예: "bitcoin,ethereum")
     * @param listener 콜백
     */
    public void getCoinsMarket(String coinIds, OnMarketDataLoadedListener listener) {
        // 캐시 확인
        if (isCacheValid() && !priceCache.isEmpty()) {
            if (listener != null) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    listener.onMarketDataLoaded(new java.util.ArrayList<>(priceCache.values()));
                });
            }
            return;
        }
        
        Call<List<CoinPrice>> call = apiService.getCoinsMarket(
            "usd",
            coinIds,
            "market_cap_desc",
            50,
            1,
            false
        );
        
        call.enqueue(new Callback<List<CoinPrice>>() {
            @Override
            public void onResponse(Call<List<CoinPrice>> call, Response<List<CoinPrice>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<CoinPrice> prices = response.body();
                    
                    // 캐시 업데이트
                    priceCache.clear();
                    for (CoinPrice price : prices) {
                        priceCache.put(price.getId(), price);
                    }
                    lastPriceUpdate = System.currentTimeMillis();
                    
                    if (listener != null) {
                        listener.onMarketDataLoaded(prices);
                    }
                } else {
                    if (listener != null) {
                        listener.onError("API 응답 오류: " + response.code());
                    }
                }
            }
            
            @Override
            public void onFailure(Call<List<CoinPrice>> call, Throwable t) {
                Log.e(TAG, "Market data fetch failed", t);
                if (listener != null) {
                    listener.onError("네트워크 오류: " + t.getMessage());
                }
            }
        });
    }
    
    /**
     * 차트 데이터 조회 (Binance API 사용)
     * 
     * @param coinId 코인 ID (예: "bitcoin")
     * @param days 조회 기간 (일)
     * @param listener 콜백
     */
    public void getMarketChart(String coinId, int days, OnChartDataLoadedListener listener) {
        // Binance 심볼로 변환
        String binanceSymbol = SYMBOL_MAP.get(coinId.toLowerCase());
        if (binanceSymbol == null) {
            if (listener != null) {
                listener.onError("지원하지 않는 코인: " + coinId);
            }
            return;
        }
        
        // 간격 결정 (days에 따라)
        String interval = "1h"; // 기본값
        int limit = 500; // 기본값
        
        if (days <= 1) {
            interval = "5m";
            limit = 288; // 24시간 / 5분 = 288개
        } else if (days <= 7) {
            interval = "1h";
            limit = days * 24; // 시간당 1개
        } else if (days <= 30) {
            interval = "4h";
            limit = days * 6; // 4시간당 1개
        } else {
            interval = "1d";
            limit = days;
        }
        
        // 시작 시간 계산 (현재 시간 - days일)
        long endTime = System.currentTimeMillis();
        long startTime = endTime - (days * 24L * 60 * 60 * 1000);
        
        Log.d(TAG, "Fetching Binance klines: " + binanceSymbol + " interval=" + interval + " limit=" + limit);
        
        Call<List<List<Object>>> call = binanceApiService.getKlines(
            binanceSymbol,
            interval,
            limit,
            startTime,
            null // endTime은 null로 (limit만 사용)
        );
        
        call.enqueue(new Callback<List<List<Object>>>() {
            @Override
            public void onResponse(Call<List<List<Object>>> call, Response<List<List<Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        // Binance klines 데이터를 CoinMarketChart 형식으로 변환
                        CoinMarketChart chartData = convertBinanceKlinesToChartData(response.body());
                        if (listener != null) {
                            listener.onChartDataLoaded(chartData);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error converting Binance klines", e);
                        if (listener != null) {
                            listener.onError("데이터 변환 오류: " + e.getMessage());
                        }
                    }
                } else {
                    if (listener != null) {
                        listener.onError("API 응답 오류: " + response.code());
                    }
                }
            }
            
            @Override
            public void onFailure(Call<List<List<Object>>> call, Throwable t) {
                Log.e(TAG, "Binance klines fetch failed", t);
                if (listener != null) {
                    listener.onError("네트워크 오류: " + t.getMessage());
                }
            }
        });
    }
    
    /**
     * Binance klines 데이터를 CoinMarketChart 형식으로 변환
     * Binance 형식: [[openTime, open, high, low, close, volume, ...], ...]
     * CoinMarketChart 형식: {prices: [[timestamp, price], ...]}
     * 
     * 실제 OHLC 데이터를 활용하기 위해 각 캔들의 open, high, low, close를 모두 포함한 데이터 포인트 생성
     * chart.html의 convertToOHLC 함수가 이를 처리할 수 있도록 여러 가격 포인트를 생성
     * 각 캔들마다 open, high, low, close를 시간 순서대로 배치하여 실제 변동을 반영
     */
    private CoinMarketChart convertBinanceKlinesToChartData(List<List<Object>> klines) {
        CoinMarketChart chartData = new CoinMarketChart();
        java.util.List<java.util.List<Double>> prices = new java.util.ArrayList<>();
        
        for (List<Object> kline : klines) {
            if (kline.size() >= 6) {
                // Binance kline 형식: [openTime, open, high, low, close, volume, ...]
                // Binance API는 숫자를 문자열로 반환할 수 있으므로 안전하게 변환
                long openTime = parseLong(kline.get(0));
                double open = parseDouble(kline.get(1));
                double high = parseDouble(kline.get(2));
                double low = parseDouble(kline.get(3));
                double close = parseDouble(kline.get(4));
                
                // 실제 OHLC 데이터를 활용하기 위해 각 가격을 포함한 데이터 포인트 생성
                // 각 캔들의 시간 범위 내에 open, high, low, close를 배치
                // 캔들 간격에 따라 시간 분배 (예: 1시간 캔들이면 15분씩)
                long candleDuration = 60 * 60 * 1000; // 기본 1시간
                long quarterDuration = candleDuration / 4;
                
                // Open (시작 시간)
                java.util.List<Double> openPoint = new java.util.ArrayList<>();
                openPoint.add((double) openTime);
                openPoint.add(open);
                prices.add(openPoint);
                
                // High (1/4 지점)
                java.util.List<Double> highPoint = new java.util.ArrayList<>();
                highPoint.add((double) (openTime + quarterDuration));
                highPoint.add(high);
                prices.add(highPoint);
                
                // Low (2/4 지점)
                java.util.List<Double> lowPoint = new java.util.ArrayList<>();
                lowPoint.add((double) (openTime + quarterDuration * 2));
                lowPoint.add(low);
                prices.add(lowPoint);
                
                // Close (끝 시간)
                java.util.List<Double> closePoint = new java.util.ArrayList<>();
                closePoint.add((double) (openTime + candleDuration - 1000)); // 캔들 종료 직전
                closePoint.add(close);
                prices.add(closePoint);
            }
        }
        
        Log.d(TAG, "Converted " + klines.size() + " klines to " + prices.size() + " price points");
        chartData.setPrices(prices);
        return chartData;
    }
    
    /**
     * Binance klines 데이터를 직접 반환 (OHLC 형식)
     * 
     * @param coinId 코인 ID (예: "bitcoin")
     * @param days 조회 기간 (일)
     * @param listener 콜백
     */
    public void getBinanceKlines(String coinId, int days, OnBinanceKlinesLoadedListener listener) {
        // Binance 심볼로 변환
        String binanceSymbol = SYMBOL_MAP.get(coinId.toLowerCase());
        if (binanceSymbol == null) {
            if (listener != null) {
                listener.onError("지원하지 않는 코인: " + coinId);
            }
            return;
        }
        
        // 간격 결정 (days에 따라)
        String interval = "1h"; // 기본값
        int limit = 500; // 기본값
        
        if (days <= 1) {
            interval = "5m";
            limit = 288; // 24시간 / 5분 = 288개
        } else if (days <= 7) {
            interval = "1h";
            limit = days * 24; // 시간당 1개
        } else if (days <= 30) {
            interval = "4h";
            limit = days * 6; // 4시간당 1개
        } else {
            interval = "1d";
            limit = days;
        }
        
        Log.d(TAG, "Fetching Binance klines directly: " + binanceSymbol + " interval=" + interval + " limit=" + limit);
        
        Call<List<List<Object>>> call = binanceApiService.getKlines(
            binanceSymbol,
            interval,
            limit,
            null,
            null
        );
        
        call.enqueue(new Callback<List<List<Object>>>() {
            @Override
            public void onResponse(Call<List<List<Object>>> call, Response<List<List<Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (listener != null) {
                        listener.onBinanceKlinesLoaded(response.body());
                    }
                } else {
                    if (listener != null) {
                        listener.onError("API 응답 오류: " + response.code());
                    }
                }
            }
            
            @Override
            public void onFailure(Call<List<List<Object>>> call, Throwable t) {
                Log.e(TAG, "Binance klines fetch failed", t);
                if (listener != null) {
                    listener.onError("네트워크 오류: " + t.getMessage());
                }
            }
        });
    }
    
    /**
     * 코인 목록 조회
     * 
     * @param listener 콜백
     */
    public void getCoinList(OnCoinListLoadedListener listener) {
        Call<List<CoinListItem>> call = apiService.getCoinList();
        
        call.enqueue(new Callback<List<CoinListItem>>() {
            @Override
            public void onResponse(Call<List<CoinListItem>> call, Response<List<CoinListItem>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (listener != null) {
                        listener.onCoinListLoaded(response.body());
                    }
                } else {
                    if (listener != null) {
                        listener.onError("API 응답 오류: " + response.code());
                    }
                }
            }
            
            @Override
            public void onFailure(Call<List<CoinListItem>> call, Throwable t) {
                Log.e(TAG, "Coin list fetch failed", t);
                if (listener != null) {
                    listener.onError("네트워크 오류: " + t.getMessage());
                }
            }
        });
    }
    
    /**
     * 캐시에서 가격 조회
     */
    public CoinPrice getCachedPrice(String coinId) {
        return priceCache.get(coinId);
    }
    
    /**
     * 캐시 유효성 확인
     */
    private boolean isCacheValid() {
        return (System.currentTimeMillis() - lastPriceUpdate) < CACHE_DURATION;
    }
    
    /**
     * 캐시 초기화
     */
    public void clearCache() {
        priceCache.clear();
        lastPriceUpdate = 0;
    }
    
    public interface OnMarketDataLoadedListener {
        void onMarketDataLoaded(List<CoinPrice> prices);
        void onError(String error);
    }
    
    public interface OnChartDataLoadedListener {
        void onChartDataLoaded(CoinMarketChart chartData);
        void onError(String error);
    }
    
    public interface OnCoinListLoadedListener {
        void onCoinListLoaded(List<CoinListItem> coins);
        void onError(String error);
    }
    
    /**
     * 실시간 가격 업데이트 리스너
     */
    public interface OnPriceUpdateListener {
        void onPriceUpdate(String coinId, double price);
        void onConnectionStatusChanged(boolean connected);
    }
    
    /**
     * 실시간 Kline 업데이트 리스너
     */
    public interface OnKlineUpdateListener {
        void onKlineUpdate(String coinId, long openTime, double open, double high, double low, double close, double volume);
    }
    
    /**
     * Binance Klines 데이터 로드 리스너
     */
    public interface OnBinanceKlinesLoadedListener {
        void onBinanceKlinesLoaded(List<List<Object>> klines);
        void onError(String error);
    }
    
    /**
     * 안전한 숫자 파싱 헬퍼 메서드
     */
    private double parseDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }
    
    /**
     * 안전한 long 파싱 헬퍼 메서드
     */
    private long parseLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
        return 0L;
    }
}

