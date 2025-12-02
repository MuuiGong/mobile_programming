package com.example.rsquare.ui.dashboard;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.rsquare.data.local.entity.Position;
import com.example.rsquare.data.local.entity.User;
import com.example.rsquare.data.repository.MarketDataRepository;
import com.example.rsquare.data.repository.TradingRepository;
import com.example.rsquare.data.repository.UserRepository;
import com.example.rsquare.domain.RiskCalculator;
import com.example.rsquare.domain.RiskMetrics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dashboard ViewModel
 * 대시보드 화면의 데이터 관리
 */
/**
 * Dashboard ViewModel
 * 대시보드 화면의 데이터 관리
 */
public class DashboardViewModel extends AndroidViewModel implements MarketDataRepository.OnPriceUpdateListener {
    
    private final UserRepository userRepository;
    private final TradingRepository tradingRepository;
    private final MarketDataRepository marketDataRepository;
    
    private final LiveData<User> currentUser;
    private final LiveData<List<Position>> recentPositions;
    private final LiveData<List<Position>> activePositions;
    private final LiveData<Double> totalPnL;
    
    // 실시간 데이터
    private final MutableLiveData<Double> unrealizedPnL = new MutableLiveData<>(0.0);
    private final MutableLiveData<Map<String, Double>> priceUpdates = new MutableLiveData<>(new HashMap<>());
    
    // 내부 상태
    private Map<String, Double> currentPrices = new HashMap<>();
    private List<Position> currentActivePositions = new ArrayList<>();
    
    private final MutableLiveData<RiskMetrics> riskMetrics = new MutableLiveData<>();
    private final MutableLiveData<TradeStatistics> tradeStatistics = new MutableLiveData<>();
    
    public DashboardViewModel(@NonNull Application application) {
        super(application);
        
        userRepository = new UserRepository(application);
        tradingRepository = new TradingRepository(application);
        marketDataRepository = new MarketDataRepository();
        marketDataRepository.setPriceUpdateListener(this);
        
        // LiveData 초기화
        currentUser = userRepository.getDefaultUser();
        recentPositions = tradingRepository.getRecentPositions(1, 10);
        activePositions = tradingRepository.getActivePositions(1);
        totalPnL = tradingRepository.getTotalPnL(1);
        
        // 초기 데이터 로드
        loadRiskMetrics();
        loadTradeStatistics();
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        marketDataRepository.stopWebSocket();
    }
    
    /**
     * 현재 사용자 조회
     */
    public LiveData<User> getCurrentUser() {
        return currentUser;
    }
    
    /**
     * 최근 포지션 조회
     */
    public LiveData<List<Position>> getRecentPositions() {
        return recentPositions;
    }

    /**
     * 활성 포지션 조회
     */
    public LiveData<List<Position>> getActivePositions() {
        return activePositions;
    }
    
    /**
     * 총 손익 조회
     */
    public LiveData<Double> getTotalPnL() {
        return totalPnL;
    }
    
    /**
     * 미실현 손익 조회 (실시간)
     */
    public LiveData<Double> getUnrealizedPnL() {
        return unrealizedPnL;
    }
    
    /**
     * 가격 업데이트 조회 (실시간)
     */
    public LiveData<Map<String, Double>> getPriceUpdates() {
        return priceUpdates;
    }
    
    /**
     * 리스크 메트릭스 조회
     */
    public LiveData<RiskMetrics> getRiskMetrics() {
        return riskMetrics;
    }
    
    /**
     * 거래 통계 조회
     */
    public LiveData<TradeStatistics> getTradeStatistics() {
        return tradeStatistics;
    }
    
    /**
     * 리스크 메트릭스 로드
     */
    public void loadRiskMetrics() {
        tradingRepository.getPositionsSince(1, 0, positions -> {
            List<Position> closedPositions = new java.util.ArrayList<>();
            for (Position position : positions) {
                if (position.isClosed()) {
                    closedPositions.add(position);
                }
            }
            
            RiskMetrics metrics = RiskCalculator.calculateRiskScore(closedPositions);
            riskMetrics.postValue(metrics);
        });
    }
    
    /**
     * 거래 통계 로드
     */
    public void loadTradeStatistics() {
        tradingRepository.getTradeStatistics(1, (winCount, lossCount, totalCount, avgPnl, totalPnl) -> {
            TradeStatistics stats = new TradeStatistics();
            stats.winCount = winCount;
            stats.lossCount = lossCount;
            stats.totalCount = totalCount;
            stats.avgPnL = avgPnl;
            stats.totalPnL = totalPnl;
            
            if (totalCount > 0) {
                stats.winRate = ((double) winCount / totalCount) * 100;
            }
            
            tradeStatistics.postValue(stats);
        });
    }
    
    /**
     * 데이터 새로고침
     */
    public void refreshData() {
        loadTradeStatistics();
    }
    
    /**
     * 활성 포지션 목록 업데이트 시 호출
     */
    public void updateActivePositionsList(List<Position> positions) {
        this.currentActivePositions = positions;
        
        // 구독할 코인 ID 목록 추출
        List<String> coinIds = new ArrayList<>();
        for (Position position : positions) {
            if (!position.isClosed()) {
                String coinId = marketDataRepository.getCoinIdFromSymbol(position.getSymbol());
                if (coinId != null && !coinIds.contains(coinId)) {
                    coinIds.add(coinId);
                }
            }
        }
        
        // 웹소켓 구독 시작
        if (!coinIds.isEmpty()) {
            marketDataRepository.startWebSocket(coinIds);
        }
        
        // 현재 가격으로 PnL 재계산
        calculateUnrealizedPnL();
    }
    
    @Override
    public void onPriceUpdate(String coinId, double price) {
        // 가격 맵 업데이트
        // 코인 ID를 심볼로 변환해야 함 (하지만 여기서는 역방향 매핑이 없으므로
        // MarketDataRepository의 SYMBOL_MAP을 참조하거나, 
        // 간단히 모든 활성 포지션을 순회하며 매칭되는지 확인)
        
        // 여기서는 간단히 코인 ID를 키로 사용하지 않고, 
        // 포지션의 심볼과 매칭되는지 확인하여 가격 맵 업데이트
        
        // 참고: MarketDataRepository.SYMBOL_MAP은 private이지만, 
        // getCoinIdFromSymbol 메서드를 통해 확인 가능
        
        boolean updated = false;
        for (Position position : currentActivePositions) {
            String id = marketDataRepository.getCoinIdFromSymbol(position.getSymbol());
            if (id != null && id.equals(coinId)) {
                currentPrices.put(position.getSymbol(), price);
                updated = true;
            }
        }
        
        if (updated) {
            priceUpdates.postValue(new HashMap<>(currentPrices));
            calculateUnrealizedPnL();
        }
    }
    
    @Override
    public void onConnectionStatusChanged(boolean connected) {
        // 연결 상태 변경 처리 (필요시)
    }
    
    /**
     * 미실현 손익 계산
     */
    private void calculateUnrealizedPnL() {
        double total = 0.0;
        for (Position position : currentActivePositions) {
            if (!position.isClosed()) {
                Double currentPrice = currentPrices.get(position.getSymbol());
                if (currentPrice != null && currentPrice > 0) {
                    total += position.calculateUnrealizedPnL(currentPrice);
                }
            }
        }
        unrealizedPnL.postValue(total);
    }
    
    /**
     * 거래 통계 데이터 클래스
     */
    public static class TradeStatistics {
        public int winCount;
        public int lossCount;
        public int totalCount;
        public double winRate;
        public double avgPnL;
        public double totalPnL;
    }
}

