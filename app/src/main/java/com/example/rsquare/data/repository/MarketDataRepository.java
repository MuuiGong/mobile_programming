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
import java.util.Set;
import java.util.HashSet;

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
    
    // 유효한 Binance 심볼 캐시
    private static Set<String> validBinanceSymbols = null;
    
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
    
    /**
     * Binance 심볼을 CoinGecko ID로 변환
     */
    public String getCoinIdFromSymbol(String binanceSymbol) {
        String symbol = binanceSymbol.toLowerCase().replace("usdt", "");
        for (java.util.Map.Entry<String, String> entry : SYMBOL_MAP.entrySet()) {
            if (entry.getValue().replace("usdt", "").equals(symbol)) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    // 간단한 메모리 캐시
    private final Map<String, CoinPrice> priceCache = new HashMap<>();
    private long lastPriceUpdate = 0;
    private static final long CACHE_DURATION = 30 * 1000; // 30초
    
    // 실시간 가격 업데이트 리스너
    private OnPriceUpdateListener priceUpdateListener;
    private OnRealtimeUpdateListener realtimeUpdateListener;
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
     * 실시간 업데이트 구독 (자산 목록용)
     * @param symbols 심볼 목록 (예: "BTCUSDT", "ETHUSDT")
     * @param listener 리스너
     */
    public void subscribeToRealtimeUpdates(List<String> symbols, OnRealtimeUpdateListener listener) {
        this.realtimeUpdateListener = listener;
        if (webSocketClient != null) {
            // 기존 연결이 있으면 끊고 다시 연결 (새로운 심볼 추가를 위해)
            // 실제로는 구독 추가 기능이 있으면 좋겠지만, 현재는 재연결로 처리
            startWebSocket(symbols);
        }
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
     * 실시간 가격 업데이트 리스너 설정 (기존)
     */
    public void setPriceUpdateListener(OnPriceUpdateListener listener) {
        this.priceUpdateListener = listener;
    }

    /**
     * 실시간 업데이트 리스너 설정 (가격 + 변동률)
     */
    public void setRealtimeUpdateListener(OnRealtimeUpdateListener listener) {
        this.realtimeUpdateListener = listener;
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
    public void onPriceUpdate(String coinId, double price, double changePercent) {
        // 캐시 업데이트
        CoinPrice cachedPrice = priceCache.get(coinId);
        if (cachedPrice != null) {
            // 가격만 업데이트
            cachedPrice.setCurrentPrice(price);
            cachedPrice.setPriceChangePercentage24h(changePercent); // CoinPrice에 필드가 있다고 가정하거나, 없으면 무시
            priceCache.put(coinId, cachedPrice);
        } else {
            // 새로 생성
            CoinPrice newPrice = new CoinPrice();
            newPrice.setId(coinId);
            newPrice.setCurrentPrice(price);
            newPrice.setPriceChangePercentage24h(changePercent);
            priceCache.put(coinId, newPrice);
        }
        
        lastPriceUpdate = System.currentTimeMillis();
        
        // 기존 리스너에 알림 (가격만)
        if (priceUpdateListener != null) {
            priceUpdateListener.onPriceUpdate(coinId, price);
        }
        
        // 새 리스너에 알림 (가격 + 변동률)
        if (realtimeUpdateListener != null) {
            realtimeUpdateListener.onRealtimeUpdate(coinId, price, changePercent);
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
     * 시가총액 상위 코인 조회
     * 
     * @param limit 조회할 개수
     * @param listener 콜백
     */
    public void getTopCoins(int limit, OnMarketDataLoadedListener listener) {
        Call<List<CoinPrice>> call = apiService.getCoinsMarket(
            "usd",
            null, // ids = null이면 전체 조회
            "market_cap_desc",
            limit,
            1,
            false
        );
        
        call.enqueue(new Callback<List<CoinPrice>>() {
            @Override
            public void onResponse(Call<List<CoinPrice>> call, Response<List<CoinPrice>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<CoinPrice> prices = response.body();
                    
                    // 캐시 업데이트
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
                Log.e(TAG, "Top coins fetch failed", t);
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
        // Binance 심볼로 변환 시도
        String binanceSymbol = SYMBOL_MAP.get(coinId.toLowerCase());
        
        // 매핑에 없으면 입력값을 그대로 심볼로 사용 (예: "BTCUSDT")
        if (binanceSymbol == null) {
            binanceSymbol = coinId.toUpperCase();
            if (binanceSymbol.isEmpty()) {
                if (listener != null) {
                    listener.onError("유효하지 않은 코인 심볼: " + coinId);
                }
                return;
            }
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
        getBinanceKlines(coinId, null, days, listener);
    }
    
    /**
     * Binance klines 데이터를 직접 반환 (OHLC 형식, 시간 프레임 지정 가능)
     * 
     * @param coinId 코인 ID (예: "bitcoin")
     * @param interval 시간 프레임 (예: "1m", "5m", "1h", "4h", "1d") - null이면 days에 따라 자동 결정
     * @param days 조회 기간 (일)
     * @param listener 콜백
     */
    public void getBinanceKlines(String coinId, String interval, int days, OnBinanceKlinesLoadedListener listener) {
        // Binance 심볼로 변환 시도
        String binanceSymbol = SYMBOL_MAP.get(coinId.toLowerCase());
        
        // 매핑에 없으면 입력값을 그대로 심볼로 사용 (예: "BTCUSDT")
        if (binanceSymbol == null) {
            // 대문자로 변환하여 사용
            binanceSymbol = coinId.toUpperCase();
            
            // USDT가 없으면 추가 (대부분의 경우 USDT 페어이므로)
            if (!binanceSymbol.endsWith("USDT")) {
                binanceSymbol += "USDT";
            }
            
            // 기본적인 유효성 검사
            if (binanceSymbol.isEmpty()) {
                if (listener != null) {
                    listener.onError("유효하지 않은 코인 심볼: " + coinId);
                }
                return;
            }
        }
        
        // 간격 결정 (interval이 지정되지 않으면 days에 따라 자동 결정)
        if (interval == null || interval.isEmpty()) {
            if (days <= 1) {
                interval = "5m";
            } else if (days <= 7) {
                interval = "1h";
            } else if (days <= 30) {
                interval = "4h";
            } else {
                interval = "1d";
            }
        }
        
        // limit 계산 (interval에 따라)
        int limit = calculateLimit(interval, days);
        
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
    /**
     * 유효한 Binance 심볼 목록 조회 (USDT 페어만)
     */
    public void getValidBinanceSymbols(OnValidSymbolsLoadedListener listener) {
        // 캐시가 있으면 바로 반환
        if (validBinanceSymbols != null && !validBinanceSymbols.isEmpty()) {
            if (listener != null) {
                listener.onValidSymbolsLoaded(validBinanceSymbols);
            }
            return;
        }
        
        Call<com.example.rsquare.data.remote.model.ExchangeInfoResponse> call = binanceApiService.getExchangeInfo();
        call.enqueue(new Callback<com.example.rsquare.data.remote.model.ExchangeInfoResponse>() {
            @Override
            public void onResponse(Call<com.example.rsquare.data.remote.model.ExchangeInfoResponse> call, Response<com.example.rsquare.data.remote.model.ExchangeInfoResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Set<String> symbols = new HashSet<>();
                    List<com.example.rsquare.data.remote.model.SymbolInfo> symbolInfos = response.body().getSymbols();
                    
                    if (symbolInfos != null) {
                        for (com.example.rsquare.data.remote.model.SymbolInfo info : symbolInfos) {
                            // 거래 가능하고 USDT로 끝나는 심볼만 추가
                            if ("TRADING".equals(info.getStatus()) && info.getSymbol().endsWith("USDT")) {
                                symbols.add(info.getSymbol());
                            }
                        }
                    }
                    
                    validBinanceSymbols = symbols;
                    Log.d(TAG, "Loaded " + symbols.size() + " valid Binance symbols");
                    
                    if (listener != null) {
                        listener.onValidSymbolsLoaded(symbols);
                    }
                } else {
                    if (listener != null) {
                        listener.onError("API 응답 오류: " + response.code());
                    }
                }
            }
            
            @Override
            public void onFailure(Call<com.example.rsquare.data.remote.model.ExchangeInfoResponse> call, Throwable t) {
                Log.e(TAG, "Exchange info fetch failed", t);
                if (listener != null) {
                    listener.onError("네트워크 오류: " + t.getMessage());
                }
            }
        });
    }

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
    
    public interface OnValidSymbolsLoadedListener {
        void onValidSymbolsLoaded(Set<String> symbols);
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
     * 실시간 업데이트 리스너 (가격 + 변동률)
     */
    public interface OnRealtimeUpdateListener {
        void onRealtimeUpdate(String coinId, double price, double changePercent);
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
     * 시간 프레임과 일수에 따라 limit 계산
     */
    private int calculateLimit(String interval, int days) {
        // interval에 따라 분당, 시간당, 일당 캔들 수 계산
        int candlesPerDay = 1;
        
        switch (interval) {
            case "1m":
                candlesPerDay = 24 * 60; // 1440
                break;
            case "5m":
                candlesPerDay = 24 * 12; // 288
                break;
            case "15m":
                candlesPerDay = 24 * 4; // 96
                break;
            case "30m":
                candlesPerDay = 24 * 2; // 48
                break;
            case "1h":
                candlesPerDay = 24; // 24
                break;
            case "4h":
                candlesPerDay = 6; // 6
                break;
            case "1d":
                candlesPerDay = 1; // 1
                break;
            default:
                candlesPerDay = 24; // 기본값: 1시간
        }
        
        int limit = days * candlesPerDay;
        
        // 최대 1000개로 제한 (Binance API 제한)
        if (limit > 1000) {
            limit = 1000;
        }
        
        // 최소 100개
        if (limit < 100) {
            limit = 100;
        }
        
        return limit;
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

