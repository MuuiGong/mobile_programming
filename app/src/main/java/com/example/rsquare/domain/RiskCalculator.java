package com.example.rsquare.domain;

import com.example.rsquare.data.local.entity.Position;
import com.example.rsquare.util.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * 리스크 계산 엔진
 * 
 * 리스크 스코어 공식:
 * score = 100 - (0.4 * normalizedVolatility + 0.4 * normalizedMDD + 0.2 * normalizedNegSharpe)
 */
public class RiskCalculator {
    
    /**
     * Risk-Reward 비율 계산
     * 
     * @param entryPrice 진입 가격
     * @param takeProfit 익절 가격
     * @param stopLoss 손절 가격
     * @param isLong 롱 포지션 여부
     * @return R:R 비율
     */
    public static double calculateRiskRewardRatio(double entryPrice, double takeProfit, 
                                                   double stopLoss, boolean isLong) {
        double potentialProfit;
        double potentialLoss;
        
        if (isLong) {
            potentialProfit = takeProfit - entryPrice;
            potentialLoss = entryPrice - stopLoss;
        } else {
            potentialProfit = entryPrice - takeProfit;
            potentialLoss = stopLoss - entryPrice;
        }
        
        if (potentialLoss <= 0) {
            return 0.0;
        }
        
        return potentialProfit / potentialLoss;
    }
    
    /**
     * 실시간 리스크 스코어 계산
     * 
     * @param closedPositions 종료된 포지션 목록
     * @return RiskMetrics 객체
     */
    public static RiskMetrics calculateRiskScore(List<Position> closedPositions) {
        RiskMetrics metrics = new RiskMetrics();
        
        if (closedPositions == null || closedPositions.isEmpty()) {
            // 거래 기록이 없을 때 기본값
            metrics.setVolatility(0);
            metrics.setMdd(0);
            metrics.setSharpeRatio(0);
            metrics.setRiskScore(100); // 최고 점수 (거래 없음 = 리스크 없음)
            return metrics;
        }
        
        // 손익 배열 생성
        List<Double> pnlList = new ArrayList<>();
        for (Position position : closedPositions) {
            pnlList.add(position.getPnl());
        }
        
        // 변동성 계산
        double volatility = calculateVolatility(pnlList);
        metrics.setVolatility(volatility);
        
        // MDD 계산
        double mdd = calculateMDD(pnlList);
        metrics.setMdd(mdd);
        
        // Sharpe Ratio 계산
        double sharpeRatio = calculateSharpeRatio(pnlList);
        metrics.setSharpeRatio(sharpeRatio);
        
        // 리스크 스코어 계산 및 정규화
        double riskScore = normalizeScore(volatility, mdd, sharpeRatio);
        metrics.setRiskScore(riskScore);
        
        return metrics;
    }
    
    /**
     * 변동성 계산 (표준편차 기반)
     * 
     * @param pnlList 손익 리스트
     * @return 변동성 (0-100)
     */
    public static double calculateVolatility(List<Double> pnlList) {
        if (pnlList == null || pnlList.isEmpty()) {
            return 0.0;
        }
        
        // 평균 계산
        double sum = 0;
        for (Double pnl : pnlList) {
            sum += pnl;
        }
        double mean = sum / pnlList.size();
        
        // 표준편차 계산
        double variance = 0;
        for (Double pnl : pnlList) {
            variance += Math.pow(pnl - mean, 2);
        }
        variance /= pnlList.size();
        double stdDev = Math.sqrt(variance);
        
        // 정규화 (0-100 범위)
        // 표준편차를 평균 잔고 대비 퍼센트로 변환
        double normalizedVolatility = (stdDev / Constants.INITIAL_BALANCE) * 100;
        
        // 0-100 범위로 제한
        return Math.min(100, Math.max(0, normalizedVolatility * 10));
    }
    
    /**
     * Maximum Drawdown (MDD) 계산
     * 
     * @param pnlList 손익 리스트
     * @return MDD (0-100)
     */
    public static double calculateMDD(List<Double> pnlList) {
        if (pnlList == null || pnlList.isEmpty()) {
            return 0.0;
        }
        
        double peak = Constants.INITIAL_BALANCE;
        double maxDrawdown = 0;
        double currentBalance = Constants.INITIAL_BALANCE;
        
        for (Double pnl : pnlList) {
            currentBalance += pnl;
            
            if (currentBalance > peak) {
                peak = currentBalance;
            }
            
            double drawdown = (peak - currentBalance) / peak;
            if (drawdown > maxDrawdown) {
                maxDrawdown = drawdown;
            }
        }
        
        // 퍼센트로 변환 (0-100)
        return maxDrawdown * 100;
    }
    
    /**
     * Sharpe Ratio 계산
     * 
     * @param pnlList 손익 리스트
     * @return Sharpe Ratio
     */
    public static double calculateSharpeRatio(List<Double> pnlList) {
        if (pnlList == null || pnlList.size() < 2) {
            return 0.0;
        }
        
        // 수익률 계산
        List<Double> returns = new ArrayList<>();
        double currentBalance = Constants.INITIAL_BALANCE;
        
        for (Double pnl : pnlList) {
            double returnRate = pnl / currentBalance;
            returns.add(returnRate);
            currentBalance += pnl;
        }
        
        // 평균 수익률
        double sum = 0;
        for (Double ret : returns) {
            sum += ret;
        }
        double meanReturn = sum / returns.size();
        
        // 수익률 표준편차
        double variance = 0;
        for (Double ret : returns) {
            variance += Math.pow(ret - meanReturn, 2);
        }
        variance /= returns.size();
        double stdDev = Math.sqrt(variance);
        
        // Sharpe Ratio (무위험 이자율 0으로 가정)
        if (stdDev == 0) {
            return 0.0;
        }
        
        return meanReturn / stdDev;
    }
    
    /**
     * 리스크 스코어 정규화 및 계산
     * 
     * score = 100 - (0.4 * volatility + 0.4 * MDD + 0.2 * negSharpe)
     * 
     * @param volatility 변동성 (0-100)
     * @param mdd MDD (0-100)
     * @param sharpeRatio Sharpe Ratio
     * @return 리스크 스코어 (0-100, 높을수록 안전)
     */
    public static double normalizeScore(double volatility, double mdd, double sharpeRatio) {
        // Sharpe Ratio를 0-100 범위로 정규화 (음수는 페널티)
        double normalizedSharpe = 0;
        if (sharpeRatio > 0) {
            // 좋은 Sharpe Ratio는 보너스
            normalizedSharpe = Math.max(0, 50 - (sharpeRatio * 10));
        } else {
            // 나쁜 Sharpe Ratio는 페널티
            normalizedSharpe = Math.min(100, 50 + (Math.abs(sharpeRatio) * 10));
        }
        
        // 가중치 적용
        double score = 100 - (
            Constants.WEIGHT_VOLATILITY * volatility +
            Constants.WEIGHT_MDD * mdd +
            Constants.WEIGHT_SHARPE * normalizedSharpe
        );
        
        // 0-100 범위로 제한
        return Math.min(100, Math.max(0, score));
    }
    
    /**
     * 포지션의 리스크 금액 계산
     * 
     * @param position 포지션
     * @return 리스크 금액 (손절 시 예상 손실)
     */
    public static double calculatePositionRisk(Position position) {
        if (position.isLong()) {
            return (position.getEntryPrice() - position.getStopLoss()) * position.getQuantity();
        } else {
            return (position.getStopLoss() - position.getEntryPrice()) * position.getQuantity();
        }
    }
    
    /**
     * 포지션의 잠재 보상 계산
     * 
     * @param position 포지션
     * @return 잠재 보상 (익절 시 예상 수익)
     */
    public static double calculatePositionReward(Position position) {
        if (position.isLong()) {
            return (position.getTakeProfit() - position.getEntryPrice()) * position.getQuantity();
        } else {
            return (position.getEntryPrice() - position.getTakeProfit()) * position.getQuantity();
        }
    }
    
    /**
     * 승률 계산
     * 
     * @param winCount 승리 횟수
     * @param totalCount 전체 거래 횟수
     * @return 승률 (0-100)
     */
    public static double calculateWinRate(int winCount, int totalCount) {
        if (totalCount == 0) {
            return 0.0;
        }
        return ((double) winCount / totalCount) * 100;
    }
    /**
     * 단일 포지션 리스크 스코어 계산
     * 
     * @param riskAmount 리스크 금액 (손절 시 손실액)
     * @param balance 계정 잔고
     * @param rrRatio 손익비 (R:R)
     * @return 리스크 스코어 (0-100)
     */
    public static double calculatePositionRiskScore(double riskAmount, double balance, double rrRatio) {
        if (balance <= 0) return 0;
        
        // 1. 리스크 비율 (Risk %)
        // 권장 리스크: 1~2% (100점)
        // 위험 리스크: 5% 이상 (감점)
        double riskPercent = (riskAmount / balance) * 100;
        
        // 기본 점수 100점에서 시작
        double score = 100;
        
        // 리스크 비율에 따른 감점
        // 1% 이하는 감점 없음
        // 1% ~ 5%: 1%당 10점 감점
        // 5% 이상: 1%당 20점 감점
        if (riskPercent > 1.0) {
            if (riskPercent <= 5.0) {
                score -= (riskPercent - 1.0) * 10;
            } else {
                score -= 40; // 5%까지의 감점 (4 * 10)
                score -= (riskPercent - 5.0) * 20;
            }
        }
        
        // 2. 손익비 (R:R) 보너스/페널티
        // R:R 1:2 이상: 보너스
        // R:R 1:1 미만: 페널티
        if (rrRatio >= 2.0) {
            score += (rrRatio - 2.0) * 5; // 2.0 이상일 때 0.1당 0.5점 보너스
        } else if (rrRatio < 1.0 && rrRatio > 0) {
            score -= (1.0 - rrRatio) * 20; // 1.0 미만일 때 페널티
        }
        
        // 0~100 범위 제한
        return Math.min(100, Math.max(0, score));
    }
}

