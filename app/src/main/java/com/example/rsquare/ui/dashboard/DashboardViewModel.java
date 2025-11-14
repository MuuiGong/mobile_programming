package com.example.rsquare.ui.dashboard;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.rsquare.data.local.entity.Position;
import com.example.rsquare.data.local.entity.User;
import com.example.rsquare.data.repository.TradingRepository;
import com.example.rsquare.data.repository.UserRepository;
import com.example.rsquare.domain.RiskCalculator;
import com.example.rsquare.domain.RiskMetrics;

import java.util.List;

/**
 * Dashboard ViewModel
 * 대시보드 화면의 데이터 관리
 */
public class DashboardViewModel extends AndroidViewModel {
    
    private final UserRepository userRepository;
    private final TradingRepository tradingRepository;
    
    private final LiveData<User> currentUser;
    private final LiveData<List<Position>> recentPositions;
    private final LiveData<Double> totalPnL;
    
    private final MutableLiveData<RiskMetrics> riskMetrics = new MutableLiveData<>();
    private final MutableLiveData<TradeStatistics> tradeStatistics = new MutableLiveData<>();
    
    public DashboardViewModel(@NonNull Application application) {
        super(application);
        
        userRepository = new UserRepository(application);
        tradingRepository = new TradingRepository(application);
        
        // LiveData 초기화
        currentUser = userRepository.getDefaultUser();
        recentPositions = tradingRepository.getRecentPositions(1, 10);
        totalPnL = tradingRepository.getTotalPnL(1);
        
        // 초기 데이터 로드
        loadRiskMetrics();
        loadTradeStatistics();
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
     * 총 손익 조회
     */
    public LiveData<Double> getTotalPnL() {
        return totalPnL;
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
    public void refresh() {
        loadRiskMetrics();
        loadTradeStatistics();
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

