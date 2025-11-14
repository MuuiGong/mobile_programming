package com.example.rsquare;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

/**
 * Application 클래스
 * 앱 전역 설정 및 초기화
 */
public class RSquareApplication extends Application {
    
    // Notification Channels
    public static final String CHANNEL_TRADE_ID = "trade_notifications";
    public static final String CHANNEL_COACH_ID = "coach_notifications";
    public static final String CHANNEL_REMINDER_ID = "reminder_notifications";
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // 알림 채널 생성
        createNotificationChannels();
    }
    
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            
            // 거래 알림 채널
            NotificationChannel tradeChannel = new NotificationChannel(
                CHANNEL_TRADE_ID,
                "거래 알림",
                NotificationManager.IMPORTANCE_HIGH
            );
            tradeChannel.setDescription("TP/SL 도달 및 포지션 관련 알림");
            notificationManager.createNotificationChannel(tradeChannel);
            
            // 코치 피드백 채널
            NotificationChannel coachChannel = new NotificationChannel(
                CHANNEL_COACH_ID,
                "코치 피드백",
                NotificationManager.IMPORTANCE_DEFAULT
            );
            coachChannel.setDescription("AI 코치의 행동 패턴 분석 및 피드백");
            notificationManager.createNotificationChannel(coachChannel);
            
            // 일일 리마인더 채널
            NotificationChannel reminderChannel = new NotificationChannel(
                CHANNEL_REMINDER_ID,
                "일일 리마인더",
                NotificationManager.IMPORTANCE_LOW
            );
            reminderChannel.setDescription("일일 미션 및 챌린지 알림");
            notificationManager.createNotificationChannel(reminderChannel);
        }
    }
}

