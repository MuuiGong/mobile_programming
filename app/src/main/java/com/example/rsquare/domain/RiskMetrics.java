package com.example.rsquare.domain;

/**
 * 리스크 지표 데이터 클래스
 */
public class RiskMetrics {
    
    private double volatility;          // 변동성 (0-100)
    private double mdd;                 // Maximum Drawdown (0-100)
    private double sharpeRatio;         // Sharpe Ratio
    private double riskScore;           // 종합 리스크 스코어 (0-100, 높을수록 안전)
    private WarningLevel warningLevel;  // 경고 레벨
    private long timestamp;
    
    public enum WarningLevel {
        SAFE("안전", "#4CAF50"),
        CAUTION("주의", "#FFC107"),
        WARNING("경고", "#FF9800"),
        DANGER("위험", "#F44336");
        
        private final String label;
        private final String color;
        
        WarningLevel(String label, String color) {
            this.label = label;
            this.color = color;
        }
        
        public String getLabel() {
            return label;
        }
        
        public String getColor() {
            return color;
        }
    }
    
    public RiskMetrics() {
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 리스크 스코어에 따른 경고 레벨 결정
     */
    public WarningLevel calculateWarningLevel() {
        if (riskScore >= 70) {
            return WarningLevel.SAFE;
        } else if (riskScore >= 50) {
            return WarningLevel.CAUTION;
        } else if (riskScore >= 30) {
            return WarningLevel.WARNING;
        } else {
            return WarningLevel.DANGER;
        }
    }
    
    // Getters and Setters
    public double getVolatility() {
        return volatility;
    }
    
    public void setVolatility(double volatility) {
        this.volatility = volatility;
    }
    
    public double getMdd() {
        return mdd;
    }
    
    public void setMdd(double mdd) {
        this.mdd = mdd;
    }
    
    public double getSharpeRatio() {
        return sharpeRatio;
    }
    
    public void setSharpeRatio(double sharpeRatio) {
        this.sharpeRatio = sharpeRatio;
    }
    
    public double getRiskScore() {
        return riskScore;
    }
    
    public void setRiskScore(double riskScore) {
        this.riskScore = riskScore;
        this.warningLevel = calculateWarningLevel();
    }
    
    public WarningLevel getWarningLevel() {
        return warningLevel;
    }
    
    public void setWarningLevel(WarningLevel warningLevel) {
        this.warningLevel = warningLevel;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}

