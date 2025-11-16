package com.example.rsquare.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.rsquare.data.local.AppDatabase;
import com.example.rsquare.data.local.dao.UserDao;
import com.example.rsquare.data.local.entity.User;

/**
 * User Repository
 * 사용자 정보 및 잔고 관리
 */
public class UserRepository {
    
    private final UserDao userDao;
    
    public UserRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        this.userDao = database.userDao();
    }
    
    /**
     * 기본 사용자 조회
     */
    public LiveData<User> getDefaultUser() {
        return userDao.getDefaultUser();
    }
    
    /**
     * 기본 사용자 조회 (동기)
     */
    public User getDefaultUserSync() {
        return userDao.getDefaultUserSync();
    }
    
    /**
     * 사용자 ID로 조회
     */
    public LiveData<User> getUserById(long userId) {
        return userDao.getUserById(userId);
    }
    
    /**
     * 사용자 ID로 조회 (동기)
     */
    public User getUserSync(long userId) {
        return userDao.getUserByIdSync(userId);
    }
    
    /**
     * 사용자 정보 업데이트
     */
    public void updateUser(User user) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            userDao.update(user);
        });
    }
    
    /**
     * 잔고 업데이트
     */
    public void updateBalance(long userId, double balance) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            userDao.updateBalance(userId, balance);
        });
    }
    
    /**
     * 잔고에 금액 추가/차감
     */
    public void addToBalance(long userId, double amount) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            userDao.addToBalance(userId, amount);
        });
    }
    
    /**
     * 사용자 생성
     */
    public void insertUser(User user, OnUserCreatedListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long userId = userDao.insert(user);
            if (listener != null) {
                listener.onUserCreated(userId);
            }
        });
    }
    
    /**
     * 사용자 수 확인
     */
    public void checkUserExists(OnUserExistsListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            int count = userDao.getUserCount();
            if (listener != null) {
                listener.onResult(count > 0);
            }
        });
    }
    
    public interface OnUserCreatedListener {
        void onUserCreated(long userId);
    }
    
    public interface OnUserExistsListener {
        void onResult(boolean exists);
    }
}

