package com.example.rsquare.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.rsquare.data.local.AppDatabase;
import com.example.rsquare.data.local.dao.PositionDao;
import com.example.rsquare.data.local.dao.TradeHistoryDao;
import com.example.rsquare.data.local.dao.UserDao;
import com.example.rsquare.data.local.entity.Position;
import com.example.rsquare.data.local.entity.TradeHistory;
import com.example.rsquare.domain.MarginCalculator;

import java.util.Date;
import java.util.List;

/**
 * Trading Repository
 * 포지션 관리 및 거래 실행
 */
public class TradingRepository {
    
    private final PositionDao positionDao;
    private final TradeHistoryDao tradeHistoryDao;
    private final UserDao userDao;
    
    public TradingRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        this.positionDao = database.positionDao();
        this.tradeHistoryDao = database.tradeHistoryDao();
        this.userDao = database.userDao();
    }
    
    /**
     * 활성 포지션 조회
     */
    public LiveData<List<Position>> getActivePositions(long userId) {
        return positionDao.getActivePositions(userId);
    }
    
    /**
     * 활성 포지션 조회 (동기)
     */
    public List<Position> getActivePositionsSync(long userId) {
        return positionDao.getActivePositionsSync(userId);
    }
    
    /**
     * 종료된 포지션 조회
     */
    public LiveData<List<Position>> getClosedPositions(long userId) {
        return positionDao.getClosedPositions(userId);
    }
    
    /**
     * 종료된 포지션 조회 (동기)
     */
    public List<Position> getClosedPositionsSync(long userId) {
        return positionDao.getClosedPositionsSync(userId);
    }
    
    /**
     * 모든 포지션 조회
     */
    public LiveData<List<Position>> getAllPositions(long userId) {
        return positionDao.getAllPositions(userId);
    }
    
    /**
     * 최근 포지션 조회
     */
    public LiveData<List<Position>> getRecentPositions(long userId, int limit) {
        return positionDao.getRecentPositions(userId, limit);
    }
    
    /**
     * 포지션 ID로 조회
     */
    public LiveData<Position> getPositionById(long positionId) {
        return positionDao.getPositionById(positionId);
    }
    
    /**
     * 포지션 ID로 조회 (동기)
     */
    public Position getPositionByIdSync(long positionId) {
        return positionDao.getPositionByIdSync(positionId);
    }
    
    /**
     * 포지션 열기 (새 거래)
     */
    public void openPosition(Position position, OnPositionOpenedListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long positionId = positionDao.insert(position);
            
            // TradeHistory 기록
            TradeHistory tradeHistory = new TradeHistory();
            tradeHistory.setPositionId(positionId);
            tradeHistory.setSymbol(position.getSymbol());
            tradeHistory.setType(position.isLong() ? TradeHistory.TradeType.BUY : TradeHistory.TradeType.SELL);
            tradeHistory.setPrice(position.getEntryPrice());
            tradeHistory.setQuantity(position.getQuantity());
            tradeHistory.setPnl(0);
            tradeHistoryDao.insert(tradeHistory);
            
            if (listener != null) {
                listener.onPositionOpened(positionId);
            }
        });
    }
    
    /**
     * 포지션 닫기
     */
    public void closePosition(long positionId, double closedPrice, TradeHistory.TradeType closeType, 
                              OnPositionClosedListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Position position = positionDao.getPositionByIdSync(positionId);
            
            if (position != null && !position.isClosed()) {
                // PnL 계산
                double pnl = position.calculateUnrealizedPnL(closedPrice);
                
                // Position 업데이트
                position.setClosed(true);
                position.setCloseTime(new Date());
                position.setPnl(pnl);
                position.setClosedPrice(closedPrice);
                positionDao.update(position);
                
                // TradeHistory 기록
                TradeHistory tradeHistory = new TradeHistory();
                tradeHistory.setPositionId(positionId);
                tradeHistory.setSymbol(position.getSymbol());
                tradeHistory.setType(closeType);
                tradeHistory.setPrice(closedPrice);
                tradeHistory.setQuantity(position.getQuantity());
                tradeHistory.setPnl(pnl);
                tradeHistoryDao.insert(tradeHistory);
                
                // 잔고 업데이트 (마진 반환 + PnL - 종료 수수료)
                double positionSize = position.getEntryPrice() * position.getQuantity();
                double requiredMargin = MarginCalculator.calculateRequiredMargin(positionSize, position.getLeverage());
                
                // 종료 수수료 (종료 시점의 가치 기준)
                double closingValue = closedPrice * position.getQuantity();
                double closingFee = closingValue * 0.0004; // 0.04% fee
                
                userDao.addToBalance(position.getUserId(), requiredMargin + pnl - closingFee);
                
                if (listener != null) {
                    listener.onPositionClosed(pnl);
                }
            }
        });
    }

    /**
     * 모든 포지션 닫기
     */
    public void closeAllPositions(long userId, java.util.Map<String, Double> currentPrices, OnAllPositionsClosedListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Position> activePositions = positionDao.getActivePositionsSync(userId);
            double totalPnl = 0;
            
            for (Position position : activePositions) {
                Double currentPrice = currentPrices.get(position.getSymbol());
                if (currentPrice == null) continue; // 가격 정보 없으면 스킵
                
                // PnL 계산
                double pnl = position.calculateUnrealizedPnL(currentPrice);
                totalPnl += pnl;
                
                // Position 업데이트
                position.setClosed(true);
                position.setCloseTime(new Date());
                position.setPnl(pnl);
                position.setClosedPrice(currentPrice);
                position.setExitReason("Close All");
                positionDao.update(position);
                
                // TradeHistory 기록
                TradeHistory tradeHistory = new TradeHistory();
                tradeHistory.setPositionId(position.getId());
                tradeHistory.setSymbol(position.getSymbol());
                tradeHistory.setType(TradeHistory.TradeType.CLOSE_MARKET); // 일괄 종료는 시장가 종료로 처리
                tradeHistory.setPrice(currentPrice);
                tradeHistory.setQuantity(position.getQuantity());
                tradeHistory.setPnl(pnl);
                tradeHistoryDao.insert(tradeHistory);
                
                // 잔고 업데이트
                double positionSize = position.getEntryPrice() * position.getQuantity();
                double requiredMargin = MarginCalculator.calculateRequiredMargin(positionSize, position.getLeverage());
                
                double closingValue = currentPrice * position.getQuantity();
                double closingFee = closingValue * 0.0004;
                
                userDao.addToBalance(position.getUserId(), requiredMargin + pnl - closingFee);
            }
            
            if (listener != null) {
                listener.onAllPositionsClosed(totalPnl);
            }
        });
    }
    
    /**
     * 포지션 닫기 (동기)
     */
    public void closePositionSync(long positionId, double closedPrice, TradeHistory.TradeType closeType, String exitReason) {
        Position position = positionDao.getPositionByIdSync(positionId);
        
        if (position != null && !position.isClosed()) {
            // PnL 계산
            double pnl = position.calculateUnrealizedPnL(closedPrice);
            
            // Position 업데이트
            position.setClosed(true);
            position.setCloseTime(new Date());
            position.setPnl(pnl);
            position.setClosedPrice(closedPrice);
            position.setExitReason(exitReason);
            positionDao.update(position);
            
            // TradeHistory 기록
            TradeHistory tradeHistory = new TradeHistory();
            tradeHistory.setPositionId(positionId);
            tradeHistory.setSymbol(position.getSymbol());
            tradeHistory.setType(closeType);
            tradeHistory.setPrice(closedPrice);
            tradeHistory.setQuantity(position.getQuantity());
            tradeHistory.setPnl(pnl);
            tradeHistoryDao.insert(tradeHistory);
            
            // 잔고 업데이트 (마진 반환 + PnL - 종료 수수료)
            double positionSize = position.getEntryPrice() * position.getQuantity();
            double requiredMargin = MarginCalculator.calculateRequiredMargin(positionSize, position.getLeverage());
            
            // 종료 수수료 (종료 시점의 가치 기준)
            double closingValue = closedPrice * position.getQuantity();
            double closingFee = closingValue * 0.0004; // 0.04% fee
            
            userDao.addToBalance(position.getUserId(), requiredMargin + pnl - closingFee);
        }
    }
    
    /**
     * 포지션 업데이트 (TP/SL 변경)
     */
    public void updatePosition(Position position) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            positionDao.update(position);
        });
    }
    
    /**
     * 포지션 업데이트 (동기)
     */
    public void updatePositionSync(Position position) {
        positionDao.update(position);
    }
    
    /**
     * 거래 히스토리 삽입
     */
    public void insertTradeHistory(TradeHistory tradeHistory) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            tradeHistoryDao.insert(tradeHistory);
        });
    }
    
    /**
     * 거래 히스토리 삽입 (동기)
     */
    public long insertTradeHistorySync(TradeHistory tradeHistory) {
        return tradeHistoryDao.insert(tradeHistory);
    }
    
    /**
     * 총 손익 조회
     */
    public LiveData<Double> getTotalPnL(long userId) {
        return positionDao.getTotalPnL(userId);
    }
    
    /**
     * 거래 통계 조회
     */
    public void getTradeStatistics(long userId, OnStatisticsLoadedListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            int winCount = positionDao.getWinCount(userId);
            int lossCount = positionDao.getLossCount(userId);
            int totalCount = positionDao.getTotalTradeCount(userId);
            Double avgPnl = positionDao.getAveragePnL(userId);
            Double totalPnl = positionDao.getTotalPnLSync(userId);
            
            if (listener != null) {
                listener.onStatisticsLoaded(winCount, lossCount, totalCount, 
                    avgPnl != null ? avgPnl : 0.0, 
                    totalPnl != null ? totalPnl : 0.0);
            }
        });
    }
    
    /**
     * 최근 거래 기록 조회
     */
    public LiveData<List<TradeHistory>> getRecentTrades(int limit) {
        return tradeHistoryDao.getRecentTrades(limit);
    }
    
    /**
     * 특정 기간 이후 포지션 조회
     */
    public void getPositionsSince(long userId, long startTime, OnPositionsLoadedListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Position> positions = positionDao.getPositionsSince(userId, startTime);
            if (listener != null) {
                listener.onPositionsLoaded(positions);
            }
        });
    }
    
    public interface OnPositionOpenedListener {
        void onPositionOpened(long positionId);
    }
    
    public interface OnPositionClosedListener {
        void onPositionClosed(double pnl);
    }

    public interface OnAllPositionsClosedListener {
        void onAllPositionsClosed(double totalPnl);
    }
    
    public interface OnStatisticsLoadedListener {
        void onStatisticsLoaded(int winCount, int lossCount, int totalCount, 
                               double avgPnl, double totalPnl);
    }
    
    public interface OnPositionsLoadedListener {
        void onPositionsLoaded(List<Position> positions);
    }
    
    /**
     * 현재 잔고 조회 (동기)
     */
    public double getBalanceSync(long userId) {
        com.example.rsquare.data.local.entity.User user = userDao.getUserByIdSync(userId);
        return user != null ? user.getBalance() : 0.0;
    }

    /**
     * 일일 손익 조회 (동기)
     */
    public double getDailyPnLSync(long userId) {
        // 오늘 00:00:00 구하기
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        long startTime = calendar.getTimeInMillis();
        
        // 오늘 종료된 포지션들의 PnL 합계
        // DAO에 쿼리 추가 필요: SELECT SUM(pnl) FROM trade_history WHERE timestamp >= :startTime
        // 임시로 모든 포지션을 가져와서 계산 (비효율적이지만 DAO 수정 없이 가능)
        List<TradeHistory> trades = tradeHistoryDao.getTradesSinceSync(startTime);
        double dailyPnl = 0;
        for (TradeHistory trade : trades) {
            dailyPnl += trade.getPnl();
        }
        return dailyPnl;
    }

    /**
     * 마지막 거래 조회 (동기)
     */
    public TradeHistory getLastTradeSync() {
        return tradeHistoryDao.getLastTradeSync();
    }
}

