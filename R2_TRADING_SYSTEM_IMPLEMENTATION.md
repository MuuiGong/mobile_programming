# R² 모의투자 시스템 구현 완료 요약

## ✅ 구현 완료 항목

### 1. 데이터 모델 확장 ✅

#### Position 엔티티 확장
- `tradeType`: "SPOT" or "FUTURES"
- `leverage`: 레버리지 (1x ~ 20x)
- `riskAmount`: 위험 자금
- `timeframe`: 타임프레임
- `exitReason`: 종료 사유 (TP_HIT, SL_HIT, MARGIN_CALL, MANUAL, TIMEOUT)
- `maxDrawdown`: 최대 낙폭
- `rrRatio`: R:R 비율

**새로운 계산 메서드:**
- `calculateUnrealizedPnL()`: 레버리지 포함 손익 계산
- `calculateRemainingToTP()`, `calculateRemainingToSL()`: TP/SL까지 거리
- `calculateProfitRatio()`, `calculateLossRatio()`: 수익도/손실도
- `calculateUsedMargin()`, `calculateAvailableMargin()`: 마진 계산
- `isMarginCall()`: 마진콜 여부 확인

#### UserSettings 엔티티 생성
- 초기 자본, 거래 모드, 레버리지 설정
- 위험 관리 방식 (고정액/비율)
- 포지션 관리 설정
- 차트 설정

### 2. 거래 계산 엔진 ✅

**TradeCalculator 클래스:**
- `calculateSpotTradeSize()`: 현물 거래 크기 계산
- `calculateFuturesTradeSize()`: 선물 거래 크기 계산 (레버리지 포함)
- `calculatePnL()`: 손익 계산 (레버리지 포함)
- `calculateRRRatio()`: R:R 비율 계산
- `calculateMaxLoss()`, `calculateMaxProfit()`: 최대 손실/수익
- `calculateTrade()`: 통합 계산 메서드

### 3. 거래 검증 시스템 ✅

**TradeValidator 클래스:**
- 기본 검증 (TP/SL 위치 확인)
- 거리 검증 (최소 0.1%)
- 위험도 검증 (1회 손실 한도)
- 포지션 수 검증
- 일일 손실 검증
- 잔고 검증
- 레버리지 검증
- R:R 비율 경고

### 4. 자동 매도 시스템 확장 ✅

**TradingMonitorWorker 확장:**
1. **TP 도달**: 자동 익절
2. **SL 도달**: 자동 손절
3. **마진콜**: 선물 거래 마진 50% 이하 시 경고, 0% 이하 시 강제 종료
4. **타임아웃**: 최대 지속 시간 초과 시 자동 종료
5. **일일 손실 한도**: 일일 손실 한도 도달 시 모든 포지션 종료

### 5. 거래 실행 엔진 ✅

**TradeExecutor 클래스:**
- 거래 검증 수행
- 거래 크기 자동 계산
- 포지션 생성 및 저장
- 잔고 자동 차감
- 경고 메시지 처리

### 6. 알림 시스템 확장 ✅

**NotificationHelper 확장:**
- `notifyMarginCall()`: 마진콜 알림
- `notifyMarginWarning()`: 마진 경고 알림
- `notifyTimeout()`: 타임아웃 알림

### 7. Repository 확장 ✅

- `UserSettingsRepository`: 사용자 설정 관리
- `TradingRepository`: 동기 메서드 추가
- `UserRepository`: 동기 메서드 추가

## 📋 남은 작업

### 1. 초기 설정 화면 (UserSettingsActivity)
- 프롬프트의 UI 스펙대로 구현 필요
- SharedPreferences 또는 Room DB에 저장

### 2. 대시보드 업데이트
- 레버리지 정보 표시
- 마진 사용량 표시
- 일일 손실 표시
- 활성 포지션 카드 업데이트

### 3. 거래 기록 화면 업데이트
- exitReason 표시
- 레버리지 정보 표시
- 필터링 기능 확장

### 4. 데이터베이스 마이그레이션
- 버전 1 → 2 마이그레이션 필요
- Position 테이블에 새 컬럼 추가
- UserSettings 테이블 생성

## 🔧 사용 방법

### 거래 실행 예시

```java
TradeExecutor executor = new TradeExecutor(context);

executor.executeTrade(
    userId,
    "BTCUSDT",
    95836.00,  // Entry
    97752.72,  // TP
    93919.28,  // SL
    true,      // Long
    5,         // Leverage
    new TradeExecutor.OnTradeExecutedListener() {
        @Override
        public void onSuccess(long positionId, TradeCalculationResult calculation) {
            // 거래 성공
        }
        
        @Override
        public void onError(String error) {
            // 검증 실패
        }
        
        @Override
        public void onWarning(String warning) {
            // 경고 표시
        }
    }
);
```

### 거래 검증 예시

```java
TradeValidator.ValidationResult result = TradeValidator.validateTrade(
    entryPrice, tpPrice, slPrice, riskAmount, leverage, isLong,
    settings, activePositionsCount, currentBalance, dailyLoss
);

if (result.isValid()) {
    // 거래 가능
    if (!result.getWarnings().isEmpty()) {
        // 경고 표시
    }
} else {
    // 오류 표시
    for (String error : result.getErrors()) {
        // 오류 메시지 표시
    }
}
```

## 📊 계산식 예시

### 현물 거래
```
거래 수량 = $100 / $95,836 = 0.001044 BTC
손익 = (가격 변화) × 수량
```

### 선물 거래 (5x 레버리지)
```
거래 규모 = $100 × 5 = $500
거래 수량 = $500 / $95,836 = 0.00522 BTC
손익 = (가격 변화) × 수량 × 5
```

### R:R 비율
```
R:R = (TP - Entry) / (Entry - SL)
```

## 🎯 다음 단계

1. **데이터베이스 마이그레이션 구현**
2. **초기 설정 화면 구현**
3. **대시보드 UI 업데이트**
4. **거래 기록 화면 업데이트**
5. **테스트 및 디버깅**

## 📝 참고사항

- 모든 계산식은 프롬프트의 요구사항을 정확히 따릅니다
- 레버리지는 1x ~ 20x 지원
- 마진콜은 선물 거래만 적용됩니다
- 일일 손실 한도는 UTC 00:00에 리셋됩니다

