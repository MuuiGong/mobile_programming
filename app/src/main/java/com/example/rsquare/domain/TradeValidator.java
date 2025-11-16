package com.example.rsquare.domain;

import com.example.rsquare.data.local.entity.Position;
import com.example.rsquare.data.local.entity.UserSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * 거래 검증 시스템
 * 프롬프트의 모든 검증 로직 구현
 */
public class TradeValidator {
    
    /**
     * 거래 생성 전 검증
     * 
     * @param entryPrice 진입 가격
     * @param tpPrice TP 가격
     * @param slPrice SL 가격
     * @param riskAmount 위험 자금
     * @param leverage 레버리지
     * @param isLong 롱 포지션 여부
     * @param settings 사용자 설정
     * @param activePositionsCount 현재 활성 포지션 수
     * @param currentBalance 현재 잔고
     * @param dailyLoss 오늘의 손실
     * @return 검증 결과
     */
    public static ValidationResult validateTrade(
            double entryPrice, double tpPrice, double slPrice,
            double riskAmount, int leverage, boolean isLong,
            UserSettings settings, int activePositionsCount,
            double currentBalance, double dailyLoss) {
        
        List<String> errors = new ArrayList<>();
        
        // 기본 검증
        if (tpPrice <= entryPrice && isLong) {
            errors.add("TP가 Entry보다 낮습니다");
        }
        
        if (slPrice >= entryPrice && isLong) {
            errors.add("SL이 Entry보다 높습니다");
        }
        
        if (tpPrice >= entryPrice && !isLong) {
            errors.add("TP가 Entry보다 높습니다 (숏 포지션)");
        }
        
        if (slPrice <= entryPrice && !isLong) {
            errors.add("SL이 Entry보다 낮습니다 (숏 포지션)");
        }
        
        // 거리 검증
        double tpDistance = isLong ? 
            ((tpPrice - entryPrice) / entryPrice * 100) : 
            ((entryPrice - tpPrice) / entryPrice * 100);
        
        double slDistance = isLong ? 
            ((entryPrice - slPrice) / entryPrice * 100) : 
            ((slPrice - entryPrice) / entryPrice * 100);
        
        if (tpDistance < 0.1) {
            errors.add("TP 거리가 너무 작습니다 (최소 0.1%)");
        }
        
        if (slDistance < 0.1) {
            errors.add("SL 거리가 너무 작습니다 (최소 0.1%)");
        }
        
        // 위험도 검증
        double tradeSize = TradeCalculator.calculateTradeSize(
            riskAmount, entryPrice, 
            settings.getTradeMode(), leverage
        );
        
        double maxLoss = TradeCalculator.calculateMaxLoss(
            tradeSize, entryPrice, slPrice, leverage, isLong
        );
        
        double maxLossPercent = (maxLoss / currentBalance) * 100.0;
        
        if (maxLossPercent > settings.getMaxLossPerTrade()) {
            errors.add(String.format(
                "1회 거래 손실 한도 초과 (한도: %.1f%%, 예상: %.1f%%)",
                settings.getMaxLossPerTrade(), maxLossPercent
            ));
        }
        
        // 포지션 수 검증
        if (activePositionsCount >= settings.getMaxPositions()) {
            errors.add(String.format(
                "최대 포지션에 도달했습니다 (한도: %d개)",
                settings.getMaxPositions()
            ));
        }
        
        // 일일 손실 검증
        double dailyLossLimitAmount = currentBalance * (settings.getDailyLossLimit() / 100.0);
        if (dailyLoss + maxLoss > dailyLossLimitAmount) {
            errors.add(String.format(
                "일일 손실 한도 초과 예상 (한도: $%.2f, 현재 손실: $%.2f, 예상 손실: $%.2f)",
                dailyLossLimitAmount, dailyLoss, maxLoss
            ));
        }
        
        // 잔고 검증
        double requiredMargin = "FUTURES".equals(settings.getTradeMode()) ?
            (tradeSize * entryPrice / leverage) : 
            (tradeSize * entryPrice);
        
        if (requiredMargin > currentBalance) {
            errors.add(String.format(
                "잔고가 부족합니다 (필요: $%.2f, 보유: $%.2f)",
                requiredMargin, currentBalance
            ));
        }
        
        // 레버리지 검증
        if (leverage < 1 || leverage > 20) {
            errors.add("레버리지는 1x ~ 20x 사이여야 합니다");
        }
        
        // R:R 비율 경고
        double rrRatio = TradeCalculator.calculateRRRatio(entryPrice, tpPrice, slPrice, isLong);
        List<String> warnings = new ArrayList<>();
        
        if (rrRatio < 1.0) {
            warnings.add("경고: R:R 비율이 1:1 미만입니다. 위험도가 높습니다.");
        }
        
        if (maxLossPercent > currentBalance * 0.05) {
            warnings.add("경고: 1회 손실이 초기 잔고의 5%를 초과합니다.");
        }
        
        if ("FUTURES".equals(settings.getTradeMode()) && leverage > 10) {
            warnings.add("경고: 매우 높은 레버리지입니다. 신중하게 진행하세요.");
        }
        
        return new ValidationResult(
            errors.isEmpty(),
            errors,
            warnings,
            rrRatio,
            maxLoss,
            maxLossPercent
        );
    }
    
    /**
     * 검증 결과 클래스
     */
    public static class ValidationResult {
        private final boolean isValid;
        private final List<String> errors;
        private final List<String> warnings;
        private final double rrRatio;
        private final double maxLoss;
        private final double maxLossPercent;
        
        public ValidationResult(boolean isValid, List<String> errors, List<String> warnings,
                               double rrRatio, double maxLoss, double maxLossPercent) {
            this.isValid = isValid;
            this.errors = errors;
            this.warnings = warnings;
            this.rrRatio = rrRatio;
            this.maxLoss = maxLoss;
            this.maxLossPercent = maxLossPercent;
        }
        
        public boolean isValid() {
            return isValid;
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        public List<String> getWarnings() {
            return warnings;
        }
        
        public double getRrRatio() {
            return rrRatio;
        }
        
        public double getMaxLoss() {
            return maxLoss;
        }
        
        public double getMaxLossPercent() {
            return maxLossPercent;
        }
    }
}

