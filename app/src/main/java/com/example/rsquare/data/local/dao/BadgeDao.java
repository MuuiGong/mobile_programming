package com.example.rsquare.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.rsquare.data.local.entity.Badge;

import java.util.List;

/**
 * Badge DAO
 */
@Dao
public interface BadgeDao {
    
    @Insert
    long insert(Badge badge);
    
    @Query("SELECT * FROM badges WHERE userId = :userId ORDER BY earnedAt DESC")
    LiveData<List<Badge>> getUserBadges(long userId);
    
    @Query("SELECT * FROM badges WHERE userId = :userId ORDER BY earnedAt DESC")
    List<Badge> getUserBadgesSync(long userId);
    
    @Query("SELECT * FROM badges WHERE userId = :userId AND badgeType = :badgeType LIMIT 1")
    Badge getBadgeByType(long userId, Badge.BadgeType badgeType);
    
    @Query("SELECT COUNT(*) FROM badges WHERE userId = :userId")
    int getBadgeCount(long userId);
    
    @Query("SELECT * FROM badges WHERE userId = :userId ORDER BY earnedAt DESC LIMIT :limit")
    LiveData<List<Badge>> getRecentBadges(long userId, int limit);
}

