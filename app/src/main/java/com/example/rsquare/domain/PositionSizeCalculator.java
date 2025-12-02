package com.example.rsquare.domain;

/**
 * 포지션 크기 자동 계산기
 * 리스크 비율에 따라 최적의 포지션 크기를 계산
 */
public class PositionSizeCalculator {
    
    /**
     * 계산 결과
     */
    public static class CalculationResult {
        public final double calculatedRiskAmount;  // 계산된 위험 금액
        public final double positionSize;           // 포지션 크기
        public final double requiredMargin;         // 필요 마진
        public final boolean isValid;               // 유효성
        public final String errorMessage;           // 오류 메시지
        
        public CalculationResult(double calculatedRiskAmount, double positionSize, 
                                double requiredMargin, boolean isValid, String errorMessage) {
            this.calculatedRiskAmount = calculatedRiskAmount;
            this.positionSize = positionSize;
            this.requiredMargin = requiredMargin;
            this.isValid = isValid;
            this.errorMessage = errorMessage;
        }
        
        public static CalculationResult error(String message) {
            return new CalculationResult(0, 0, 0, false, message);
        }
        
        public static CalculationResult success(double riskAmount, double positionSize, double requiredMargin) {
            return new CalculationResult(riskAmount, positionSize, requiredMargin, true, null);
        }
    }
    
    /**
     * 최적 포지션 크기 계산
     * 
     * @param balance          계좌 잔고
     * @param riskPercentage   리스크 비율 (%)
     * @param entryPrice       진입가
     * @param stopLoss         손절가
     * @param leverage         레버리지
     * @param isLong           롱 포지션 여부
     * @return 계산 결과
     */
    public static CalculationResult calculateOptimalPositionSize(
            double balance,
            double riskPercentage,
            double entryPrice,
            double stopLoss,
            int leverage,
            boolean isLong) {
        
        // 입력 검증
        if (balance <= 0) {
            return CalculationResult.error("잔고가 0보다 커야 합니다");
        }
        
        if (riskPercentage <= 0 || riskPercentage > 100) {
            return CalculationResult.error("리스크 비율은 0~100 사이여야 합니다");
        }
        
        if (entryPrice <= 0) {
            return CalculationResult.error("진입가가 0보다 커야 합니다");
        }
        
        if (stopLoss <= 0) {
            return CalculationResult.error("손절가가 0보다 커야 합니다");
        }
        
        if (leverage < 1 || leverage > 125) {
            return CalculationResult.error("레버리지는 1~125 사이여야 합니다");
        }
        
        // 롱/숏 유효성 검증
        if (isLong && stopLoss >= entryPrice) {
            return CalculationResult.error("롱 포지션: 손절가는 진입가보다 낮아야 합니다");
        }
        
        if (!isLong && stopLoss <= entryPrice) {
            return CalculationResult.error("숏 포지션: 손절가는 진입가보다 높아야 합니다");
        }
        
        // 1. 최대 위험 금액 계산
        double maxRiskAmount = balance * (riskPercentage / 100.0);
        
        // 2. SL 거리 계산 (절대값)
        double slDistance = Math.abs(entryPrice - stopLoss);
        
        // 3. SL 거리 비율 (%)
        double slDistancePercent = slDistance / entryPrice;
        
        // 4. 포지션 크기 계산
        // 포지션 크기 = 최대 위험 금액 / SL 거리
        double positionSize = maxRiskAmount / slDistance;
        
        // 5. 필요 마진 계산
        double requiredMargin = (positionSize * entryPrice) / leverage;
        
        // 6. 마진 유효성 검증
        if (requiredMargin > balance) {
            return CalculationResult.error(
                String.format("필요 마진($%.2f)이 계좌 잔고($%.2f)를 초과합니다", 
                    requiredMargin, balance)
            );
        }
        
        // 7. 최소 포지션 크기 검증 (너무 작으면 거래 불가)
        if (positionSize < 0.001) {
            return CalculationResult.error("포지션 크기가 너무 작습니다 (최소: 0.001)");
        }
        
        return CalculationResult.success(maxRiskAmount, positionSize, requiredMargin);
    }
    
    /**
     * 위험 금액만 간단히 계산
     * 
     * @param balance        계좌 잔고
     * @param riskPercentage 리스크 비율 (%)
     * @return 계산된 위험 금액
     */
    public static double calculateRiskAmount(double balance, double riskPercentage) {
        if (balance <= 0 || riskPercentage <= 0) {
            return 0.0;
        }
        return balance * (riskPercentage / 100.0);
    }
    
    /**
     * 포지션 크기 유효성 검증
     * 
     * @param positionSize 포지션 크기
     * @param entryPrice   진입가
     * @param balance      계좌 잔고
     * @param leverage     레버리지
     * @return 유효 여부
     */
    public static boolean validatePositionSize(double positionSize, double entryPrice, 
                                               double balance, int leverage) {
        if (positionSize <= 0) {
            return false;
        }
        
        double requiredMargin = (positionSize * entryPrice) / leverage;
        return requiredMargin <= balance;
    }
}
