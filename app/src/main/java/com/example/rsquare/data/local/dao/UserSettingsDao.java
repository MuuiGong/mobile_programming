package com.example.rsquare.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.rsquare.data.local.entity.UserSettings;

/**
 * 사용자 설정 DAO
 */
@Dao
public interface UserSettingsDao {
    
    /**
     * 사용자 설정 삽입
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(UserSettings settings);
    
    /**
     * 사용자 설정 업데이트
     */
    @Update
    void update(UserSettings settings);
    
    /**
     * 사용자 ID로 설정 조회
     */
    @Query("SELECT * FROM user_settings WHERE userId = :userId LIMIT 1")
    LiveData<UserSettings> getSettingsByUserId(long userId);
    
    /**
     * 사용자 ID로 설정 조회 (동기)
     */
    @Query("SELECT * FROM user_settings WHERE userId = :userId LIMIT 1")
    UserSettings getSettingsByUserIdSync(long userId);
    
    /**
     * 사용자 설정 삭제
     */
    @Query("DELETE FROM user_settings WHERE userId = :userId")
    void deleteByUserId(long userId);
}

