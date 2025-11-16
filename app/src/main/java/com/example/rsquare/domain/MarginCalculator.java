package com.example.rsquare.domain;

/**
 * 마진 계산 엔진
 * 레버리지 거래의 마진, 청산 가격 등을 계산
 */
public class MarginCalculator {
    
    private static final double DEFAULT_TAKER_FEE = 0.0004; // 0.04% (Binance 기준)
    
    /**
     * 필요 증거금 계산
     * 
     * @param positionSize 거래 규모 (진입가 × 수량)
     * @param leverage 레버리지
     * @return 필요 증거금
     */
    public static double calculateRequiredMargin(double positionSize, double leverage) {
        if (leverage <= 0) {
            throw new IllegalArgumentException("레버리지는 0보다 커야 합니다");
        }
        return positionSize / leverage;
    }
    
    /**
     * 사용 마진 계산
     * 
     * @param entryPrice 진입 가격
     * @param tradeSize 거래 수량
     * @param leverage 레버리지
     * @param takerFee 테이커 수수료 (기본값 사용)
     * @return 사용 마진
     */
    public static double calculateUsedMargin(double entryPrice, double tradeSize, 
                                             double leverage, double takerFee) {
        double positionSize = entryPrice * tradeSize;
        double tradingFee = positionSize * takerFee;
        double requiredMargin = calculateRequiredMargin(positionSize, leverage);
        return requiredMargin + tradingFee;
    }
    
    /**
     * 사용 마진 계산 (기본 수수료 사용)
     */
    public static double calculateUsedMargin(double entryPrice, double tradeSize, double leverage) {
        return calculateUsedMargin(entryPrice, tradeSize, leverage, DEFAULT_TAKER_FEE);
    }
    
    /**
     * 가용 마진 계산
     * 
     * @param totalMargin 총 마진 (증거금)
     * @param usedMargin 사용 중인 마진
     * @param currentPnL 현재 미실현 손익
     * @return 가용 마진
     */
    public static double calculateAvailableMargin(double totalMargin, double usedMargin, 
                                                  double currentPnL) {
        return totalMargin + currentPnL - usedMargin;
    }
    
    /**
     * 마진 비율 계산 (%)
     * 
     * @param availableMargin 가용 마진
     * @param usedMargin 사용 마진
     * @return 마진 비율 (%)
     */
    public static double calculateMarginRatio(double availableMargin, double usedMargin) {
        if (usedMargin <= 0) {
            return 100.0;
        }
        return (availableMargin / usedMargin) * 100.0;
    }
    
    /**
     * 청산 가격 계산
     * 
     * @param entryPrice 진입 가격
     * @param tradeSize 거래 수량
     * @param leverage 레버리지
     * @param totalMargin 총 마진 (증거금)
     * @param isLongPosition 롱 포지션 여부
     * @return 청산 가격
     */
    public static double calculateLiquidationPrice(double entryPrice, double tradeSize,
                                                  double leverage, double totalMargin,
                                                  boolean isLongPosition) {
        if (tradeSize <= 0 || leverage <= 0) {
            return entryPrice;
        }
        
        // 청산 = 마진이 완전히 소진되는 가격
        // 롱 포지션 청산가 = Entry - (totalMargin / (tradeSize * leverage))
        // 숏 포지션 청산가 = Entry + (totalMargin / (tradeSize * leverage))
        
        double pnlPerUnit = totalMargin / (tradeSize * leverage);
        
        if (isLongPosition) {
            return entryPrice - pnlPerUnit;
        } else {
            return entryPrice + pnlPerUnit;
        }
    }
    
    /**
     * 마진콜 발생 여부 확인
     * 
     * @param marginRatio 마진 비율 (%)
     * @return 마진콜 발생 여부
     */
    public static boolean isMarginCall(double marginRatio) {
        return marginRatio <= 50.0; // 마진 50% 이하
    }
    
    /**
     * 자동 청산 여부 확인
     * 
     * @param marginRatio 마진 비율 (%)
     * @return 자동 청산 필요 여부
     */
    public static boolean shouldLiquidate(double marginRatio) {
        return marginRatio <= 0.0; // 마진 0% 이하
    }
    
    /**
     * 마진 상태 레벨 반환
     * 
     * @param marginRatio 마진 비율 (%)
     * @return 마진 상태 레벨
     */
    public static MarginStatus getMarginStatus(double marginRatio) {
        if (marginRatio <= 0) {
            return MarginStatus.LIQUIDATED;
        } else if (marginRatio <= 20) {
            return MarginStatus.CRITICAL;
        } else if (marginRatio <= 50) {
            return MarginStatus.WARNING;
        } else if (marginRatio <= 100) {
            return MarginStatus.CAUTION;
        } else {
            return MarginStatus.NORMAL;
        }
    }
    
    /**
     * 마진 상태 열거형
     */
    public enum MarginStatus {
        NORMAL("정상", 100.0),
        CAUTION("주의", 50.0),
        WARNING("경고", 20.0),
        CRITICAL("위험", 0.0),
        LIQUIDATED("청산", -1.0);
        
        private final String label;
        private final double threshold;
        
        MarginStatus(String label, double threshold) {
            this.label = label;
            this.threshold = threshold;
        }
        
        public String getLabel() {
            return label;
        }
        
        public double getThreshold() {
            return threshold;
        }
    }
}

