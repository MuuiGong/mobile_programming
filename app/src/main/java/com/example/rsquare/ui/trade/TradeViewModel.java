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
                
                // 잔고 업데이트
                userRepository.addToBalance(1, pnl);
                
                String message = pnl >= 0 ? 
                    "포지션 종료! 수익: " + String.format("$%.2f", pnl) :
                    "포지션 종료. 손실: " + String.format("$%.2f", pnl);
                successMessage.postValue(message);
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
                    userRepository.addToBalance(1, pnl);
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
                    userRepository.addToBalance(1, pnl);
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
        
        return true;
    }
}

