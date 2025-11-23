# 거래 금액 입력 필드 로직 설계

## 🎯 설계 원칙

1. **기본값 제공**: UserSettings의 기본값을 초기값으로 사용
2. **실시간 계산**: 금액 변경 시 거래 수량, 예상 손익 등 자동 계산
3. **검증**: 최소/최대 금액, 잔고 초과 여부 검증
4. **사용자 경험**: 직관적이고 명확한 정보 표시

## 📋 로직 흐름

### 1. 초기화 단계
```
TradingActivity.onCreate()
  ↓
UserSettings 조회
  ↓
기본 위험 자금 계산 (UserSettings.calculateRiskAmount())
  ↓
거래 금액 입력 필드에 기본값 설정
  ↓
updateRiskMetrics() 호출 (초기 계산)
```

### 2. 사용자 입력 단계
```
사용자가 거래 금액 입력
  ↓
TextWatcher.onTextChanged() 호출
  ↓
입력값 검증
  ├─ 빈 값 → UserSettings 기본값 사용
  ├─ 유효하지 않은 값 → 에러 표시
  └─ 유효한 값 → 입력값 사용
  ↓
updateRiskMetrics() 호출
  ├─ 거래 수량 계산
  ├─ 예상 손익 계산
  ├─ R:R 비율 계산 (기존)
  └─ Risk Score 계산 (기존)
```

### 3. 거래 실행 단계
```
"거래 진입" 버튼 클릭
  ↓
입력값 검증
  ├─ 금액이 비어있으면 → UserSettings 기본값 사용
  ├─ 금액이 0 이하 → 에러
  ├─ 금액이 잔고 초과 → 에러
  └─ 유효한 값 → 거래 실행
  ↓
TradeExecutor.executeTrade() 호출
  ├─ riskAmount 파라미터로 전달
  └─ TradeExecutor 내부에서 추가 검증
```

## 🔧 구현 상세

### 1. UI 추가 (activity_trading.xml)

```xml
<!-- Risk Amount (거래 금액) -->
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:layout_marginBottom="12dp">

    <TextView
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:text="거래 금액"
        android:textSize="14sp"
        android:textColor="@color/tds_text_secondary" />

    <EditText
        android:id="@+id/risk_amount_input"
        android:layout_width="140dp"
        android:layout_height="wrap_content"
        android:hint="자동"
        android:textSize="14sp"
        android:textColor="@color/tds_text_primary"
        android:inputType="numberDecimal"
        android:background="@drawable/edit_text_background"
        android:paddingHorizontal="12dp"
        android:paddingVertical="8dp" />
</LinearLayout>
```

### 2. TradingActivity.java 수정

#### 2.1 변수 추가
```java
private EditText riskAmountInput;
private TextView tradeQuantityText;  // 거래 수량 표시 (선택적)
private TextView expectedPnLText;    // 예상 손익 표시 (선택적)
private double defaultRiskAmount = 0.0;  // UserSettings 기본값
```

#### 2.2 초기화
```java
private void initViews() {
    // ... 기존 코드 ...
    riskAmountInput = findViewById(R.id.risk_amount_input);
    // tradeQuantityText, expectedPnLText도 추가 (선택적)
}

private void loadInitialData() {
    // ... 기존 코드 ...
    
    // UserSettings에서 기본 위험 자금 계산
    new Thread(() -> {
        UserSettings settings = settingsRepository.getSettingsSync(1);
        User user = userRepository.getUserSync(1);
        
        if (settings != null && user != null) {
            defaultRiskAmount = TradeCalculator.calculateRiskAmount(
                settings, user.getBalance()
            );
            
            runOnUiThread(() -> {
                // 기본값을 힌트로 표시 (선택적)
                riskAmountInput.setHint(String.format(Locale.US, "$%.2f", defaultRiskAmount));
                updateRiskMetrics();
            });
        }
    }).start();
}
```

#### 2.3 리스너 추가
```java
private void setupListeners() {
    // ... 기존 코드 ...
    
    // Risk Amount 변경
    riskAmountInput.addTextChangedListener(new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            updateRiskMetrics();
        }
        
        @Override
        public void afterTextChanged(Editable s) {}
    });
}
```

#### 2.4 updateRiskMetrics() 확장
```java
private void updateRiskMetrics() {
    // ... 기존 가격 파싱 코드 ...
    
    // 거래 금액 파싱
    String riskAmountText = riskAmountInput.getText() != null ? 
        riskAmountInput.getText().toString().trim() : "";
    double riskAmount = 0.0;
    
    if (riskAmountText.isEmpty()) {
        // 입력이 없으면 기본값 사용
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
    
    // 모든 가격이 유효한지 확인
    if (entryPrice > 0 && tpPrice > 0 && slPrice > 0 && riskAmount > 0) {
        // ... 기존 R:R 비율 계산 ...
        
        // 거래 수량 계산
        String tradeType = "FUTURES"; // 또는 UserSettings에서 가져오기
        double tradeSize = TradeCalculator.calculateTradeSize(
            riskAmount, entryPrice, tradeType, leverage
        );
        
        // 예상 손익 계산
        double maxLoss = TradeCalculator.calculateMaxLoss(
            tradeSize, entryPrice, slPrice, leverage, isLong
        );
        double maxProfit = TradeCalculator.calculateMaxProfit(
            tradeSize, entryPrice, tpPrice, leverage, isLong
        );
        
        // UI 업데이트 (선택적)
        if (tradeQuantityText != null) {
            tradeQuantityText.setText(String.format(Locale.US, "%.6f", tradeSize));
        }
        if (expectedPnLText != null) {
            expectedPnLText.setText(String.format(Locale.US, 
                "손실: $%.2f / 수익: $%.2f", maxLoss, maxProfit));
        }
    }
}
```

#### 2.5 executeTrade() 수정
```java
private void executeTrade() {
    // ... 기존 가격 파싱 코드 ...
    
    // 거래 금액 파싱
    String riskAmountText = riskAmountInput.getText() != null ? 
        riskAmountInput.getText().toString().trim() : "";
    double riskAmount = 0.0;
    
    if (riskAmountText.isEmpty()) {
        // 입력이 없으면 기본값 사용
        riskAmount = defaultRiskAmount;
    } else {
        try {
            riskAmount = Double.parseDouble(riskAmountText);
            if (riskAmount <= 0) {
                Toast.makeText(this, "거래 금액은 0보다 커야 합니다", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "거래 금액 형식이 올바르지 않습니다", Toast.LENGTH_SHORT).show();
            return;
        }
    }
    
    // 잔고 검증 (선택적)
    // User user = userRepository.getUserSync(userId);
    // if (user != null && riskAmount > user.getBalance()) {
    //     Toast.makeText(this, "거래 금액이 잔고를 초과합니다", Toast.LENGTH_SHORT).show();
    //     return;
    // }
    
    // 거래 실행
    new Thread(() -> {
        tradeExecutor.executeTrade(
            userId,
            currentSymbol,
            entryPrice,
            tpPrice,
            slPrice,
            isLong,
            leverage,
            riskAmount,  // 추가
            new TradeExecutor.OnTradeExecutedListener() {
                // ... 기존 리스너 코드 ...
            }
        );
    }).start();
}
```

### 3. TradeExecutor.java 수정

```java
public void executeTrade(long userId, String symbol, double entryPrice, 
                         double tpPrice, double slPrice, boolean isLong, 
                         Integer leverage, Double riskAmount,  // 추가 (nullable)
                         OnTradeExecutedListener listener) {
    
    // ... 기존 사용자 및 설정 조회 코드 ...
    
    // riskAmount가 null이거나 0 이하면 UserSettings 기본값 사용
    double finalRiskAmount;
    if (riskAmount == null || riskAmount <= 0) {
        finalRiskAmount = TradeCalculator.calculateRiskAmount(settings, user.getBalance());
    } else {
        finalRiskAmount = riskAmount;
    }
    
    // 거래 검증 (기존 코드에서 riskAmount 대신 finalRiskAmount 사용)
    TradeValidator.ValidationResult validation = TradeValidator.validateTrade(
        entryPrice, tpPrice, slPrice, finalRiskAmount, actualLeverage, isLong,
        settings, activePositionsCount, user.getBalance(), dailyLoss
    );
    
    // ... 나머지 코드는 finalRiskAmount 사용 ...
}
```

## 🎨 UI/UX 개선 사항 (선택적)

### 1. 거래 수량 표시
```xml
<TextView
    android:id="@+id/trade_quantity_text"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="수량: 0.000000"
    android:textSize="12sp"
    android:textColor="@color/tds_text_secondary" />
```

### 2. 예상 손익 표시
```xml
<TextView
    android:id="@+id/expected_pnl_text"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="예상 손익: -$0.00 / +$0.00"
    android:textSize="12sp"
    android:textColor="@color/tds_text_secondary" />
```

### 3. 잔고 표시
```xml
<TextView
    android:id="@+id/balance_text"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="잔고: $10,000.00"
    android:textSize="12sp"
    android:textColor="@color/tds_text_secondary" />
```

## ✅ 검증 로직

### 1. 최소/최대 금액
```java
private boolean validateRiskAmount(double riskAmount) {
    if (riskAmount <= 0) {
        return false;
    }
    
    // 최소 금액 (예: $10)
    if (riskAmount < 10.0) {
        Toast.makeText(this, "최소 거래 금액은 $10입니다", Toast.LENGTH_SHORT).show();
        return false;
    }
    
    // 최대 금액 (예: 잔고의 50%)
    User user = userRepository.getUserSync(userId);
    if (user != null && riskAmount > user.getBalance() * 0.5) {
        Toast.makeText(this, "거래 금액은 잔고의 50%를 초과할 수 없습니다", Toast.LENGTH_SHORT).show();
        return false;
    }
    
    return true;
}
```

### 2. 잔고 초과 검증
```java
private boolean validateBalance(double riskAmount) {
    User user = userRepository.getUserSync(userId);
    if (user == null) {
        return false;
    }
    
    // 선물 거래는 마진만 필요하므로 검증 로직이 다름
    String tradeType = settings.getTradeMode();
    if ("SPOT".equals(tradeType)) {
        // 현물: 전체 금액 필요
        if (riskAmount > user.getBalance()) {
            Toast.makeText(this, "거래 금액이 잔고를 초과합니다", Toast.LENGTH_SHORT).show();
            return false;
        }
    } else {
        // 선물: 마진만 필요 (거래 금액 / 레버리지)
        double requiredMargin = riskAmount / leverage;
        if (requiredMargin > user.getBalance()) {
            Toast.makeText(this, "필요한 마진이 잔고를 초과합니다", Toast.LENGTH_SHORT).show();
            return false;
        }
    }
    
    return true;
}
```

## 📝 구현 우선순위

### Phase 1: 기본 기능
1. ✅ 거래 금액 입력 필드 추가
2. ✅ 기본값 설정 (UserSettings)
3. ✅ executeTrade()에 금액 전달
4. ✅ TradeExecutor 수정

### Phase 2: 실시간 계산 (선택적)
5. ⚠️ 거래 수량 표시
6. ⚠️ 예상 손익 표시
7. ⚠️ 잔고 표시

### Phase 3: 검증 강화 (선택적)
8. ⚠️ 최소/최대 금액 검증
9. ⚠️ 잔고 초과 검증
10. ⚠️ 실시간 검증 피드백

## 🤔 고려 사항

### 1. 기본값 표시 방식
- **옵션 A**: 힌트로 표시 (현재 입력 필드가 비어있을 때)
- **옵션 B**: 초기값으로 채우기 (사용자가 수정 가능)
- **옵션 C**: "자동" 텍스트 표시 + 기본값 사용

### 2. 거래 수량 표시
- 거래 수량을 표시하면 사용자가 더 명확하게 이해할 수 있음
- 하지만 UI가 복잡해질 수 있음

### 3. 예상 손익 표시
- 예상 손익을 표시하면 사용자가 리스크를 더 잘 이해할 수 있음
- 하지만 계산이 복잡해질 수 있음

## 💡 추천 구현 방식

1. **기본값**: 힌트로 표시 (옵션 A)
2. **거래 수량**: 표시하지 않음 (UI 단순화)
3. **예상 손익**: 표시하지 않음 (UI 단순화)
4. **검증**: 기본 검증만 (잔고 초과, 0 이하)

이렇게 하면 구현이 간단하면서도 핵심 기능은 모두 제공됩니다.

