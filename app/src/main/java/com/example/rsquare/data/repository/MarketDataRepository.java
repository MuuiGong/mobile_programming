package com.example.rsquare.data.repository;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.rsquare.data.remote.CoinGeckoApiService;
import com.example.rsquare.data.remote.NetworkModule;
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
public class MarketDataRepository {
    
    private static final String TAG = "MarketDataRepository";
    private final CoinGeckoApiService apiService;
    
    // 간단한 메모리 캐시
    private final Map<String, CoinPrice> priceCache = new HashMap<>();
    private long lastPriceUpdate = 0;
    private static final long CACHE_DURATION = 30 * 1000; // 30초
    
    public MarketDataRepository() {
        this.apiService = NetworkModule.getApiService();
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
     * 차트 데이터 조회
     * 
     * @param coinId 코인 ID (예: "bitcoin")
     * @param days 조회 기간 (일)
     * @param listener 콜백
     */
    public void getMarketChart(String coinId, int days, OnChartDataLoadedListener listener) {
        Call<CoinMarketChart> call = apiService.getMarketChart(coinId, "usd", days);
        
        call.enqueue(new Callback<CoinMarketChart>() {
            @Override
            public void onResponse(Call<CoinMarketChart> call, Response<CoinMarketChart> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (listener != null) {
                        listener.onChartDataLoaded(response.body());
                    }
                } else {
                    if (listener != null) {
                        listener.onError("API 응답 오류: " + response.code());
                    }
                }
            }
            
            @Override
            public void onFailure(Call<CoinMarketChart> call, Throwable t) {
                Log.e(TAG, "Chart data fetch failed", t);
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
}

