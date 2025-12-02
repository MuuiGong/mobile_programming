package com.example.rsquare.ui.trade;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.rsquare.data.local.entity.Position;
import com.example.rsquare.data.local.entity.TradeHistory;
import com.example.rsquare.data.repository.TradingRepository;
import com.example.rsquare.data.repository.UserRepository;

import java.util.List;

/**
 * Trade ViewModel
 * 포지션 관리 및 거래 실행
 */
public class TradeViewModel extends AndroidViewModel {
    
    private final TradingRepository tradingRepository;
    private final UserRepository userRepository;
    
    private final LiveData<List<Position>> activePositions;
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    
    public TradeViewModel(@NonNull Application application) {
        super(application);
        
        tradingRepository = new TradingRepository(application);
        userRepository = new UserRepository(application);
        
        activePositions = tradingRepository.getActivePositions(1);
    }
    
    /**
     * 활성 포지션 조회
     */
    public LiveData<List<Position>> getActivePositions() {
        return activePositions;
    }
    
    /**
     * 에러 메시지
     */
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * 성공 메시지
     */
    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }
    
    /**
     * 로딩 상태
     */
    public LiveData<Boolean> getLoading() {
        return loading;
    }
    
    /**
     * 새 포지션 열기
     */
    public void openPosition(Position position) {
        loading.setValue(true);
        
        // 입력 검증
        if (!validatePosition(position)) {
            loading.setValue(false);
            return;
        }
        
        // 포지션 열기
        tradingRepository.openPosition(position, positionId -> {
            loading.postValue(false);
            successMessage.postValue("포지션이 열렸습니다! ID: " + positionId);
        });
    }
    
    /**
     * 포지션 닫기
     */
    public void closePosition(long positionId, double closedPrice) {
        loading.setValue(true);
        
        tradingRepository.closePosition(
            positionId, 
            closedPrice, 
            TradeHistory.TradeType.SELL,
            pnl -> {
                loading.postValue(false);
                

                
                String message = pnl >= 0 ? 
                    "포지션 종료! 수익: " + String.format("$%.2f", pnl) :
                    "포지션 종료. 손실: " + String.format("$%.2f", pnl);
                successMessage.postValue(message);
                
                // 목표 수익 달성 체크
                checkTargetProfit();
            }
        );
    }
    
    /**
     * 포지션 업데이트 (TP/SL 변경)
     */
    public void updatePosition(Position position) {
        if (!validatePosition(position)) {
            return;
        }
        
        tradingRepository.updatePosition(position);
        successMessage.setValue("포지션이 업데이트되었습니다");
    }
    
    /**
     * TP 도달 체크 및 자동 종료
     */
    public void checkAndCloseTakeProfit(Position position, double currentPrice) {
        if (position.isTakeProfitReached(currentPrice)) {
            tradingRepository.closePosition(
                position.getId(),
                currentPrice,
                TradeHistory.TradeType.CLOSE_TP,
                pnl -> {

                    successMessage.postValue("TP 도달! 수익 확정: " + String.format("$%.2f", pnl));
                }
            );
        }
    }
    
    /**
     * SL 도달 체크 및 자동 종료
     */
    public void checkAndCloseStopLoss(Position position, double currentPrice) {
        if (position.isStopLossReached(currentPrice)) {
            tradingRepository.closePosition(
                position.getId(),
                currentPrice,
                TradeHistory.TradeType.CLOSE_SL,
                pnl -> {

                    successMessage.postValue("SL 도달. 손실 제한: " + String.format("$%.2f", pnl));
                }
            );
        }
    }
    
    /**
     * 모든 활성 포지션 TP/SL 체크
     */
    public void checkAllPositions(double currentPrice) {
        List<Position> positions = activePositions.getValue();
        if (positions != null) {
            for (Position position : positions) {
                checkAndCloseTakeProfit(position, currentPrice);
                checkAndCloseStopLoss(position, currentPrice);
            }
        }
    }
    
    /**
     * 리스크 설정에 따른 추천 수량 계산
     */
    public double calculateRecommendedQuantity(double entryPrice, double stopLossPrice) {
        android.content.SharedPreferences prefs = getApplication().getSharedPreferences("r2_prefs", android.content.Context.MODE_PRIVATE);
        float riskPercentage = prefs.getFloat("risk_per_trade", 2.0f);
        
        // 실제 잔고 가져오기
        double accountBalance = userRepository.getUserSync(UserRepository.TEST_USER_ID).getBalance();
        
        return com.example.rsquare.domain.MarginCalculator.calculatePositionSize(
            accountBalance, riskPercentage, entryPrice, stopLossPrice
        );
    }

    /**
     * 포지션 검증
     */
    private boolean validatePosition(Position position) {
        if (position.getQuantity() <= 0) {
            errorMessage.setValue("수량은 0보다 커야 합니다");
            return false;
        }
        
        if (position.getEntryPrice() <= 0) {
            errorMessage.setValue("진입 가격이 유효하지 않습니다");
            return false;
        }
        
        if (position.getTakeProfit() <= 0) {
            errorMessage.setValue("익절 가격이 유효하지 않습니다");
            return false;
        }
        
        if (position.getStopLoss() <= 0) {
            errorMessage.setValue("손절 가격이 유효하지 않습니다");
            return false;
        }
        
        // 롱 포지션 검증
        if (position.isLong()) {
            if (position.getTakeProfit() <= position.getEntryPrice()) {
                errorMessage.setValue("롱 포지션: 익절 가격은 진입 가격보다 높아야 합니다");
                return false;
            }
            if (position.getStopLoss() >= position.getEntryPrice()) {
                errorMessage.setValue("롱 포지션: 손절 가격은 진입 가격보다 낮아야 합니다");
                return false;
            }
        } else {
            // 숏 포지션 검증
            if (position.getTakeProfit() >= position.getEntryPrice()) {
                errorMessage.setValue("숏 포지션: 익절 가격은 진입 가격보다 낮아야 합니다");
                return false;
            }
            if (position.getStopLoss() <= position.getEntryPrice()) {
                errorMessage.setValue("숏 포지션: 손절 가격은 진입 가격보다 높아야 합니다");
                return false;
            }
        }
        
        // 리스크 관리 체크
        if (!checkRiskManagement()) {
            return false;
        }
        
        return true;
    }

    /**
     * 리스크 관리 규칙 확인
     * @return true if safe to trade, false if blocked
     */
    private boolean checkRiskManagement() {
        android.content.SharedPreferences prefs = getApplication().getSharedPreferences("r2_prefs", android.content.Context.MODE_PRIVATE);
        
        // 1. 일일 손실 한도 체크
        boolean enableDailyLoss = prefs.getBoolean("enable_daily_loss_limit", false);
        if (enableDailyLoss) {
            float dailyLossLimit = prefs.getFloat("daily_loss_limit", 500f);
            if (!checkDailyLossLimit(dailyLossLimit)) {
                return false;
            }
        }
        
        // 2. 쿨다운 모드 체크
        boolean enableCooldown = prefs.getBoolean("enable_cooldown", false);
        if (enableCooldown) {
            int cooldownDuration = prefs.getInt("cooldown_duration", 60);
            if (!checkCooldown(cooldownDuration)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 일일 손실 한도 체크
     */
    private boolean checkDailyLossLimit(float limit) {
        // 백그라운드 스레드에서 실행해야 함 (임시로 메인 스레드 허용 - 실제로는 코루틴이나 RxJava 사용 권장)
        // 여기서는 간단히 Thread를 사용하여 결과를 기다리는 방식으로 구현 (비권장하지만 구조상 불가피)
        final boolean[] result = {true};
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        
        new Thread(() -> {
            try {
                // 오늘 00:00:00 구하기
                java.util.Calendar calendar = java.util.Calendar.getInstance();
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
                calendar.set(java.util.Calendar.MINUTE, 0);
                calendar.set(java.util.Calendar.SECOND, 0);
                calendar.set(java.util.Calendar.MILLISECOND, 0);
                long startTime = calendar.getTimeInMillis();
                
                // 오늘 발생한 손익 합계
                double dailyPnl = tradingRepository.getDailyPnLSync(1); // userId=1 (임시)
                
                if (dailyPnl < -limit) {
                    errorMessage.postValue("일일 손실 한도 초과! 금일 매매가 제한됩니다.");
                    result[0] = false;
                }
            } finally {
                latch.countDown();
            }
        }).start();
        
        try {
            latch.await(1, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        return result[0];
    }
    
    /**
     * 쿨다운 체크
     */
    private boolean checkCooldown(int durationMinutes) {
        final boolean[] result = {true};
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        
        new Thread(() -> {
            try {
                TradeHistory lastTrade = tradingRepository.getLastTradeSync();
                if (lastTrade != null && lastTrade.getPnl() < 0) {
                    // 마지막 거래가 손실인 경우 시간 체크
                    long lastTradeTime = lastTrade.getTimestamp().getTime();
                    long currentTime = System.currentTimeMillis();
                    long diffMinutes = (currentTime - lastTradeTime) / (60 * 1000);
                    
                    if (diffMinutes < durationMinutes) {
                        errorMessage.postValue("쿨다운 모드: " + (durationMinutes - diffMinutes) + "분 후 매매 가능");
                        result[0] = false;
                    }
                }
            } finally {
                latch.countDown();
            }
        }).start();
        
        try {
            latch.await(1, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        return result[0];
    }
    
    /**
     * 목표 수익 달성 체크 (포지션 종료 시 호출)
     */
    private void checkTargetProfit() {
        new Thread(() -> {
            android.content.SharedPreferences prefs = getApplication().getSharedPreferences("r2_prefs", android.content.Context.MODE_PRIVATE);
            boolean enableTargetProfit = prefs.getBoolean("enable_target_profit", false);
            
            if (enableTargetProfit) {
                float targetProfit = prefs.getFloat("target_profit", 1000f);
                double dailyPnl = tradingRepository.getDailyPnLSync(1); // userId=1
                
                if (dailyPnl >= targetProfit) {
                    successMessage.postValue("축하합니다! 일일 목표 수익 달성! 🚀");
                }
            }
        }).start();
    }
}

