package com.example.rsquare.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.example.rsquare.util.DateConverter;

import java.util.Date;

/**
 * 챌린지 엔티티
 */
@Entity(
    tableName = "challenges",
    foreignKeys = @ForeignKey(
        entity = User.class,
        parentColumns = "id",
        childColumns = "userId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("userId"), @Index("status")}
)
@TypeConverters(DateConverter.class)
public class Challenge {
    
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    private long userId;
    private String title;
    private String description;
    private double targetValue;
    private String targetType; // "risk_reward", "win_streak", "consistent_trading", etc.
    private Difficulty difficulty;
    private Status status;
    private double progress;
    private Date createdAt;
    private Date completedAt;
    
    public enum Difficulty {
        EASY(1),
        MEDIUM(2),
        HARD(3);
        
        private final int level;
        
        Difficulty(int level) {
            this.level = level;
        }
        
        public int getLevel() {
            return level;
        }
    }
    
    public enum Status {
        ACTIVE,
        COMPLETED,
        FAILED,
        EXPIRED
    }
    
    public Challenge() {
        this.createdAt = new Date();
        this.status = Status.ACTIVE;
        this.progress = 0.0;
    }
    
    /**
     * 진행률 업데이트
     */
    public void updateProgress(double currentValue) {
        this.progress = Math.min(100.0, (currentValue / targetValue) * 100.0);
        
        if (this.progress >= 100.0 && this.status == Status.ACTIVE) {
            this.status = Status.COMPLETED;
            this.completedAt = new Date();
        }
    }
    
    // Getters and Setters
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public long getUserId() {
        return userId;
    }
    
    public void setUserId(long userId) {
        this.userId = userId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public double getTargetValue() {
        return targetValue;
    }
    
    public void setTargetValue(double targetValue) {
        this.targetValue = targetValue;
    }
    
    public String getTargetType() {
        return targetType;
    }
    
    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }
    
    public Difficulty getDifficulty() {
        return difficulty;
    }
    
    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }
    
    public Status getStatus() {
        return status;
    }
    
    public void setStatus(Status status) {
        this.status = status;
    }
    
    public double getProgress() {
        return progress;
    }
    
    public void setProgress(double progress) {
        this.progress = progress;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    public Date getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(Date completedAt) {
        this.completedAt = completedAt;
    }
}

