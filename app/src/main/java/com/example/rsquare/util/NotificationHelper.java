package com.example.rsquare.util;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.example.rsquare.R;
import com.example.rsquare.RSquareApplication;
import com.example.rsquare.data.local.entity.Challenge;
import com.example.rsquare.data.local.entity.Position;
import com.example.rsquare.domain.RiskMetrics;
import com.example.rsquare.ui.MainActivity;

/**
 * 알림 헬퍼 클래스
 * 앱의 모든 알림 관리
 */
public class NotificationHelper {
    
    private final Context context;
    private final NotificationManager notificationManager;
    
    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }
    
    /**
     * TP 도달 알림
     */
    public void notifyTPReached(Position position) {
        String title = "✅ 익절 달성!";
        String message = position.getSymbol() + " 포지션 TP 도달. 수익: " + 
            NumberFormatter.formatPnL(position.getPnl());
        
        sendNotification(
            Constants.NOTIFICATION_ID_TP_REACHED + (int) position.getId(),
            title,
            message,
            RSquareApplication.CHANNEL_TRADE_ID
        );
    }
    
    /**
     * SL 도달 알림
     */
    public void notifySLReached(Position position) {
        String title = "⚠️ 손절 실행";
        String message = position.getSymbol() + " 포지션 SL 도달. 손실 제한: " + 
            NumberFormatter.formatPnL(position.getPnl());
        
        sendNotification(
            Constants.NOTIFICATION_ID_SL_REACHED + (int) position.getId(),
            title,
            message,
            RSquareApplication.CHANNEL_TRADE_ID
        );
    }
    
    /**
     * 리스크 경고 알림
     */
    public void notifyRiskWarning(RiskMetrics metrics) {
        String title = "🚨 리스크 경고";
        String message = "현재 리스크 스코어: " + String.format("%.0f", metrics.getRiskScore()) + 
            " - " + metrics.getWarningLevel().getLabel();
        
        sendNotification(
            Constants.NOTIFICATION_ID_RISK_WARNING,
            title,
            message,
            RSquareApplication.CHANNEL_COACH_ID
        );
    }
    
    /**
     * 챌린지 완료 알림
     */
    public void notifyChallengeCompleted(Challenge challenge) {
        String title = "🎉 챌린지 완료!";
        String message = "'" + challenge.getTitle() + "' 챌린지를 완료했습니다!";
        
        sendNotification(
            Constants.NOTIFICATION_ID_CHALLENGE_COMPLETE + (int) challenge.getId(),
            title,
            message,
            RSquareApplication.CHANNEL_REMINDER_ID
        );
    }
    
    /**
     * 일일 리마인더 알림
     */
    public void notifyDailyReminder() {
        String title = "📊 R² 리마인더";
        String message = "오늘의 거래 계획을 확인하고 리스크를 점검하세요!";
        
        sendNotification(
            Constants.NOTIFICATION_ID_DAILY_REMINDER,
            title,
            message,
            RSquareApplication.CHANNEL_REMINDER_ID
        );
    }
    
    /**
     * 포지션 모니터링 알림
     */
    public void notifyPositionUpdate(String symbol, double currentPrice, double pnl) {
        String title = symbol + " 포지션 업데이트";
        String message = "현재 가격: " + NumberFormatter.formatPrice(currentPrice) + 
            " | 미실현 손익: " + NumberFormatter.formatPnL(pnl);
        
        sendNotification(
            Constants.NOTIFICATION_ID_TP_REACHED,
            title,
            message,
            RSquareApplication.CHANNEL_TRADE_ID
        );
    }
    /**
     * 주문 체결 알림
     */
    public void notifyOrderFilled(Position position) {
        String title = "🔔 주문 체결 완료";
        String message = position.getSymbol() + " 대기 주문이 체결되었습니다. " +
            "진입가: " + NumberFormatter.formatPrice(position.getEntryPrice());
        
        sendNotification(
            Constants.NOTIFICATION_ID_ORDER_FILLED + (int) position.getId(),
            title,
            message,
            RSquareApplication.CHANNEL_TRADE_ID
        );
    }
    
    /**
     * 마진콜 알림 (프롬프트 요구사항)
     */
    public void notifyMarginCall(Position position) {
        String title = "🚨 마진콜! 포지션 강제 종료";
        String message = position.getSymbol() + " 포지션이 마진 부족으로 강제 종료되었습니다. " +
            "손실: " + NumberFormatter.formatPnL(position.getPnl());
        
        sendNotification(
            Constants.NOTIFICATION_ID_SL_REACHED + (int) position.getId() + 1000,
            title,
            message,
            RSquareApplication.CHANNEL_TRADE_ID
        );
    }
    
    /**
     * 마진 경고 알림
     */
    public void notifyMarginWarning(Position position, double marginRatio) {
        String title = "⚠️ 마진 경고";
        String message = position.getSymbol() + " 포지션 마진 비율: " + 
            String.format("%.1f", marginRatio) + "% (50% 이하)";
        
        sendNotification(
            Constants.NOTIFICATION_ID_RISK_WARNING + (int) position.getId(),
            title,
            message,
            RSquareApplication.CHANNEL_TRADE_ID
        );
    }
    
    /**
     * 타임아웃 알림 (프롬프트 요구사항)
     */
    public void notifyTimeout(Position position) {
        String title = "⏰ 포지션 타임아웃";
        String message = position.getSymbol() + " 포지션이 최대 지속 시간을 초과하여 종료되었습니다. " +
            "손익: " + NumberFormatter.formatPnL(position.getPnl());
        
        sendNotification(
            Constants.NOTIFICATION_ID_SL_REACHED + (int) position.getId() + 2000,
            title,
            message,
            RSquareApplication.CHANNEL_TRADE_ID
        );
    }
    
    /**
     * 청산 알림
     */
    public void notifyLiquidation(Position position, double liquidationPrice) {
        String title = "💥 포지션 청산!";
        String message = position.getSymbol() + " 포지션이 마진 부족으로 청산되었습니다. " +
            "청산 가격: " + NumberFormatter.formatPrice(liquidationPrice) + 
            " | 손실: " + NumberFormatter.formatPnL(position.getPnl());
        
        sendNotification(
            Constants.NOTIFICATION_ID_SL_REACHED + (int) position.getId() + 3000,
            title,
            message,
            RSquareApplication.CHANNEL_TRADE_ID
        );
    }
    
    /**
     * 위험 마진 알림 (20% 이하)
     */
    public void notifyMarginCritical(Position position, double marginRatio, double liquidationPrice) {
        String title = "🔴 위험 마진!";
        String message = position.getSymbol() + " 포지션 마진 비율: " + 
            String.format("%.1f", marginRatio) + "% (위험 수준) " +
            "청산 가격: " + NumberFormatter.formatPrice(liquidationPrice);
        
        sendNotification(
            Constants.NOTIFICATION_ID_RISK_WARNING + (int) position.getId() + 1000,
            title,
            message,
            RSquareApplication.CHANNEL_TRADE_ID
        );
    }
    
    /**
     * 기본 알림 전송
     */
    private void sendNotification(int notificationId, String title, String message, String channelId) {
        // MainActivity로 이동하는 Intent
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // 알림 빌드
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(message));
        
        // 알림 표시
        notificationManager.notify(notificationId, builder.build());
    }
    
    /**
     * 알림 취소
     */
    public void cancelNotification(int notificationId) {
        notificationManager.cancel(notificationId);
    }
    
    /**
     * 모든 알림 취소
     */
    public void cancelAllNotifications() {
        notificationManager.cancelAll();
    }
}

