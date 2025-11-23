# 거래 금액 설정 문제 분석

## 🔴 현재 문제점

### 1. 거래 금액 입력 UI가 없음
- `TradingActivity`에는 EP, TP, SL 가격 입력 필드만 있음
- 거래 금액을 입력하는 필드가 없음
- 사용자가 거래할 때마다 금액을 조정할 수 없음

### 2. 현재 거래 금액 결정 방식
```java
// TradeExecutor.java line 77
double riskAmount = TradeCalculator.calculateRiskAmount(settings, user.getBalance());
```

```java
// UserSettings.java line 233-238
public double calculateRiskAmount(double currentBalance) {
    if (useFixedRiskAmount) {
        return fixedRiskAmount;  // 고정액: $100 (기본값)
    } else {
        return currentBalance * (riskPercentage / 100.0);  // 비율: 잔고의 2% (기본값)
    }
}
```

**기본값:**
- `useFixedRiskAmount = false` (비율 방식)
- `riskPercentage = 2.0` (잔고의 2%)
- `fixedRiskAmount = 100.0` (사용 안 됨)

### 3. 문제점
- 사용자가 거래할 때마다 금액을 직접 설정할 수 없음
- `UserSettings`의 기본값만 사용됨
- 거래마다 다른 금액을 사용하고 싶어도 불가능

## 💡 해결 방안

### 옵션 1: 거래 화면에 금액 입력 필드 추가 (추천)
```
TradingActivity에 "거래 금액" 입력 필드 추가
  ↓
사용자가 직접 금액 입력 (예: $100, $500)
  ↓
TradeExecutor.executeTrade()에 금액 파라미터 추가
  ↓
입력된 금액을 riskAmount로 사용
```

**장점:**
- 사용자가 거래마다 금액을 자유롭게 조정 가능
- 직관적이고 명확함
- 유연성 높음

**단점:**
- UI에 필드 하나 추가 필요
- `TradeExecutor.executeTrade()` 시그니처 변경 필요

### 옵션 2: UserSettings의 기본값만 사용
```
현재 상태 유지
  ↓
UserSettings에서 고정액 또는 비율 설정
  ↓
모든 거래에 동일한 금액 적용
```

**장점:**
- 구현 간단
- 일관된 위험 관리

**단점:**
- 거래마다 금액 조정 불가능
- 유연성 낮음

### 옵션 3: 하이브리드 방식
```
UserSettings의 기본값을 기본값으로 사용
  ↓
TradingActivity에 금액 입력 필드 추가 (선택적)
  ↓
입력 필드가 비어있으면 UserSettings 값 사용
  ↓
입력 필드에 값이 있으면 입력된 값 사용
```

**장점:**
- 기본값 설정 가능
- 필요시 금액 조정 가능
- 유연성과 편의성 모두 확보

**단점:**
- 구현이 약간 복잡함

## 🎯 추천 방안

**옵션 1 (거래 화면에 금액 입력 필드 추가)**를 추천합니다.

### 구현 예시

#### 1. activity_trading.xml에 금액 입력 필드 추가
```xml
<EditText
    android:id="@+id/risk_amount_input"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:hint="거래 금액 ($)"
    android:inputType="numberDecimal" />
```

#### 2. TradingActivity.java 수정
```java
private EditText riskAmountInput;

private void initViews() {
    // ...
    riskAmountInput = findViewById(R.id.risk_amount_input);
}

private void executeTrade() {
    // ...
    String riskAmountText = riskAmountInput.getText().toString().trim();
    double riskAmount = 0.0;
    
    if (riskAmountText.isEmpty()) {
        // 입력이 없으면 UserSettings 기본값 사용
        riskAmount = TradeCalculator.calculateRiskAmount(settings, user.getBalance());
    } else {
        // 입력된 금액 사용
        riskAmount = Double.parseDouble(riskAmountText);
    }
    
    // TradeExecutor.executeTrade()에 riskAmount 전달
    tradeExecutor.executeTrade(
        userId, symbol, entryPrice, tpPrice, slPrice, 
        isLong, leverage, riskAmount, listener
    );
}
```

#### 3. TradeExecutor.java 수정
```java
public void executeTrade(long userId, String symbol, double entryPrice, 
                         double tpPrice, double slPrice, boolean isLong, 
                         Integer leverage, Double riskAmount,  // 추가
                         OnTradeExecutedListener listener) {
    
    // ...
    
    // riskAmount가 null이면 UserSettings 값 사용
    if (riskAmount == null || riskAmount <= 0) {
        riskAmount = TradeCalculator.calculateRiskAmount(settings, user.getBalance());
    }
    
    // ...
}
```

## 📝 다음 단계

1. **사용자 의견 확인**: 어떤 방식을 원하는지 확인
2. **UI 디자인**: 금액 입력 필드 위치 및 스타일 결정
3. **구현**: 선택한 방안에 따라 코드 수정
4. **테스트**: 다양한 시나리오 테스트

