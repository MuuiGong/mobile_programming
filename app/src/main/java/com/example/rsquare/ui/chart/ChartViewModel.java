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
     * 차트 데이터 로드
     */
    public void loadChartData(String coinId, int days) {
        loading.setValue(true);
        
        marketDataRepository.getMarketChart(coinId, days, 
            new MarketDataRepository.OnChartDataLoadedListener() {
                @Override
                public void onChartDataLoaded(CoinMarketChart data) {
                    loading.postValue(false);
                    chartData.postValue(data);
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
     * 코인 선택
     */
    public void selectCoin(String coinId) {
        selectedCoinId.setValue(coinId);
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
}

