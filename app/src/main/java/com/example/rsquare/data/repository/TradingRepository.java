package com.example.rsquare.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.rsquare.data.local.AppDatabase;
import com.example.rsquare.data.local.dao.PositionDao;
import com.example.rsquare.data.local.dao.TradeHistoryDao;
import com.example.rsquare.data.local.entity.Position;
import com.example.rsquare.data.local.entity.TradeHistory;

import java.util.Date;
import java.util.List;

/**
 * Trading Repository
 * 포지션 관리 및 거래 실행
 */
public class TradingRepository {
    
    private final PositionDao positionDao;
    private final TradeHistoryDao tradeHistoryDao;
    
    public TradingRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        this.positionDao = database.positionDao();
        this.tradeHistoryDao = database.tradeHistoryDao();
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
                
                if (listener != null) {
                    listener.onPositionClosed(pnl);
                }
            }
        });
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
    
    public interface OnStatisticsLoadedListener {
        void onStatisticsLoaded(int winCount, int lossCount, int totalCount, 
                               double avgPnl, double totalPnl);
    }
    
    public interface OnPositionsLoadedListener {
        void onPositionsLoaded(List<Position> positions);
    }
}

