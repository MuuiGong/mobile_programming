package com.example.rsquare.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.rsquare.data.local.entity.TradeHistory;

import java.util.List;

/**
 * TradeHistory DAO
 */
@Dao
public interface TradeHistoryDao {
    
    @Insert
    long insert(TradeHistory tradeHistory);
    
    @Query("SELECT * FROM trade_history ORDER BY timestamp DESC")
    LiveData<List<TradeHistory>> getAllTrades();
    
    @Query("SELECT * FROM trade_history WHERE positionId = :positionId ORDER BY timestamp DESC")
    LiveData<List<TradeHistory>> getTradesByPosition(long positionId);
    
    @Query("SELECT * FROM trade_history WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    List<TradeHistory> getTradesSince(long startTime);
    
    @Query("SELECT * FROM trade_history ORDER BY timestamp DESC LIMIT :limit")
    LiveData<List<TradeHistory>> getRecentTrades(int limit);
    
    @Query("SELECT SUM(pnl) FROM trade_history WHERE timestamp >= :startTime")
    Double getTotalPnLSince(long startTime);
    
    @Query("SELECT COUNT(*) FROM trade_history WHERE timestamp >= :startTime")
    int getTradeCountSince(long startTime);
}

