# R² - TDS 디자인 시스템 적용 가이드

## 🎨 개요

[Toss Design System (TDS) Mobile](https://tossmini-docs.toss.im/tds-mobile/)의 디자인 철학과 시각적 스타일을 Android 네이티브로 구현했습니다.

---

## ✅ 적용된 TDS 요소

### 1. 색상 시스템 (`colors_tds.xml`)

#### Primary Colors (토스 블루)
```xml
<color name="tds_blue_500">#0064FF</color>
<color name="tds_blue_400">#3182FF</color>
<color name="tds_blue_300">#5CA1FF</color>
```

#### Grayscale (10단계)
```xml
<color name="tds_gray_50">#F9FAFB</color>  <!-- 배경 -->
<color name="tds_gray_900">#191F28</color> <!-- 주요 텍스트 -->
```

#### Semantic Colors
```xml
<color name="tds_success">#12B564</color>  <!-- 수익 -->
<color name="tds_error">#F04452</color>    <!-- 손실 -->
<color name="tds_warning">#FFB800</color>  <!-- 경고 -->
```

### 2. 간격 시스템 (`dimens_tds.xml`)

**8dp 기반 일관된 간격**
```xml
<dimen name="tds_spacing_2">8dp</dimen>
<dimen name="tds_spacing_4">16dp</dimen>
<dimen name="tds_spacing_6">24dp</dimen>
```

### 3. 라운드 코너 시스템

**토스 특유의 부드러운 라운드**
```xml
<dimen name="tds_radius_small">8dp</dimen>
<dimen name="tds_radius_medium">12dp</dimen>
<dimen name="tds_radius_large">16dp</dimen>
```

### 4. 타이포그래피 시스템 (`styles_tds.xml`)

```xml
<style name="TDS_Text_Display">     <!-- 32sp, 굵게 -->
<style name="TDS_Text_Title1">      <!-- 26sp -->
<style name="TDS_Text_Title2">      <!-- 22sp -->
<style name="TDS_Text_Title3">      <!-- 18sp -->
<style name="TDS_Text_Body1">       <!-- 16sp -->
<style name="TDS_Text_Body2">       <!-- 14sp -->
<style name="TDS_Text_Caption">     <!-- 12sp -->
```

### 5. 컴포넌트 스타일

#### Card (TDS Board Row 스타일)
```xml
<style name="TDS_Card">
    <item name="cardCornerRadius">16dp</item>
    <item name="cardElevation">0dp</item>      <!-- Flat 디자인 -->
    <item name="cardBackgroundColor">@color/tds_background</item>
</style>
```

#### Button (TDS Button 스타일)
```xml
<style name="TDS_Button_Primary">
    <item name="backgroundTint">@color/tds_blue_500</item>
    <item name="cornerRadius">12dp</item>
    <item name="android:minHeight">48dp</item>
    <item name="elevation">0dp</item>         <!-- Flat 버튼 -->
</style>
```

#### TextField (TDS TextField 스타일)
```xml
<style name="TDS_TextField">
    <item name="boxCornerRadius">12dp</item>
    <item name="boxStrokeColor">@color/tds_gray_300</item>
</style>
```

---

## 🖼️ 적용된 화면별 TDS 컴포넌트

### Dashboard (`fragment_dashboard_tds.xml`)

#### TDS 카드 기반 레이아웃
- **배경**: `tds_gray_50` (밝은 회색)
- **카드**: 16dp 라운드, 0dp elevation (Flat)
- **간격**: 16dp 일관된 패딩

#### 잔고 카드
- **Display 타이포그래피** (32sp)
- **통계 그리드**: 3컬럼 균등 배치
- **배경 구분**: `tds_gray_50` 섹션

#### 리스크 게이지
- **Custom View** 유지
- **TDS 색상** 적용 (초록/노랑/주황/빨강)

#### 최근 거래 리스트
- **TDS ListRow 스타일**
- **원형 아이콘** (40dp)
- **계층 구조**: 심볼 > 타입/상태 > 손익

### Chart (`fragment_chart_tds.xml`)

#### 상단 헤더
- **흰색 배경** 분리
- **Display 가격** 표시 (32sp)
- **코인 선택** Spinner

#### WebView 차트
- **TradingView Lightweight Chart** 유지
- **흰색 배경**

#### 거래 패널 (TDS BottomSheet)
- **라운드 상단** (16dp)
- **핸들 바** 추가 (40dp × 4dp)
- **R:R 표시**: 파란색 배경 (`#100064FF`)

#### Segmented Control (롱/숏)
- **TDS 스타일** 토글
- **선택 시**: 파란색 배경 + 흰 텍스트
- **미선택 시**: 흰 배경 + 회색 텍스트

#### TextField
- **12dp 라운드**
- **배경색 힌트**:
  - 익절: `tds_success_light`
  - 손절: `tds_error_light`

### Coach (`fragment_coach_tds.xml`)

#### TDS Result 스타일 알림
- **아이콘 + 메시지** 조합
- **48dp 원형 아이콘**
- **배경색**: semantic colors

#### 피드백 카드
- **TDS Card** 스타일
- **아이콘 + 타입 + 메시지** 구조

#### 추천 챌린지
- **Primary 버튼** (파란색)
- **16dp 패딩**

### Journal (`fragment_journal_tds.xml`)

#### Chip Filter (TDS Chip)
- **감정별 필터**
- **배경색**: semantic colors
- **Horizontal Scroll**

#### 저널 아이템
- **TDS Card** 기반
- **감정 뱃지**: 이모지 + 텍스트
- **타임스탬프**: caption 스타일

#### Fixed BottomCTA
- **전체 너비 버튼**
- **하단 고정**
- **4dp elevation**

### Challenge (`fragment_challenge_tds.xml`)

#### Tab Layout (TDS Tab)
- **파란색 Indicator** (3dp)
- **선택/미선택 색상** 구분

#### 챌린지 카드
- **난이도 뱃지** (별 아이콘)
- **진행률 바**: 8dp 높이
- **TDS Progress Bar** 스타일

---

## 🎯 TDS 디자인 원칙 적용

### 1. **일관성** (Consistency)
- ✅ 8dp 기반 간격 시스템
- ✅ 타이포그래피 계층 구조
- ✅ 색상 팔레트 통일

### 2. **간결함** (Simplicity)
- ✅ Flat 디자인 (elevation 0dp)
- ✅ 최소한의 테두리
- ✅ 넉넉한 여백

### 3. **명확성** (Clarity)
- ✅ 계층적 정보 구조
- ✅ 명확한 액션 버튼
- ✅ 색상으로 의미 전달

### 4. **반응성** (Responsiveness)
- ✅ Ripple 효과
- ✅ 상태별 색상 변경
- ✅ Material Design 인터랙션

---

## 🔄 Before & After

### Before (Material Design 3 Dark)
- 다크 테마 (`#1e222d`)
- 복잡한 그림자
- 어두운 색상 팔레트
- 작은 라운드 코너 (8dp)

### After (TDS Light)
- 라이트 테마 (`#F9FAFB`)
- Flat 디자인 (그림자 제거)
- 토스 블루 (`#0064FF`) 강조
- 큰 라운드 코너 (16dp)
- 넉넉한 패딩 (20dp)

---

## 📱 실제 사용된 TDS 패턴

### 1. **Card (Board Row)**
```xml
<MaterialCardView
    style="@style/TDS_Card"
    app:cardCornerRadius="16dp"
    app:cardElevation="0dp"
    android:backgroundTint="@color/tds_background" />
```

### 2. **ListRow**
```xml
<LinearLayout
    android:orientation="horizontal"
    android:padding="16dp">
    
    <View 40dp 원형 아이콘 />
    <TextView 제목 + 설명 />
    <TextView 우측 값 />
</LinearLayout>
```

### 3. **Bottom Sheet**
```xml
<ScrollView
    android:background="@drawable/tds_bottom_sheet_background"
    android:elevation="8dp">
    
    <View 핸들 바 (40dp × 4dp) />
    <LinearLayout 내용 />
</ScrollView>
```

### 4. **Segmented Control**
```xml
<MaterialButtonToggleGroup
    app:singleSelection="true">
    
    <Button 선택 시: 파란색 />
    <Button 미선택 시: 흰색 테두리 />
</MaterialButtonToggleGroup>
```

### 5. **Badge**
```xml
<TextView
    android:background="@color/tds_success_light"
    android:textColor="@color/tds_success"
    android:padding="8dp" />
```

### 6. **Progress Bar**
```xml
<ProgressBar
    style="?android:attr/progressBarStyleHorizontal"
    android:height="8dp"
    android:progressTint="@color/tds_blue_500" />
```

---

## 🎨 TDS vs Material Design 3

| 요소 | Material Design 3 | TDS |
|-----|------------------|-----|
| **주 색상** | Purple/Teal | 토스 블루 (#0064FF) |
| **배경** | Dark (#1e222d) | Light (#F9FAFB) |
| **Elevation** | 2-8dp | 0dp (Flat) |
| **Corner Radius** | 4-8dp | 12-16dp |
| **Padding** | 8-12dp | 16-20dp |
| **Typography** | Roboto | Sans-serif Medium |
| **버튼 높이** | 40dp | 48-56dp |

---

## 📊 적용 통계

| 항목 | 수량 |
|-----|------|
| **TDS 색상** | 30+ |
| **TDS 스타일** | 15+ |
| **TDS 레이아웃** | 8개 |
| **TDS Drawable** | 6개 |
| **총 리소스 파일** | 20+ |

---

## 🚀 결과

### ✅ 완료된 TDS 적용

1. **색상 시스템** - 토스 블루 중심의 밝은 팔레트
2. **타이포그래피** - 계층적 텍스트 스타일
3. **간격 시스템** - 8dp 기반 일관된 간격
4. **컴포넌트 스타일** - Card, Button, TextField
5. **레이아웃 패턴** - ListRow, BottomSheet, Badge
6. **테마** - Light 테마 기본, 다크 모드 지원

### 📱 화면별 적용

- ✅ **Dashboard**: TDS Card + ListRow
- ✅ **Chart**: TDS BottomSheet + Segmented Control
- ✅ **Coach**: TDS Result + Badge
- ✅ **Challenge**: TDS Tab + Progress Bar
- ✅ **Journal**: TDS Chip + Fixed BottomCTA

### 🎯 TDS 디자인 원칙 준수

- ✅ **일관성**: 8dp 간격, 통일된 라운드
- ✅ **간결함**: Flat 디자인, 최소 테두리
- ✅ **명확성**: 계층적 정보, 명확한 액션
- ✅ **브랜드**: 토스 블루 강조색

---

## 💡 TDS vs 기존 디자인 비교

### 주요 변경사항

1. **다크 → 라이트**
   - Before: `#1e222d` 다크
   - After: `#F9FAFB` 라이트

2. **그림자 제거**
   - Before: 2-4dp elevation
   - After: 0dp (Flat 디자인)

3. **라운드 증가**
   - Before: 8dp
   - After: 16dp

4. **패딩 증가**
   - Before: 12dp
   - After: 20dp

5. **색상 변경**
   - Before: 파랑(`#2196F3`), 초록(`#26a69a`)
   - After: 토스 블루(`#0064FF`), TDS 초록(`#12B564`)

---

## 🎨 디자인 가이드라인

### 카드 사용
```xml
<MaterialCardView
    style="@style/TDS_Card"
    android:backgroundTint="@color/tds_background" />
```

### 버튼 사용
```xml
<!-- Primary -->
<Button
    style="@style/TDS_Button_Primary"
    android:background="@drawable/tds_button_primary" />

<!-- Secondary -->
<Button
    style="@style/TDS_Button_Secondary"
    android:background="@drawable/tds_button_secondary" />
```

### 텍스트 사용
```xml
<TextView
    style="@style/TDS_Text_Title1"
    android:text="제목" />

<TextView
    style="@style/TDS_Text_Body2"
    android:text="본문" />
```

---

## 📚 참고 링크

- [TDS Mobile 공식 문서](https://tossmini-docs.toss.im/tds-mobile/)
- TDS 핵심 원칙: 일관성, 간결함, 명확성
- TDS 컴포넌트: Card, Button, Badge, BottomSheet, Chip, Tab

---

## 🏆 성과

✅ **완전한 TDS 스타일 적용**
✅ **30+ TDS 색상 정의**
✅ **15+ TDS 스타일 컴포넌트**
✅ **8개 TDS 레이아웃**
✅ **빌드 성공** (7.9MB APK)

R² 앱이 이제 **토스 스타일의 세련되고 일관된 디자인**을 갖추게 되었습니다! 🎉

