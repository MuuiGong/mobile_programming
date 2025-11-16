# R² 차트 구현 프롬프트 — BingX 스타일 TP/SL 라인 (정확한 스펙)

## 🎯 목표: BingX 차트 TP/SL 기능 재현

> **목표:** BingX 거래 차트의 **Entry(현재가), TP(익절), SL(손절) 라인**을 정확히 재현합니다.  
> 사용자가 라인을 **드래그**하거나 **숫자로 입력**할 수 있고,  
> 실시간으로 **R:R 비율, P&L, 리스크 비율** 등이 표시되어야 합니다.

---

## 📐 BingX 차트 TP/SL 디자인 분석

### 핵심 특징 (BingX 참고)

1. **라인 렌더링**
   - **수평 선** (얇고 명확함, 1-2px)
   - **TP 라인:** 초록색 (#00C853 또는 유사)
   - **SL 라인:** 빨간색 (#FF1744 또는 유사)
   - **Entry 라인:** 현재가 라인 (흰색 또는 연한 색)

2. **라인에 붙은 정보 박스**
   - **왼쪽에 라벨 박스** (TP, SL, 현재가 등)
   - **오른쪽에 가격 표시** (예: "$97800.66")
   - 라인 색상과 일치하는 배경색
   - 라인을 따라 수직으로 정렬

3. **드래그 기능**
   - 사용자가 라인을 **위아래로 터치/드래그**
   - 드래그 중: 라인이 **점선(dashed)** 또는 **색상 강조**로 변경
   - 드래그 중: 실시간으로 **가격과 R:R 업데이트**
   - 드래그 종료 시: 라인 위치 최종 확정

4. **숫자 입력**
   - 차트 아래 **입력 필드**에서 직접 값 입력 가능
   - 입력 시 라인이 **실시간으로 이동**

5. **영역 색상**
   - **Entry-TP 사이:** 초록색 반투명 영역 (수익 구간 표시)
   - **Entry-SL 사이:** 빨간색 반투명 영역 (손실 구간 표시)
   - 비율로 면적이 표시됨 (R:R 시각화)

6. **정보 표시**
   - 포지션 정보 (진입가, TP, SL)
   - R:R 비율 (크게, 눈에 띄게)
   - 현재 P&L (또는 예상 P&L)
   - 진입 가능 여부 표시

---

## 🔧 구현 상세 가이드

### A. Android Activity 레이아웃 (activity_chart.xml)

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#0D1117">

    <!-- 상단 바 (코인 정보) -->
    <LinearLayout
        android:id="@+id/topBar"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:background="#1C1F26"
        android:padding="12dp"
        app:layout_constraintTop_toTopOf="parent">

        <!-- 코인명 + 현재가 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:layout_marginBottom="8dp">

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
                android:textSize="18sp"
                android:textStyle="bold" />
        </LinearLayout>

        <!-- 가격 변화 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal">

            <TextView
                android:id="@+id/priceChange"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="+0.85%"
                android:textColor="#00C853"
                android:textSize="12sp" />

            <Space
                android:layout_width="0dp"
                android:layout_height="1dp"
                android:layout_weight="1" />

            <TextView
                android:id="@+id/priceChangeValue"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="+815.44 USDT"
                android:textColor="#00C853"
                android:textSize="12sp" />
        </LinearLayout>

        <!-- 타임프레임 버튼들 -->
        <HorizontalScrollView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            android:scrollbars="none">

            <LinearLayout
                android:layout_width="wrap_content"
                android:layout_height="40dp"
                android:orientation="horizontal"
                android:spacing="6dp">

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

    <!-- 차트 컨테이너 (WebView) -->
    <FrameLayout
        android:id="@+id/chartContainer"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        app:layout_constraintTop_toBottomOf="@id/topBar"
        app:layout_constraintBottom_toTopOf="@id/controlPanel"
        android:background="#0D1117">

        <WebView
            android:id="@+id/tradingViewChart"
            android:layout_width="match_parent"
            android:layout_height="match_parent" />
    </FrameLayout>

    <!-- 하단 컨트롤 패널 -->
    <LinearLayout
        android:id="@+id/controlPanel"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:background="#1C1F26"
        android:padding="16dp"
        app:layout_constraintBottom_toBottomOf="parent">

        <!-- R:R 비율 큰 표시 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:background="@drawable/r2_ratio_bg"
            android:padding="12dp"
            android:layout_marginBottom="12dp">

            <TextView
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="R:R 비율"
                android:textColor="#9E9E9E"
                android:textSize="12sp" />

            <TextView
                android:id="@+id/rrRatioDisplay"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="R:R 1.00:1"
                android:textColor="#FF9800"
                android:textSize="20sp"
                android:textStyle="bold" />
        </LinearLayout>

        <!-- Entry, TP, SL 입력 필드 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:spacing="8dp">

            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:orientation="vertical">

                <TextView
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="진입가"
                    android:textColor="#757575"
                    android:textSize="10sp"
                    android:layout_marginBottom="4dp" />

                <EditText
                    android:id="@+id/entryInput"
                    android:layout_width="match_parent"
                    android:layout_height="44dp"
                    android:background="@drawable/input_bg"
                    android:text="95883.0"
                    android:textColor="@android:color/white"
                    android:textSize="14sp"
                    android:paddingStart="12dp"
                    android:paddingEnd="12dp"
                    android:inputType="numberDecimal"
                    android:gravity="center_vertical" />
            </LinearLayout>

            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:orientation="vertical">

                <TextView
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="익절(TP)"
                    android:textColor="#757575"
                    android:textSize="10sp"
                    android:layout_marginBottom="4dp" />

                <EditText
                    android:id="@+id/tpInput"
                    android:layout_width="match_parent"
                    android:layout_height="44dp"
                    android:background="@drawable/input_bg"
                    android:text="97800.66"
                    android:textColor="@android:color/white"
                    android:textSize="14sp"
                    android:paddingStart="12dp"
                    android:paddingEnd="12dp"
                    android:inputType="numberDecimal"
                    android:gravity="center_vertical" />
            </LinearLayout>

            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:orientation="vertical">

                <TextView
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="손절(SL)"
                    android:textColor="#757575"
                    android:textSize="10sp"
                    android:layout_marginBottom="4dp" />

                <EditText
                    android:id="@+id/slInput"
                    android:layout_width="match_parent"
                    android:layout_height="44dp"
                    android:background="@drawable/input_bg"
                    android:text="93965.34"
                    android:textColor="@android:color/white"
                    android:textSize="14sp"
                    android:paddingStart="12dp"
                    android:paddingEnd="12dp"
                    android:inputType="numberDecimal"
                    android:gravity="center_vertical" />
            </LinearLayout>
        </LinearLayout>

        <!-- 거래 정보 요약 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:layout_marginTop="12dp"
            android:background="@drawable/info_bg"
            android:padding="12dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal">

                <TextView
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="현재 P&amp;L"
                    android:textColor="#9E9E9E"
                    android:textSize="12sp" />

                <TextView
                    android:id="@+id/currentPnL"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="+74.44 USDT"
                    android:textColor="#00C853"
                    android:textSize="12sp"
                    android:textStyle="bold" />
            </LinearLayout>

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:layout_marginTop="6dp">

                <TextView
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="위험도"
                    android:textColor="#9E9E9E"
                    android:textSize="12sp" />

                <TextView
                    android:id="@+id/riskLevel"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Moderate"
                    android:textColor="#FF9800"
                    android:textSize="12sp"
                    android:textStyle="bold" />
            </LinearLayout>
        </LinearLayout>

        <!-- 거래 실행 버튼 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:spacing="8dp"
            android:layout_marginTop="12dp">

            <Button
                android:id="@+id/btnLong"
                android:layout_width="0dp"
                android:layout_height="48dp"
                android:layout_weight="1"
                android:text="롱 진입"
                android:textColor="@android:color/white"
                android:textSize="14sp"
                android:backgroundTint="#00C853" />

            <Button
                android:id="@+id/btnShort"
                android:layout_width="0dp"
                android:layout_height="48dp"
                android:layout_weight="1"
                android:text="숏 진입"
                android:textColor="@android:color/white"
                android:textSize="14sp"
                android:backgroundTint="#FF1744" />
        </LinearLayout>
    </LinearLayout>

</androidx.constraintlayout.widget.ConstraintLayout>
```

---

### B. Drawable 리소스

#### drawable/input_bg.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#2C2F36" />
    <corners android:radius="6dp" />
    <stroke
        android:width="1dp"
        android:color="#424242" />
</shape>
```

#### drawable/r2_ratio_bg.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#2C2F36" />
    <corners android:radius="8dp" />
    <stroke
        android:width="1dp"
        android:color="#FF9800" />
</shape>
```

#### drawable/info_bg.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#1C1F26" />
    <corners android:radius="8dp" />
    <stroke
        android:width="1dp"
        android:color="#424242" />
</shape>
```

---

### C. TradingView 차트 HTML (assets/tradingview_chart.html)

```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>R² Trading Chart</title>
    <script src="https://unpkg.com/lightweight-charts@4/dist/lightweight-charts.standalone.production.js"></script>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            background-color: #0D1117;
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            overflow: hidden;
        }

        #chartContainer {
            width: 100%;
            height: 100%;
            position: relative;
        }

        /* 라인 라벨 스타일 */
        .line-label {
            position: absolute;
            background-color: #00C853;
            color: white;
            padding: 4px 8px;
            border-radius: 3px;
            font-size: 12px;
            font-weight: bold;
            display: none;
            z-index: 100;
            white-space: nowrap;
            cursor: pointer;
            user-select: none;
        }

        .line-label.tp {
            background-color: #00C853;
        }

        .line-label.sl {
            background-color: #FF1744;
        }

        .line-label.entry {
            background-color: #9E9E9E;
        }

        /* 드래그 중 활성화 */
        .line-label.dragging {
            opacity: 0.8;
            transform: scale(1.1);
        }

        /* 가격 툴팁 */
        .price-tooltip {
            position: absolute;
            background-color: rgba(0, 0, 0, 0.9);
            color: white;
            padding: 6px 12px;
            border-radius: 4px;
            font-size: 13px;
            font-weight: bold;
            display: none;
            z-index: 150;
            pointer-events: none;
        }

        /* 정보 오버레이 */
        .info-overlay {
            position: absolute;
            top: 10px;
            right: 10px;
            background-color: rgba(0, 0, 0, 0.8);
            border: 1px solid #424242;
            border-radius: 8px;
            padding: 12px;
            color: white;
            font-size: 11px;
            z-index: 90;
            max-width: 200px;
        }

        .info-row {
            display: flex;
            justify-content: space-between;
            margin-bottom: 6px;
            line-height: 1.4;
        }

        .info-label {
            color: #9E9E9E;
            margin-right: 10px;
        }

        .info-value {
            color: white;
            font-weight: bold;
            text-align: right;
        }

        .info-value.positive {
            color: #00C853;
        }

        .info-value.negative {
            color: #FF1744;
        }

        .info-value.orange {
            color: #FF9800;
        }
    </style>
</head>
<body>
    <div id="chartContainer"></div>
    
    <!-- 라인 라벨들 -->
    <div class="line-label entry" id="entryLabel">Entry</div>
    <div class="line-label tp" id="tpLabel">TP</div>
    <div class="line-label sl" id="slLabel">SL</div>
    
    <!-- 가격 툴팁 -->
    <div class="price-tooltip" id="priceTooltip"></div>
    
    <!-- 정보 오버레이 -->
    <div class="info-overlay" id="infoOverlay"></div>

    <script>
        // ============= 차트 초기화 =============
        const container = document.getElementById('chartContainer');
        const chart = LightweightCharts.createChart(container, {
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
            width: container.offsetWidth,
            height: container.offsetHeight,
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
        const data = generateCandleData();
        candleSeries.setData(data);
        chart.timeScale().fitContent();

        // ============= 라인 데이터 =============
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

        // ============= 드래그 상태 관리 =============
        let isDragging = false;
        let draggingLineType = null;
        const DRAG_THRESHOLD = 50; // px

        // ============= 라인 렌더링 함수 =============
        function renderLines() {
            // 기존 시리즈 제거
            if (lineSeries.entry) chart.removeSeries(lineSeries.entry);
            if (lineSeries.tp) chart.removeSeries(lineSeries.tp);
            if (lineSeries.sl) chart.removeSeries(lineSeries.sl);
            areaSeriesList.forEach(s => chart.removeSeries(s));
            areaSeriesList = [];

            // TP와 Entry 사이 초록 영역
            const profitArea = chart.addAreaSeries({
                topColor: 'rgba(0, 200, 83, 0.2)',
                bottomColor: 'rgba(0, 200, 83, 0.05)',
                lineColor: 'transparent',
                lineWidth: 0,
            });
            profitArea.setData([
                { time: data[0].time, value: Math.max(lineData.tp, lineData.entry) },
                { time: data[data.length - 1].time, value: Math.max(lineData.tp, lineData.entry) },
                { time: data[data.length - 1].time, value: Math.min(lineData.tp, lineData.entry) },
                { time: data[0].time, value: Math.min(lineData.tp, lineData.entry) },
            ]);
            areaSeriesList.push(profitArea);

            // SL과 Entry 사이 빨강 영역
            const lossArea = chart.addAreaSeries({
                topColor: 'rgba(255, 23, 68, 0.2)',
                bottomColor: 'rgba(255, 23, 68, 0.05)',
                lineColor: 'transparent',
                lineWidth: 0,
            });
            lossArea.setData([
                { time: data[0].time, value: Math.max(lineData.entry, lineData.sl) },
                { time: data[data.length - 1].time, value: Math.max(lineData.entry, lineData.sl) },
                { time: data[data.length - 1].time, value: Math.min(lineData.entry, lineData.sl) },
                { time: data[0].time, value: Math.min(lineData.entry, lineData.sl) },
            ]);
            areaSeriesList.push(lossArea);

            // Entry 라인
            lineSeries.entry = chart.addLineSeries({
                color: '#9E9E9E',
                lineWidth: 2,
            });
            lineSeries.entry.setData([
                { time: data[0].time, value: lineData.entry },
                { time: data[data.length - 1].time, value: lineData.entry },
            ]);

            // TP 라인 (드래그 중 점선)
            lineSeries.tp = chart.addLineSeries({
                color: '#00C853',
                lineWidth: isDragging && draggingLineType === 'tp' ? 3 : 2,
                lineStyle: isDragging && draggingLineType === 'tp' ? 1 : 0,
            });
            lineSeries.tp.setData([
                { time: data[0].time, value: lineData.tp },
                { time: data[data.length - 1].time, value: lineData.tp },
            ]);

            // SL 라인 (드래그 중 점선)
            lineSeries.sl = chart.addLineSeries({
                color: '#FF1744',
                lineWidth: isDragging && draggingLineType === 'sl' ? 3 : 2,
                lineStyle: isDragging && draggingLineType === 'sl' ? 1 : 0,
            });
            lineSeries.sl.setData([
                { time: data[0].time, value: lineData.sl },
                { time: data[data.length - 1].time, value: lineData.sl },
            ]);

            updateLabelPositions();
            updateInfoOverlay();
        }

        // ============= 라벨 위치 업데이트 =============
        function updateLabelPositions() {
            const entryY = chart.priceToCoordinate(lineData.entry);
            const tpY = chart.priceToCoordinate(lineData.tp);
            const slY = chart.priceToCoordinate(lineData.sl);

            const offset = 20;

            if (entryY !== null) {
                document.getElementById('entryLabel').style.top = (entryY - 12) + 'px';
                document.getElementById('entryLabel').style.left = offset + 'px';
                document.getElementById('entryLabel').style.display = 'block';
                document.getElementById('entryLabel').textContent = `Entry: $${lineData.entry.toFixed(2)}`;
            }

            if (tpY !== null) {
                document.getElementById('tpLabel').style.top = (tpY - 12) + 'px';
                document.getElementById('tpLabel').style.left = offset + 'px';
                document.getElementById('tpLabel').style.display = 'block';
                document.getElementById('tpLabel').textContent = `TP: $${lineData.tp.toFixed(2)}`;
            }

            if (slY !== null) {
                document.getElementById('slLabel').style.top = (slY - 12) + 'px';
                document.getElementById('slLabel').style.left = offset + 'px';
                document.getElementById('slLabel').style.display = 'block';
                document.getElementById('slLabel').textContent = `SL: $${lineData.sl.toFixed(2)}`;
            }
        }

        // ============= 정보 오버레이 업데이트 =============
        function updateInfoOverlay() {
            const rrRatio = (lineData.tp - lineData.entry) / (lineData.entry - lineData.sl);
            const pnl = 100 * (rrRatio - 1); // 가정의 계산

            let html = `
                <div class="info-row">
                    <span class="info-label">Entry:</span>
                    <span class="info-value">$${lineData.entry.toFixed(2)}</span>
                </div>
                <div class="info-row">
                    <span class="info-label">TP:</span>
                    <span class="info-value">$${lineData.tp.toFixed(2)}</span>
                </div>
                <div class="info-row">
                    <span class="info-label">SL:</span>
                    <span class="info-value">$${lineData.sl.toFixed(2)}</span>
                </div>
                <div class="info-row">
                    <span class="info-label">R:R:</span>
                    <span class="info-value orange">${rrRatio.toFixed(2)}</span>
                </div>
                <div class="info-row">
                    <span class="info-label">위험도:</span>
                    <span class="info-value ${pnl > 0 ? 'positive' : 'negative'}">
                        ${pnl > 0 ? '+' : ''}${pnl.toFixed(2)}%
                    </span>
                </div>
            `;
            document.getElementById('infoOverlay').innerHTML = html;
        }

        // ============= 드래그 핸들러 =============
        container.addEventListener('mousedown', startDrag);
        container.addEventListener('touchstart', startDrag);

        function startDrag(e) {
            const y = getEventY(e);
            const price = chart.coordinateToPrice(y);

            if (!price) return;

            const threshold = Math.abs(lineData.entry) * 0.001; // ±0.1%

            if (Math.abs(price - lineData.tp) < threshold) {
                isDragging = true;
                draggingLineType = 'tp';
            } else if (Math.abs(price - lineData.sl) < threshold) {
                isDragging = true;
                draggingLineType = 'sl';
            }

            if (isDragging) {
                renderLines();
            }
        }

        document.addEventListener('mousemove', handleDrag);
        document.addEventListener('touchmove', handleDrag);

        function handleDrag(e) {
            if (!isDragging || !draggingLineType) return;

            const y = getEventY(e);
            const price = chart.coordinateToPrice(y);

            if (price) {
                lineData[draggingLineType] = price;

                // 툴팁 표시
                const tooltip = document.getElementById('priceTooltip');
                tooltip.textContent = `$${price.toFixed(2)}`;
                tooltip.style.top = (y - 30) + 'px';
                const clientX = e.clientX || e.touches?.[0]?.clientX || 0;
                tooltip.style.left = (clientX + 20) + 'px';
                tooltip.style.display = 'block';

                renderLines();

                // Android로 알림
                if (window.AndroidBridge) {
                    window.AndroidBridge.onLineUpdated(draggingLineType, price);
                }
            }
        }

        document.addEventListener('mouseup', endDrag);
        document.addEventListener('touchend', endDrag);

        function endDrag() {
            if (isDragging) {
                isDragging = false;
                draggingLineType = null;
                document.getElementById('priceTooltip').style.display = 'none';
                renderLines();
            }
        }

        function getEventY(e) {
            const rect = container.getBoundingClientRect();
            if (e.touches) {
                return e.touches[0].clientY - rect.top;
            }
            return e.clientY - rect.top;
        }

        // ============= Android 인터페이스 =============
        window.updateLines = function(entry, tp, sl) {
            lineData.entry = entry;
            lineData.tp = tp;
            lineData.sl = sl;
            renderLines();
        };

        // ============= 초기 렌더링 및 리사이즈 =============
        renderLines();

        window.addEventListener('resize', () => {
            chart.applyOptions({
                width: container.offsetWidth,
                height: container.offsetHeight,
            });
            updateLabelPositions();
        });

        if (window.AndroidBridge) {
            window.AndroidBridge.ready();
        }

        // ============= 샘플 데이터 생성 =============
        function generateCandleData() {
            const basePrice = 95000;
            const result = [];
            const now = Math.floor(Date.now() / 1000);

            for (let i = 0; i < 150; i++) {
                const time = now - (150 - i) * 3600;
                const volatility = Math.random() * 2000 - 1000;
                const open = basePrice + volatility;
                const close = open + (Math.random() * 1500 - 750);
                const high = Math.max(open, close) + Math.random() * 800;
                const low = Math.min(open, close) - Math.random() * 800;

                result.push({
                    time: time,
                    open,
                    high,
                    low,
                    close,
                });
            }

            return result;
        }
    </script>
</body>
</html>
```

---

### D. Java ViewModel

```java
public class ChartViewModel extends ViewModel {
    private MutableLiveData<Double> entryPrice = new MutableLiveData<>(95883.0);
    private MutableLiveData<Double> tpPrice = new MutableLiveData<>(97800.66);
    private MutableLiveData<Double> slPrice = new MutableLiveData<>(93965.34);
    private MutableLiveData<Double> currentPrice = new MutableLiveData<>(95957.44);

    private MutableLiveData<Double> rrRatio = new MutableLiveData<>(0.0);
    private MutableLiveData<Double> pnl = new MutableLiveData<>(0.0);
    private MutableLiveData<String> riskLevel = new MutableLiveData<>("Moderate");

    public LiveData<Double> getEntryPrice() { return entryPrice; }
    public LiveData<Double> getTpPrice() { return tpPrice; }
    public LiveData<Double> getSlPrice() { return slPrice; }
    public LiveData<Double> getRrRatio() { return rrRatio; }
    public LiveData<Double> getPnl() { return pnl; }
    public LiveData<String> getRiskLevel() { return riskLevel; }

    public void setEntryPrice(double price) {
        entryPrice.setValue(price);
        calculateMetrics();
    }

    public void setTpPrice(double price) {
        tpPrice.setValue(price);
        calculateMetrics();
    }

    public void setSlPrice(double price) {
        slPrice.setValue(price);
        calculateMetrics();
    }

    private void calculateMetrics() {
        double entry = entryPrice.getValue() != null ? entryPrice.getValue() : 0;
        double tp = tpPrice.getValue() != null ? tpPrice.getValue() : 0;
        double sl = slPrice.getValue() != null ? slPrice.getValue() : 0;

        if (entry != 0 && entry != sl) {
            double ratio = (tp - entry) / (entry - sl);
            rrRatio.setValue(Math.max(ratio, 0.01));

            // 위험도 평가
            String risk = "Low";
            if (ratio < 0.5) risk = "High";
            else if (ratio < 1.0) risk = "Moderate";
            else if (ratio >= 2.0) risk = "Low";

            riskLevel.setValue(risk);
        }
    }
}
```

---

## ✅ BingX 스타일 구현 체크리스트

- [ ] **라인 렌더링:** 초록(TP), 빨강(SL), 회색(Entry) 얇은 선
- [ ] **라인 라벨:** 라인 끝에 가격 표시하는 라벨 (드래그 가능)
- [ ] **영역 색상:** Entry-TP(초록), Entry-SL(빨강) 반투명
- [ ] **드래그 기능:** 마우스/터치로 라인 이동, 점선 표시
- [ ] **가격 툴팁:** 드래그 중 실시간 가격 표시
- [ ] **정보 오버레이:** 우상단에 Entry, TP, SL, R:R 표시
- [ ] **입력 필드:** 숫자 입력 시 라인 실시간 이동
- [ ] **R:R 계산:** 실시간 업데이트 및 표시
- [ ] **위험도 평가:** Low/Moderate/High 표시

---

## 🎯 BingX와의 주요 차이점 반영

1. **라인이 명확한 가격 표시:** 각 라인에 정확한 가격 표시 (예: "Entry: $95883.00")
2. **영역 색상화:** 수익/리스크 영역이 명확하게 구분
3. **정보 오버레이:** 차트 우상단에 모든 거래 정보 한눈에 표시
4. **직관적 드래그:** 라인을 터치하면 즉시 반응
5. **실시간 계산:** R:R, 위험도가 실시간으로 갱신

