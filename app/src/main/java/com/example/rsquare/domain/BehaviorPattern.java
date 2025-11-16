package com.example.rsquare.domain;

/**
 * 행동 패턴 데이터 클래스
 */
public class BehaviorPattern {
    
    private PatternType type;
    private Severity severity;
    private int frequency;
    private long lastOccurrence;
    private String description;
    private String recommendation;
    
    public enum PatternType {
        LOSS_AVERSION("손실 회피"),
        REVENGE_TRADING("복수 매매"),
        OVERTRADING("과매매"),
        IMPULSIVE_BEHAVIOR("충동적 행동"),
        POOR_RISK_MANAGEMENT("부실한 리스크 관리"),
        EMOTIONAL_TRADING("감정적 거래"),
        FOMO("놓칠까봐 조급함"),
        EARLY_EXIT("조기 청산"),
        POSITION_SIZING_ERROR("포지션 크기 오류"),
        CONSECUTIVE_LOSSES("연속 손실"),
        MOVING_STOP_LOSS("손절 변경"),
        IMPULSIVE_ENTRY("충동적 진입");
        
        private final String displayName;
        
        PatternType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public enum Severity {
        LOW("낮음", "#4CAF50"),
        MEDIUM("중간", "#FFC107"),
        HIGH("높음", "#FF9800"),
        CRITICAL("심각", "#F44336");
        
        private final String label;
        private final String color;
        
        Severity(String label, String color) {
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
    
    public BehaviorPattern() {
    }
    
    public BehaviorPattern(PatternType type, Severity severity, int frequency) {
        this.type = type;
        this.severity = severity;
        this.frequency = frequency;
        this.lastOccurrence = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public PatternType getType() {
        return type;
    }
    
    public void setType(PatternType type) {
        this.type = type;
    }
    
    public Severity getSeverity() {
        return severity;
    }
    
    public void setSeverity(Severity severity) {
        this.severity = severity;
    }
    
    public int getFrequency() {
        return frequency;
    }
    
    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }
    
    public long getLastOccurrence() {
        return lastOccurrence;
    }
    
    public void setLastOccurrence(long lastOccurrence) {
        this.lastOccurrence = lastOccurrence;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getRecommendation() {
        return recommendation;
    }
    
    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }
}

