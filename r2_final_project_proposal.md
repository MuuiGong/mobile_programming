# R² (Risk and Reward)
## 리스크 관리 능력 훈련 모바일 앱 — 최종 프로젝트 제안서

---

**작성일:** 2025년 11월 16일  
**버전:** 1.0  
**상태:** 완성 (과제 제출용)

---

## 📌 1. 프로젝트 개요

### 1.1 프로젝트명
**R² (Risk and Reward) — 리스크 관리 능력 훈련 모바일 앱**

### 1.2 한 줄 소개
**"수익이 아닌, 리스크 관리"** — 초보 거래자들이 실제 금전 손실 없이 **위험 감각과 자금 관리 능력**을 훈련하는 모의 거래 앱

### 1.3 핵심 목표
- ❌ 거래로 돈 버는 것이 목표가 아님
- ✅ **리스크 관리 능력** 배양이 목표
- ✅ 초보자의 **감정적 거래, 손실 회피, 오버트레이딩** 교정
- ✅ 실제 손실 없이 **현실적인 거래 경험** 제공

### 1.4 플랫폼
- **OS:** Android (Java)
- **대상:** 암호화폐 거래 초보자 ~ 중급자
- **설치:** Google Play Store

---

## 🎯 2. R²만의 차별성

### 2.1 기존 거래 앱의 문제점
```
❌ 수익 중심 → 무리한 거래 조장
❌ 리스크 무시 → 파산 위험
❌ 심리 분석 부재 → 행동 교정 불가
❌ 단순 기록만 → 학습 효과 낮음
```

### 2.2 R²의 혁신적 해결책
```
✅ 리스크 관리 능력 중심
   - 모든 거래에서 R:R 비율 강제
   - TP/SL 필수 설정
   - 위험도 점수 실시간 표시

✅ 행동 피드백 시스템
   - AI 코치가 거래 패턴 분석
   - 감정 저널로 심리 추적
   - 개선 제안 실시간 제공

✅ 마진 시스템 & 자동 청산
   - 실제 레버리지 거래의 위험 시뮬레이션
   - 마진콜 경고 & 자동 청산 체험
   - 리스크 관리의 중요성 깨달음

✅ 정량적 성과 분석
   - Sharpe Ratio, Max Drawdown 계산
   - Monte Carlo 시뮬레이션
   - 시각적 대시보드
```

---

## 🏗️ 3. 시스템 아키텍처

### 3.1 기술 스택
| 계층 | 기술 | 상세 |
|------|------|------|
| **UI/UX** | Java (Activity/Fragment) | MVVM 패턴 |
| **Design** | TDS (Toss Design System) | 다크 모드 기본 |
| **Layout** | XML + ConstraintLayout | 반응형 설계 |
| **차트** | TradingView Advanced Charts | WebView 연동 |
| **데이터** | Room (SQLite) | 로컬 DB |
| **API** | CoinGecko API | 실시간 시세 |
| **실시간** | WebSocket + Handler | 1초 단위 업데이트 |
| **알림** | NotificationManager | 푸시 알림 |

### 3.2 주요 컴포넌트
```
┌─────────────────────────────────────┐
│     MainActivity (대시보드)         │
│  [계정] [Risk Score] [포지션]       │
└──────────────┬──────────────────────┘
               │
    ┌──────────┼──────────┐
    │          │          │
    ▼          ▼          ▼
TradingActivity  HistoryActivity  AnalysisActivity
(거래 실행)      (기록 조회)      (성과 분석)
    │          │          │
    └──────────┼──────────┘
               │
        ┌──────▼────────┐
        │  TradeEngine  │
        │  (계산 엔진)  │
        └──────────────┘
               │
        ┌──────▼──────────────┐
        │  MarginCalculator   │
        │  MarginMonitor      │
        │  LiquidationEngine  │
        │  (마진 & 청산)      │
        └─────────────────────┘
               │
        ┌──────▼────────┐
        │  Room DB      │
        │  (거래 기록)  │
        └───────────────┘
```

---

## 📊 4. 핵심 기능 설계

### 4.1 대시보드 화면 (MainActivity)
```
┌─────────────────────────────┐
│  R² 리스크 트레이닝         │
│  수익이 아닌, 리스크 감각   │
├─────────────────────────────┤
│                             │
│ 💰 계정 현황               │
│   잔고: $10,000.00          │
│   순자산: $10,250.50 (+2.5%)│
│                             │
│ ⚠️ Risk Score              │
│   75/100 🟢 (안정)          │
│   [████████░░]              │
│                             │
│ 📈 활성 포지션: 2개        │
│  [1] BTCUSDT 5x | +$33.30  │
│  [2] ETHUSDT 1x | -$15.50  │
│                             │
│ 🎯 오늘의 성과              │
│   거래: 5회 | 승률: 60%    │
│   총 수익: +$250.50         │
│                             │
│ [+ 새 거래] [기록] [분석]  │
└─────────────────────────────┘
```

### 4.2 거래 실행 화면 (TradingActivity)
```
[TradingView 차트]
├─ 실시간 BTCUSDT 1H 차트
├─ Entry/TP/SL 라인 드래그 가능
└─ R:R 비율 실시간 표시

[거래 제어판]
├─ Entry: $95,836.00
├─ TP: $97,752.72 (초록)
├─ SL: $93,919.28 (빨강)
├─ Leverage: 5x [1x▼]
├─ R:R Ratio: 1.24:1
├─ Risk Score: 75/100 🟢
└─ [거래 진입]

[자동 종료 시스템]
├─ TP 도달 → 자동 익절 + 알림
├─ SL 도달 → 자동 손절 + 알림
└─ 마진 0% → 자동 청산 + 경고
```

### 4.3 Risk Score 시스템
```
공식: Score = 100 - (0.4×Volatility + 0.4×MDD + 0.2×NegativeSharpe)

해석:
  🟢 71-100: 안정적 (좋음)
  🟡 31-70: 주의 (주의 필요)
  🔴 0-30: 위험 (즉시 개선)

측정 기준:
  ├─ Volatility: 수익의 변동성
  ├─ MDD: 최대 낙폭
  ├─ Sharpe Ratio: 위험대비 수익률
  ├─ Win Rate: 승률
  └─ R:R Ratio: 수익손실 비율
```

### 4.4 마진 & 청산 시스템
```
3단계 경고:

1️⃣ 정상 (100% 초과)
   상태: ✅ 안전
   마진 비율: 충분
   조치: 계속 거래 가능

2️⃣ 마진콜 (50% ~ 100%)
   상태: 🟡 주의
   마진 비율: 부족
   조치: 경고 표시 + 긴급 종료 옵션

3️⃣ 자동 청산 (0% 이하)
   상태: 💥 청산됨
   마진 비율: 소진
   조치: 자동 포지션 종료
        → 심리적 영향 시뮬레이션
        → 레버리지의 위험 체험
```

### 4.5 AI 코치 피드백
```
감지되는 위험 패턴:
  ├─ Overtrading: "거래가 너무 많습니다"
  ├─ Loss Aversion: "손실 복구 시도가 보입니다"
  ├─ Moving SL: "손절을 변경하고 있습니다"
  └─ Emotional Trading: "감정적 거래 패턴"

제공되는 피드백:
  "최근 3거래가 모두 손절입니다.
   더 큰 SL 범위를 고려하세요."

  "R:R 비율이 0.8:1입니다.
   최소 1.0:1 이상을 유지하세요."

  "좋은 진입입니다! 이런 R:R 설정을 유지하세요."
```

---

## 💾 5. 데이터 구조

### 5.1 핵심 엔티티 관계도
```
User
├─ userId
├─ initialBalance ($10,000)
├─ tradeMode (SPOT / FUTURES)
└─ createdAt

Position (활성 포지션)
├─ positionId
├─ symbol (BTCUSDT)
├─ entryPrice, tp, sl
├─ leverage (1x ~ 20x)
├─ tradeSize, currentPrice
├─ marginRatio (마진 비율)
└─ status (OPEN)

Trade (종료된 거래)
├─ tradeId
├─ entryPrice, exitPrice
├─ pnl, pnlPercent
├─ rrRatio (1.24:1)
├─ exitReason (TP/SL/LIQUIDATION)
├─ duration (거래 시간)
└─ status (CLOSED)

Journal (감정 기록)
├─ journalId
├─ tradeId (FK)
├─ emotion (불안/집중/욕심)
├─ memo
└─ createdAt
```

### 5.2 거래 기록 저장 정보
```json
{
  "tradeId": "trade_20251116_001",
  "symbol": "BTCUSDT",
  "timeframe": "1H",
  "tradeType": "FUTURES",
  "leverage": 5,
  "entryTime": "2025-11-16T18:00:00Z",
  "entryPrice": 95836.00,
  "tp": 97752.72,
  "sl": 93919.28,
  "exitTime": "2025-11-16T18:45:00Z",
  "exitPrice": 97752.72,
  "exitReason": "TP_HIT",
  "pnl": 49.95,
  "pnlPercent": 49.95,
  "rrRatio": 1.24,
  "duration": "00:45:00",
  "status": "CLOSED"
}
```

---

## 🎨 6. UI/UX 설계 (TDS 다크 모드)

### 6.1 색상 팔레트
| 요소 | 색상 | 용도 |
|------|------|------|
| 배경 | #050A0E | 메인 배경 |
| 카드 | #0D1117 | 콘텐츠 배경 |
| 텍스트 주 | #FFFFFF | 제목, 주요 텍스트 |
| 텍스트 보조 | #9CA3AF | 라벨, 보조 텍스트 |
| 성공 (수익) | #10B981 | TP, 수익 표시 |
| 오류 (손실) | #EF4444 | SL, 손실 표시 |
| 주의 | #F59E0B | 마진콜 경고 |
| 정보 | #3B82F6 | 기타 정보 |

### 6.2 주요 화면 레이아웃
```
Activity 구조:

1. MainActivity (대시보드)
   ├─ HeaderLayout (상단 헤더)
   ├─ ScrollView
   │  ├─ AccountCard (계정 현황)
   │  ├─ RiskScoreCard (위험도)
   │  ├─ PositionsCard (활성 포지션)
   │  └─ PerformanceCard (성과)
   └─ BottomNavigation ([새 거래] [기록] [분석])

2. TradingActivity (거래)
   ├─ Toolbar (심볼, 타임프레임)
   ├─ WebView (TradingView 차트)
   └─ ControlPanel
      ├─ InputFields (Entry/TP/SL)
      ├─ InfoDisplay (R:R, Risk Score)
      └─ ActionButton ([거래 진입])

3. HistoryActivity (기록)
   ├─ FilterBar ([오늘/전체] [승리/손실])
   ├─ RecyclerView (거래 목록)
   └─ Statistics (통계 요약)

4. AnalysisActivity (분석)
   ├─ PerformanceChart (수익 곡선)
   ├─ StatisticsPanel (Sharpe, MDD)
   ├─ CoachFeedback (AI 피드백)
   └─ MonteCarloSimulation (시뮬레이션)
```

---

## 🔧 7. 주요 Java 클래스 설계

### 7.1 데이터 모델
```java
// Trade.java - 거래 기록
public class Trade {
    public long tradeId;
    public String symbol;
    public double entryPrice, tp, sl;
    public double leverage;
    public double tradeSize;
    public double pnl, pnlPercent;
    public double rrRatio;
    public String exitReason;  // TP_HIT, SL_HIT, LIQUIDATION
    public long duration;
    public String status;  // CLOSED
}

// Position.java - 활성 포지션
public class Position {
    public long positionId;
    public String symbol;
    public double entryPrice, tp, sl;
    public double leverage;
    public double currentPrice;
    public double pnl, pnlPercent;
    public double marginRatio;  // 마진 비율 (%)
    public double liquidationPrice;
    public boolean isMarginCallTriggered;
    public String status;  // OPEN
}

// Settings.java - 사용자 설정
public class Settings {
    public double initialBalance;  // $10,000
    public String tradeMode;  // SPOT / FUTURES
    public double defaultLeverage;  // 1x ~ 20x
    public double riskAmount;  // $100
    public int maxPositions;  // 3개
    public double maxDailyLoss;  // 10%
}
```

### 7.2 거래 엔진
```java
// TradeEngine.java - 거래 계산
public class TradeEngine {
    public double calculateTradeSize(double entry, double sl, 
                                    double risk, double leverage);
    public double calculatePnL(double entry, double exit, 
                             double size, double leverage);
    public double calculateRRRatio(double entry, double tp, double sl);
    public ValidationResult validateTrade(double entry, double tp, 
                                         double sl, double risk);
}

// RiskCalculator.java - 위험도 계산
public class RiskCalculator {
    public int calculateRiskScore(double leverage, double rrRatio, 
                                 double winRate, double maxDrawdown);
    public double calculateSharpeRatio(List<Double> returns);
    public double calculateMaxDrawdown(List<Double> equityCurve);
}

// MarginCalculator.java - 마진 계산
public class MarginCalculator {
    public double calculateRequiredMargin(double positionSize, double leverage);
    public double calculateMarginRatio(double available, double used);
    public double calculateLiquidationPrice(double entry, double size, 
                                           double leverage, double margin);
    public boolean shouldLiquidate(double marginRatio);
}

// LiquidationEngine.java - 자동 청산
public class LiquidationEngine {
    public void executeLiquidation(Position position, double price, 
                                  String reason);
}
```

### 7.3 서비스
```java
// PositionService.java - 포지션 관리
public class PositionService {
    public Position createPosition(...);
    public Trade closePosition(Position position, double exit, String reason);
    public List<Position> getActivePositions();
}

// PositionMonitoringService.java - 통합 모니터링
public class PositionMonitoringService {
    public void startMonitoring(Position position);
    // PriceListener, MarginListener, LiquidationListener 구현
}

// TradeDao.java - 거래 데이터 접근
@Dao
public interface TradeDao {
    @Insert long insertTrade(Trade trade);
    @Query LiveData<List<Trade>> getAllClosedTrades();
    @Query LiveData<Double> getTotalPnL();
}
```

---

## 📈 8. 개발 일정 (10주)

| Phase | 기간 | 작업 항목 | 완료도 |
|-------|------|---------|-------|
| **1. UI 설계** | 1주 | XML Layout, TDS 적용 | ✅ |
| **2. 데이터층** | 2주 | Room DB, API 연동 | 70% |
| **3. 거래 엔진** | 2주 | TradeEngine, 계산 로직 | ✅ |
| **4. 마진 시스템** | 1주 | MarginCalculator, 청산 | ✅ |
| **5. UI 구현** | 2주 | Activity, 실시간 업데이트 | 50% |
| **6. 고급 기능** | 1주 | AI 코치, 시뮬레이션 | 0% |
| **7. 테스트** | 1주 | 단위/UI 테스트 | 0% |

---

## ✅ 9. 성공 지표

### 기술적 지표
- ✅ 거래 자동 종료 정확도: 100%
- ✅ 실시간 업데이트: <1초
- ✅ 앱 크래시: 0건
- ✅ 데이터 손실: 0건

### 사용자 경험
- ✅ 초보자 이해도: 90%
- ✅ Risk Score 인식: 85%
- ✅ 위험 관리 개선: 70%

### 교육 효과
- ✅ 거래 심리 이해: 80%
- ✅ 자금 관리 규율: 75%
- ✅ 장기 성공률: 향상 기대

---

## 🎯 10. 결론

### R²의 혁신성
**R²는 기존 거래 앱의 패러다임을 바꾸는 앱입니다.**

- ❌ "돈을 버는 법" 가르치는 앱이 아님
- ✅ **"돈을 잃지 않는 법" 가르치는 앱**

### 핵심 가치
```
1. 심리 교정
   - 감정적 거래 패턴 인식
   - 위험 회피 심리 개선
   - 장기 거래 마인드셋 형성

2. 능력 배양
   - 리스크 관리 능력
   - 자금 관리 기술
   - 기술적 분석 스킬

3. 안전한 학습
   - 실제 손실 없음
   - 현실과 동일한 경험
   - 반복 학습 가능

4. 과학적 분석
   - 정량적 피드백
   - 행동 패턴 분석
   - 데이터 기반 개선안
```

### 기대 효과
- 초보자의 거래 성공률 증가
- 장기적 수익성 개선
- 거래 심리학 이해도 향상
- 리스크 관리 규율 형성
- 감정 기반 거래 감소

---

## 📚 11. 참고 자료

### 학술 참고
1. **Markowitz, H.** (1952). "Portfolio Selection"
   - 현대 포트폴리오 이론의 기초

2. **Sharpe, W. F.** (1966). "Mutual Fund Performance"
   - Sharpe Ratio 개발자

3. **Kahneman, D., & Tversky, A.** (1979). "Prospect Theory"
   - 행동 경제학 (손실 회피, 위험 인식)

4. **Thaler, R. H.** (1999). "Mental Accounting and Marketmaking"
   - 거래자의 심리적 편향

### 기술 참고
- **TradingView API Docs:** https://www.tradingview.com/charting-library-docs/
- **CoinGecko API:** https://www.coingecko.com/api/documentations/v3
- **Android Development:** https://developer.android.com/
- **Room Database:** https://developer.android.com/training/data-storage/room
- **Retrofit2:** https://square.github.io/retrofit/

### 디자인 참고
- **Toss Design System:** https://toss.tech/design

---

## 📋 부록: 구현 체크리스트

### Phase 1: UI/UX (✅ 완료)
- [x] 색상 체계 정의 (TDS)
- [x] MainActivity.xml 설계
- [x] TradingActivity.xml 설계
- [x] HistoryActivity.xml 설계
- [x] AnalysisActivity.xml 설계

### Phase 2: 데이터 계층 (70% 진행)
- [x] Trade.java 모델
- [x] Position.java 모델
- [x] Settings.java 모델
- [ ] TradeDao 구현
- [ ] Room Database 구현

### Phase 3: 거래 엔진 (✅ 완료)
- [x] TradeEngine.java
- [x] RiskCalculator.java
- [x] MarginCalculator.java
- [x] LiquidationEngine.java
- [x] PriceMonitor.java

### Phase 4: 서비스 (50% 진행)
- [x] PositionService.java
- [x] PositionMonitoringService.java
- [ ] NotificationService.java
- [ ] PriceUpdateService.java

### Phase 5: Activity 구현 (50% 진행)
- [x] MainActivity.java (기본 구조)
- [x] TradingActivity.java (기본 구조)
- [ ] HistoryActivity.java
- [ ] AnalysisActivity.java
- [ ] LiquidationWarningActivity.java

### Phase 6: 고급 기능 (0% 진행)
- [ ] AI 코치 (패턴 분석)
- [ ] Monte Carlo 시뮬레이션
- [ ] 감정 저널 기능
- [ ] 차트 분석 도구

### Phase 7: 테스트 (0% 진행)
- [ ] 단위 테스트 (JUnit)
- [ ] UI 테스트 (Espresso)
- [ ] 통합 테스트
- [ ] 성능 테스트

---

## 🚀 마지막 말씀

**R²은 단순한 거래 앱이 아닙니다.**

이 앱은 초보 거래자들이:
- 실제 자금 손실 없이
- 현실적인 거래 경험을 하면서
- 리스크 관리의 중요성을 깨닫고
- 장기적으로 거래 성공의 길을 걷도록 돕는

**거래 교육 플랫폼**입니다.

---

**R²와 함께, 진정한 거래 마스터가 되세요!** 🚀

**제안서 작성 완료**  
**2025년 11월 16일**

