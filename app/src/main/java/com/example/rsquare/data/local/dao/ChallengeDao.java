package com.example.rsquare.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.rsquare.data.local.entity.Challenge;

import java.util.List;

/**
 * Challenge DAO
 */
@Dao
public interface ChallengeDao {
    
    @Insert
    long insert(Challenge challenge);
    
    @Update
    void update(Challenge challenge);
    
    @Query("SELECT * FROM challenges WHERE id = :challengeId")
    LiveData<Challenge> getChallengeById(long challengeId);
    
    @Query("SELECT * FROM challenges WHERE id = :challengeId")
    Challenge getChallengeByIdSync(long challengeId);
    
    @Query("SELECT * FROM challenges WHERE userId = :userId AND status = 'ACTIVE' ORDER BY createdAt DESC")
    LiveData<List<Challenge>> getActiveChallenges(long userId);
    
    @Query("SELECT * FROM challenges WHERE userId = :userId AND status = 'ACTIVE' ORDER BY createdAt DESC")
    List<Challenge> getActiveChallengesSync(long userId);
    
    @Query("SELECT * FROM challenges WHERE userId = :userId AND status = 'COMPLETED' ORDER BY completedAt DESC")
    LiveData<List<Challenge>> getCompletedChallenges(long userId);
    
    @Query("SELECT * FROM challenges WHERE userId = :userId ORDER BY createdAt DESC")
    LiveData<List<Challenge>> getAllChallenges(long userId);
    
    @Query("UPDATE challenges SET progress = :progress WHERE id = :challengeId")
    void updateProgress(long challengeId, double progress);
    
    @Query("UPDATE challenges SET status = :status, completedAt = :completedAt WHERE id = :challengeId")
    void updateStatus(long challengeId, Challenge.Status status, Long completedAt);
    
    @Query("SELECT COUNT(*) FROM challenges WHERE userId = :userId AND status = 'COMPLETED'")
    int getCompletedChallengeCount(long userId);
    
    @Query("SELECT * FROM challenges WHERE userId = :userId AND targetType = :targetType AND status = 'ACTIVE' LIMIT 1")
    Challenge getActiveChallengeByType(long userId, String targetType);
}

