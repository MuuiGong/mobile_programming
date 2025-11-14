package com.example.rsquare.worker;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.rsquare.util.Constants;

import java.util.concurrent.TimeUnit;

/**
 * WorkManager 헬퍼 클래스
 * 백그라운드 작업 스케줄링
 */
public class WorkManagerHelper {
    
    /**
     * 트레이딩 모니터 작업 시작
     */
    public static void scheduleTradingMonitor(Context context) {
        // 작업 제약 조건
        Constraints constraints = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build();
        
        // 주기적 작업 요청
        PeriodicWorkRequest tradingMonitorWork = new PeriodicWorkRequest.Builder(
            TradingMonitorWorker.class,
            Constants.TRADING_MONITOR_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        )
        .setConstraints(constraints)
        .build();
        
        // 작업 등록 (기존 작업이 있으면 유지)
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            Constants.WORK_TRADING_MONITOR,
            ExistingPeriodicWorkPolicy.KEEP,
            tradingMonitorWork
        );
    }
    
    /**
     * 트레이딩 모니터 작업 중지
     */
    public static void cancelTradingMonitor(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(Constants.WORK_TRADING_MONITOR);
    }
    
    /**
     * 모든 작업 중지
     */
    public static void cancelAllWork(Context context) {
        WorkManager.getInstance(context).cancelAllWork();
    }
}

