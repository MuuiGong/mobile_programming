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
    
    // 새로운 필드들 (프롬프트 요구사항)
    private String tradeType; // "SPOT" or "FUTURES"
    private int leverage; // 레버리지 (1x ~ 20x)
    private double riskAmount; // 위험 자금
    private String timeframe; // 타임프레임 (1H, 4H, 1D 등)
    private String exitReason; // 종료 사유 (TP_HIT, SL_HIT, MARGIN_CALL, MANUAL, TIMEOUT)
    private double maxDrawdown; // 최대 낙폭
    private double rrRatio; // R:R 비율
    private String marginMode; // 마진 모드: "ISOLATED" or "CROSS"
    
    public Position() {
        this.openTime = new Date();
        this.isClosed = false;
        this.pnl = 0.0;
        this.tradeType = "SPOT"; // 기본값: 현물
        this.leverage = 1; // 기본값: 1x
        this.riskAmount = 0.0;
        this.timeframe = "1H";
        this.maxDrawdown = 0.0;
        this.marginMode = "CROSS"; // 기본값: Cross 마진 모드
    }
    
    /**
     * 현재 가격 기반 미실현 손익 계산 (레버리지 포함)
     */
    public double calculateUnrealizedPnL(double currentPrice) {
        if (isClosed) {
            return pnl;
        }
        
        double priceDiff = isLong ? 
            (currentPrice - entryPrice) : 
            (entryPrice - currentPrice);
        
        // 레버리지 적용
        return priceDiff * quantity * leverage;
    }
    
    /**
     * 미실현 손익 퍼센트 계산
     */
    public double calculateUnrealizedPnLPercent(double currentPrice) {
        if (riskAmount <= 0) return 0.0;
        double unrealizedPnL = calculateUnrealizedPnL(currentPrice);
        return (unrealizedPnL / riskAmount) * 100.0;
    }
    
    /**
     * TP까지 남은 거리 계산
     */
    public double calculateRemainingToTP(double currentPrice) {
        if (isLong) {
            return takeProfit - currentPrice;
        } else {
            return currentPrice - takeProfit;
        }
    }
    
    /**
     * SL까지 남은 거리 계산
     */
    public double calculateRemainingToSL(double currentPrice) {
        if (isLong) {
            return currentPrice - stopLoss;
        } else {
            return stopLoss - currentPrice;
        }
    }
    
    /**
     * 수익도 계산 (TP까지 남은 거리 비율)
     */
    public double calculateProfitRatio(double currentPrice) {
        double remainingToTP = calculateRemainingToTP(currentPrice);
        double totalToTP = isLong ? 
            (takeProfit - entryPrice) : 
            (entryPrice - takeProfit);
        
        if (totalToTP <= 0) return 0.0;
        return Math.max(0.0, Math.min(1.0, 1.0 - (remainingToTP / totalToTP)));
    }
    
    /**
     * 손실도 계산 (SL까지 남은 거리 비율)
     */
    public double calculateLossRatio(double currentPrice) {
        double remainingToSL = calculateRemainingToSL(currentPrice);
        double totalToSL = isLong ? 
            (entryPrice - stopLoss) : 
            (stopLoss - entryPrice);
        
        if (totalToSL <= 0) return 0.0;
        return Math.max(0.0, Math.min(1.0, remainingToSL / totalToSL));
    }
    
    /**
     * 포지션 가치 계산
     */
    public double calculatePositionValue(double currentPrice) {
        return quantity * currentPrice;
    }
    
    /**
     * 사용 마진 계산 (선물 거래만)
     */
    public double calculateUsedMargin() {
        if ("SPOT".equals(tradeType)) {
            return calculatePositionValue(entryPrice); // 현물은 전체 자금 사용
        } else {
            return calculatePositionValue(entryPrice) / leverage; // 선물은 레버리지로 나눔
        }
    }
    
    /**
     * 가용 마진 계산
     */
    public double calculateAvailableMargin(double currentPrice, double totalBalance) {
        double usedMargin = calculateUsedMargin();
        double unrealizedPnL = calculateUnrealizedPnL(currentPrice);
        return totalBalance - usedMargin + unrealizedPnL;
    }
    
    /**
     * 마진 비율 계산
     */
    public double calculateMarginRatio(double currentPrice, double totalBalance) {
        double usedMargin = calculateUsedMargin();
        double unrealizedPnL = calculateUnrealizedPnL(currentPrice);
        double totalMargin = usedMargin + unrealizedPnL;
        
        if (usedMargin <= 0) return 0.0;
        return (totalMargin / usedMargin) * 100.0;
    }
    
    /**
     * 마진콜 여부 확인
     */
    public boolean isMarginCall(double currentPrice, double totalBalance) {
        if ("SPOT".equals(tradeType)) {
            return false; // 현물 거래는 마진콜 없음
        }
        
        double marginRatio = calculateMarginRatio(currentPrice, totalBalance);
        return marginRatio <= 50.0; // 마진 50% 이하
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
    
    // 새로운 필드들의 Getter/Setter
    public String getTradeType() {
        return tradeType;
    }
    
    public void setTradeType(String tradeType) {
        this.tradeType = tradeType;
    }
    
    public int getLeverage() {
        return leverage;
    }
    
    public void setLeverage(int leverage) {
        this.leverage = leverage;
    }
    
    public double getRiskAmount() {
        return riskAmount;
    }
    
    public void setRiskAmount(double riskAmount) {
        this.riskAmount = riskAmount;
    }
    
    public String getTimeframe() {
        return timeframe;
    }
    
    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe;
    }
    
    public String getExitReason() {
        return exitReason;
    }
    
    public void setExitReason(String exitReason) {
        this.exitReason = exitReason;
    }
    
    public double getMaxDrawdown() {
        return maxDrawdown;
    }
    
    public void setMaxDrawdown(double maxDrawdown) {
        this.maxDrawdown = maxDrawdown;
    }
    
    public double getRrRatio() {
        return rrRatio;
    }
    
    public void setRrRatio(double rrRatio) {
        this.rrRatio = rrRatio;
    }
    
    public String getMarginMode() {
        return marginMode;
    }
    
    public void setMarginMode(String marginMode) {
        this.marginMode = marginMode;
    }
}

