package com.example.rsquare.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.rsquare.data.local.entity.Position;
import com.example.rsquare.data.local.entity.TradeHistory;
import com.example.rsquare.data.remote.model.CoinPrice;
import com.example.rsquare.data.repository.MarketDataRepository;
import com.example.rsquare.data.repository.TradingRepository;
import com.example.rsquare.data.repository.UserRepository;
import com.example.rsquare.util.NotificationHelper;

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
     * 포지션 평가 및 자동 청산
     */
    private void evaluatePosition(Position position, double currentPrice) {
        // TP 도달 체크
        if (position.isTakeProfitReached(currentPrice)) {
            Log.d(TAG, "Take profit reached for position " + position.getId());
            closePosition(position, currentPrice, TradeHistory.TradeType.CLOSE_TP);
            notificationHelper.notifyTPReached(position);
        }
        // SL 도달 체크
        else if (position.isStopLossReached(currentPrice)) {
            Log.d(TAG, "Stop loss reached for position " + position.getId());
            closePosition(position, currentPrice, TradeHistory.TradeType.CLOSE_SL);
            notificationHelper.notifySLReached(position);
        }
    }
    
    /**
     * 포지션 청산
     */
    private void closePosition(Position position, double closedPrice, TradeHistory.TradeType closeType) {
        // 포지션 업데이트
        position.setClosed(true);
        position.setCloseTime(new java.util.Date());
        position.setClosedPrice(closedPrice);
        
        double pnl = position.calculateUnrealizedPnL(closedPrice);
        position.setPnl(pnl);
        
        // DB 업데이트 (동기)
        tradingRepository.updatePosition(position);
        
        // 거래 히스토리 기록
        TradeHistory tradeHistory = new TradeHistory();
        tradeHistory.setPositionId(position.getId());
        tradeHistory.setSymbol(position.getSymbol());
        tradeHistory.setType(closeType);
        tradeHistory.setPrice(closedPrice);
        tradeHistory.setQuantity(position.getQuantity());
        tradeHistory.setPnl(pnl);
        
        // 잔고 업데이트
        userRepository.addToBalance(position.getUserId(), pnl);
        
        Log.d(TAG, "Position closed: " + position.getId() + ", PnL: " + pnl);
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

