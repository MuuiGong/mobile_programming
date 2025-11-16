package com.example.rsquare.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.example.rsquare.util.DateConverter;

import java.util.Date;

/**
 * 사용자 설정 엔티티
 * 초기 설정 및 거래 설정 저장
 */
@Entity(
    tableName = "user_settings",
    foreignKeys = @ForeignKey(
        entity = User.class,
        parentColumns = "id",
        childColumns = "userId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("userId")}
)
@TypeConverters(DateConverter.class)
public class UserSettings {
    
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    private long userId;
    
    // 초기 자본
    private double initialCapital;
    
    // 거래 모드
    private String tradeMode; // "SPOT" or "FUTURES"
    private int defaultLeverage; // 기본 레버리지 (1x ~ 20x)
    
    // 위험 관리 방식
    private boolean useFixedRiskAmount; // true: 고정액, false: 비율
    private double fixedRiskAmount; // 고정 위험 자금
    private double riskPercentage; // 위험 비율 (%)
    
    // 포지션 관리
    private int maxPositions; // 최대 동시 포지션 수
    private double maxLossPerTrade; // 1회 최대 손실 (%)
    private double dailyLossLimit; // 일일 최대 손실 (%)
    private String maxPositionDuration; // 포지션 최대 지속 시간 ("UNLIMITED", "1H", "4H", "1D" 등)
    
    // 거래 기호
    private String defaultSymbol; // 기본 거래 기호 (BTCUSDT)
    private String availableSymbols; // 사용 가능한 기호들 (JSON 배열 문자열)
    
    // 차트 설정
    private String defaultTimeframe; // 기본 타임프레임 (1H)
    private String chartType; // 차트 유형 (Candlestick, Line 등)
    
    // 설정 시간
    private Date createdAt;
    private Date updatedAt;
    
    public UserSettings() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
        // 기본값 설정
        this.initialCapital = 10000.0;
        this.tradeMode = "FUTURES";
        this.defaultLeverage = 1;
        this.useFixedRiskAmount = false;
        this.fixedRiskAmount = 100.0;
        this.riskPercentage = 2.0;
        this.maxPositions = 3;
        this.maxLossPerTrade = 5.0;
        this.dailyLossLimit = 10.0;
        this.maxPositionDuration = "UNLIMITED";
        this.defaultSymbol = "BTCUSDT";
        this.availableSymbols = "[\"BTCUSDT\",\"ETHUSDT\"]";
        this.defaultTimeframe = "1H";
        this.chartType = "Candlestick";
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
    
    public double getInitialCapital() {
        return initialCapital;
    }
    
    public void setInitialCapital(double initialCapital) {
        this.initialCapital = initialCapital;
    }
    
    public String getTradeMode() {
        return tradeMode;
    }
    
    public void setTradeMode(String tradeMode) {
        this.tradeMode = tradeMode;
    }
    
    public int getDefaultLeverage() {
        return defaultLeverage;
    }
    
    public void setDefaultLeverage(int defaultLeverage) {
        this.defaultLeverage = defaultLeverage;
    }
    
    public boolean isUseFixedRiskAmount() {
        return useFixedRiskAmount;
    }
    
    public void setUseFixedRiskAmount(boolean useFixedRiskAmount) {
        this.useFixedRiskAmount = useFixedRiskAmount;
    }
    
    public double getFixedRiskAmount() {
        return fixedRiskAmount;
    }
    
    public void setFixedRiskAmount(double fixedRiskAmount) {
        this.fixedRiskAmount = fixedRiskAmount;
    }
    
    public double getRiskPercentage() {
        return riskPercentage;
    }
    
    public void setRiskPercentage(double riskPercentage) {
        this.riskPercentage = riskPercentage;
    }
    
    public int getMaxPositions() {
        return maxPositions;
    }
    
    public void setMaxPositions(int maxPositions) {
        this.maxPositions = maxPositions;
    }
    
    public double getMaxLossPerTrade() {
        return maxLossPerTrade;
    }
    
    public void setMaxLossPerTrade(double maxLossPerTrade) {
        this.maxLossPerTrade = maxLossPerTrade;
    }
    
    public double getDailyLossLimit() {
        return dailyLossLimit;
    }
    
    public void setDailyLossLimit(double dailyLossLimit) {
        this.dailyLossLimit = dailyLossLimit;
    }
    
    public String getMaxPositionDuration() {
        return maxPositionDuration;
    }
    
    public void setMaxPositionDuration(String maxPositionDuration) {
        this.maxPositionDuration = maxPositionDuration;
    }
    
    public String getDefaultSymbol() {
        return defaultSymbol;
    }
    
    public void setDefaultSymbol(String defaultSymbol) {
        this.defaultSymbol = defaultSymbol;
    }
    
    public String getAvailableSymbols() {
        return availableSymbols;
    }
    
    public void setAvailableSymbols(String availableSymbols) {
        this.availableSymbols = availableSymbols;
    }
    
    public String getDefaultTimeframe() {
        return defaultTimeframe;
    }
    
    public void setDefaultTimeframe(String defaultTimeframe) {
        this.defaultTimeframe = defaultTimeframe;
    }
    
    public String getChartType() {
        return chartType;
    }
    
    public void setChartType(String chartType) {
        this.chartType = chartType;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    public Date getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    /**
     * 위험 자금 계산
     */
    public double calculateRiskAmount(double currentBalance) {
        if (useFixedRiskAmount) {
            return fixedRiskAmount;
        } else {
            return currentBalance * (riskPercentage / 100.0);
        }
    }
}

