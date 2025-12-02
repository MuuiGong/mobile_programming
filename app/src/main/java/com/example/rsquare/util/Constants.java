package com.example.rsquare.util;

/**
 * 앱 전역 상수
 */
public class Constants {
    
    // API
    public static final String COINGECKO_BASE_URL = "https://api.coingecko.com/api/v3/";
    
    // Database
    public static final String DATABASE_NAME = "rsquare_database";
    
    // Initial Balance
    public static final double INITIAL_BALANCE = 100000.0; // 가상 잔고 10만
    
    // Risk Score Weights
    public static final double WEIGHT_VOLATILITY = 0.4;
    public static final double WEIGHT_MDD = 0.4;
    public static final double WEIGHT_SHARPE = 0.2;
    
    // Monte Carlo
    public static final int DEFAULT_SIMULATION_ITERATIONS = 100;
    
    // Trading
    public static final double MIN_RISK_REWARD_RATIO = 0.5;
    public static final double RECOMMENDED_RR_RATIO = 2.0;
    
    // Behavior Analysis Thresholds
    public static final int OVERTRADING_THRESHOLD = 10; // 일일 거래 수
    public static final long REVENGE_TRADING_WINDOW = 60 * 60 * 1000; // 1시간
    public static final double LOSS_AVERSION_THRESHOLD = 0.5; // 손실 후 거래 크기 50% 감소
    
    // Challenge Types
    public static final String CHALLENGE_TYPE_RR = "risk_reward";
    public static final String CHALLENGE_TYPE_WIN_STREAK = "win_streak";
    public static final String CHALLENGE_TYPE_CONSISTENT = "consistent_trading";
    public static final String CHALLENGE_TYPE_EMOTION = "emotion_control";
    
    // Notification IDs
    public static final int NOTIFICATION_ID_TP_REACHED = 1001;
    public static final int NOTIFICATION_ID_SL_REACHED = 1002;
    public static final int NOTIFICATION_ID_RISK_WARNING = 1003;
    public static final int NOTIFICATION_ID_CHALLENGE_COMPLETE = 1004;
    public static final int NOTIFICATION_ID_DAILY_REMINDER = 1005;
    public static final int NOTIFICATION_ID_ORDER_FILLED = 1006;
    
    // WorkManager
    public static final String WORK_TRADING_MONITOR = "trading_monitor_work";
    public static final long TRADING_MONITOR_INTERVAL_MINUTES = 15;
}

