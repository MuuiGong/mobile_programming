package com.example.rsquare.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.rsquare.data.local.entity.Position;

import java.util.List;

/**
 * Position DAO
 */
@Dao
public interface PositionDao {
    
    @Insert
    long insert(Position position);
    
    @Update
    void update(Position position);
    
    @Query("SELECT * FROM positions WHERE id = :positionId")
    LiveData<Position> getPositionById(long positionId);
    
    @Query("SELECT * FROM positions WHERE id = :positionId")
    Position getPositionByIdSync(long positionId);
    
    @Query("SELECT * FROM positions WHERE userId = :userId AND isClosed = 0 ORDER BY openTime DESC")
    LiveData<List<Position>> getActivePositions(long userId);
    
    @Query("SELECT * FROM positions WHERE userId = :userId AND isClosed = 0 ORDER BY openTime DESC")
    List<Position> getActivePositionsSync(long userId);
    
    @Query("SELECT * FROM positions WHERE userId = :userId AND isClosed = 1 ORDER BY closeTime DESC")
    LiveData<List<Position>> getClosedPositions(long userId);
    
    @Query("SELECT * FROM positions WHERE userId = :userId ORDER BY openTime DESC")
    LiveData<List<Position>> getAllPositions(long userId);
    
    @Query("SELECT * FROM positions WHERE userId = :userId ORDER BY openTime DESC LIMIT :limit")
    LiveData<List<Position>> getRecentPositions(long userId, int limit);
    
    @Query("SELECT SUM(pnl) FROM positions WHERE userId = :userId AND isClosed = 1")
    LiveData<Double> getTotalPnL(long userId);
    
    @Query("SELECT SUM(pnl) FROM positions WHERE userId = :userId AND isClosed = 1")
    Double getTotalPnLSync(long userId);
    
    @Query("SELECT COUNT(*) FROM positions WHERE userId = :userId AND isClosed = 1 AND pnl > 0")
    int getWinCount(long userId);
    
    @Query("SELECT COUNT(*) FROM positions WHERE userId = :userId AND isClosed = 1 AND pnl < 0")
    int getLossCount(long userId);
    
    @Query("SELECT COUNT(*) FROM positions WHERE userId = :userId AND isClosed = 1")
    int getTotalTradeCount(long userId);
    
    @Query("SELECT AVG(pnl) FROM positions WHERE userId = :userId AND isClosed = 1")
    Double getAveragePnL(long userId);
    
    @Query("SELECT * FROM positions WHERE userId = :userId AND isClosed = 1 AND openTime >= :startTime ORDER BY closeTime DESC")
    List<Position> getPositionsSince(long userId, long startTime);
}

