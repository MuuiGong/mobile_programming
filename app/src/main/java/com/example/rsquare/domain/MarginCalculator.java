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
        
        double pnlPerUnit = totalMargin / tradeSize;
        
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
     * Isolated 모드: 포지션별 마진 계산
     * 각 포지션의 마진이 독립적으로 관리됨
     * 
     * @param entryPrice 진입 가격
     * @param tradeSize 거래 수량
     * @param leverage 레버리지
     * @return 포지션별 사용 마진
     */
    public static double calculateIsolatedMargin(double entryPrice, double tradeSize, double leverage) {
        return calculateUsedMargin(entryPrice, tradeSize, leverage);
    }
    
    /**
     * Cross 모드: 공유 마진 풀에서 사용 마진 계산
     * 모든 포지션의 마진을 합산
     * 
     * @param positions 활성 포지션 리스트
     * @return 총 사용 마진
     */
    public static double calculateCrossTotalMargin(java.util.List<com.example.rsquare.data.local.entity.Position> positions) {
        double totalUsedMargin = 0.0;
        for (com.example.rsquare.data.local.entity.Position pos : positions) {
            if (!pos.isClosed() && "FUTURES".equals(pos.getTradeType())) {
                totalUsedMargin += calculateUsedMargin(
                    pos.getEntryPrice(), pos.getQuantity(), pos.getLeverage()
                );
            }
        }
        return totalUsedMargin;
    }
    
    /**
     * Isolated 모드 청산 가격 계산
     * 포지션별 마진만 고려
     * 
     * @param entryPrice 진입 가격
     * @param tradeSize 거래 수량
     * @param leverage 레버리지
     * @param isolatedMargin 포지션별 마진
     * @param isLongPosition 롱 포지션 여부
     * @return 청산 가격
     */
    public static double calculateIsolatedLiquidationPrice(
        double entryPrice, double tradeSize, double leverage,
        double isolatedMargin, boolean isLongPosition) {
        return calculateLiquidationPrice(
            entryPrice, tradeSize, leverage, isolatedMargin, isLongPosition
        );
    }
    
    /**
     * Cross 모드 청산 가격 계산
     * 모든 포지션의 미실현 손익을 고려
     * 
     * @param position 현재 포지션
     * @param allPositions 모든 활성 포지션 리스트
     * @param totalBalance 총 잔고
     * @param currentPrice 현재 가격
     * @return 청산 가격
     */
    public static double calculateCrossLiquidationPrice(
        com.example.rsquare.data.local.entity.Position position,
        java.util.List<com.example.rsquare.data.local.entity.Position> allPositions,
        double totalBalance, double currentPrice) {
        
        // 현재 포지션의 사용 마진
        double positionMargin = calculateUsedMargin(
            position.getEntryPrice(), position.getQuantity(), position.getLeverage()
        );
        
        // 다른 포지션들의 사용 마진 합산
        double otherPositionsMargin = 0.0;
        double otherPositionsPnL = 0.0;
        for (com.example.rsquare.data.local.entity.Position pos : allPositions) {
            if (pos.getId() != position.getId() && !pos.isClosed() && "FUTURES".equals(pos.getTradeType())) {
                otherPositionsMargin += calculateUsedMargin(
                    pos.getEntryPrice(), pos.getQuantity(), pos.getLeverage()
                );
                otherPositionsPnL += pos.calculateUnrealizedPnL(currentPrice);
            }
        }
        
        // 가용 마진 = 총 잔고 + 다른 포지션들의 미실현 손익 - 다른 포지션들의 마진
        double availableMargin = totalBalance + otherPositionsPnL - otherPositionsMargin;
        
        // 현재 포지션이 청산되려면 가용 마진이 0 이하가 되어야 함
        // 즉, 현재 포지션의 손실이 가용 마진을 모두 소진해야 함
        double maxLoss = availableMargin;
        
        // 청산 가격 계산
        double positionSize = position.getEntryPrice() * position.getQuantity();
        double pnlPerUnit = maxLoss / position.getQuantity();
        
        if (position.isLong()) {
            return position.getEntryPrice() - pnlPerUnit;
        } else {
            return position.getEntryPrice() + pnlPerUnit;
        }
    }
    
    /**
     * 거래 전 예상 청산 가격 계산 (모드별)
     * 
     * @param entryPrice 진입 가격
     * @param riskAmount 거래 금액 (위험 자금)
     * @param leverage 레버리지
     * @param marginMode 마진 모드 ("ISOLATED" or "CROSS")
     * @param isLongPosition 롱 포지션 여부
     * @param existingPositions 기존 활성 포지션 리스트 (Cross 모드일 때 필요)
     * @param totalBalance 총 잔고 (Cross 모드일 때 필요)
     * @return 예상 청산 가격
     */
    public static double calculateEstimatedLiquidationPrice(
        double entryPrice, double riskAmount, int leverage, String marginMode,
        boolean isLongPosition, java.util.List<com.example.rsquare.data.local.entity.Position> existingPositions,
        double totalBalance) {
        
        if (riskAmount <= 0 || leverage <= 0) {
            return entryPrice;
        }
        
        // 거래 수량 계산
        double tradeSize = com.example.rsquare.domain.TradeCalculator.calculateFuturesTradeSize(
            riskAmount, leverage, entryPrice
        );
        
        if ("ISOLATED".equals(marginMode)) {
            // Isolated 모드: 포지션별 마진만 고려
            double isolatedMargin = calculateIsolatedMargin(entryPrice, tradeSize, leverage);
            return calculateIsolatedLiquidationPrice(
                entryPrice, tradeSize, leverage, isolatedMargin, isLongPosition
            );
        } else {
            // Cross 모드: 모든 포지션의 마진과 손익 고려
            // 임시 포지션 생성 (계산용)
            com.example.rsquare.data.local.entity.Position tempPosition = 
                new com.example.rsquare.data.local.entity.Position();
            tempPosition.setEntryPrice(entryPrice);
            tempPosition.setQuantity(tradeSize);
            tempPosition.setLeverage(leverage);
            tempPosition.setLong(isLongPosition);
            tempPosition.setTradeType("FUTURES");
            
            // 기존 포지션 리스트에 임시 포지션 추가
            java.util.List<com.example.rsquare.data.local.entity.Position> allPositions = 
                new java.util.ArrayList<>(existingPositions != null ? existingPositions : new java.util.ArrayList<>());
            allPositions.add(tempPosition);
            
            // Cross 모드 청산 가격 계산
            return calculateCrossLiquidationPrice(
                tempPosition, allPositions, totalBalance, entryPrice
            );
        }
    }
    
    /**
     * 리스크 기반 포지션 규모 계산
     * 
     * @param accountBalance 계좌 잔고
     * @param riskPercentage 리스크 비율 (%)
     * @param entryPrice 진입 가격
     * @param stopLossPrice 손절 가격
     * @return 추천 포지션 수량 (코인 개수)
     */
    public static double calculatePositionSize(double accountBalance, double riskPercentage, 
                                               double entryPrice, double stopLossPrice) {
        if (accountBalance <= 0 || riskPercentage <= 0 || entryPrice <= 0 || stopLossPrice <= 0) {
            return 0.0;
        }
        
        // 리스크 금액 = 잔고 * (리스크 비율 / 100)
        double riskAmount = accountBalance * (riskPercentage / 100.0);
        
        // 가격 차이 (절대값)
        double priceDiff = Math.abs(entryPrice - stopLossPrice);
        
        if (priceDiff == 0) {
            return 0.0;
        }
        
        // 포지션 수량 = 리스크 금액 / 가격 차이
        return riskAmount / priceDiff;
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

