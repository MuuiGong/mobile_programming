package com.example.rsquare.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.rsquare.data.local.AppDatabase;
import com.example.rsquare.data.local.dao.UserSettingsDao;
import com.example.rsquare.data.local.entity.UserSettings;

/**
 * 사용자 설정 Repository
 */
public class UserSettingsRepository {
    
    private final UserSettingsDao settingsDao;
    
    public UserSettingsRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        this.settingsDao = database.userSettingsDao();
    }
    
    /**
     * 사용자 설정 조회
     */
    public LiveData<UserSettings> getSettings(long userId) {
        return settingsDao.getSettingsByUserId(userId);
    }
    
    /**
     * 사용자 설정 조회 (동기)
     */
    public UserSettings getSettingsSync(long userId) {
        return settingsDao.getSettingsByUserIdSync(userId);
    }
    
    /**
     * 사용자 설정 저장/업데이트
     */
    public void saveSettings(UserSettings settings, OnSettingsSavedListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            settings.setUpdatedAt(new java.util.Date());
            long id = settingsDao.insert(settings);
            if (listener != null) {
                listener.onSettingsSaved(id);
            }
        });
    }
    
    /**
     * 사용자 설정 저장/업데이트 (동기)
     */
    public long saveSettingsSync(UserSettings settings) {
        settings.setUpdatedAt(new java.util.Date());
        return settingsDao.insert(settings);
    }
    
    /**
     * 사용자 설정 삭제
     */
    public void deleteSettings(long userId) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            settingsDao.deleteByUserId(userId);
        });
    }
    
    public interface OnSettingsSavedListener {
        void onSettingsSaved(long settingsId);
    }
}

