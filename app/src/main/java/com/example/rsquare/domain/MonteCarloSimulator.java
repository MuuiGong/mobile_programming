package com.example.rsquare.domain;

import com.example.rsquare.util.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Monte Carlo 시뮬레이션 엔진
 * 
 * R:R 비율과 승률을 기반으로 반복 거래를 시뮬레이션하여
 * 예상 결과 분포를 계산
 */
public class MonteCarloSimulator {
    
    private final Random random;
    
    public MonteCarloSimulator() {
        this.random = new Random();
    }
    
    public MonteCarloSimulator(long seed) {
        this.random = new Random(seed);
    }
    
    /**
     * Monte Carlo 시뮬레이션 실행
     * 
     * @param riskRewardRatio R:R 비율 (예: 2.0 = 2:1)
     * @param winRate 승률 (0-1, 예: 0.6 = 60%)
     * @param tradeAmount 거래당 투자 금액
     * @param numberOfTrades 거래 횟수
     * @param iterations 시뮬레이션 반복 횟수
     * @return SimulationResult
     */
    public SimulationResult simulate(double riskRewardRatio, double winRate, 
                                      double tradeAmount, int numberOfTrades, 
                                      int iterations) {
        
        List<Double> finalReturns = new ArrayList<>();
        List<List<Double>> allCumulativeReturns = new ArrayList<>();
        
        // 각 시뮬레이션 실행
        for (int i = 0; i < iterations; i++) {
            double currentBalance = Constants.INITIAL_BALANCE;
            List<Double> cumulativeReturns = new ArrayList<>();
            cumulativeReturns.add(0.0); // 시작점
            
            // 거래 시뮬레이션
            for (int trade = 0; trade < numberOfTrades; trade++) {
                double outcome = simulateTrade(riskRewardRatio, winRate, tradeAmount);
                currentBalance += outcome;
                
                // 누적 수익률 계산
                double returnRate = ((currentBalance - Constants.INITIAL_BALANCE) 
                                    / Constants.INITIAL_BALANCE) * 100;
                cumulativeReturns.add(returnRate);
                
                // 파산 방지
                if (currentBalance <= 0) {
                    currentBalance = 0;
                    break;
                }
            }
            
            // 최종 수익 저장
            double finalReturn = currentBalance - Constants.INITIAL_BALANCE;
            finalReturns.add(finalReturn);
            allCumulativeReturns.add(cumulativeReturns);
        }
        
        // 결과 분석
        return analyzeResults(finalReturns, allCumulativeReturns, iterations);
    }
    
    /**
     * 단일 거래 시뮬레이션
     * 
     * @param riskRewardRatio R:R 비율
     * @param winRate 승률
     * @param tradeAmount 거래 금액
     * @return 거래 결과 (손익)
     */
    private double simulateTrade(double riskRewardRatio, double winRate, double tradeAmount) {
        double randomValue = random.nextDouble();
        
        if (randomValue < winRate) {
            // 승리: 투자 금액 * R:R 비율
            return tradeAmount * riskRewardRatio;
        } else {
            // 손실: -투자 금액
            return -tradeAmount;
        }
    }
    
    /**
     * 시뮬레이션 결과 분석
     * 
     * @param finalReturns 최종 수익 목록
     * @param allCumulativeReturns 모든 누적 수익률
     * @param iterations 반복 횟수
     * @return SimulationResult
     */
    private SimulationResult analyzeResults(List<Double> finalReturns, 
                                            List<List<Double>> allCumulativeReturns,
                                            int iterations) {
        
        SimulationResult result = new SimulationResult();
        result.setIterations(iterations);
        
        // 기대 수익 (평균)
        double sum = 0;
        for (Double ret : finalReturns) {
            sum += ret;
        }
        double expectedReturn = sum / iterations;
        result.setExpectedReturn(expectedReturn);
        
        // 최대/최소
        double maxProfit = Collections.max(finalReturns);
        double maxLoss = Collections.min(finalReturns);
        result.setMaxProfit(maxProfit);
        result.setMaxLoss(maxLoss);
        
        // 승률 계산 (양수 수익 비율)
        int winCount = 0;
        for (Double ret : finalReturns) {
            if (ret > 0) winCount++;
        }
        double overallWinRate = ((double) winCount / iterations) * 100;
        result.setWinRate(overallWinRate);
        
        // Sharpe Ratio 계산
        double sharpeRatio = calculateSharpeRatio(finalReturns, expectedReturn);
        result.setSharpeRatio(sharpeRatio);
        
        // 분포 저장 (정렬)
        List<Double> sortedReturns = new ArrayList<>(finalReturns);
        Collections.sort(sortedReturns);
        result.setDistribution(sortedReturns);
        
        // 백분위수 계산
        result.setPercentile25(getPercentile(sortedReturns, 0.25));
        result.setPercentile50(getPercentile(sortedReturns, 0.50));
        result.setPercentile75(getPercentile(sortedReturns, 0.75));
        
        // 평균 누적 수익률 계산 (Probability Cone 용)
        List<Double> avgCumulativeReturns = calculateAverageCumulative(allCumulativeReturns);
        result.setCumulativeReturns(avgCumulativeReturns);
        
        return result;
    }
    
    /**
     * Sharpe Ratio 계산
     */
    private double calculateSharpeRatio(List<Double> returns, double meanReturn) {
        if (returns.size() < 2) return 0.0;
        
        // 표준편차 계산
        double variance = 0;
        for (Double ret : returns) {
            variance += Math.pow(ret - meanReturn, 2);
        }
        variance /= returns.size();
        double stdDev = Math.sqrt(variance);
        
        if (stdDev == 0) return 0.0;
        
        // Sharpe Ratio (무위험 이자율 0 가정)
        return meanReturn / stdDev;
    }
    
    /**
     * 백분위수 계산
     */
    private double getPercentile(List<Double> sortedValues, double percentile) {
        if (sortedValues.isEmpty()) return 0.0;
        
        int index = (int) Math.ceil(percentile * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        
        return sortedValues.get(index);
    }
    
    /**
     * 평균 누적 수익률 계산
     */
    private List<Double> calculateAverageCumulative(List<List<Double>> allCumulativeReturns) {
        if (allCumulativeReturns.isEmpty()) return new ArrayList<>();
        
        int maxLength = 0;
        for (List<Double> returns : allCumulativeReturns) {
            maxLength = Math.max(maxLength, returns.size());
        }
        
        List<Double> avgReturns = new ArrayList<>();
        
        for (int i = 0; i < maxLength; i++) {
            double sum = 0;
            int count = 0;
            
            for (List<Double> returns : allCumulativeReturns) {
                if (i < returns.size()) {
                    sum += returns.get(i);
                    count++;
                }
            }
            
            avgReturns.add(count > 0 ? sum / count : 0);
        }
        
        return avgReturns;
    }
    
    /**
     * 간단한 시뮬레이션 (기본 파라미터)
     * 
     * @param riskRewardRatio R:R 비율
     * @param winRate 승률 (0-1)
     * @return SimulationResult
     */
    public SimulationResult simulate(double riskRewardRatio, double winRate) {
        return simulate(
            riskRewardRatio, 
            winRate, 
            1000.0,  // 거래당 1000 투자
            100,     // 100회 거래
            Constants.DEFAULT_SIMULATION_ITERATIONS
        );
    }
}

