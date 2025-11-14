package com.example.rsquare.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.example.rsquare.util.DateConverter;

import java.util.Date;

/**
 * 거래 히스토리 엔티티
 */
@Entity(
    tableName = "trade_history",
    foreignKeys = @ForeignKey(
        entity = Position.class,
        parentColumns = "id",
        childColumns = "positionId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("positionId"), @Index("timestamp")}
)
@TypeConverters(DateConverter.class)
public class TradeHistory {
    
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    private long positionId;
    private String symbol;
    private TradeType type;
    private double price;
    private double quantity;
    private double pnl;
    private Date timestamp;
    
    public enum TradeType {
        BUY,
        SELL,
        CLOSE_TP,
        CLOSE_SL
    }
    
    public TradeHistory() {
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
    
    public String getSymbol() {
        return symbol;
    }
    
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
    
    public TradeType getType() {
        return type;
    }
    
    public void setType(TradeType type) {
        this.type = type;
    }
    
    public double getPrice() {
        return price;
    }
    
    public void setPrice(double price) {
        this.price = price;
    }
    
    public double getQuantity() {
        return quantity;
    }
    
    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }
    
    public double getPnl() {
        return pnl;
    }
    
    public void setPnl(double pnl) {
        this.pnl = pnl;
    }
    
    public Date getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}

