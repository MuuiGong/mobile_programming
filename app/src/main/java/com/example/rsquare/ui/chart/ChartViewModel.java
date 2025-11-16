package com.example.rsquare.ui.chart;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.rsquare.data.remote.model.CoinMarketChart;
import com.example.rsquare.data.remote.model.CoinPrice;
import com.example.rsquare.data.repository.MarketDataRepository;
import com.example.rsquare.domain.RiskCalculator;

import java.util.List;

/**
 * Chart ViewModel
 * 차트 데이터 및 실시간 가격 관리
 */
public class ChartViewModel extends AndroidViewModel {
    
    private final MarketDataRepository marketDataRepository;
    
    private final MutableLiveData<List<CoinPrice>> coinPrices = new MutableLiveData<>();
    private final MutableLiveData<CoinMarketChart> chartData = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    
    // 현재 선택된 코인 및 가격
    private final MutableLiveData<String> selectedCoinId = new MutableLiveData<>("bitcoin");
    private final MutableLiveData<Double> currentPrice = new MutableLiveData<>();
    
    // 실시간 캔들스틱 업데이트
    public static class KlineData {
        public final String coinId;
        public final long openTime;
        public final double open;
        public final double high;
        public final double low;
        public final double close;
        public final double volume;
        
        public KlineData(String coinId, long openTime, double open, double high, double low, double close, double volume) {
            this.coinId = coinId;
            this.openTime = openTime;
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
            this.volume = volume;
        }
    }
    private final MutableLiveData<KlineData> klineUpdate = new MutableLiveData<>();
    
    // Binance OHLC 데이터 (직접 사용)
    private final MutableLiveData<List<List<Object>>> binanceKlines = new MutableLiveData<>();
    
    // 트레이딩 입력
    private final MutableLiveData<Double> entryPrice = new MutableLiveData<>();
    private final MutableLiveData<Double> takeProfit = new MutableLiveData<>();
    private final MutableLiveData<Double> stopLoss = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLong = new MutableLiveData<>(true);
    
    // R:R 계산 결과
    private final MutableLiveData<Double> riskRewardRatio = new MutableLiveData<>();
    
    public ChartViewModel(@NonNull Application application) {
        super(application);
        
        marketDataRepository = new MarketDataRepository();
        
        // 웹소켓 리스너 설정 (실시간 가격 업데이트)
        marketDataRepository.setPriceUpdateListener(new MarketDataRepository.OnPriceUpdateListener() {
            @Override
            public void onPriceUpdate(String coinId, double price) {
                // 선택된 코인의 가격이 업데이트되면 LiveData 업데이트
                String selectedId = selectedCoinId.getValue();
                if (selectedId != null && selectedId.equals(coinId)) {
                    currentPrice.postValue(price);
                }
            }
            
            @Override
            public void onConnectionStatusChanged(boolean connected) {
                android.util.Log.d("ChartViewModel", "WebSocket connected: " + connected);
            }
        });
        
        // 웹소켓 Kline 리스너 설정 (실시간 캔들스틱 업데이트)
        marketDataRepository.setKlineUpdateListener(new MarketDataRepository.OnKlineUpdateListener() {
            @Override
            public void onKlineUpdate(String coinId, long openTime, double open, double high, double low, double close, double volume) {
                // 선택된 코인의 캔들이 업데이트되면 차트에 반영
                String selectedId = selectedCoinId.getValue();
                if (selectedId != null && selectedId.equals(coinId)) {
                    android.util.Log.d("ChartViewModel", "Kline update: " + coinId + " " + close);
                    klineUpdate.postValue(new KlineData(coinId, openTime, open, high, low, close, volume));
                }
            }
        });
        
        // 초기 데이터 로드
        loadMarketData();
        loadChartData("bitcoin", 7);
    }
    
    /**
     * 시장 데이터 로드
     */
    public void loadMarketData() {
        loading.setValue(true);
        
        // 주요 코인들 조회
        marketDataRepository.getCoinsMarket(
            "bitcoin,ethereum,cardano,solana,ripple,polkadot,dogecoin,avalanche-2",
            new MarketDataRepository.OnMarketDataLoadedListener() {
                @Override
                public void onMarketDataLoaded(List<CoinPrice> prices) {
                    loading.postValue(false);
                    coinPrices.postValue(prices);
                    
                    // 선택된 코인의 현재 가격 업데이트
                    String selectedId = selectedCoinId.getValue();
                    if (selectedId != null) {
                        for (CoinPrice price : prices) {
                            if (price.getId().equals(selectedId)) {
                                currentPrice.postValue(price.getCurrentPrice());
                                
                                // 진입가가 설정되지 않았으면 현재 가격으로 설정
                                if (entryPrice.getValue() == null) {
                                    entryPrice.postValue(price.getCurrentPrice());
                                }
                                break;
                            }
                        }
                    }
                    
                    // 웹소켓 연결 시작 (실시간 가격 + 캔들스틱 업데이트)
                    List<String> coinIds = new java.util.ArrayList<>();
                    for (CoinPrice price : prices) {
                        coinIds.add(price.getId());
                    }
                    // 1분 캔들스틱 구독
                    marketDataRepository.startWebSocket(coinIds, "1m");
                }
                
                @Override
                public void onError(String error) {
                    loading.postValue(false);
                    errorMessage.postValue(error);
                }
            }
        );
    }
    
    /**
     * 차트 데이터 로드 (Binance OHLC 데이터 직접 사용)
     */
    public void loadChartData(String coinId, int days) {
        loading.setValue(true);
        
        // Binance klines 데이터를 직접 가져와서 OHLC 형식으로 전달
        marketDataRepository.getBinanceKlines(coinId, days,
            new MarketDataRepository.OnBinanceKlinesLoadedListener() {
                @Override
                public void onBinanceKlinesLoaded(List<List<Object>> klines) {
                    loading.postValue(false);
                    // Binance klines를 직접 저장
                    binanceKlines.postValue(klines);
                    
                    // 기존 호환성을 위해 CoinMarketChart 형식으로도 변환하여 저장
                    CoinMarketChart convertedChartData = convertBinanceKlinesToChartData(klines);
                    ChartViewModel.this.chartData.postValue(convertedChartData);
                }
                
                @Override
                public void onError(String error) {
                    loading.postValue(false);
                    errorMessage.postValue(error);
                }
            }
        );
    }
    
    /**
     * Binance klines를 CoinMarketChart로 변환 (기존 호환성 유지)
     */
    private CoinMarketChart convertBinanceKlinesToChartData(List<List<Object>> klines) {
        CoinMarketChart chartData = new CoinMarketChart();
        java.util.List<java.util.List<Double>> prices = new java.util.ArrayList<>();
        
        for (List<Object> kline : klines) {
            if (kline.size() >= 6) {
                // Binance API는 숫자를 문자열로 반환할 수 있으므로 안전하게 변환
                long openTime = parseLong(kline.get(0));
                double open = parseDouble(kline.get(1));
                double high = parseDouble(kline.get(2));
                double low = parseDouble(kline.get(3));
                double close = parseDouble(kline.get(4));
                
                // 실제 OHLC 데이터를 활용하기 위해 각 가격을 포함한 데이터 포인트 생성
                long candleDuration = 60 * 60 * 1000; // 기본 1시간
                long quarterDuration = candleDuration / 4;
                
                java.util.List<Double> openPoint = new java.util.ArrayList<>();
                openPoint.add((double) openTime);
                openPoint.add(open);
                prices.add(openPoint);
                
                java.util.List<Double> highPoint = new java.util.ArrayList<>();
                highPoint.add((double) (openTime + quarterDuration));
                highPoint.add(high);
                prices.add(highPoint);
                
                java.util.List<Double> lowPoint = new java.util.ArrayList<>();
                lowPoint.add((double) (openTime + quarterDuration * 2));
                lowPoint.add(low);
                prices.add(lowPoint);
                
                java.util.List<Double> closePoint = new java.util.ArrayList<>();
                closePoint.add((double) (openTime + candleDuration - 1000));
                closePoint.add(close);
                prices.add(closePoint);
            }
        }
        
        chartData.setPrices(prices);
        return chartData;
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
    
    /**
     * 코인 선택
     */
    public void selectCoin(String coinId) {
        selectedCoinId.setValue(coinId);
        // 차트 데이터 로드
        loadChartData(coinId, 7);
        
        // 현재 가격 업데이트
        List<CoinPrice> prices = coinPrices.getValue();
        if (prices != null) {
            for (CoinPrice price : prices) {
                if (price.getId().equals(coinId)) {
                    currentPrice.setValue(price.getCurrentPrice());
                    entryPrice.setValue(price.getCurrentPrice());
                    break;
                }
            }
        }
    }
    
    /**
     * 가격 입력 업데이트
     */
    public void updateEntryPrice(double price) {
        entryPrice.setValue(price);
        calculateRiskReward();
    }
    
    public void updateTakeProfit(double price) {
        takeProfit.setValue(price);
        calculateRiskReward();
    }
    
    public void updateStopLoss(double price) {
        stopLoss.setValue(price);
        calculateRiskReward();
    }
    
    public void setIsLong(boolean long_position) {
        isLong.setValue(long_position);
        calculateRiskReward();
    }
    
    /**
     * R:R 비율 계산
     */
    private void calculateRiskReward() {
        Double entry = entryPrice.getValue();
        Double tp = takeProfit.getValue();
        Double sl = stopLoss.getValue();
        Boolean longPos = isLong.getValue();
        
        if (entry != null && tp != null && sl != null && longPos != null) {
            double rr = RiskCalculator.calculateRiskRewardRatio(entry, tp, sl, longPos);
            riskRewardRatio.setValue(rr);
        }
    }
    
    // Getters for LiveData
    public MutableLiveData<List<CoinPrice>> getCoinPrices() {
        return coinPrices;
    }
    
    public MutableLiveData<CoinMarketChart> getChartData() {
        return chartData;
    }
    
    public MutableLiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public MutableLiveData<Boolean> getLoading() {
        return loading;
    }
    
    public MutableLiveData<String> getSelectedCoinId() {
        return selectedCoinId;
    }
    
    public MutableLiveData<Double> getCurrentPrice() {
        return currentPrice;
    }
    
    public MutableLiveData<Double> getEntryPrice() {
        return entryPrice;
    }
    
    public MutableLiveData<Double> getTakeProfit() {
        return takeProfit;
    }
    
    public MutableLiveData<Double> getStopLoss() {
        return stopLoss;
    }
    
    public MutableLiveData<Boolean> getIsLong() {
        return isLong;
    }
    
    public MutableLiveData<Double> getRiskRewardRatio() {
        return riskRewardRatio;
    }
    
    public MutableLiveData<KlineData> getKlineUpdate() {
        return klineUpdate;
    }
    
    public MutableLiveData<List<List<Object>>> getBinanceKlines() {
        return binanceKlines;
    }
}

