package com.example.rsquare.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.example.rsquare.util.DateConverter;

import java.util.Date;

/**
 * 뱃지 엔티티
 */
@Entity(
    tableName = "badges",
    foreignKeys = @ForeignKey(
        entity = User.class,
        parentColumns = "id",
        childColumns = "userId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("userId"), @Index("badgeType")}
)
@TypeConverters(DateConverter.class)
public class Badge {
    
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    private long userId;
    private BadgeType badgeType;
    private String name;
    private String description;
    private Date earnedAt;
    
    public enum BadgeType {
        FIRST_TRADE("첫 거래"),
        RISK_MASTER("리스크 마스터"),
        WIN_STREAK_5("5연승"),
        WIN_STREAK_10("10연승"),
        CONSISTENT_TRADER("일관된 트레이더"),
        EMOTION_CONTROL("감정 컨트롤"),
        SHARP_TRADER("샤프 트레이더"),
        CHALLENGE_HERO("챌린지 히어로"),
        WEEK_WARRIOR("주간 워리어"),
        MONTH_MASTER("월간 마스터");
        
        private final String displayName;
        
        BadgeType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public Badge() {
        this.earnedAt = new Date();
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
    
    public BadgeType getBadgeType() {
        return badgeType;
    }
    
    public void setBadgeType(BadgeType badgeType) {
        this.badgeType = badgeType;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Date getEarnedAt() {
        return earnedAt;
    }
    
    public void setEarnedAt(Date earnedAt) {
        this.earnedAt = earnedAt;
    }
}

