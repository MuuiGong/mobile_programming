package com.example.rsquare.domain;

import com.example.rsquare.data.local.entity.Position;
import com.example.rsquare.data.local.entity.UserSettings;

/**
 * 거래 계산 엔진
 * 프롬프트의 모든 계산식 구현
 */
public class TradeCalculator {
    
    /**
     * 현물 거래 크기 계산
     * 
     * @param riskAmount 위험 자금
     * @param entryPrice 진입 가격
     * @return 거래 수량
     */
    public static double calculateSpotTradeSize(double riskAmount, double entryPrice) {
        if (entryPrice <= 0) return 0.0;
        return riskAmount / entryPrice;
    }
    
    /**
     * 선물 거래 크기 계산 (레버리지 포함)
     * 
     * @param riskAmount 위험 자금
     * @param leverage 레버리지 (1x ~ 20x)
     * @param entryPrice 진입 가격
     * @return 거래 수량
     */
    public static double calculateFuturesTradeSize(double riskAmount, int leverage, double entryPrice) {
        if (entryPrice <= 0 || leverage <= 0) return 0.0;
        double totalFunds = riskAmount * leverage;
        return totalFunds / entryPrice;
    }
    
    /**
     * 손익 계산 (레버리지 포함)
     * 
     * @param tradeSize 거래 수량
     * @param entryPrice 진입 가격
     * @param exitPrice 청산 가격
     * @param leverage 레버리지
     * @param isLong 롱 포지션 여부
     * @return 손익 (PnL)
     */
    public static double calculatePnL(double tradeSize, double entryPrice, double exitPrice, 
                                      int leverage, boolean isLong) {
        double priceDiff = isLong ? 
            (exitPrice - entryPrice) : 
            (entryPrice - exitPrice);
        
        return priceDiff * tradeSize * leverage;
    }
    
    /**
     * 손익 퍼센트 계산
     * 
     * @param pnl 손익
     * @param riskAmount 위험 자금
     * @return 손익 퍼센트
     */
    public static double calculatePnLPercent(double pnl, double riskAmount) {
        if (riskAmount <= 0) return 0.0;
        return (pnl / riskAmount) * 100.0;
    }
    
    /**
     * R:R 비율 계산
     * 
     * @param entryPrice 진입 가격
     * @param tpPrice TP 가격
     * @param slPrice SL 가격
     * @param isLong 롱 포지션 여부
     * @return R:R 비율
     */
    public static double calculateRRRatio(double entryPrice, double tpPrice, double slPrice, boolean isLong) {
        if (isLong) {
            double profitDistance = tpPrice - entryPrice;
            double lossDistance = entryPrice - slPrice;
            if (lossDistance <= 0) return 0.0;
            return profitDistance / lossDistance;
        } else {
            double profitDistance = entryPrice - tpPrice;
            double lossDistance = slPrice - entryPrice;
            if (lossDistance <= 0) return 0.0;
            return profitDistance / lossDistance;
        }
    }
    
    /**
     * 최대 손실 계산
     * 
     * @param tradeSize 거래 수량
     * @param entryPrice 진입 가격
     * @param slPrice SL 가격
     * @param leverage 레버리지
     * @param isLong 롱 포지션 여부
     * @return 최대 손실
     */
    public static double calculateMaxLoss(double tradeSize, double entryPrice, double slPrice, 
                                         int leverage, boolean isLong) {
        return calculatePnL(tradeSize, entryPrice, slPrice, leverage, isLong);
    }
    
    /**
     * 최대 수익 계산
     * 
     * @param tradeSize 거래 수량
     * @param entryPrice 진입 가격
     * @param tpPrice TP 가격
     * @param leverage 레버리지
     * @param isLong 롱 포지션 여부
     * @return 최대 수익
     */
    public static double calculateMaxProfit(double tradeSize, double entryPrice, double tpPrice, 
                                           int leverage, boolean isLong) {
        return calculatePnL(tradeSize, entryPrice, tpPrice, leverage, isLong);
    }
    
    /**
     * 거래 크기 계산 (거래 유형에 따라 자동 선택)
     * 
     * @param riskAmount 위험 자금
     * @param entryPrice 진입 가격
     * @param tradeType 거래 유형 ("SPOT" or "FUTURES")
     * @param leverage 레버리지 (현물일 경우 무시)
     * @return 거래 수량
     */
    public static double calculateTradeSize(double riskAmount, double entryPrice, 
                                           String tradeType, int leverage) {
        if ("SPOT".equals(tradeType)) {
            return calculateSpotTradeSize(riskAmount, entryPrice);
        } else {
            return calculateFuturesTradeSize(riskAmount, leverage, entryPrice);
        }
    }
    
    /**
     * 사용자 설정 기반 위험 자금 계산
     * 
     * @param settings 사용자 설정
     * @param currentBalance 현재 잔고
     * @return 위험 자금
     */
    public static double calculateRiskAmount(UserSettings settings, double currentBalance) {
        return settings.calculateRiskAmount(currentBalance);
    }
    
    /**
     * 포지션 생성 시 필요한 모든 값 계산
     * 
     * @param entryPrice 진입 가격
     * @param tpPrice TP 가격
     * @param slPrice SL 가격
     * @param riskAmount 위험 자금
     * @param tradeType 거래 유형
     * @param leverage 레버리지
     * @param isLong 롱 포지션 여부
     * @return 계산 결과 객체
     */
    public static TradeCalculationResult calculateTrade(
            double entryPrice, double tpPrice, double slPrice,
            double riskAmount, String tradeType, int leverage, boolean isLong) {
        
        // 거래 크기 계산
        double tradeSize = calculateTradeSize(riskAmount, entryPrice, tradeType, leverage);
        
        // R:R 비율 계산
        double rrRatio = calculateRRRatio(entryPrice, tpPrice, slPrice, isLong);
        
        // 최대 손실/수익 계산
        double maxLoss = calculateMaxLoss(tradeSize, entryPrice, slPrice, leverage, isLong);
        double maxProfit = calculateMaxProfit(tradeSize, entryPrice, tpPrice, leverage, isLong);
        
        return new TradeCalculationResult(
            tradeSize,
            rrRatio,
            maxLoss,
            maxProfit,
            riskAmount
        );
    }
    
    /**
     * 거래 계산 결과 클래스
     */
    public static class TradeCalculationResult {
        private final double tradeSize;
        private final double rrRatio;
        private final double maxLoss;
        private final double maxProfit;
        private final double riskAmount;
        
        public TradeCalculationResult(double tradeSize, double rrRatio, 
                                     double maxLoss, double maxProfit, double riskAmount) {
            this.tradeSize = tradeSize;
            this.rrRatio = rrRatio;
            this.maxLoss = maxLoss;
            this.maxProfit = maxProfit;
            this.riskAmount = riskAmount;
        }
        
        public double getTradeSize() {
            return tradeSize;
        }
        
        public double getRrRatio() {
            return rrRatio;
        }
        
        public double getMaxLoss() {
            return maxLoss;
        }
        
        public double getMaxProfit() {
            return maxProfit;
        }
        
        public double getRiskAmount() {
            return riskAmount;
        }
    }
}

