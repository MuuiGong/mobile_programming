package com.example.rsquare.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.rsquare.data.local.entity.User;

/**
 * User DAO
 */
@Dao
public interface UserDao {
    
    @Insert
    long insert(User user);
    
    @Update
    void update(User user);
    
    @Query("SELECT * FROM users WHERE id = :userId")
    LiveData<User> getUserById(long userId);
    
    @Query("SELECT * FROM users WHERE id = :userId")
    User getUserByIdSync(long userId);
    
    @Query("SELECT * FROM users LIMIT 1")
    LiveData<User> getDefaultUser();
    
    @Query("SELECT * FROM users LIMIT 1")
    User getDefaultUserSync();
    
    @Query("UPDATE users SET balance = :balance WHERE id = :userId")
    void updateBalance(long userId, double balance);
    
    @Query("UPDATE users SET balance = balance + :amount WHERE id = :userId")
    void addToBalance(long userId, double amount);
    
    @Query("SELECT COUNT(*) FROM users")
    int getUserCount();
}

