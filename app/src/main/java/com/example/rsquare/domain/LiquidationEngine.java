package com.example.rsquare.domain;

import android.util.Log;

import com.example.rsquare.data.local.entity.Position;
import com.example.rsquare.data.local.entity.TradeHistory;
import com.example.rsquare.data.repository.TradingRepository;
import com.example.rsquare.data.repository.UserRepository;

/**
 * 자동 청산 엔진
 * 마진 부족 시 포지션을 자동으로 청산
 */
public class LiquidationEngine {
    
    private static final String TAG = "LiquidationEngine";
    
    private final TradingRepository tradingRepository;
    private final UserRepository userRepository;
    private LiquidationListener listener;
    
    public interface LiquidationListener {
        void onLiquidationStart(Position position, String reason);
        void onLiquidationComplete(Position position, double liquidationPrice, double pnl);
        void onLiquidationFailed(Position position, Exception error);
    }
    
    public LiquidationEngine(TradingRepository tradingRepository, UserRepository userRepository) {
        this.tradingRepository = tradingRepository;
        this.userRepository = userRepository;
    }
    
    /**
     * 자동 청산 실행
     * 
     * @param position 청산할 포지션
     * @param currentPrice 현재 가격
     * @param reason 청산 사유
     */
    public void executeLiquidation(Position position, double currentPrice, String reason) {
        try {
            // 1. 청산 시작 알림
            if (listener != null) {
                listener.onLiquidationStart(position, reason);
            }
            
            // 2. 청산 가격 계산 (현재 가격 또는 청산 가격 중 더 나쁜 쪽)
            double usedMargin = MarginCalculator.calculateUsedMargin(
                position.getEntryPrice(),
                position.getQuantity(),
                position.getLeverage()
            );
            
            double liquidationPrice = MarginCalculator.calculateLiquidationPrice(
                position.getEntryPrice(),
                position.getQuantity(),
                position.getLeverage(),
                usedMargin,
                position.isLong()
            );
            
            // 롱 포지션: 더 낮은 가격, 숏 포지션: 더 높은 가격
            double executionPrice = position.isLong() ?
                Math.min(currentPrice, liquidationPrice) :
                Math.max(currentPrice, liquidationPrice);
            
            // 3. P&L 계산 (손실)
            double liquidationPnL = position.calculateUnrealizedPnL(executionPrice);
            
            // 4. 포지션 종료
            position.setClosed(true);
            position.setCloseTime(new java.util.Date());
            position.setClosedPrice(executionPrice);
            position.setExitReason(reason);
            position.setPnl(liquidationPnL);
            
            // DB 업데이트
            tradingRepository.updatePositionSync(position);
            
            // 5. 거래 히스토리 기록
            TradeHistory tradeHistory = new TradeHistory();
            tradeHistory.setPositionId(position.getId());
            tradeHistory.setSymbol(position.getSymbol());
            tradeHistory.setType(TradeHistory.TradeType.CLOSE_SL);
            tradeHistory.setPrice(executionPrice);
            tradeHistory.setQuantity(position.getQuantity());
            tradeHistory.setPnl(liquidationPnL);
            tradeHistory.setTimestamp(new java.util.Date());
            tradingRepository.insertTradeHistorySync(tradeHistory);
            
            // 6. 잔고 업데이트
            userRepository.addToBalance(position.getUserId(), liquidationPnL);
            
            Log.d(TAG, String.format(
                "Liquidation completed: Position %d, Price: %.2f, PnL: %.2f, Reason: %s",
                position.getId(), executionPrice, liquidationPnL, reason
            ));
            
            // 7. 청산 완료 알림
            if (listener != null) {
                listener.onLiquidationComplete(position, executionPrice, liquidationPnL);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Liquidation failed for position " + position.getId(), e);
            if (listener != null) {
                listener.onLiquidationFailed(position, e);
            }
        }
    }
    
    public void setListener(LiquidationListener listener) {
        this.listener = listener;
    }
}

