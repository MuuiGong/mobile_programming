package com.example.rsquare.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.rsquare.data.local.entity.Journal;

import java.util.List;

/**
 * Journal DAO
 */
@Dao
public interface JournalDao {
    
    @Insert
    long insert(Journal journal);
    
    @Update
    void update(Journal journal);
    
    @Query("SELECT * FROM journal ORDER BY timestamp DESC")
    LiveData<List<Journal>> getAllJournals();
    
    @Query("SELECT * FROM journal WHERE positionId = :positionId ORDER BY timestamp DESC")
    LiveData<List<Journal>> getJournalsByPosition(long positionId);
    
    @Query("SELECT * FROM journal WHERE positionId = :positionId ORDER BY timestamp DESC")
    List<Journal> getJournalsByPositionSync(long positionId);
    
    @Query("SELECT * FROM journal WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    List<Journal> getJournalsSince(long startTime);
    
    @Query("SELECT emotion, COUNT(*) as count FROM journal WHERE timestamp >= :startTime GROUP BY emotion")
    List<EmotionCount> getEmotionDistribution(long startTime);
    
    @Query("SELECT * FROM journal ORDER BY timestamp DESC LIMIT :limit")
    LiveData<List<Journal>> getRecentJournals(int limit);
    
    /**
     * 감정 분포 통계용 클래스
     */
    class EmotionCount {
        public Journal.Emotion emotion;
        public int count;
    }
}

