# R² (Risk & Reward) - 리스크 감각 트레이닝 Android 앱

## 📱 프로젝트 개요

R²은 초보~중수 트레이더를 위한 리스크 감각 트레이닝 모바일 앱입니다. 수익보다는 **리스크 관리 역량**과 **행동 심리적 습관 개선**을 데이터 기반으로 학습시키는 것이 핵심입니다.

## 🏗️ 아키텍처

- **패턴**: MVVM (Model-View-ViewModel)
- **언어**: Java
- **최소 SDK**: 24 (Android 7.0)
- **타겟 SDK**: 35

## 🛠️ 기술 스택

### 데이터베이스
- **Room** (2.6.1): 로컬 데이터베이스
  - User, Position, TradeHistory, Journal, Challenge, Badge 엔티티

### 네트워크
- **Retrofit2** (2.9.0): RESTful API 통신
- **OkHttp** (4.12.0): HTTP 클라이언트
- **CoinGecko API**: 실시간 암호화폐 시세

### UI
- **Material Design Components** (1.13.0)
- **MPAndroidChart** (3.1.0): 차트 시각화
- **TradingView Lightweight Chart**: WebView 기반 인터랙티브 차트

### 백그라운드 작업
- **WorkManager** (2.9.1): 주기적 포지션 모니터링 및 자동 청산

### 기타
- **Lifecycle Components** (2.8.7): ViewModel, LiveData
- **Navigation Component** (2.8.5): Fragment 네비게이션

## 📦 프로젝트 구조

```
com.example.rsquare/
├── data/
│   ├── local/              # Room 데이터베이스
│   │   ├── entity/         # 엔티티 클래스
│   │   ├── dao/            # DAO 인터페이스
│   │   └── AppDatabase     # 데이터베이스 인스턴스
│   ├── remote/             # API 통신
│   │   ├── model/          # API 모델
│   │   └── CoinGeckoApiService
│   └── repository/         # 리포지토리 패턴
│       ├── UserRepository
│       ├── TradingRepository
│       ├── JournalRepository
│       ├── ChallengeRepository
│       └── MarketDataRepository
├── domain/                 # 비즈니스 로직
│   ├── RiskCalculator      # 리스크 계산 엔진
│   ├── RiskMetrics
│   ├── MonteCarloSimulator # Monte Carlo 시뮬레이션
│   ├── BehaviorAnalyzer    # 행동 패턴 분석
│   └── CoachingEngine      # AI 코치 엔진
├── ui/                     # UI 레이어
│   ├── dashboard/          # 대시보드
│   ├── chart/              # 차트 및 거래
│   ├── trade/              # 포지션 관리
│   ├── journal/            # 거래 저널
│   ├── challenge/          # 챌린지 시스템
│   └── coach/              # AI 코치
├── worker/                 # 백그라운드 작업
│   └── TradingMonitorWorker
└── util/                   # 유틸리티
    ├── Constants
    ├── NotificationHelper
    └── NumberFormatter
```

## 🎨 디자인 시스템

**[Toss Design System (TDS)](https://tossmini-docs.toss.im/tds-mobile/) 스타일 적용**

- ✅ 토스 블루 (#0064FF) 중심의 색상 팔레트
- ✅ 8dp 기반 일관된 간격 시스템
- ✅ Flat 디자인 (elevation 0dp)
- ✅ 부드러운 라운드 코너 (12-16dp)
- ✅ 계층적 타이포그래피 (7단계)
- ✅ TDS 컴포넌트: Card, Button, Badge, BottomSheet, Chip

자세한 내용: [TDS_DESIGN_GUIDE.md](TDS_DESIGN_GUIDE.md)

## 🎯 주요 기능

### 1. 대시보드
- 잔고 및 누적 손익 표시
- 실시간 리스크 스코어 게이지
- 최근 거래 목록
- 주간 리포트 (변동성, Sharpe Ratio, MDD)

### 2. 인터랙티브 차트 기반 가상 트레이딩
- TradingView 차트 (WebView + JavaScript Bridge)
- 드래그 가능한 TP/SL 라인
- 실시간 R:R 비율 계산 및 피드백
- 리스크 스코어 즉시 표시

### 3. 모의 트레이딩 엔진
- 가상 잔고 관리
- 자동 TP/SL 청산 (WorkManager)
- 거래 기록 자동 저장
- 알림 시스템

### 4. 리스크 계산 엔진
- **리스크 스코어 계산** (0-100)
  ```
  score = 100 - (0.4×변동성 + 0.4×MDD + 0.2×negSharpe)
  ```
- 변동성 (Volatility)
- 최대 낙폭 (Maximum Drawdown)
- 샤프 비율 (Sharpe Ratio)
- R:R 비율 계산

### 5. Monte Carlo 시뮬레이션
- 현재 R:R 값 기반 100회 가상 매매
- 기대 수익, 최대 손실, Sharpe Ratio 계산
- 확률 분포 시각화
- Probability Cone 차트

### 6. 트레이드 기록 & 저널
- 모든 거래 기록 저장
- 감정 태그 (불안/욕심/집중/FOMO 등)
- 거래별 메모 작성
- 리플레이 모드 (구현 예정)

### 7. AI 리스크 코치
- **행동 패턴 자동 감지**:
  - 손실 회피 (Loss Aversion)
  - 복수 매매 (Revenge Trading)
  - 과매매 (Overtrading)
  - 충동적 행동 (Impulsive Behavior)
  - 부실한 리스크 관리
- 맞춤 피드백 메시지 제공
- 챌린지 자동 추천
- 주간 리포트 생성

### 8. 챌린지 & 뱃지 시스템
- 목표 기반 미션
  - R:R 비율 달성
  - 연속 성공 (Win Streak)
  - 일관된 거래
  - 감정 조절
- 난이도별 챌린지 (Easy/Medium/Hard)
- 뱃지 획득 시스템
- 진행률 추적

## 🔔 알림 시스템

- **거래 알림**: TP/SL 도달
- **코치 피드백**: 행동 패턴 감지 및 리스크 경고
- **일일 리마인더**: 거래 계획 점검

## 🚀 시작하기

### 요구사항
- Android Studio Arctic Fox 이상
- JDK 11
- Android SDK 24 이상

### 빌드 및 실행

1. 프로젝트 클론
```bash
git clone <repository-url>
cd RSquare
```

2. Android Studio에서 프로젝트 열기

3. Gradle 동기화
```bash
./gradlew build
```

4. 에뮬레이터 또는 실제 기기에서 실행

## 📊 데이터베이스 스키마

### User
- id (PK)
- nickname
- balance
- createdAt

### Position
- id (PK)
- userId (FK)
- symbol
- quantity
- entryPrice, takeProfit, stopLoss
- isLong
- openTime, closeTime
- isClosed, pnl

### TradeHistory
- id (PK)
- positionId (FK)
- symbol, type (BUY/SELL/CLOSE_TP/CLOSE_SL)
- price, quantity, pnl
- timestamp

### Journal
- id (PK)
- positionId (FK)
- emotion (ENUM)
- note
- timestamp

### Challenge
- id (PK)
- userId (FK)
- title, description
- targetValue, targetType
- difficulty, status, progress
- createdAt, completedAt

### Badge
- id (PK)
- userId (FK)
- badgeType (ENUM)
- name, description
- earnedAt

## 🧪 테스트

### 테스트 데이터 생성
`TestDataGenerator` 클래스를 사용하여 개발/테스트용 목 데이터 생성:

```java
User testUser = TestDataGenerator.createTestUser();
List<Position> positions = TestDataGenerator.createTestPositions(userId, 10, true);
Journal journal = TestDataGenerator.createRandomJournal(positionId);
```

## 🔒 권한

앱에서 사용하는 권한:
- `INTERNET`: API 통신
- `ACCESS_NETWORK_STATE`: 네트워크 상태 확인
- `POST_NOTIFICATIONS`: 알림 전송
- `VIBRATE`: 진동 피드백

## 📝 TODO

### 완료된 기능
- ✅ MVVM 아키텍처 구조
- ✅ Room 데이터베이스 전체 구현
- ✅ Retrofit + CoinGecko API 통합
- ✅ 리스크 계산 엔진
- ✅ Monte Carlo 시뮬레이션
- ✅ AI 행동 패턴 분석 엔진
- ✅ 코칭 시스템
- ✅ ViewModel 레이어 전체
- ✅ 알림 시스템
- ✅ WorkManager 백그라운드 모니터링
- ✅ **TradingView Lightweight Chart WebView 통합**
- ✅ **JavaScript Bridge 구현**
- ✅ **MPAndroidChart 통합**
- ✅ **Custom Views (RiskGaugeView, ProbabilityConeView)**
- ✅ **대시보드 Fragment 완전 구현**
- ✅ **차트 Fragment with WebView**
- ✅ **다크 테마 완성**
- ✅ **Material Design 3 UI**

### 추가 개선 가능
- ⬜ 리플레이 모드 고도화
- ⬜ 주간 리포트 상세 UI
- ⬜ 애니메이션 및 트랜지션 추가
- ⬜ 단위 테스트 작성
- ⬜ UI 테스트 작성
- ⬜ 실제 거래 기록 페이징 처리
- ⬜ 오프라인 모드 지원

## 📖 참고 문서

- [PRD.md](PRD.md): 전체 요구사항 문서
- [Android Developer Guide](https://developer.android.com)
- [Room Persistence Library](https://developer.android.com/training/data-storage/room)
- [Retrofit](https://square.github.io/retrofit/)
- [CoinGecko API](https://www.coingecko.com/en/api)
- [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart)

## 🤝 기여

이 프로젝트는 교육 목적으로 개발되었습니다.

## 📄 문서

- [README.md](README.md) - 프로젝트 개요
- [PRD.md](PRD.md) - 원본 요구사항
- [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md) - 구현 상세
- [TDS_DESIGN_GUIDE.md](TDS_DESIGN_GUIDE.md) - TDS 적용 가이드
- [FINAL_SUMMARY.md](FINAL_SUMMARY.md) - 최종 요약

## 📄 라이선스

이 프로젝트는 교육 및 연구 목적으로 자유롭게 사용할 수 있습니다.

## 🎓 교육적 가치

R² 앱은 다음과 같은 교육적 목표를 달성합니다:

1. **리스크 우선 사고**: 수익보다 리스크 관리를 먼저 생각하는 습관
2. **데이터 기반 의사결정**: 감정이 아닌 데이터로 판단
3. **행동 심리 인식**: 자신의 거래 패턴을 객관적으로 파악
4. **점진적 개선**: 챌린지를 통한 단계적 실력 향상
5. **안전한 학습 환경**: 실제 자금 없이 리스크 감각 훈련

---

**Built with ❤️ for traders who want to master risk management**

