package com.example.rsquare.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.example.rsquare.util.DateConverter;

import java.util.Date;

/**
 * 거래 저널 엔티티
 */
@Entity(
    tableName = "journal",
    foreignKeys = @ForeignKey(
        entity = Position.class,
        parentColumns = "id",
        childColumns = "positionId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("positionId"), @Index("timestamp")}
)
@TypeConverters(DateConverter.class)
public class Journal {
    
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    private long positionId;
    private Emotion emotion;
    private String note;
    private Date timestamp;
    
    public enum Emotion {
        CONFIDENT("집중/자신감"),
        ANXIOUS("불안"),
        GREEDY("욕심"),
        FEAR("두려움"),
        FOMO("놓칠까봐 조급함"),
        REVENGE("복수 심리"),
        CALM("평온");
        
        private final String description;
        
        Emotion(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    public Journal() {
        this.timestamp = new Date();
    }
    
    // Getters and Setters
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public long getPositionId() {
        return positionId;
    }
    
    public void setPositionId(long positionId) {
        this.positionId = positionId;
    }
    
    public Emotion getEmotion() {
        return emotion;
    }
    
    public void setEmotion(Emotion emotion) {
        this.emotion = emotion;
    }
    
    public String getNote() {
        return note;
    }
    
    public void setNote(String note) {
        this.note = note;
    }
    
    public Date getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}

