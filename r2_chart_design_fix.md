# R² 차트 디자인 정확한 구현 프롬프트 — TradingView 스타일 TP/SL 라인

## 🎯 디자인 목표

> **중요:** 이전 구현은 라인이 너무 두껍고, 정보 패널이 크며, 차트와 컨트롤이 분리되어 있습니다.  
> 아래는 **원본 디자인을 정확히 재현**하기 위한 상세 가이드입니다.

---

## 🖼️ 원본 디자인 핵심 요소

### 1. 차트 레이아웃
- **차트가 화면의 60-70% 차지** (전체적으로 차트 중심)
- **배경:** 어두운 그레이/블랙 (#0D1117 또는 유사)
- **캔들스틱:** 상승(초록 #26a69a), 하락(빨강 #ef5350)
- 타임프레임 버튼이 차트 상단에 오버레이

### 2. TP/SL 라인 스타일 (핵심!)

#### 라인 외관
- **매우 얇은 수평선** (1-2px)
- **TP 라인:** 초록색 (#00C853 또는 #26a69a)
- **SL 라인:** 빨간색 (#FF1744 또는 #ef5350)
- **Entry 라인:** 회색 또는 흰색 (#9E9E9E)

#### 라인 라벨 (TP, SL, 1L 버튼)
- 라인의 **왼쪽 끝**에 작은 직사각형 버튼 형태로 붙음
- **크기:** 약 40-50px 너비, 24-28px 높이
- **배경:** 라인과 같은 색상 (TP=초록, SL=빨강, Entry=회색)
- **텍스트:** 흰색, 볼드, 12-14sp
- **텍스트 내용:** "TP", "SL", "1L"(Entry)
- **모서리:** 약간 둥글게 (4-6dp radius)

#### 라인 드래그 중 시각 피드백
- 드래그 중: 라인이 **점선**(dashed)으로 변경
- 드래그 중: 라인 옆에 **현재 가격 툴팁** 표시 (예: "$93965.34")
- 드래그 중: 라인 **굵기 약간 증가** (2-3px)
- 드래그 종료 시: 다시 원래 얇은 실선으로 복귀

#### 영역 색상 채우기 (핵심!)
- **Entry와 TP 사이:** 초록색 반투명 영역 (#00C853 20% opacity)
- **Entry와 SL 사이:** 빨간색 반투명 영역 (#FF1744 20% opacity)
- 이 영역은 **리스크-리워드 비율을 시각적으로 표현**

### 3. 포지션 정보 패널

#### 위치 & 크기
- 차트 **우상단 코너**에 배치
- **크기:** 약 200dp x 180dp (컴팩트하게)
- **배경:** 반투명 검정 (rgba(0, 0, 0, 0.75))
- **모서리:** 둥글게 (8dp radius)
- **패딩:** 12dp

#### 내용 구조
```
Position:       100000.00
                Long
Entry:          $95883.00
TP:             $97800.66
SL:             $93965.34
R:R:            1.00
P&L:            +74.44 USDT
Risk Score:     65
```

#### 텍스트 스타일
- **라벨 (Position, Entry 등):** 흰색, 12sp, regular
- **값:** 오른쪽 정렬, 흰색, 12-13sp, bold
- **방향 (Long/Short):** Long=초록(#00C853), Short=빨강(#FF1744)
- **P&L:** 양수=초록, 음수=빨강
- **R:R:** 노란색 또는 오렌지 (#FDD835)
- **줄 간격:** 4-6dp

### 4. 하단 컨트롤 영역

#### R:R 비율 표시 (중요!)
- 차트 바로 아래, **별도 카드 형태**
- **배경:** 어두운 그레이 (#1C1F26)
- **좌측:** "R:R 비율" 텍스트 (회색)
- **우측:** 실제 비율 값 (예: "R:R 1.00:1"), **큰 글씨(24sp)**, **오렌지/골드 색상**
- **높이:** 약 60dp

#### 입력 필드
- **수량(Quantity):** 0.1
- **진입가(Entry):** $95883.0
- **익절(TP):** $97800.66

입력 필드 스타일:
- **배경:** 어두운 그레이 (#1C1F26)
- **테두리:** 없음 또는 매우 얇은 회색 선
- **텍스트:** 흰색, 16sp
- **라벨:** 작은 회색 텍스트 (10-11sp)

#### 하단 네비게이션 바
- **대시보드, 차트, 거래, 알림, 교육** 5개 아이콘
- **선택된 탭:** 밝은 색상(파랑/초록)
- **비선택 탭:** 회색

---

## 📐 상세 구현 가이드

### A. XML 레이아웃 구조

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#0D1117">

    <!-- 상단바 (코인명, 가격, 타임프레임) -->
    <LinearLayout
        android:id="@+id/topBar"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:background="#1C1F26"
        android:padding="12dp"
        app:layout_constraintTop_toTopOf="parent">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical">

            <TextView
                android:id="@+id/coinName"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="BITCOIN"
                android:textColor="@android:color/white"
                android:textSize="18sp"
                android:textStyle="bold" />

            <Space
                android:layout_width="0dp"
                android:layout_height="1dp"
                android:layout_weight="1" />

            <TextView
                android:id="@+id/currentPrice"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="$95957.44"
                android:textColor="#00C853"
                android:textSize="20sp"
                android:textStyle="bold" />
        </LinearLayout>

        <!-- 타임프레임 버튼들 -->
        <HorizontalScrollView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:scrollbars="none">

            <LinearLayout
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:orientation="horizontal">

                <Button
                    android:id="@+id/btn1M"
                    style="@style/TimeframeButton"
                    android:text="1M" />
                <Button
                    android:id="@+id/btn5M"
                    style="@style/TimeframeButton"
                    android:text="5M" />
                <Button
                    android:id="@+id/btn15M"
                    style="@style/TimeframeButton"
                    android:text="15M" />
                <Button
                    android:id="@+id/btn30M"
                    style="@style/TimeframeButton"
                    android:text="30M" />
                <Button
                    android:id="@+id/btn1H"
                    style="@style/TimeframeButton"
                    android:text="1H"
                    android:backgroundTint="#1E88E5" />
                <Button
                    android:id="@+id/btn4H"
                    style="@style/TimeframeButton"
                    android:text="4H" />
                <Button
                    android:id="@+id/btn1D"
                    style="@style/TimeframeButton"
                    android:text="1D" />
            </LinearLayout>
        </HorizontalScrollView>
    </LinearLayout>

    <!-- 차트 컨테이너 (WebView + 오버레이) -->
    <FrameLayout
        android:id="@+id/chartContainer"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintTop_toBottomOf="@id/topBar"
        app:layout_constraintBottom_toTopOf="@id/rrRatioCard"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent">

        <!-- TradingView WebView -->
        <WebView
            android:id="@+id/tradingViewChart"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:background="#0D1117" />

        <!-- 포지션 정보 패널 (우상단 오버레이) -->
        <androidx.cardview.widget.CardView
            android:id="@+id/positionInfoCard"
            android:layout_width="200dp"
            android:layout_height="wrap_content"
            android:layout_gravity="top|end"
            android:layout_margin="12dp"
            app:cardBackgroundColor="#BF000000"
            app:cardCornerRadius="8dp"
            app:cardElevation="4dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="12dp">

                <!-- Position -->
                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal">
                    <TextView
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="Position:"
                        android:textColor="#BDBDBD"
                        android:textSize="11sp" />
                    <TextView
                        android:id="@+id/positionAmount"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="100000.00"
                        android:textColor="@android:color/white"
                        android:textSize="11sp"
                        android:textStyle="bold" />
                </LinearLayout>

                <!-- Direction -->
                <TextView
                    android:id="@+id/positionDirection"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="Long"
                    android:textColor="#00C853"
                    android:textSize="11sp"
                    android:textStyle="bold"
                    android:gravity="end"
                    android:layout_marginBottom="4dp" />

                <!-- Entry -->
                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:layout_marginTop="4dp">
                    <TextView
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="Entry:"
                        android:textColor="#BDBDBD"
                        android:textSize="11sp" />
                    <TextView
                        android:id="@+id/entryPrice"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="$95883.00"
                        android:textColor="@android:color/white"
                        android:textSize="11sp"
                        android:textStyle="bold" />
                </LinearLayout>

                <!-- TP -->
                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:layout_marginTop="2dp">
                    <TextView
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="TP:"
                        android:textColor="#BDBDBD"
                        android:textSize="11sp" />
                    <TextView
                        android:id="@+id/tpPrice"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="$97800.66"
                        android:textColor="@android:color/white"
                        android:textSize="11sp"
                        android:textStyle="bold" />
                </LinearLayout>

                <!-- SL -->
                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:layout_marginTop="2dp">
                    <TextView
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="SL:"
                        android:textColor="#BDBDBD"
                        android:textSize="11sp" />
                    <TextView
                        android:id="@+id/slPrice"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="$93965.34"
                        android:textColor="@android:color/white"
                        android:textSize="11sp"
                        android:textStyle="bold" />
                </LinearLayout>

                <!-- R:R -->
                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:layout_marginTop="2dp">
                    <TextView
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="R:R:"
                        android:textColor="#BDBDBD"
                        android:textSize="11sp" />
                    <TextView
                        android:id="@+id/rrRatio"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="1.00"
                        android:textColor="@android:color/white"
                        android:textSize="11sp"
                        android:textStyle="bold" />
                </LinearLayout>

                <!-- P&L -->
                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:layout_marginTop="2dp">
                    <TextView
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="P&amp;L:"
                        android:textColor="#BDBDBD"
                        android:textSize="11sp" />
                    <TextView
                        android:id="@+id/pnlValue"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="+74.44 USDT"
                        android:textColor="#00C853"
                        android:textSize="11sp"
                        android:textStyle="bold" />
                </LinearLayout>

                <!-- Risk Score -->
                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:layout_marginTop="2dp">
                    <TextView
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="Risk Score:"
                        android:textColor="#BDBDBD"
                        android:textSize="11sp" />
                    <TextView
                        android:id="@+id/riskScore"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="65"
                        android:textColor="@android:color/white"
                        android:textSize="11sp"
                        android:textStyle="bold" />
                </LinearLayout>
            </LinearLayout>
        </androidx.cardview.widget.CardView>
    </FrameLayout>

    <!-- R:R 비율 카드 -->
    <androidx.cardview.widget.CardView
        android:id="@+id/rrRatioCard"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintBottom_toTopOf="@id/controlPanel"
        app:cardBackgroundColor="#1C1F26"
        app:cardCornerRadius="0dp"
        app:cardElevation="2dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:padding="16dp"
            android:gravity="center_vertical">

            <TextView
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="R:R 비율"
                android:textColor="#9E9E9E"
                android:textSize="14sp" />

            <TextView
                android:id="@+id/rrRatioDisplay"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="R:R 1.00:1"
                android:textColor="#FF9800"
                android:textSize="24sp"
                android:textStyle="bold" />
        </LinearLayout>
    </androidx.cardview.widget.CardView>

    <!-- 하단 컨트롤 패널 -->
    <LinearLayout
        android:id="@+id/controlPanel"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:background="#1C1F26"
        android:padding="16dp"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintBottom_toTopOf="@id/bottomNav">

        <!-- 수량 입력 -->
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="수량"
            android:textColor="#757575"
            android:textSize="11sp"
            android:layout_marginBottom="4dp" />

        <EditText
            android:id="@+id/quantityInput"
            android:layout_width="match_parent"
            android:layout_height="48dp"
            android:background="@drawable/input_bg"
            android:text="0.1"
            android:textColor="@android:color/white"
            android:textSize="16sp"
            android:paddingStart="12dp"
            android:paddingEnd="12dp"
            android:inputType="numberDecimal"
            android:layout_marginBottom="12dp" />

        <!-- 진입가, 익절 입력 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal">

            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:orientation="vertical"
                android:layout_marginEnd="8dp">

                <TextView
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="진입가"
                    android:textColor="#757575"
                    android:textSize="11sp"
                    android:layout_marginBottom="4dp" />

                <EditText
                    android:id="@+id/entryInput"
                    android:layout_width="match_parent"
                    android:layout_height="48dp"
                    android:background="@drawable/input_bg"
                    android:text="95883.0"
                    android:textColor="@android:color/white"
                    android:textSize="16sp"
                    android:paddingStart="12dp"
                    android:paddingEnd="12dp"
                    android:inputType="numberDecimal" />
            </LinearLayout>

            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:orientation="vertical"
                android:layout_marginStart="8dp">

                <TextView
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="익절"
                    android:textColor="#757575"
                    android:textSize="11sp"
                    android:layout_marginBottom="4dp" />

                <EditText
                    android:id="@+id/tpInput"
                    android:layout_width="match_parent"
                    android:layout_height="48dp"
                    android:background="@drawable/input_bg"
                    android:text="97800.66"
                    android:textColor="@android:color/white"
                    android:textSize="16sp"
                    android:paddingStart="12dp"
                    android:paddingEnd="12dp"
                    android:inputType="numberDecimal" />
            </LinearLayout>
        </LinearLayout>

        <!-- 롱/숏 버튼 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:layout_marginTop="16dp">

            <Button
                android:id="@+id/btnLong"
                android:layout_width="0dp"
                android:layout_height="48dp"
                android:layout_weight="1"
                android:text="롱"
                android:textColor="@android:color/white"
                android:backgroundTint="#00C853"
                android:layout_marginEnd="8dp" />

            <Button
                android:id="@+id/btnShort"
                android:layout_width="0dp"
                android:layout_height="48dp"
                android:layout_weight="1"
                android:text="숏"
                android:textColor="@android:color/white"
                android:backgroundTint="#FF1744"
                android:layout_marginStart="8dp" />
        </LinearLayout>
    </LinearLayout>

    <!-- 하단 네비게이션 바 -->
    <com.google.android.material.bottomnavigation.BottomNavigationView
        android:id="@+id/bottomNav"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:menu="@menu/bottom_nav_menu"
        app:itemIconTint="@color/bottom_nav_color"
        app:itemTextColor="@color/bottom_nav_color"
        android:background="#1C1F26" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

### B. Drawable 리소스 (input_bg.xml)

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#2C2F36" />
    <corners android:radius="4dp" />
    <stroke
        android:width="1dp"
        android:color="#424242" />
</shape>
```

### C. Styles.xml (타임프레임 버튼 스타일)

```xml
<style name="TimeframeButton">
    <item name="android:layout_width">48dp</item>
    <item name="android:layout_height">32dp</item>
    <item name="android:layout_marginEnd">4dp</item>
    <item name="android:backgroundTint">#2C2F36</item>
    <item name="android:textColor">@android:color/white</item>
    <item name="android:textSize">12sp</item>
    <item name="android:textAllCaps">false</item>
</style>
```

---

## 🎨 JavaScript/HTML 차트 라인 렌더링 (핵심 개선!)

### 업데이트된 tradingview_chart.html

```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>R² Trading Chart</title>
    <script src="https://unpkg.com/lightweight-charts@4/dist/lightweight-charts.standalone.production.js"></script>
    <style>
        body {
            margin: 0;
            padding: 0;
            background-color: #0D1117;
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            overflow: hidden;
        }

        #chart {
            width: 100%;
            height: 100%;
            position: relative;
        }

        /* 라인 라벨 스타일 */
        .line-label {
            position: absolute;
            padding: 4px 10px;
            border-radius: 4px;
            font-size: 12px;
            font-weight: bold;
            color: white;
            z-index: 10;
            pointer-events: none;
            user-select: none;
        }

        .tp-label {
            background-color: #00C853;
        }

        .sl-label {
            background-color: #FF1744;
        }

        .entry-label {
            background-color: #9E9E9E;
        }

        /* 드래그 중 툴팁 */
        .drag-tooltip {
            position: absolute;
            background-color: rgba(0, 0, 0, 0.9);
            color: white;
            padding: 6px 12px;
            border-radius: 4px;
            font-size: 13px;
            font-weight: bold;
            display: none;
            z-index: 20;
            pointer-events: none;
        }
    </style>
</head>
<body>
    <div id="chart"></div>
    <div class="line-label tp-label" id="tpLabel">TP</div>
    <div class="line-label sl-label" id="slLabel">SL</div>
    <div class="line-label entry-label" id="entryLabel">1L</div>
    <div class="drag-tooltip" id="dragTooltip">$0.00</div>

    <script>
        // 차트 초기화
        const chartContainer = document.getElementById('chart');
        const chart = LightweightCharts.createChart(chartContainer, {
            layout: {
                background: { color: '#0D1117' },
                textColor: '#BDBDBD',
            },
            grid: {
                vertLines: { color: '#1C1F26' },
                horzLines: { color: '#1C1F26' },
            },
            timeScale: {
                timeVisible: true,
                secondsVisible: false,
                borderColor: '#2C2F36',
            },
            rightPriceScale: {
                borderColor: '#2C2F36',
            },
            width: chartContainer.offsetWidth,
            height: chartContainer.offsetHeight,
        });

        // 캔들스틱 시리즈
        const candleSeries = chart.addCandlestickSeries({
            upColor: '#26a69a',
            downColor: '#ef5350',
            borderDownColor: '#ef5350',
            borderUpColor: '#26a69a',
            wickDownColor: '#ef5350',
            wickUpColor: '#26a69a',
        });

        // 샘플 데이터
        const data = generateSampleData();
        candleSeries.setData(data);

        // 라인 데이터
        let lineData = {
            entry: 95883.00,
            tp: 97800.66,
            sl: 93965.34,
        };

        let lineSeries = {
            entry: null,
            tp: null,
            sl: null,
        };

        let areaSeriesList = [];

        // 드래그 상태
        let isDragging = false;
        let draggingLineType = null;

        /**
         * 라인 및 영역 렌더링
         */
        function renderLines() {
            // 기존 라인/영역 제거
            if (lineSeries.entry) chart.removeSeries(lineSeries.entry);
            if (lineSeries.tp) chart.removeSeries(lineSeries.tp);
            if (lineSeries.sl) chart.removeSeries(lineSeries.sl);
            areaSeriesList.forEach(series => chart.removeSeries(series));
            areaSeriesList = [];

            // Entry-TP 사이 초록 영역
            const profitArea = chart.addAreaSeries({
                topColor: 'rgba(0, 200, 83, 0.3)',
                bottomColor: 'rgba(0, 200, 83, 0.05)',
                lineColor: 'rgba(0, 200, 83, 0)',
                lineWidth: 0,
            });
            profitArea.setData([
                { time: data[0].time, value: lineData.tp },
                { time: data[data.length - 1].time, value: lineData.tp },
                { time: data[data.length - 1].time, value: lineData.entry },
                { time: data[0].time, value: lineData.entry },
            ]);
            areaSeriesList.push(profitArea);

            // Entry-SL 사이 빨강 영역
            const lossArea = chart.addAreaSeries({
                topColor: 'rgba(255, 23, 68, 0.3)',
                bottomColor: 'rgba(255, 23, 68, 0.05)',
                lineColor: 'rgba(255, 23, 68, 0)',
                lineWidth: 0,
            });
            lossArea.setData([
                { time: data[0].time, value: lineData.entry },
                { time: data[data.length - 1].time, value: lineData.entry },
                { time: data[data.length - 1].time, value: lineData.sl },
                { time: data[0].time, value: lineData.sl },
            ]);
            areaSeriesList.push(lossArea);

            // Entry 라인 (얇은 회색)
            lineSeries.entry = chart.addLineSeries({
                color: '#9E9E9E',
                lineWidth: 2,
                lineStyle: 0, // 실선
            });
            lineSeries.entry.setData([
                { time: data[0].time, value: lineData.entry },
                { time: data[data.length - 1].time, value: lineData.entry },
            ]);

            // TP 라인 (얇은 초록)
            lineSeries.tp = chart.addLineSeries({
                color: '#00C853',
                lineWidth: 2,
                lineStyle: isDragging && draggingLineType === 'tp' ? 1 : 0, // 드래그 중 점선
            });
            lineSeries.tp.setData([
                { time: data[0].time, value: lineData.tp },
                { time: data[data.length - 1].time, value: lineData.tp },
            ]);

            // SL 라인 (얇은 빨강)
            lineSeries.sl = chart.addLineSeries({
                color: '#FF1744',
                lineWidth: 2,
                lineStyle: isDragging && draggingLineType === 'sl' ? 1 : 0,
            });
            lineSeries.sl.setData([
                { time: data[0].time, value: lineData.sl },
                { time: data[data.length - 1].time, value: lineData.sl },
            ]);

            // 라벨 위치 업데이트
            updateLabelPositions();
        }

        /**
         * 라벨 위치 업데이트
         */
        function updateLabelPositions() {
            const tpY = chart.priceToCoordinate(lineData.tp);
            const slY = chart.priceToCoordinate(lineData.sl);
            const entryY = chart.priceToCoordinate(lineData.entry);

            const tpLabel = document.getElementById('tpLabel');
            const slLabel = document.getElementById('slLabel');
            const entryLabel = document.getElementById('entryLabel');

            if (tpY !== null) {
                tpLabel.style.top = (tpY - 14) + 'px';
                tpLabel.style.left = '10px';
                tpLabel.style.display = 'block';
            }

            if (slY !== null) {
                slLabel.style.top = (slY - 14) + 'px';
                slLabel.style.left = '10px';
                slLabel.style.display = 'block';
            }

            if (entryY !== null) {
                entryLabel.style.top = (entryY - 14) + 'px';
                entryLabel.style.left = '10px';
                entryLabel.style.display = 'block';
            }
        }

        /**
         * Android로 업데이트 전송
         */
        function notifyAndroid(lineType, price) {
            if (window.AndroidBridge) {
                window.AndroidBridge.onLineUpdated(lineType, price);
            }
        }

        /**
         * 드래그 감지 영역 (터치/마우스)
         */
        const TOUCH_THRESHOLD = 50; // 50px 이내면 드래그 시작

        chartContainer.addEventListener('mousedown', startDrag);
        chartContainer.addEventListener('touchstart', startDrag);

        function startDrag(e) {
            const y = getEventY(e);
            const price = chart.coordinateToPrice(y);

            if (Math.abs(price - lineData.tp) < (lineData.entry * 0.002)) {
                isDragging = true;
                draggingLineType = 'tp';
            } else if (Math.abs(price - lineData.sl) < (lineData.entry * 0.002)) {
                isDragging = true;
                draggingLineType = 'sl';
            } else if (Math.abs(price - lineData.entry) < (lineData.entry * 0.002)) {
                isDragging = true;
                draggingLineType = 'entry';
            }

            if (isDragging) {
                renderLines(); // 드래그 시작 시 점선으로 변경
            }
        }

        chartContainer.addEventListener('mousemove', handleDrag);
        chartContainer.addEventListener('touchmove', handleDrag);

        function handleDrag(e) {
            if (!isDragging || !draggingLineType) return;

            e.preventDefault();
            const y = getEventY(e);
            const price = chart.coordinateToPrice(y);

            lineData[draggingLineType] = price;

            // 툴팁 표시
            const tooltip = document.getElementById('dragTooltip');
            tooltip.textContent = '$' + price.toFixed(2);
            tooltip.style.top = (y - 30) + 'px';
            tooltip.style.left = (e.clientX || e.touches[0].clientX) + 'px';
            tooltip.style.display = 'block';

            renderLines();
            notifyAndroid(draggingLineType, price);
        }

        chartContainer.addEventListener('mouseup', endDrag);
        chartContainer.addEventListener('touchend', endDrag);

        function endDrag() {
            if (isDragging) {
                isDragging = false;
                draggingLineType = null;
                document.getElementById('dragTooltip').style.display = 'none';
                renderLines(); // 다시 실선으로
            }
        }

        function getEventY(e) {
            const rect = chartContainer.getBoundingClientRect();
            if (e.touches) {
                return e.touches[0].clientY - rect.top;
            }
            return e.clientY - rect.top;
        }

        /**
         * Android에서 호출
         */
        window.updateLines = function(entry, tp, sl) {
            lineData.entry = entry;
            lineData.tp = tp;
            lineData.sl = sl;
            renderLines();
        };

        /**
         * 초기 렌더링
         */
        renderLines();

        // 창 크기 조정
        window.addEventListener('resize', () => {
            chart.applyOptions({
                width: chartContainer.offsetWidth,
                height: chartContainer.offsetHeight,
            });
            updateLabelPositions();
        });

        // 샘플 데이터 생성
        function generateSampleData() {
            const basePrice = 95000;
            const result = [];
            const now = Math.floor(Date.now() / 1000);

            for (let i = 0; i < 100; i++) {
                const time = now - (100 - i) * 3600;
                const open = basePrice + Math.random() * 3000 - 1500;
                const close = open + Math.random() * 1000 - 500;
                const high = Math.max(open, close) + Math.random() * 500;
                const low = Math.min(open, close) - Math.random() * 500;

                result.push({
                    time: time,
                    open: open,
                    high: high,
                    low: low,
                    close: close,
                });
            }

            return result;
        }

        // Android에 준비 완료 알림
        if (window.AndroidBridge) {
            window.AndroidBridge.ready();
        }
    </script>
</body>
</html>
```

---

## ✅ 핵심 차이점 요약

| **항목** | **이전 구현 (문제)** | **수정된 디자인 (정답)** |
|---------|------------------|---------------------|
| **라인 두께** | 너무 두꺼운 박스 형태 | 매우 얇은 선 (1-2px) |
| **라인 라벨** | 별도 큰 박스 | 라인 끝에 붙은 작은 버튼 |
| **영역 채우기** | 없음 | Entry-TP(초록), Entry-SL(빨강) 반투명 영역 |
| **정보 패널** | 너무 큼, 차트 가림 | 작고 간결 (200x180dp) |
| **차트 비율** | 컨트롤이 너무 큼 | 차트 60-70% 차지, 컨트롤 최소화 |
| **드래그 피드백** | 없음 | 점선 + 가격 툴팁 표시 |
| **R:R 표시** | 작게 표시 | 별도 카드로 크게 표시 (24sp) |

---

## 🚀 구현 가이드

1. 위 XML 레이아웃을 `activity_chart.xml`에 복사
2. `drawable/input_bg.xml` 생성
3. `styles.xml`에 `TimeframeButton` 스타일 추가
4. `assets/tradingview_chart.html` 업데이트
5. Activity/ViewModel 코드 연결
6. 테스트: 라인 드래그, 영역 색상, 라벨 위치

---

이제 **정확한 디자인**대로 구현 가능합니다!
