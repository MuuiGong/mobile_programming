package com.example.rsquare.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.example.rsquare.util.DateConverter;

import java.util.Date;

/**
 * 포지션 엔티티
 */
@Entity(
    tableName = "positions",
    foreignKeys = @ForeignKey(
        entity = User.class,
        parentColumns = "id",
        childColumns = "userId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("userId"), @Index("isClosed")}
)
@TypeConverters(DateConverter.class)
public class Position {
    
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    private long userId;
    private String symbol;
    private double quantity;
    private double entryPrice;
    private double takeProfit;
    private double stopLoss;
    private boolean isLong; // true: 롱, false: 숏
    private Date openTime;
    private Date closeTime;
    private boolean isClosed;
    private double pnl; // 실현 손익
    private Double closedPrice; // 청산 가격
    
    public Position() {
        this.openTime = new Date();
        this.isClosed = false;
        this.pnl = 0.0;
    }
    
    /**
     * 현재 가격 기반 미실현 손익 계산
     */
    public double calculateUnrealizedPnL(double currentPrice) {
        if (isClosed) {
            return pnl;
        }
        
        double priceDiff = isLong ? 
            (currentPrice - entryPrice) : 
            (entryPrice - currentPrice);
        
        return priceDiff * quantity;
    }
    
    /**
     * TP 도달 여부 확인
     */
    public boolean isTakeProfitReached(double currentPrice) {
        if (isClosed) return false;
        
        if (isLong) {
            return currentPrice >= takeProfit;
        } else {
            return currentPrice <= takeProfit;
        }
    }
    
    /**
     * SL 도달 여부 확인
     */
    public boolean isStopLossReached(double currentPrice) {
        if (isClosed) return false;
        
        if (isLong) {
            return currentPrice <= stopLoss;
        } else {
            return currentPrice >= stopLoss;
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
    
    public String getSymbol() {
        return symbol;
    }
    
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
    
    public double getQuantity() {
        return quantity;
    }
    
    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }
    
    public double getEntryPrice() {
        return entryPrice;
    }
    
    public void setEntryPrice(double entryPrice) {
        this.entryPrice = entryPrice;
    }
    
    public double getTakeProfit() {
        return takeProfit;
    }
    
    public void setTakeProfit(double takeProfit) {
        this.takeProfit = takeProfit;
    }
    
    public double getStopLoss() {
        return stopLoss;
    }
    
    public void setStopLoss(double stopLoss) {
        this.stopLoss = stopLoss;
    }
    
    public boolean isLong() {
        return isLong;
    }
    
    public void setLong(boolean isLong) {
        this.isLong = isLong;
    }
    
    public Date getOpenTime() {
        return openTime;
    }
    
    public void setOpenTime(Date openTime) {
        this.openTime = openTime;
    }
    
    public Date getCloseTime() {
        return closeTime;
    }
    
    public void setCloseTime(Date closeTime) {
        this.closeTime = closeTime;
    }
    
    public boolean isClosed() {
        return isClosed;
    }
    
    public void setClosed(boolean closed) {
        isClosed = closed;
    }
    
    public double getPnl() {
        return pnl;
    }
    
    public void setPnl(double pnl) {
        this.pnl = pnl;
    }
    
    public Double getClosedPrice() {
        return closedPrice;
    }
    
    public void setClosedPrice(Double closedPrice) {
        this.closedPrice = closedPrice;
    }
}

