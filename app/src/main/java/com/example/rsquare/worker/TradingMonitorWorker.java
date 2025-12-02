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
        
        if (coinId == null) {
            Log.w(TAG, "Could not convert symbol to coinId: " + position.getSymbol());
            return;
        }
        
        // 캐시된 가격 먼저 확인
        CoinPrice cachedPrice = marketDataRepository.getCachedPrice(coinId);
        if (cachedPrice != null && cachedPrice.getCurrentPrice() > 0) {
            double currentPrice = cachedPrice.getCurrentPrice();
            
            Log.d(TAG, "Checking position " + position.getId() + 
                ", Symbol: " + position.getSymbol() + 
                ", CoinId: " + coinId + 
                ", CurrentPrice: " + currentPrice +
                ", TP: " + position.getTakeProfit() +
                ", SL: " + position.getStopLoss());
            
            // PENDING 상태인 경우 진입 조건 체크
            if ("PENDING".equals(position.getStatus())) {
                checkPendingOrder(position, currentPrice);
            } else {
                // ACTIVE 상태인 경우 청산/TP/SL 체크
                evaluatePosition(position, currentPrice);
            }
        } else {
            // 캐시에 없으면 로그만 남기고 스킵 (다음 실행 시 다시 시도)
            Log.d(TAG, "Price not in cache for " + coinId + " (symbol: " + position.getSymbol() + ")");
        }
    }
    
    /**
     * 대기 주문 체크 (Limit/Stop)
     */
    private void checkPendingOrder(Position position, double currentPrice) {
        boolean triggered = false;
        double entryPrice = position.getEntryPrice();
        
        if (position.isLong()) {
            // 롱 포지션
            // 1. Limit Buy: 현재가가 진입가보다 낮았다가 진입가 이하로 내려오면 체결? 
            //    보통 Limit Buy는 현재가보다 낮은 가격에 걸어두고, 가격이 내려와서 닿으면 체결.
            //    Stop Buy는 현재가보다 높은 가격에 걸어두고, 가격이 올라와서 닿으면 체결.
            
            // 여기서는 단순하게 "가격이 진입가에 도달하거나 더 유리해지면" 체결로 간주
            // 하지만 PENDING으로 설정될 때의 가격을 모르므로, 
            // "현재 가격이 진입가보다 낮거나 같으면" (Limit Buy 가정) 체결?
            // 아니면 "현재 가격이 진입가보다 높거나 같으면" (Stop Buy 가정) 체결?
            
            // TradeExecutor에서 PENDING으로 설정된 로직:
            // Math.abs(entryPrice - currentPrice) / currentPrice > 0.001
            
            // 사용자의 의도를 정확히 알 수 없으므로, "진입가에 도달"하면 체결로 단순화
            // 즉, 진입가와의 차이가 매우 작거나, 
            // Limit Buy (Low): Current <= Entry
            // Stop Buy (High): Current >= Entry
            
            // 하지만 우리는 주문 당시의 가격을 모름.
            // 따라서 단순히 "진입가 근처에 도달"했는지만 체크하거나,
            // 아니면 더 정교하게 "교차"를 체크해야 함 (이전 가격 필요).
            
            // 여기서는 Worker가 주기적으로 돌므로 "교차"를 놓칠 수 있음.
            // 따라서 "현재 가격이 진입가 조건을 만족"하는지로 판단.
            
            // 문제: Limit Buy인지 Stop Buy인지 구분할 필드가 없음.
            // 해결: Position에 orderType 필드가 없으므로, 
            // 일단은 "현재 가격이 진입가와 매우 가까우면" (0.1% 이내) 체결로 처리?
            // 아니면, 사용자가 입력한 EP가 현재가보다 낮으면 Limit, 높으면 Stop으로 가정했어야 함.
            
            // TradeExecutor에서 status를 PENDING으로 설정할 때 orderType도 저장했으면 좋았겠지만,
            // 지금은 "진입가에 도달했다"는 것을 "현재가와 진입가의 차이가 0.1% 이내"인 경우로 한정하거나,
            // 아니면 단순히 "지나쳤으면" 체결로 봐야 함.
            
            // 개선된 로직:
            // PENDING 상태일 때, 
            // 1. 만약 현재가가 진입가보다 낮다면 (Limit Buy 대기 중이었거나, 이미 지나침) -> 체결 (더 싸게 사는 건 좋음)
            // 2. 만약 현재가가 진입가보다 높다면 (Stop Buy 대기 중이었거나, 아직 안 옴) -> 
            //    이건 모호함. Limit Buy라면 아직 안 온 거고, Stop Buy라면 이미 지난 거임.
            
            // 안전한 접근: "현재 가격이 진입가와 0.1% 이내로 근접하면 체결"
            double diffPercent = Math.abs(currentPrice - entryPrice) / entryPrice;
            if (diffPercent <= 0.001) {
                triggered = true;
            }
            
        } else {
            // 숏 포지션
            // Limit Sell (High): Current >= Entry -> 체결 (더 비싸게 파는 건 좋음)
            // Stop Sell (Low): Current <= Entry
            
            // 마찬가지로 "현재 가격이 진입가와 0.1% 이내로 근접하면 체결"
            double diffPercent = Math.abs(currentPrice - entryPrice) / entryPrice;
            if (diffPercent <= 0.001) {
                triggered = true;
            }
        }
        
        if (triggered) {
            Log.d(TAG, "Pending order triggered! Position " + position.getId() + 
                ", Entry: " + entryPrice + ", Current: " + currentPrice);
            
            activatePosition(position);
        }
    }
    
    /**
     * 포지션 활성화
     */
    private void activatePosition(Position position) {
        position.setStatus("ACTIVE");
        position.setOpenTime(new Date()); // 체결 시간으로 업데이트
        
        tradingRepository.updatePositionSync(position);
        
        notificationHelper.notifyOrderFilled(position);
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
            tradingRepository.closePositionSync(position.getId(), currentPrice, TradeHistory.TradeType.CLOSE_TP, "TP_HIT");
            notificationHelper.notifyTPReached(position);
            return;
        }
        
        // 2. SL 도달 체크
        if (position.isStopLossReached(currentPrice)) {
            Log.d(TAG, "Stop loss reached for position " + position.getId());
            tradingRepository.closePositionSync(position.getId(), currentPrice, TradeHistory.TradeType.CLOSE_SL, "SL_HIT");
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
                tradingRepository.closePositionSync(position.getId(), currentPrice, TradeHistory.TradeType.CLOSE_SL, "MARGIN_CALL_LIQUIDATION");
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
                tradingRepository.closePositionSync(position.getId(), currentPrice, TradeHistory.TradeType.CLOSE_SL, "TIMEOUT");
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
            tradingRepository.closePositionSync(pos.getId(), currentPrice, TradeHistory.TradeType.CLOSE_SL, reason);
        }
    }
    
    /**
     * 포지션 청산 (프롬프트 요구사항 반영)
     */

    
    /**
     * 심볼을 CoinGecko ID로 변환 (MainActivity와 동일한 매핑)
     */
    private String getCoinIdFromSymbol(String symbol) {
        // Binance 심볼 -> CoinGecko ID 매핑
        java.util.Map<String, String> symbolToCoinId = new java.util.HashMap<>();
        symbolToCoinId.put("BTCUSDT", "bitcoin");
        symbolToCoinId.put("ETHUSDT", "ethereum");
        symbolToCoinId.put("ADAUSDT", "cardano");
        symbolToCoinId.put("SOLUSDT", "solana");
        symbolToCoinId.put("XRPUSDT", "ripple");
        symbolToCoinId.put("DOTUSDT", "polkadot");
        symbolToCoinId.put("DOGEUSDT", "dogecoin");
        symbolToCoinId.put("AVAXUSDT", "avalanche-2");
        
        String coinId = symbolToCoinId.get(symbol);
        if (coinId != null) {
            return coinId;
        }
        
        // 매핑에 없으면 기존 로직 사용
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
                Log.w(TAG, "Unknown symbol: " + symbol + ", using lowercase as coinId");
                return symbol.toLowerCase();
        }
    }
}

