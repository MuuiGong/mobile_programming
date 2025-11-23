# R² 거래 시스템 흐름 정리

## 📋 현재 구현 상태

### 1. 거래 진입 흐름 (Trade Entry Flow)

```
사용자 (TradingActivity)
  ↓
[1] EP, TP, SL 가격 입력
  ↓
[2] "거래 진입" 버튼 클릭
  ↓
[3] executeTrade() 호출
  ↓
[4] TradeExecutor.executeTrade() 호출
  ↓
[5] 거래 검증 (TradeValidator)
    ├─ TP/SL 위치 검증
    ├─ 거리 검증 (최소 0.1%)
    ├─ 위험도 검증 (1회 손실 한도)
    ├─ 포지션 수 검증
    ├─ 일일 손실 검증
    ├─ 잔고 검증
    └─ 레버리지 검증
  ↓
[6] 거래 크기 계산 (TradeCalculator)
    ├─ 위험 자금 계산 (UserSettings 기반)
    ├─ 거래 수량 계산
    └─ R:R 비율 계산
  ↓
[7] 포지션 생성 (TradingRepository.openPosition)
    ├─ Position 엔티티 생성
    ├─ DB에 저장
    └─ TradeHistory 기록 (BUY/SELL)
  ↓
[8] 잔고 차감
    ├─ 현물: 전체 거래 금액 차감
    └─ 선물: 마진만 차감 (거래 금액 / 레버리지)
  ↓
[9] 성공/실패 콜백
```

### 2. 포지션 모니터링 흐름 (Position Monitoring Flow)

```
TradingMonitorWorker (주기적 실행)
  ↓
[1] 활성 포지션 조회 (getActivePositionsSync)
  ↓
[2] 각 포지션에 대해 현재 가격 확인
    └─ MarketDataRepository.getCachedPrice()
  ↓
[3] 포지션 평가 (evaluatePosition)
    ├─ [3-1] TP 도달 체크
    │   └─ 도달 시 → 자동 익절 (CLOSE_TP)
    │
    ├─ [3-2] SL 도달 체크
    │   └─ 도달 시 → 자동 손절 (CLOSE_SL)
    │
    ├─ [3-3] 마진콜 체크 (선물 거래만)
    │   ├─ 마진 비율 0% 이하 → 강제 청산
    │   ├─ 마진 비율 50% 이하 → 경고 알림
    │   └─ 마진 비율 20% 이하 → 위험 알림
    │
    ├─ [3-4] 타임아웃 체크
    │   └─ 최대 지속 시간 초과 → 자동 종료
    │
    └─ [3-5] 일일 손실 한도 체크
        └─ 한도 도달 → 모든 포지션 강제 종료
```

### 3. 포지션 종료 흐름 (Position Close Flow)

```
포지션 종료 트리거
  ├─ TP 도달 (자동)
  ├─ SL 도달 (자동)
  ├─ 마진콜 청산 (자동)
  ├─ 타임아웃 (자동)
  └─ 수동 종료 (사용자)
  ↓
TradingRepository.closePosition()
  ↓
[1] PnL 계산
    └─ position.calculateUnrealizedPnL(closedPrice)
  ↓
[2] Position 업데이트
    ├─ closed = true
    ├─ closeTime 설정
    ├─ closedPrice 설정
    ├─ pnl 설정
    └─ exitReason 설정 (TP_HIT, SL_HIT, MARGIN_CALL, TIMEOUT, MANUAL)
  ↓
[3] TradeHistory 기록
    └─ CLOSE_TP 또는 CLOSE_SL 타입으로 기록
  ↓
[4] 잔고 업데이트
    └─ userRepository.addToBalance(userId, pnl)
  ↓
[5] 알림 발송 (NotificationHelper)
    ├─ TP 도달 알림
    ├─ SL 도달 알림
    ├─ 마진콜 알림
    └─ 청산 알림
```

## 🔧 주요 클래스 역할

### TradeExecutor
- **역할**: 거래 실행 엔진
- **책임**:
  - 거래 검증 (TradeValidator 호출)
  - 거래 크기 계산 (TradeCalculator 호출)
  - 포지션 생성 및 저장
  - 잔고 차감

### TradeValidator
- **역할**: 거래 검증 시스템
- **책임**:
  - TP/SL 위치 검증
  - 위험도 검증
  - 포지션 수 제한 검증
  - 일일 손실 한도 검증
  - 잔고 검증

### TradeCalculator
- **역할**: 거래 계산 엔진
- **책임**:
  - 위험 자금 계산
  - 거래 수량 계산
  - R:R 비율 계산
  - 손익 계산 (레버리지 포함)

### TradingRepository
- **역할**: 포지션 데이터 관리
- **책임**:
  - 포지션 CRUD 작업
  - TradeHistory 기록
  - 활성 포지션 조회

### TradingMonitorWorker
- **역할**: 포지션 모니터링 워커
- **책임**:
  - 주기적으로 활성 포지션 체크
  - TP/SL 도달 감지
  - 마진콜 감지 및 처리
  - 타임아웃 감지
  - 일일 손실 한도 체크

## 📊 데이터 흐름

### 거래 진입 시
```
UserSettings (위험 관리 설정)
  ↓
TradeCalculator.calculateRiskAmount()
  ↓
TradeCalculator.calculateTrade()
  ↓
Position 엔티티 생성
  ├─ entryPrice
  ├─ takeProfit
  ├─ stopLoss
  ├─ quantity (계산된 거래 수량)
  ├─ leverage
  ├─ riskAmount
  └─ rrRatio
  ↓
TradingRepository.openPosition()
  ↓
DB 저장 (Position 테이블)
```

### 포지션 모니터링 시
```
MarketDataRepository (실시간 가격)
  ↓
TradingMonitorWorker.checkPosition()
  ↓
Position.isTakeProfitReached(currentPrice)
Position.isStopLossReached(currentPrice)
  ↓
조건 만족 시 → closePosition()
```

### 포지션 종료 시
```
Position.calculateUnrealizedPnL(closedPrice)
  ↓
PnL 계산 (레버리지 반영)
  ↓
Position 업데이트
  ├─ pnl
  ├─ closedPrice
  └─ exitReason
  ↓
UserRepository.addToBalance(userId, pnl)
  ↓
잔고 업데이트
```

## ⚠️ 현재 문제점 및 개선 필요 사항

### 1. 거래 진입 로직
- ✅ **구현됨**: TradeExecutor가 거래 검증, 계산, 실행을 담당
- ✅ **구현됨**: TradingActivity에서 UI 입력 처리
- ⚠️ **확인 필요**: TradeExecutor의 비동기 처리 방식 (현재 Thread 사용)

### 2. 포지션 모니터링
- ✅ **구현됨**: TradingMonitorWorker가 주기적으로 체크
- ⚠️ **확인 필요**: Worker 실행 주기 설정
- ⚠️ **확인 필요**: 실시간 가격 업데이트와의 동기화

### 3. 포지션 종료
- ✅ **구현됨**: TP/SL 자동 종료
- ✅ **구현됨**: 마진콜 청산
- ✅ **구현됨**: 타임아웃 종료
- ⚠️ **확인 필요**: 수동 종료 기능 (TradingActivity에서)

### 4. 데이터 일관성
- ⚠️ **확인 필요**: 잔고 차감/증가 시점
- ⚠️ **확인 필요**: 동시 거래 처리 (락 메커니즘)

## 🎯 명확히 정해야 할 사항

### 1. 거래 진입 시점 ✅ **옵션 B로 구현됨**
- **현재 상태**: 
  - 차트에서 EP, TP, SL 라인을 드래그하면
  - `ChartWebViewInterface.onLineUpdated()` 호출
  - `TradingActivity`의 콜백(`onEntryPriceChanged`, `onTakeProfitChanged`, `onStopLossChanged`)이 호출되어
  - 입력 필드(`entryPriceInput`, `tpPriceInput`, `slPriceInput`)에 자동으로 값이 채워짐
  - 사용자가 "거래 진입" 버튼을 클릭해야 실제 거래가 실행됨
  
- **결정 완료**: ✅ **옵션 B** (차트에서 라인을 그린 후, 별도로 "거래 진입" 버튼을 눌러야 함)

### 2. 거래 검증 시점
- **현재 상태**: 
  - `TradeExecutor.executeTrade()` 내부에서 `TradeValidator.validateTrade()` 호출
  - 검증 실패 시 에러 메시지 반환
  - 검증 경고 시 경고 메시지 반환
  
- **확인 필요**:
  - UI에서 실시간으로 검증 결과를 보여주는가? (현재는 거래 실행 시점에만)
  - R:R 비율, Risk Score 등은 실시간으로 계산되어 표시되는가? (TradingActivity에서 일부 구현됨)

### 3. 포지션 모니터링 주기
- **현재 상태**: 
  - `TradingMonitorWorker`가 주기적으로 실행
  - `MarketDataRepository.getCachedPrice()`로 현재 가격 확인
  
- **확인 필요**:
  - Worker 실행 주기는 얼마인가? (WorkManager 설정 확인 필요)
  - 실시간 가격 업데이트(WebSocket)와 모니터링이 어떻게 동기화되는가?
  - 모니터링 주기가 너무 길면 TP/SL 도달을 놓칠 수 있음

### 4. 수동 포지션 종료 ⚠️ **미완성**
- **현재 상태**: 
  - `TradeExecutor.closePositionManually()` 메서드 존재
  - `MainActivity`에는 수동 종료 기능 구현됨
  - `TradingMonitorActivity`에는 **TODO로 남아있음** (line 114: "포지션 종료 기능")
  
- **필요한 작업**:
  - `TradingMonitorActivity`의 `ActivePositionAdapter`에서 포지션 종료 버튼 클릭 시 실제 종료 로직 구현
  - `TradeExecutor.closePositionManually()` 또는 `TradingRepository.closePosition()` 호출

### 5. 잔고 관리
- **현재 상태**: 
  - 거래 진입 시: 잔고 차감 (현물: 전체 금액, 선물: 마진만)
  - 포지션 종료 시: 잔고 증가 (PnL 반영)
  
- **확인 필요**:
  - 미실현 손익은 어떻게 표시되는가? (UI에서 실시간으로 보여주는가?)
  - 선물 거래 시 마진 계산이 정확한가? (`MarginCalculator` 사용)
  - 잔고 업데이트가 트랜잭션으로 처리되는가? (동시 거래 시 데이터 일관성)

## 📝 다음 단계 제안

1. **거래 진입 UI/UX 명확화**
   - 차트에서 EP, TP, SL 라인을 그리면 자동으로 거래가 생성되는지 확인
   - "거래 진입" 버튼의 역할 명확화

2. **실시간 모니터링 개선**
   - TradingMonitorWorker 실행 주기 확인 및 최적화
   - 실시간 가격 업데이트와 모니터링 동기화

3. **수동 종료 기능 구현**
   - TradingMonitorActivity에서 수동 종료 버튼 추가
   - TradeExecutor.closePositionManually() 활용

4. **데이터 일관성 보장**
   - 동시 거래 처리 시 락 메커니즘 추가
   - 잔고 업데이트 트랜잭션 처리

5. **테스트 및 검증**
   - 거래 진입 → 모니터링 → 종료 전체 플로우 테스트
   - 마진콜 및 청산 시나리오 테스트

