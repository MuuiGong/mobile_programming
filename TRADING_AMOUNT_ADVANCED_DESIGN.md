# 거래 금액 입력 + 청산 예산가 + 마진 모드 설계

## 🎯 문제 인식

거래 금액을 자유롭게 설정하면 다음이 필요합니다:

1. **청산 예산가 계산**: 거래 전에 "이 금액으로 거래하면 청산가는 얼마인가?"
2. **마진 모드 선택**: Isolated vs Cross
   - **Isolated**: 포지션별로 마진 격리, 각 포지션의 손실이 다른 포지션에 영향 없음
   - **Cross**: 모든 포지션이 공유 마진 풀 사용, 한 포지션의 손실이 다른 포지션에 영향

## 📊 현재 상태 분석

### 1. 청산 가격 계산 (이미 구현됨)
```java
// MarginCalculator.calculateLiquidationPrice()
// 하지만 이건 포지션 생성 후의 계산
// 거래 전 예상 청산가를 계산하는 메서드가 필요함
```

### 2. 마진 모드 (현재 미구현)
- 현재는 모든 포지션이 Cross 모드처럼 동작
- `Position` 엔티티에 `marginMode` 필드 없음
- `UserSettings`에 마진 모드 설정 없음

## 🔧 설계 방안

### 옵션 1: 단순화 (추천)
**거래 금액 입력 + 청산 예산가만 표시, 마진 모드는 Cross로 고정**

**장점:**
- 구현 간단
- 사용자 혼란 최소화
- 대부분의 거래소가 Cross 모드를 기본으로 사용

**단점:**
- Isolated 모드 사용 불가
- 고급 사용자 요구사항 미충족

**구현:**
- 거래 금액 입력 필드 추가
- 실시간 청산 예산가 계산 및 표시
- 마진 모드는 Cross로 고정 (코드에 명시)

### 옵션 2: 완전 구현
**거래 금액 입력 + 청산 예산가 + Isolated/Cross 선택**

**장점:**
- 모든 기능 제공
- 고급 사용자 요구사항 충족
- 실제 거래소와 유사한 경험

**단점:**
- 구현 복잡도 높음
- 사용자 혼란 가능성
- 테스트 시나리오 증가

**구현:**
- 거래 금액 입력 필드
- 마진 모드 선택 (Isolated/Cross)
- 포지션별 마진 격리 (Isolated 모드)
- 실시간 청산 예산가 계산 (모드별로 다름)

## 💡 추천: 옵션 1 (단순화)

### Phase 1: 거래 금액 + 청산 예산가

#### 1.1 UI 추가
```xml
<!-- 거래 금액 입력 -->
<EditText
    android:id="@+id/risk_amount_input"
    android:hint="거래 금액 ($)"
    ... />

<!-- 청산 예산가 표시 (읽기 전용) -->
<TextView
    android:id="@+id/liquidation_price_text"
    android:text="청산 예산가: --"
    android:textColor="@color/tds_warning"
    ... />
```

#### 1.2 청산 예산가 계산 메서드 추가
```java
// MarginCalculator.java에 추가
/**
 * 거래 전 예상 청산 가격 계산
 * 
 * @param entryPrice 진입 가격
 * @param riskAmount 거래 금액 (위험 자금)
 * @param leverage 레버리지
 * @param isLongPosition 롱 포지션 여부
 * @return 예상 청산 가격
 */
public static double calculateEstimatedLiquidationPrice(
    double entryPrice, double riskAmount, int leverage, boolean isLongPosition) {
    
    if (riskAmount <= 0 || leverage <= 0) {
        return entryPrice;
    }
    
    // 거래 수량 계산
    double tradeSize = TradeCalculator.calculateFuturesTradeSize(
        riskAmount, leverage, entryPrice
    );
    
    // 사용 마진 계산 (증거금)
    double usedMargin = calculateUsedMargin(entryPrice, tradeSize, leverage);
    
    // 청산 가격 계산
    return calculateLiquidationPrice(
        entryPrice, tradeSize, leverage, usedMargin, isLongPosition
    );
}
```

#### 1.3 TradingActivity 수정
```java
private void updateRiskMetrics() {
    // ... 기존 가격 파싱 코드 ...
    
    // 거래 금액 파싱
    String riskAmountText = riskAmountInput.getText() != null ? 
        riskAmountInput.getText().toString().trim() : "";
    double riskAmount = 0.0;
    
    if (riskAmountText.isEmpty()) {
        riskAmount = defaultRiskAmount;
    } else {
        try {
            riskAmount = Double.parseDouble(riskAmountText);
            if (riskAmount <= 0) {
                riskAmount = defaultRiskAmount;
            }
        } catch (NumberFormatException e) {
            riskAmount = defaultRiskAmount;
        }
    }
    
    // 선물 거래일 때만 청산 예산가 계산
    if ("FUTURES".equals(tradeType) && leverage > 1 && 
        entryPrice > 0 && riskAmount > 0) {
        
        double estimatedLiquidationPrice = MarginCalculator.calculateEstimatedLiquidationPrice(
            entryPrice, riskAmount, leverage, isLong
        );
        
        // 청산 예산가 표시
        if (liquidationPriceText != null) {
            String liquidationText = String.format(Locale.US, 
                "청산 예산가: $%.2f", estimatedLiquidationPrice);
            liquidationPriceText.setText(liquidationText);
            
            // 청산가가 SL보다 가까우면 경고
            if (isLong && estimatedLiquidationPrice > slPrice) {
                liquidationPriceText.setTextColor(getColor(R.color.risk_danger));
            } else if (!isLong && estimatedLiquidationPrice < slPrice) {
                liquidationPriceText.setTextColor(getColor(R.color.risk_danger));
            } else {
                liquidationPriceText.setTextColor(getColor(R.color.tds_warning));
            }
        }
    } else {
        // 현물 거래이거나 레버리지가 1x면 청산가 없음
        if (liquidationPriceText != null) {
            liquidationPriceText.setText("청산 예산가: 없음 (현물 거래)");
            liquidationPriceText.setTextColor(getColor(R.color.tds_text_secondary));
        }
    }
}
```

### Phase 2: Isolated/Cross 마진 모드 (나중에 추가 가능)

#### 2.1 데이터 모델 확장
```java
// Position.java에 추가
private String marginMode; // "ISOLATED" or "CROSS"

// UserSettings.java에 추가
private String defaultMarginMode; // "ISOLATED" or "CROSS"
```

#### 2.2 마진 계산 로직 분리
```java
// MarginCalculator.java에 추가
/**
 * Isolated 모드: 포지션별 마진 격리
 */
public static double calculateIsolatedMargin(
    double entryPrice, double tradeSize, double leverage) {
    // 포지션별로 독립적인 마진 계산
    return calculateUsedMargin(entryPrice, tradeSize, leverage);
}

/**
 * Cross 모드: 공유 마진 풀 사용
 */
public static double calculateCrossMargin(
    double totalBalance, List<Position> activePositions) {
    // 모든 포지션의 마진을 합산
    double totalUsedMargin = 0.0;
    for (Position pos : activePositions) {
        totalUsedMargin += calculateUsedMargin(
            pos.getEntryPrice(), pos.getQuantity(), pos.getLeverage()
        );
    }
    return totalUsedMargin;
}
```

#### 2.3 청산 가격 계산 (모드별)
```java
/**
 * Isolated 모드 청산 가격
 */
public static double calculateIsolatedLiquidationPrice(
    double entryPrice, double tradeSize, double leverage, 
    double isolatedMargin, boolean isLong) {
    // 포지션별 마진만 고려
    return calculateLiquidationPrice(
        entryPrice, tradeSize, leverage, isolatedMargin, isLong
    );
}

/**
 * Cross 모드 청산 가격 (복잡함)
 */
public static double calculateCrossLiquidationPrice(
    Position position, List<Position> allPositions, 
    double totalBalance, boolean isLong) {
    // 모든 포지션의 미실현 손익을 고려
    // 가장 복잡한 계산
    // ...
}
```

## 🎯 최종 추천 구현 계획

### Step 1: 기본 기능 (지금 구현)
1. ✅ 거래 금액 입력 필드 추가
2. ✅ 청산 예산가 계산 및 표시
3. ✅ 마진 모드는 Cross로 고정 (코드에 명시)
4. ✅ 실시간 업데이트 (금액 변경 시)

### Step 2: 고급 기능 (나중에 추가)
5. ⚠️ Isolated/Cross 마진 모드 선택
6. ⚠️ 포지션별 마진 격리 (Isolated)
7. ⚠️ 공유 마진 풀 관리 (Cross)

## 📝 구현 예시 코드

### 1. MarginCalculator 확장
```java
/**
 * 거래 전 예상 청산 가격 계산 (Cross 모드 기준)
 */
public static double calculateEstimatedLiquidationPrice(
    double entryPrice, double riskAmount, int leverage, boolean isLong) {
    
    if (riskAmount <= 0 || leverage <= 0) {
        return entryPrice;
    }
    
    // 거래 수량 계산
    double tradeSize = TradeCalculator.calculateFuturesTradeSize(
        riskAmount, leverage, entryPrice
    );
    
    // 사용 마진 계산 (증거금)
    // Cross 모드: 이 포지션만 고려 (다른 포지션은 나중에 합산)
    double usedMargin = calculateUsedMargin(entryPrice, tradeSize, leverage);
    
    // 청산 가격 계산
    return calculateLiquidationPrice(
        entryPrice, tradeSize, leverage, usedMargin, isLong
    );
}
```

### 2. TradingActivity UI 업데이트
```java
private void updateRiskMetrics() {
    // ... 기존 코드 ...
    
    // 청산 예산가 계산 (선물 거래만)
    if (isFutures && leverage > 1 && entryPrice > 0 && riskAmount > 0) {
        double estimatedLiquidationPrice = MarginCalculator.calculateEstimatedLiquidationPrice(
            entryPrice, riskAmount, leverage, isLong
        );
        
        updateLiquidationPriceDisplay(estimatedLiquidationPrice, slPrice);
    }
}

private void updateLiquidationPriceDisplay(double liquidationPrice, double slPrice) {
    if (liquidationPriceText == null) return;
    
    String text = String.format(Locale.US, "청산 예산가: $%.2f", liquidationPrice);
    liquidationPriceText.setText(text);
    
    // 경고: 청산가가 SL보다 가까우면 위험
    boolean isDangerous = isLong ? 
        (liquidationPrice > slPrice) : 
        (liquidationPrice < slPrice);
    
    if (isDangerous) {
        liquidationPriceText.setTextColor(getColor(R.color.risk_danger));
        liquidationPriceText.append(" ⚠️ SL보다 가까움");
    } else {
        liquidationPriceText.setTextColor(getColor(R.color.tds_warning));
    }
}
```

## 🤔 결정 사항

1. **마진 모드**: 
   - 옵션 A: Cross로 고정 (간단)
   - 옵션 B: Isolated/Cross 선택 (복잡)

2. **청산 예산가 표시 위치**:
   - 옵션 A: Risk Score 아래
   - 옵션 B: 거래 금액 입력 필드 옆
   - 옵션 C: 별도 섹션

3. **경고 표시**:
   - 청산가가 SL보다 가까우면 경고 표시?

## 💡 최종 추천

1. **마진 모드**: Cross로 고정 (옵션 A)
2. **청산 예산가**: Risk Score 아래 표시 (옵션 A)
3. **경고 표시**: 청산가가 SL보다 가까우면 경고 (옵션 A)

이렇게 하면 구현이 간단하면서도 핵심 기능은 모두 제공됩니다.

