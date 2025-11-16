package com.example.rsquare.domain;

import android.content.Context;
import android.util.Log;

import com.example.rsquare.data.local.entity.Position;
import com.example.rsquare.data.local.entity.UserSettings;
import com.example.rsquare.data.repository.TradingRepository;
import com.example.rsquare.data.repository.UserRepository;
import com.example.rsquare.data.repository.UserSettingsRepository;

/**
 * 거래 실행 엔진
 * 프롬프트의 거래 생성 및 실행 로직 구현
 */
public class TradeExecutor {
    
    private static final String TAG = "TradeExecutor";
    
    private final Context context;
    private final TradingRepository tradingRepository;
    private final UserRepository userRepository;
    private final UserSettingsRepository settingsRepository;
    
    public TradeExecutor(Context context) {
        this.context = context;
        this.tradingRepository = new TradingRepository(context);
        this.userRepository = new UserRepository(context);
        this.settingsRepository = new UserSettingsRepository(context);
    }
    
    /**
     * 거래 실행 (프롬프트 요구사항 반영)
     * 
     * @param userId 사용자 ID
     * @param symbol 거래 기호
     * @param entryPrice 진입 가격
     * @param tpPrice TP 가격
     * @param slPrice SL 가격
     * @param isLong 롱 포지션 여부
     * @param leverage 레버리지 (선물 거래만)
     * @param listener 실행 결과 리스너
     */
    public void executeTrade(long userId, String symbol, double entryPrice, 
                             double tpPrice, double slPrice, boolean isLong, 
                             Integer leverage, OnTradeExecutedListener listener) {
        
        // 사용자 및 설정 조회
        com.example.rsquare.data.local.entity.User user = userRepository.getUserSync(userId);
        if (user == null) {
            Log.e(TAG, "User not found: " + userId);
            if (listener != null) {
                listener.onError("사용자를 찾을 수 없습니다");
            }
            return;
        }
        
        UserSettings settings = settingsRepository.getSettingsSync(userId);
        if (settings == null) {
            // 기본 설정 생성
            settings = new UserSettings();
            settings.setUserId(userId);
            settingsRepository.saveSettingsSync(settings);
        }
        
        // 거래 유형 결정
        String tradeType = settings.getTradeMode();
        int initialLeverage = leverage != null ? leverage : settings.getDefaultLeverage();
        
        // final 변수로 복사 (내부 클래스에서 사용하기 위해)
        final int actualLeverage = "SPOT".equals(tradeType) ? 1 : initialLeverage;
        final String finalTradeType = tradeType;
        final long finalUserId = userId;
        final double finalEntryPrice = entryPrice;
        
        // 위험 자금 계산
        double riskAmount = TradeCalculator.calculateRiskAmount(settings, user.getBalance());
        
        // 거래 검증
        int activePositionsCount = tradingRepository.getActivePositionsSync(userId).size();
        double dailyLoss = calculateDailyLoss(userId);
        
        TradeValidator.ValidationResult validation = TradeValidator.validateTrade(
            entryPrice, tpPrice, slPrice, riskAmount, actualLeverage, isLong,
            settings, activePositionsCount, user.getBalance(), dailyLoss
        );
        
        if (!validation.isValid()) {
            Log.w(TAG, "Trade validation failed: " + validation.getErrors());
            if (listener != null) {
                listener.onError(String.join("\n", validation.getErrors()));
            }
            return;
        }
        
        // 경고 표시
        if (!validation.getWarnings().isEmpty() && listener != null) {
            listener.onWarning(String.join("\n", validation.getWarnings()));
        }
        
        // 거래 크기 계산
        final TradeCalculator.TradeCalculationResult calculation = TradeCalculator.calculateTrade(
            entryPrice, tpPrice, slPrice, riskAmount, finalTradeType, actualLeverage, isLong
        );
        
        // 포지션 생성
        Position position = new Position();
        position.setUserId(userId);
        position.setSymbol(symbol);
        position.setEntryPrice(entryPrice);
        position.setTakeProfit(tpPrice);
        position.setStopLoss(slPrice);
        position.setQuantity(calculation.getTradeSize());
        position.setLong(isLong);
        position.setTradeType(finalTradeType);
        position.setLeverage(actualLeverage);
        position.setRiskAmount(riskAmount);
        position.setTimeframe(settings.getDefaultTimeframe());
        position.setRrRatio(calculation.getRrRatio());
        
        // 포지션 열기
        tradingRepository.openPosition(position, new TradingRepository.OnPositionOpenedListener() {
            @Override
            public void onPositionOpened(long positionId) {
                Log.d(TAG, "Position opened: " + positionId);
                
                // 잔고 차감 (현물은 전체, 선물은 마진만)
                double balanceToDeduct = "SPOT".equals(finalTradeType) ?
                    (calculation.getTradeSize() * finalEntryPrice) :
                    (calculation.getTradeSize() * finalEntryPrice / actualLeverage);
                
                userRepository.addToBalance(finalUserId, -balanceToDeduct);
                
                if (listener != null) {
                    listener.onSuccess(positionId, calculation);
                }
            }
        });
    }
    
    /**
     * 일일 손실 계산
     */
    private double calculateDailyLoss(long userId) {
        java.util.Calendar today = java.util.Calendar.getInstance();
        today.set(java.util.Calendar.HOUR_OF_DAY, 0);
        today.set(java.util.Calendar.MINUTE, 0);
        today.set(java.util.Calendar.SECOND, 0);
        today.set(java.util.Calendar.MILLISECOND, 0);
        
        java.util.Date startOfDay = today.getTime();
        
        com.example.rsquare.data.local.AppDatabase db = 
            com.example.rsquare.data.local.AppDatabase.getInstance(context);
        
        java.util.List<com.example.rsquare.data.local.entity.TradeHistory> todayTrades = 
            db.tradeHistoryDao().getTradesByDateRangeSync(userId, startOfDay, new java.util.Date());
        
        double totalLoss = 0.0;
        for (com.example.rsquare.data.local.entity.TradeHistory trade : todayTrades) {
            if (trade.getPnl() < 0) {
                totalLoss += Math.abs(trade.getPnl());
            }
        }
        
        return totalLoss;
    }
    
    /**
     * 수동 포지션 종료
     */
    public void closePositionManually(long positionId, double currentPrice, OnPositionClosedListener listener) {
        tradingRepository.closePosition(positionId, currentPrice, 
            com.example.rsquare.data.local.entity.TradeHistory.TradeType.CLOSE_SL,
            new TradingRepository.OnPositionClosedListener() {
                @Override
                public void onPositionClosed(double pnl) {
                    if (listener != null) {
                        listener.onClosed(pnl);
                    }
                }
            });
    }
    
    public interface OnTradeExecutedListener {
        void onSuccess(long positionId, TradeCalculator.TradeCalculationResult calculation);
        void onError(String error);
        void onWarning(String warning);
    }
    
    public interface OnPositionClosedListener {
        void onClosed(double pnl);
    }
}

