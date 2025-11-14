package com.example.rsquare.domain;

import java.util.List;

/**
 * Monte Carlo 시뮬레이션 결과 클래스
 */
public class SimulationResult {
    
    private double expectedReturn;      // 기대 수익
    private double maxProfit;           // 최대 수익
    private double maxLoss;             // 최대 손실
    private double sharpeRatio;         // Sharpe Ratio
    private double winRate;             // 승률
    private List<Double> distribution;  // 손익 분포
    private List<Double> cumulativeReturns; // 누적 수익률
    private int iterations;             // 시뮬레이션 반복 횟수
    
    // 확률 구간
    private double percentile25;        // 25% 분위수
    private double percentile50;        // 50% 분위수 (중앙값)
    private double percentile75;        // 75% 분위수
    
    public SimulationResult() {
    }
    
    // Getters and Setters
    public double getExpectedReturn() {
        return expectedReturn;
    }
    
    public void setExpectedReturn(double expectedReturn) {
        this.expectedReturn = expectedReturn;
    }
    
    public double getMaxProfit() {
        return maxProfit;
    }
    
    public void setMaxProfit(double maxProfit) {
        this.maxProfit = maxProfit;
    }
    
    public double getMaxLoss() {
        return maxLoss;
    }
    
    public void setMaxLoss(double maxLoss) {
        this.maxLoss = maxLoss;
    }
    
    public double getSharpeRatio() {
        return sharpeRatio;
    }
    
    public void setSharpeRatio(double sharpeRatio) {
        this.sharpeRatio = sharpeRatio;
    }
    
    public double getWinRate() {
        return winRate;
    }
    
    public void setWinRate(double winRate) {
        this.winRate = winRate;
    }
    
    public List<Double> getDistribution() {
        return distribution;
    }
    
    public void setDistribution(List<Double> distribution) {
        this.distribution = distribution;
    }
    
    public List<Double> getCumulativeReturns() {
        return cumulativeReturns;
    }
    
    public void setCumulativeReturns(List<Double> cumulativeReturns) {
        this.cumulativeReturns = cumulativeReturns;
    }
    
    public int getIterations() {
        return iterations;
    }
    
    public void setIterations(int iterations) {
        this.iterations = iterations;
    }
    
    public double getPercentile25() {
        return percentile25;
    }
    
    public void setPercentile25(double percentile25) {
        this.percentile25 = percentile25;
    }
    
    public double getPercentile50() {
        return percentile50;
    }
    
    public void setPercentile50(double percentile50) {
        this.percentile50 = percentile50;
    }
    
    public double getPercentile75() {
        return percentile75;
    }
    
    public void setPercentile75(double percentile75) {
        this.percentile75 = percentile75;
    }
}

