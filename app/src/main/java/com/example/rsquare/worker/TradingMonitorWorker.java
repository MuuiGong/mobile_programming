package com.example.rsquare.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.rsquare.data.local.AppDatabase;
import com.example.rsquare.data.local.entity.Position;
import com.example.rsquare.data.local.entity.TradeHistory;
import com.example.rsquare.data.local.entity.User;
import com.example.rsquare.data.local.entity.UserSettings;
import com.example.rsquare.data.remote.model.CoinPrice;
import com.example.rsquare.data.repository.MarketDataRepository;
import com.example.rsquare.data.repository.TradingRepository;
import com.example.rsquare.data.repository.UserRepository;
import com.example.rsquare.domain.MarginCalculator;
import com.example.rsquare.util.NotificationHelper;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Trading Monitor Worker
 * 주기적으로 활성 포지션을 체크하고 TP/SL 도달 시 자동 청산
 */
public class TradingMonitorWorker extends Worker {
    
    private static final String TAG = "TradingMonitorWorker";
    
    private final TradingRepository tradingRepository;
    private final MarketDataRepository marketDataRepository;
    private final UserRepository userRepository;
    private final NotificationHelper notificationHelper;
    
    public TradingMonitorWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        
        this.tradingRepository = new TradingRepository(context);
        this.marketDataRepository = new MarketDataRepository();
        this.userRepository = new UserRepository(context);
        this.notificationHelper = new NotificationHelper(context);
    }
    
    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Trading monitor worker started");
        
        try {
            // 활성 포지션 조회
            List<Position> activePositions = tradingRepository.getActivePositionsSync(1);
            
            if (activePositions.isEmpty()) {
                Log.d(TAG, "No active positions to monitor");
                return Result.success();
            }
            
            // 각 포지션의 현재 가격 확인
            for (Position position : activePositions) {
                checkPosition(position);
            }
            
            return Result.success();
            
        } catch (Exception e) {
            Log.e(TAG, "Error in trading monitor worker", e);
            return Result.retry();
        }
    }
    
    /**
     * 개별 포지션 체크
     */
    private void checkPosition(Position position) {
        String coinId = getCoinIdFromSymbol(position.getSymbol());
        
        // 캐시된 가격 먼저 확인
        CoinPrice cachedPrice = marketDataRepository.getCachedPrice(coinId);
        if (cachedPrice != null) {
            double currentPrice = cachedPrice.getCurrentPrice();
            evaluatePosition(position, currentPrice);
        } else {
            // 캐시에 없으면 API 호출 (동기적으로)
            // 실제 프로덕션에서는 더 나은 동기화 방식 필요
            Log.d(TAG, "Price not in cache for " + coinId);
        }
    }
    
    /**
     * 포지션 평가 및 자동 청산 (프롬프트 요구사항 반영)
     */
    private void evaluatePosition(Position position, double currentPrice) {
        // 사용자 및 설정 조회
        User user = userRepository.getUserSync(position.getUserId());
        if (user == null) {
            Log.e(TAG, "User not found for position " + position.getId());
            return;
        }
        
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        UserSettings settings = db.userSettingsDao().getSettingsByUserIdSync(position.getUserId());
        
        double totalBalance = user.getBalance();
        
        // 1. TP 도달 체크
        if (position.isTakeProfitReached(currentPrice)) {
            Log.d(TAG, "Take profit reached for position " + position.getId());
            closePosition(position, currentPrice, TradeHistory.TradeType.CLOSE_TP, "TP_HIT");
            notificationHelper.notifyTPReached(position);
            return;
        }
        
        // 2. SL 도달 체크
        if (position.isStopLossReached(currentPrice)) {
            Log.d(TAG, "Stop loss reached for position " + position.getId());
            closePosition(position, currentPrice, TradeHistory.TradeType.CLOSE_SL, "SL_HIT");
            notificationHelper.notifySLReached(position);
            return;
        }
        
        // 3. 마진콜 및 청산 체크 (선물 거래만)
        if ("FUTURES".equals(position.getTradeType()) && position.getLeverage() > 1) {
            String marginMode = position.getMarginMode();
            if (marginMode == null || marginMode.isEmpty()) {
                marginMode = "CROSS"; // 기본값
            }
            
            double liquidationPrice;
            double marginRatio;
            
            if ("ISOLATED".equals(marginMode)) {
                // Isolated 모드: 포지션별 마진만 고려
                double isolatedMargin = MarginCalculator.calculateIsolatedMargin(
                position.getEntryPrice(),
                position.getQuantity(),
                position.getLeverage()
            );
            
            // 미실현 손익 계산
            double unrealizedPnL = position.calculateUnrealizedPnL(currentPrice);
            
                // 가용 마진 = 포지션별 마진 + 미실현 손익
                double availableMargin = isolatedMargin + unrealizedPnL;
            
            // 마진 비율 계산
                marginRatio = MarginCalculator.calculateMarginRatio(availableMargin, isolatedMargin);
            
                // 청산 가격 계산 (Isolated 모드)
                liquidationPrice = MarginCalculator.calculateIsolatedLiquidationPrice(
                position.getEntryPrice(),
                position.getQuantity(),
                position.getLeverage(),
                    isolatedMargin,
                position.isLong()
            );
            } else {
                // Cross 모드: 모든 포지션의 마진과 손익 고려
                List<Position> allPositions = tradingRepository.getActivePositionsSync(position.getUserId());
                
                // 현재 포지션의 사용 마진
                double positionMargin = MarginCalculator.calculateUsedMargin(
                    position.getEntryPrice(),
                    position.getQuantity(),
                    position.getLeverage()
                );
                
                // 다른 포지션들의 사용 마진 합산
                double otherPositionsMargin = 0.0;
                double otherPositionsPnL = 0.0;
                for (Position pos : allPositions) {
                    if (pos.getId() != position.getId() && !pos.isClosed() && "FUTURES".equals(pos.getTradeType())) {
                        otherPositionsMargin += MarginCalculator.calculateUsedMargin(
                            pos.getEntryPrice(), pos.getQuantity(), pos.getLeverage()
                        );
                        otherPositionsPnL += pos.calculateUnrealizedPnL(currentPrice);
                    }
                }
                
                // 현재 포지션의 미실현 손익
                double currentPnL = position.calculateUnrealizedPnL(currentPrice);
                
                // 가용 마진 = 총 잔고 + 다른 포지션들의 미실현 손익 - 다른 포지션들의 마진
                double availableMargin = totalBalance + otherPositionsPnL - otherPositionsMargin;
                
                // 마진 비율 계산 (현재 포지션 기준)
                marginRatio = MarginCalculator.calculateMarginRatio(
                    availableMargin + currentPnL, positionMargin
                );
                
                // 청산 가격 계산 (Cross 모드)
                liquidationPrice = MarginCalculator.calculateCrossLiquidationPrice(
                    position, allPositions, totalBalance, currentPrice
                );
            }
            
            // 자동 청산 체크 (마진 비율 0% 이하)
            if (MarginCalculator.shouldLiquidate(marginRatio)) {
                Log.w(TAG, "Liquidation triggered! Position " + position.getId() + 
                    ", Margin ratio: " + marginRatio + "%, Mode: " + marginMode);
                closePosition(position, currentPrice, TradeHistory.TradeType.CLOSE_SL, "MARGIN_CALL_LIQUIDATION");
                notificationHelper.notifyLiquidation(position, liquidationPrice);
                return;
            }
            
            // 마진콜 경고 체크 (마진 비율 50% 이하)
            if (MarginCalculator.isMarginCall(marginRatio)) {
                Log.w(TAG, "Margin call warning! Position " + position.getId() + 
                    ", Margin ratio: " + marginRatio + "%, Mode: " + marginMode);
                notificationHelper.notifyMarginWarning(position, marginRatio);
            }
            
            // 마진 상태에 따른 경고 (20% 이하)
            MarginCalculator.MarginStatus status = MarginCalculator.getMarginStatus(marginRatio);
            if (status == MarginCalculator.MarginStatus.CRITICAL) {
                Log.w(TAG, "Critical margin status! Position " + position.getId() + 
                    ", Margin ratio: " + marginRatio + "%, Mode: " + marginMode);
                notificationHelper.notifyMarginCritical(position, marginRatio, liquidationPrice);
            }
        }
        
        // 4. 타임아웃 체크
        if (settings != null && !"UNLIMITED".equals(settings.getMaxPositionDuration())) {
            long durationMs = System.currentTimeMillis() - position.getOpenTime().getTime();
            long maxDurationMs = parseDuration(settings.getMaxPositionDuration());
            
            if (maxDurationMs > 0 && durationMs >= maxDurationMs) {
                Log.d(TAG, "Position timeout for position " + position.getId());
                closePosition(position, currentPrice, TradeHistory.TradeType.CLOSE_SL, "TIMEOUT");
                notificationHelper.notifyTimeout(position);
                return;
            }
        }
        
        // 5. 일일 손실 한도 체크 (전체 포지션에 대해)
        if (settings != null) {
            double dailyLoss = calculateDailyLoss(position.getUserId());
            double dailyLossLimit = totalBalance * (settings.getDailyLossLimit() / 100.0);
            
            if (dailyLoss >= dailyLossLimit) {
                Log.w(TAG, "Daily loss limit reached: " + dailyLoss + " / " + dailyLossLimit);
                // 모든 활성 포지션 종료
                closeAllPositions(position.getUserId(), currentPrice, "DAILY_LOSS_LIMIT");
                return;
            }
        }
    }
    
    /**
     * 일일 손실 계산
     */
    private double calculateDailyLoss(long userId) {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        
        Date startOfDay = today.getTime();
        
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        List<TradeHistory> todayTrades = db.tradeHistoryDao().getTradesByDateRangeSync(
            userId, startOfDay, new Date()
        );
        
        double totalLoss = 0.0;
        for (TradeHistory trade : todayTrades) {
            if (trade.getPnl() < 0) {
                totalLoss += Math.abs(trade.getPnl());
            }
        }
        
        return totalLoss;
    }
    
    /**
     * 지속 시간 파싱 (예: "1H", "4H", "1D")
     */
    private long parseDuration(String duration) {
        if (duration == null || "UNLIMITED".equals(duration)) {
            return 0;
        }
        
        try {
            if (duration.endsWith("H")) {
                int hours = Integer.parseInt(duration.substring(0, duration.length() - 1));
                return hours * 60 * 60 * 1000L;
            } else if (duration.endsWith("D")) {
                int days = Integer.parseInt(duration.substring(0, duration.length() - 1));
                return days * 24 * 60 * 60 * 1000L;
            } else if (duration.endsWith("M")) {
                int minutes = Integer.parseInt(duration.substring(0, duration.length() - 1));
                return minutes * 60 * 1000L;
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid duration format: " + duration);
        }
        
        return 0;
    }
    
    /**
     * 모든 활성 포지션 종료
     */
    private void closeAllPositions(long userId, double currentPrice, String reason) {
        List<Position> activePositions = tradingRepository.getActivePositionsSync(userId);
        for (Position pos : activePositions) {
            closePosition(pos, currentPrice, TradeHistory.TradeType.CLOSE_SL, reason);
        }
    }
    
    /**
     * 포지션 청산 (프롬프트 요구사항 반영)
     */
    private void closePosition(Position position, double closedPrice, TradeHistory.TradeType closeType, String exitReason) {
        // 포지션 업데이트
        position.setClosed(true);
        position.setCloseTime(new Date());
        position.setClosedPrice(closedPrice);
        position.setExitReason(exitReason);
        
        double pnl = position.calculateUnrealizedPnL(closedPrice);
        position.setPnl(pnl);
        
        // DB 업데이트 (동기)
        tradingRepository.updatePositionSync(position);
        
        // 거래 히스토리 기록
        TradeHistory tradeHistory = new TradeHistory();
        tradeHistory.setPositionId(position.getId());
        tradeHistory.setSymbol(position.getSymbol());
        tradeHistory.setType(closeType);
        tradeHistory.setPrice(closedPrice);
        tradeHistory.setQuantity(position.getQuantity());
        tradeHistory.setPnl(pnl);
        tradingRepository.insertTradeHistorySync(tradeHistory);
        
        // 잔고 업데이트
        userRepository.addToBalance(position.getUserId(), pnl);
        
        Log.d(TAG, String.format(
            "Position closed: %d, Exit: %s, PnL: %.2f, Price: %.2f",
            position.getId(), exitReason, pnl, closedPrice
        ));
    }
    
    /**
     * 심볼을 CoinGecko ID로 변환
     */
    private String getCoinIdFromSymbol(String symbol) {
        // 간단한 매핑 (실제로는 더 복잡한 매핑 필요)
        switch (symbol.toUpperCase()) {
            case "BTC":
            case "BITCOIN":
                return "bitcoin";
            case "ETH":
            case "ETHEREUM":
                return "ethereum";
            case "ADA":
            case "CARDANO":
                return "cardano";
            case "SOL":
            case "SOLANA":
                return "solana";
            case "XRP":
            case "RIPPLE":
                return "ripple";
            case "DOT":
            case "POLKADOT":
                return "polkadot";
            case "DOGE":
            case "DOGECOIN":
                return "dogecoin";
            case "AVAX":
            case "AVALANCHE":
                return "avalanche-2";
            default:
                return symbol.toLowerCase();
        }
    }
}

