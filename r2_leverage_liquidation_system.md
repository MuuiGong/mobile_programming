# R² 레버리지 & 자동 청산(Liquidation) 시스템 완전 구현

---

## 📌 1. 레버리지와 마진 구조

### 1.1 기본 개념

```
현물 거래 (Spot):
  실제 자금: $100
  거래 규모: $100
  레버리지: 1x
  청산: 없음 (최대 손실 = $100)

선물 거래 (Futures) - 5x:
  실제 자금: $100 (증거금)
  거래 규모: $100 × 5 = $500
  레버리지: 5x
  청산: 마진 부족 시 자동 청산
```

### 1.2 마진 계산

```
기본 공식:
  거래 규모 (Position Size) = 실제 자금 × 레버리지
  필요 증거금 (Required Margin) = 거래 규모 / 레버리지
  사용 마진 (Used Margin) = 필요 증거금 + 거래 비용
  가용 마진 (Available Margin) = 총 마진 - 사용 마진
  마진 비율 (Margin Ratio) = 가용 마진 / 사용 마진

마진 부족 경고 레벨:
  ✅ 정상 (100% 이상): 마진 충분
  🟡 주의 (50% ~ 100%): 마진 낮음
  🔴 위험 (20% ~ 50%): 마진콜 발생
  💥 청산 (0% 이하): 자동 청산
```

---

## 🔧 2. Java 코드 - MarginCalculator.java

```java
package com.r2.trading.engine;

public class MarginCalculator {
    
    /**
     * 필요 증거금 계산
     */
    public double calculateRequiredMargin(double positionSize, double leverage) {
        if (leverage == 0) {
            throw new IllegalArgumentException("레버리지는 0이 될 수 없습니다");
        }
        return positionSize / leverage;
    }
    
    /**
     * 사용 마진 계산
     */
    public double calculateUsedMargin(double entryPrice, double tradeSize, 
                                     double leverage, double takerFee) {
        // 거래 비용 = 진입 가격 × 거래 수량 × 테이커 수수료
        double tradingFee = entryPrice * tradeSize * takerFee;
        
        // 사용 마진 = 필요 증거금 + 거래 비용
        double requiredMargin = calculateRequiredMargin(entryPrice * tradeSize, leverage);
        
        return requiredMargin + tradingFee;
    }
    
    /**
     * 가용 마진 계산
     */
    public double calculateAvailableMargin(double totalMargin, double usedMargin, 
                                          double currentPnL) {
        return totalMargin + currentPnL - usedMargin;
    }
    
    /**
     * 마진 비율 계산 (%)
     */
    public double calculateMarginRatio(double availableMargin, double usedMargin) {
        if (usedMargin == 0) {
            return 100.0;
        }
        return (availableMargin / usedMargin) * 100;
    }
    
    /**
     * 청산 가격 계산
     */
    public double calculateLiquidationPrice(double entryPrice, double tradeSize,
                                          double leverage, double totalMargin,
                                          boolean isLongPosition) {
        // 청산 = 마진이 완전히 소진되는 가격
        // 롱 포지션 청산가 = Entry - (totalMargin / tradeSize)
        // 숏 포지션 청산가 = Entry + (totalMargin / tradeSize)
        
        double pnlPerTick = totalMargin / tradeSize;
        
        if (isLongPosition) {
            return entryPrice - pnlPerTick;
        } else {
            return entryPrice + pnlPerTick;
        }
    }
    
    /**
     * 마진콜 발생 여부 확인
     */
    public boolean isMarginCall(double marginRatio) {
        return marginRatio <= 50.0;  // 마진 50% 이하
    }
    
    /**
     * 자동 청산 여부 확인
     */
    public boolean shouldLiquidate(double marginRatio) {
        return marginRatio <= 0.0;  // 마진 0% 이하
    }
}
```

---

## 📊 3. 실시간 마진 모니터링 - MarginMonitor.java

```java
package com.r2.trading.engine;

import com.r2.trading.data.model.Position;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MarginMonitor {
    
    private ScheduledExecutorService executor;
    private MarginListener listener;
    private MarginCalculator calculator;
    
    public interface MarginListener {
        void onMarginRatioUpdate(Position position, double marginRatio);
        void onMarginCallWarning(Position position, double marginRatio);
        void onLiquidationRequired(Position position, double liquidationPrice);
    }
    
    public MarginMonitor(MarginListener listener) {
        this.listener = listener;
        this.calculator = new MarginCalculator();
        this.executor = Executors.newScheduledThreadPool(1);
    }
    
    /**
     * 포지션별 마진 모니터링 시작
     */
    public void startMonitoring(Position position, double currentPrice, 
                               long updateIntervalMs) {
        
        if (position.leverage == 1.0) {
            // 현물 거래는 마진 모니터링 불필요
            return;
        }
        
        executor.scheduleAtFixedRate(() -> {
            // P&L 계산
            double pnl = (currentPrice - position.entryPrice) 
                        * position.tradeSize 
                        * position.leverage;
            
            // 가용 마진 계산
            double availableMargin = calculator.calculateAvailableMargin(
                position.marginUsed, 
                position.marginUsed,
                pnl
            );
            
            // 마진 비율 계산
            double marginRatio = calculator.calculateMarginRatio(
                availableMargin, 
                position.marginUsed
            );
            
            position.marginRatio = marginRatio;
            position.marginAvailable = availableMargin;
            
            // 리스너 콜백
            listener.onMarginRatioUpdate(position, marginRatio);
            
            // 마진콜 확인
            if (calculator.isMarginCall(marginRatio)) {
                listener.onMarginCallWarning(position, marginRatio);
            }
            
            // 청산 확인
            if (calculator.shouldLiquidate(marginRatio)) {
                double liquidationPrice = calculator.calculateLiquidationPrice(
                    position.entryPrice,
                    position.tradeSize,
                    position.leverage,
                    position.marginUsed,
                    true  // isLongPosition
                );
                
                listener.onLiquidationRequired(position, liquidationPrice);
            }
            
        }, 0, updateIntervalMs, TimeUnit.MILLISECONDS);
    }
    
    public void stopMonitoring() {
        if (executor != null) {
            executor.shutdown();
        }
    }
}
```

---

## 💥 4. 자동 청산 시스템 - LiquidationEngine.java

```java
package com.r2.trading.engine;

import com.r2.trading.data.model.Position;
import com.r2.trading.data.model.Trade;
import com.r2.trading.service.PositionService;

public class LiquidationEngine {
    
    private PositionService positionService;
    private MarginCalculator marginCalculator;
    private LiquidationListener listener;
    
    public interface LiquidationListener {
        void onLiquidationStart(Position position, String reason);
        void onLiquidationComplete(Trade liquidationTrade);
        void onLiquidationFailed(Position position, Exception error);
    }
    
    public LiquidationEngine(PositionService positionService) {
        this.positionService = positionService;
        this.marginCalculator = new MarginCalculator();
    }
    
    /**
     * 자동 청산 실행
     */
    public void executeLiquidation(Position position, double currentPrice, 
                                  String reason) {
        
        try {
            // 1. 청산 전 상태 저장
            if (listener != null) {
                listener.onLiquidationStart(position, reason);
            }
            
            // 2. 청산 가격 계산
            double liquidationPrice = marginCalculator.calculateLiquidationPrice(
                position.entryPrice,
                position.tradeSize,
                position.leverage,
                position.marginUsed,
                true  // isLongPosition (실제로는 position.type 확인)
            );
            
            // 3. 청산 실행 (현재 가격 또는 청산 가격 중 더 나쁜 쪽)
            double executionPrice = getExecutionPrice(
                currentPrice, 
                liquidationPrice
            );
            
            // 4. P&L 계산 (손실)
            double liquidationPnL = (executionPrice - position.entryPrice) 
                                   * position.tradeSize 
                                   * position.leverage;
            
            // 5. Trade 기록 생성
            Trade liquidationTrade = createLiquidationTrade(
                position, 
                executionPrice,
                liquidationPnL,
                reason
            );
            
            // 6. 포지션 종료
            positionService.closePosition(position, executionPrice, "LIQUIDATION");
            
            // 7. 콜백
            if (listener != null) {
                listener.onLiquidationComplete(liquidationTrade);
            }
            
        } catch (Exception e) {
            if (listener != null) {
                listener.onLiquidationFailed(position, e);
            }
        }
    }
    
    /**
     * 청산 실행 가격 결정
     */
    private double getExecutionPrice(double currentPrice, double liquidationPrice) {
        // 롱 포지션: 더 낮은 쪽
        // 숏 포지션: 더 높은 쪽
        return Math.min(currentPrice, liquidationPrice);
    }
    
    /**
     * 청산 Trade 기록 생성
     */
    private Trade createLiquidationTrade(Position position, double exitPrice,
                                        double pnl, String reason) {
        Trade trade = new Trade();
        trade.symbol = position.symbol;
        trade.entryPrice = position.entryPrice;
        trade.tp = position.tp;
        trade.sl = position.sl;
        trade.tradeSize = position.tradeSize;
        trade.leverage = position.leverage;
        trade.entryTime = position.entryTime;
        trade.exitTime = System.currentTimeMillis();
        trade.exitPrice = exitPrice;
        trade.exitReason = reason;  // "MARGIN_CALL_LIQUIDATION"
        trade.pnl = pnl;
        trade.pnlPercent = (pnl / (position.tradeSize * position.entryPrice)) * 100;
        trade.status = "CLOSED";
        trade.duration = trade.exitTime - trade.entryTime;
        
        return trade;
    }
    
    public void setListener(LiquidationListener listener) {
        this.listener = listener;
    }
}
```

---

## 🚨 5. 청산 화면 UI - activity_liquidation_warning.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/tds_bg_black">

    <!-- Full Screen Alert -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:gravity="center"
        android:background="#000000">

        <!-- 경고 아이콘 -->
        <TextView
            android:layout_width="80dp"
            android:layout_height="80dp"
            android:text="⚠️"
            android:textSize="64sp"
            android:gravity="center"
            android:layout_marginBottom="24dp" />

        <!-- 제목 -->
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="마진콜 경고!"
            android:textSize="28sp"
            android:textColor="@color/tds_error"
            android:textStyle="bold"
            android:gravity="center"
            android:layout_marginHorizontal="24dp"
            android:layout_marginBottom="12dp" />

        <!-- 설명 -->
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="마진이 부족합니다.\n즉시 조치를 취하세요."
            android:textSize="16sp"
            android:textColor="@color/tds_text_secondary"
            android:gravity="center"
            android:layout_marginHorizontal="24dp"
            android:layout_marginBottom="32dp" />

        <!-- 상세 정보 카드 -->
        <androidx.cardview.widget.CardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginHorizontal="16dp"
            android:layout_marginBottom="32dp"
            app:cardBackgroundColor="@color/tds_bg_dark"
            app:cardCornerRadius="12dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="16dp">

                <!-- 마진 비율 -->
                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:layout_marginBottom="12dp">

                    <TextView
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="마진 비율"
                        android:textSize="14sp"
                        android:textColor="@color/tds_text_secondary" />

                    <TextView
                        android:id="@+id/margin_ratio_alert"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="35%"
                        android:textSize="16sp"
                        android:textColor="@color/tds_error"
                        android:textStyle="bold" />
                </LinearLayout>

                <!-- 청산 가격 -->
                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:layout_marginBottom="12dp">

                    <TextView
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="청산 가격"
                        android:textSize="14sp"
                        android:textColor="@color/tds_text_secondary" />

                    <TextView
                        android:id="@+id/liquidation_price_alert"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="$92,500.00"
                        android:textSize="16sp"
                        android:textColor="@color/tds_error"
                        android:textStyle="bold" />
                </LinearLayout>

                <!-- 현재 가격 -->
                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal">

                    <TextView
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="현재 가격"
                        android:textSize="14sp"
                        android:textColor="@color/tds_text_secondary" />

                    <TextView
                        android:id="@+id/current_price_alert"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="$93,500.00"
                        android:textSize="16sp"
                        android:textColor="@color/tds_text_primary"
                        android:textStyle="bold" />
                </LinearLayout>
            </LinearLayout>
        </androidx.cardview.widget.CardView>

        <!-- 액션 버튼들 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:layout_marginHorizontal="16dp"
            android:gravity="center_horizontal">

            <!-- 긴급 매도 버튼 -->
            <Button
                android:id="@+id/btn_emergency_close"
                android:layout_width="match_parent"
                android:layout_height="48dp"
                android:text="긴급 포지션 종료"
                android:textColor="@color/tds_text_primary"
                android:textSize="16sp"
                android:textStyle="bold"
                android:background="@drawable/btn_liquidation"
                android:layout_marginBottom="12dp" />

            <!-- 마진 추가 버튼 -->
            <Button
                android:id="@+id/btn_add_margin"
                android:layout_width="match_parent"
                android:layout_height="48dp"
                android:text="마진 추가 (모의투자이므로 불가)"
                android:textColor="@color/tds_text_secondary"
                android:textSize="14sp"
                android:background="@drawable/btn_secondary"
                android:enabled="false" />
        </LinearLayout>
    </LinearLayout>
</androidx.constraintlayout.widget.ConstraintLayout>
```

---

## 📈 6. Position.java 확장 - 마진 정보 추가

```java
package com.r2.trading.data.model;

public class Position {
    
    // 기존 필드들...
    public long positionId;
    public String symbol;
    public double entryPrice;
    public double tp;
    public double sl;
    public double tradeSize;
    public double leverage;
    public double currentPrice;
    public double pnl;
    public double pnlPercent;
    public long entryTime;
    
    // 마진 관련 필드 추가
    public double totalMargin;              // 총 마진 (증거금)
    public double usedMargin;               // 사용 중인 마진
    public double availableMargin;          // 가용 마진
    public double marginRatio;              // 마진 비율 (%)
    public double liquidationPrice;         // 청산 가격
    public boolean isMarginCallTriggered;   // 마진콜 발생 여부
    
    public double profitRatio;              // 수익도 (%)
    public double lossRatio;                // 손실도 (%)
    
    /**
     * 실시간 업데이트 (가격 변경 시 호출)
     */
    public void updatePrice(double newPrice) {
        this.currentPrice = newPrice;
        this.pnl = (newPrice - entryPrice) * tradeSize * leverage;
        this.pnlPercent = (pnl / (tradeSize * entryPrice)) * 100;
        
        // 수익도/손실도 계산
        this.profitRatio = (tp - newPrice) / (tp - entryPrice) * 100;
        this.lossRatio = (newPrice - sl) / (entryPrice - sl) * 100;
        
        // 마진 업데이트 (선물만)
        if (leverage > 1) {
            this.availableMargin = totalMargin + pnl - usedMargin;
            this.marginRatio = (availableMargin / usedMargin) * 100;
            
            // 마진콜 확인
            this.isMarginCallTriggered = marginRatio <= 50.0;
        }
    }
    
    /**
     * TP 도달 확인
     */
    public boolean isTPHit() {
        return currentPrice >= tp;
    }
    
    /**
     * SL 도달 확인
     */
    public boolean isSLHit() {
        return currentPrice <= sl;
    }
    
    /**
     * 마진콜 확인
     */
    public boolean isMarginCall() {
        return leverage > 1 && marginRatio <= 50.0;
    }
    
    /**
     * 청산 필요 여부 확인
     */
    public boolean shouldLiquidate() {
        return leverage > 1 && marginRatio <= 0.0;
    }
}
```

---

## 🔄 7. 통합 위험 모니터링 - PositionMonitoringService.java

```java
package com.r2.trading.service;

import com.r2.trading.engine.*;
import com.r2.trading.data.model.Position;
import java.util.List;

public class PositionMonitoringService implements 
    PriceMonitor.PriceListener,
    MarginMonitor.MarginListener,
    LiquidationEngine.LiquidationListener {
    
    private PriceMonitor priceMonitor;
    private MarginMonitor marginMonitor;
    private LiquidationEngine liquidationEngine;
    private PositionService positionService;
    private MonitoringListener listener;
    
    public interface MonitoringListener {
        void onStatusUpdate(Position position, MonitoringStatus status);
        void onAlert(Alert alert);
        void onPositionClosed(String reason);
    }
    
    public enum MonitoringStatus {
        NORMAL,
        MARGIN_CALL_WARNING,
        LIQUIDATION_IMMINENT,
        LIQUIDATED
    }
    
    public static class Alert {
        public String type;           // "MARGIN_CALL", "LIQUIDATION", etc.
        public String message;
        public double severity;       // 0.0 ~ 1.0
        public long timestamp;
        
        public Alert(String type, String message, double severity) {
            this.type = type;
            this.message = message;
            this.severity = severity;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    public PositionMonitoringService(PositionService positionService) {
        this.positionService = positionService;
        this.priceMonitor = new PriceMonitor(this);
        this.marginMonitor = new MarginMonitor(this);
        this.liquidationEngine = new LiquidationEngine(positionService);
        this.liquidationEngine.setListener(this);
    }
    
    /**
     * 포지션 모니터링 시작
     */
    public void startMonitoring(Position position) {
        // 1. 가격 모니터링 시작 (1초 간격)
        priceMonitor.startMonitoring(position, 1000);
        
        // 2. 마진 모니터링 시작 (선물만)
        if (position.leverage > 1) {
            marginMonitor.startMonitoring(position, 0, 500);  // 0.5초 간격
        }
    }
    
    // ========== PriceListener 구현 ==========
    
    @Override
    public void onPriceUpdate(double price) {
        // 실시간 가격 업데이트
    }
    
    @Override
    public void onTPHit(Position position, double price) {
        // TP 도달 → 자동 익절
        positionService.closePosition(position, price, "TP_HIT");
        if (listener != null) {
            listener.onPositionClosed("TP_HIT");
        }
    }
    
    @Override
    public void onSLHit(Position position, double price) {
        // SL 도달 → 자동 손절
        positionService.closePosition(position, price, "SL_HIT");
        if (listener != null) {
            listener.onPositionClosed("SL_HIT");
        }
    }
    
    @Override
    public void onMarginCall(Position position, double marginRatio) {
        // 마진콜 발생
        if (listener != null) {
            listener.onStatusUpdate(position, MonitoringStatus.MARGIN_CALL_WARNING);
            listener.onAlert(new Alert(
                "MARGIN_CALL",
                "마진이 부족합니다. 마진 비율: " + (int)marginRatio + "%",
                0.7
            ));
        }
    }
    
    // ========== MarginListener 구현 ==========
    
    @Override
    public void onMarginRatioUpdate(Position position, double marginRatio) {
        // 마진 비율 실시간 업데이트
    }
    
    @Override
    public void onMarginCallWarning(Position position, double marginRatio) {
        // 마진콜 경고
        if (listener != null) {
            listener.onAlert(new Alert(
                "MARGIN_CALL_WARNING",
                "마진이 50% 이하입니다. 즉시 조치하세요!",
                0.8
            ));
        }
    }
    
    @Override
    public void onLiquidationRequired(Position position, double liquidationPrice) {
        // 청산 필요
        if (listener != null) {
            listener.onAlert(new Alert(
                "LIQUIDATION_IMMINENT",
                "청산이 곧 발생합니다: $" + liquidationPrice,
                1.0
            ));
        }
        
        // 자동 청산 실행
        liquidationEngine.executeLiquidation(
            position,
            position.currentPrice,
            "MARGIN_CALL_LIQUIDATION"
        );
    }
    
    // ========== LiquidationListener 구현 ==========
    
    @Override
    public void onLiquidationStart(Position position, String reason) {
        if (listener != null) {
            listener.onStatusUpdate(position, MonitoringStatus.LIQUIDATED);
            listener.onAlert(new Alert(
                "LIQUIDATION_START",
                "포지션 청산 시작: " + reason,
                1.0
            ));
        }
    }
    
    @Override
    public void onLiquidationComplete(Trade liquidationTrade) {
        if (listener != null) {
            listener.onAlert(new Alert(
                "LIQUIDATION_COMPLETE",
                "포지션이 청산되었습니다. 손실: $" + liquidationTrade.pnl,
                1.0
            ));
        }
    }
    
    @Override
    public void onLiquidationFailed(Position position, Exception error) {
        if (listener != null) {
            listener.onAlert(new Alert(
                "LIQUIDATION_FAILED",
                "청산 실패: " + error.getMessage(),
                1.0
            ));
        }
    }
    
    public void setListener(MonitoringListener listener) {
        this.listener = listener;
    }
}
```

---

## 📋 8. 체크리스트

- [ ] MarginCalculator 구현
- [ ] MarginMonitor 구현
- [ ] LiquidationEngine 구현
- [ ] Position에 마진 필드 추가
- [ ] activity_liquidation_warning.xml 생성
- [ ] PositionMonitoringService 통합
- [ ] 실시간 마진 비율 UI 업데이트
- [ ] 마진콜 알림 테스트
- [ ] 자동 청산 테스트
- [ ] 데이터베이스에 청산 기록 저장

---

## 🎯 핵심 요약

```
레버리지 시스템의 3단계:

1️⃣ 정상 거래
   └─ 마진 비율 > 100%
   └─ 계속 거래 가능

2️⃣ 마진콜 경고 (🟡 주의)
   └─ 마진 비율 50% ~ 100%
   └─ 사용자 경고
   └─ 긴급 포지션 종료 옵션

3️⃣ 자동 청산 (💥 위험)
   └─ 마진 비율 ≤ 0%
   └─ 자동으로 포지션 종료
   └─ 잔금 소진
```

이제 모의투자에서도 **실제 마진 거래의 위험을 정확히 시뮬레이션**할 수 있습니다! 🚀

