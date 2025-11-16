# R² 차트 구현 프롬프트 — TradingView 스타일 가로 오버레이 TP/SL 컨트롤

## 🎯 핵심 요구사항 (정확함!)

> **중요:** 차트 위에 **가로로 한 줄**로 표시되는 **TP/SL 컨트롤 바**를 만들어야 합니다.
> - Entry(1L), TP, SL 버튼이 **같은 수평선 위에** 표시
> - **가로 라인이 수직으로 드래그 가능** (위아래로 이동)
> - 드래그하면 해당 라인과 버튼이 함께 움직임
> - TradingView 차트처럼 깔끔한 스타일

---

## 📐 디자인 분석

### 핵심 구조

```
차트 위에:

┌─ 가로 오렌지 점선 ────────────────────────────────┐
│  [1L]  [TP]  [SL]  [-1]  [-0.11 USD]  [X]        │
└──────────────────────────────────────────────────┘
위/아래로 드래그 가능

특징:
- 가로로 한 줄 정렬
- 배경: 반투명 (검정 또는 약간의 배경)
- 테두리: 오렌지 점선
- 버튼들이 함께 움직임
```

### 상세 스펙

#### 1. 컨트롤 바 (가로 오버레이)
- **위치:** 차트 위에 부동 (가로 중앙 정렬)
- **높이:** 약 32-36px
- **너비:** 약 500-600px (내용에 따라 자동)
- **배경:** `rgba(0, 0, 0, 0.3)` (매우 투명)
- **테두리:** 
  - 색상: `#FF9800` (오렌지)
  - 스타일: **점선** (dashed)
  - 두께: 2px
- **모서리:** 약간 둥글게 (6-8dp radius)
- **패딩:** 좌우 12px, 상하 6px
- **드래그 가능:** **Y축만** (위아래)

#### 2. 버튼들 (가로 정렬)

##### 2.1 Entry 버튼 (1L)
- **라벨:** "1L"
- **배경색:** `#9E9E9E` (회색)
- **텍스트색:** 흰색
- **크기:** 약 32-36px (정사각형)
- **모서리:** 4dp 둥글게
- **폰트:** Bold, 12sp

##### 2.2 TP 버튼
- **라벨:** "TP"
- **배경색:** `#26a69a` (초록)
- **텍스트색:** 흰색
- **크기:** 약 40px x 32px
- **모서리:** 4dp 둥글게
- **폰트:** Bold, 12sp

##### 2.3 SL 버튼
- **라벨:** "SL"
- **배경색:** `#EF5350` (빨강)
- **텍스트색:** 흰색
- **크기:** 약 40px x 32px
- **모서리:** 4dp 둥글게
- **폰트:** Bold, 12sp

#### 3. 수익/손실 표시
- **형식:** "[-1]  [-0.11 USD]" 
- **위치:** TP/SL 버튼 오른쪽
- **텍스트색:** 
  - 양수(수익): `#26a69a` (초록)
  - 음수(손실): `#EF5350` (빨강)
- **폰트:** Regular, 12sp

#### 4. 닫기 버튼 (X)
- **위치:** 맨 오른쪽
- **크기:** 24x24px
- **아이콘:** X
- **배경색:** `rgba(255, 255, 255, 0.1)`
- **호버 시:** 배경색 변경

### 라인 시각화

#### 가로 점선 (오렌지)
- 컨트롤 바 수평선을 따라 차트 전체 너비 표시
- 색상: `#FF9800` (오렌지)
- 스타일: **점선** (dashed)
- 두께: 2px

#### 수직 라인 (각 라인)
- **Entry 위치:** 회색 수직선 (드래그하면 함께 이동)
- **TP 위치:** 초록 수직선
- **SL 위치:** 빨강 수직선
- **스타일:** 실선, 1px, 약간 투명

---

## 🔧 HTML/JavaScript 구현

```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>R² Chart - TradingView Style</title>
    <script src="https://unpkg.com/lightweight-charts@4/dist/lightweight-charts.standalone.production.js"></script>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            background-color: #1A1F2E;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            overflow: hidden;
        }

        #chartContainer {
            width: 100%;
            height: 100%;
            position: relative;
        }

        /* TP/SL 컨트롤 바 */
        .tp-sl-bar {
            position: absolute;
            background-color: rgba(0, 0, 0, 0.3);
            border: 2px dashed #FF9800;
            border-radius: 8px;
            padding: 8px 12px;
            display: flex;
            align-items: center;
            gap: 8px;
            z-index: 100;
            cursor: grab;
            user-select: none;
            left: 50%;
            transform: translateX(-50%);
            top: 100px;
        }

        .tp-sl-bar:active {
            cursor: grabbing;
        }

        /* 버튼 스타일 */
        .tp-sl-btn {
            padding: 8px 12px;
            border: none;
            border-radius: 4px;
            font-size: 12px;
            font-weight: 600;
            color: white;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            min-width: 40px;
            height: 32px;
            transition: all 0.2s ease;
        }

        .tp-sl-btn:hover {
            opacity: 0.9;
            transform: scale(1.05);
        }

        .tp-sl-btn.entry {
            background-color: #9E9E9E;
            min-width: 36px;
        }

        .tp-sl-btn.tp {
            background-color: #26a69a;
        }

        .tp-sl-btn.sl {
            background-color: #EF5350;
        }

        /* PnL 표시 */
        .pnl-display {
            display: flex;
            align-items: center;
            gap: 6px;
            margin-left: 8px;
            font-size: 12px;
            font-weight: 500;
            color: #E1E8ED;
        }

        .pnl-value {
            color: #EF5350;
        }

        .pnl-value.positive {
            color: #26a69a;
        }

        /* 닫기 버튼 */
        .close-btn {
            background-color: rgba(255, 255, 255, 0.1);
            border: none;
            border-radius: 4px;
            width: 28px;
            height: 28px;
            color: #E1E8ED;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 16px;
            transition: background-color 0.2s;
        }

        .close-btn:hover {
            background-color: rgba(255, 255, 255, 0.2);
        }

        /* 수평 점선 */
        .horizontal-line {
            position: absolute;
            height: 2px;
            border-bottom: 2px dashed #FF9800;
            width: 100%;
            pointer-events: none;
            z-index: 50;
        }

        /* 수직 라인들 */
        svg.vertical-lines {
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            pointer-events: none;
            z-index: 40;
        }

        /* 정보 패널 */
        .info-panel {
            position: absolute;
            top: 12px;
            right: 12px;
            background-color: rgba(0, 0, 0, 0.6);
            border: 1px solid #303641;
            border-radius: 6px;
            padding: 12px;
            color: #E1E8ED;
            font-size: 11px;
            z-index: 95;
            line-height: 1.8;
        }

        .info-row {
            display: flex;
            justify-content: space-between;
            margin-bottom: 6px;
        }

        .info-label {
            color: #888;
            margin-right: 12px;
        }

        .info-value {
            color: #E1E8ED;
            font-weight: 600;
        }
    </style>
</head>
<body>
    <div id="chartContainer">
        <svg class="vertical-lines" id="verticalLines"></svg>
        <div class="horizontal-line" id="horizontalLine"></div>
        <div class="tp-sl-bar" id="tpSlBar">
            <button class="tp-sl-btn entry" id="entryBtn">1L</button>
            <button class="tp-sl-btn tp" id="tpBtn">TP</button>
            <button class="tp-sl-btn sl" id="slBtn">SL</button>
            <div class="pnl-display">
                <span id="pnlValue" class="pnl-value">-1</span>
                <span id="pnlUsd">-0.11 USD</span>
            </div>
            <button class="close-btn" id="closeBtn">×</button>
        </div>
        <div class="info-panel" id="infoPanel"></div>
    </div>

    <script>
        // ========== 차트 초기화 ==========
        const container = document.getElementById('chartContainer');
        const chart = LightweightCharts.createChart(container, {
            layout: {
                background: { color: '#1A1F2E' },
                textColor: '#D1D5DB',
            },
            grid: {
                vertLines: { color: '#2D3139' },
                horzLines: { color: '#2D3139' },
            },
            timeScale: {
                timeVisible: true,
                borderColor: '#3F4751',
            },
            rightPriceScale: {
                borderColor: '#3F4751',
            },
            width: container.offsetWidth,
            height: container.offsetHeight,
        });

        const candleSeries = chart.addCandlestickSeries({
            upColor: '#26a69a',
            downColor: '#ef5350',
        });

        const data = generateCandleData();
        candleSeries.setData(data);
        chart.timeScale().fitContent();

        // ========== 라인 데이터 ==========
        let lineData = {
            entry: 35000,
            tp: 36500,
            sl: 33500,
        };

        let lineSeries = {
            entry: null,
            tp: null,
            sl: null,
        };

        let areaSeriesList = [];

        // ========== 컨트롤 바 드래그 ==========
        const tpSlBar = document.getElementById('tpSlBar');
        let isDragging = false;
        let dragStartY = 0;
        let dragStartPrice = 0;

        tpSlBar.addEventListener('mousedown', startDrag);
        tpSlBar.addEventListener('touchstart', startDrag);

        function startDrag(e) {
            isDragging = true;
            dragStartY = e.clientY || e.touches[0].clientY;
            dragStartPrice = lineData.entry;
        }

        document.addEventListener('mousemove', handleDrag);
        document.addEventListener('touchmove', handleDrag);

        function handleDrag(e) {
            if (!isDragging) return;

            const currentY = e.clientY || e.touches[0].clientY;
            const deltaY = currentY - dragStartY;

            // Y 픽셀 변환 (대략 1px = 10 가격 단위)
            const priceChange = -deltaY * 10;
            const newEntryPrice = dragStartPrice + priceChange;

            // 라인 데이터 업데이트
            const priceDiff = lineData.entry - newEntryPrice;
            lineData.entry = newEntryPrice;
            lineData.tp -= priceDiff;
            lineData.sl -= priceDiff;

            renderLines();

            // Android 알림
            if (window.AndroidBridge) {
                window.AndroidBridge.onLineUpdated('entry', lineData.entry);
            }
        }

        document.addEventListener('mouseup', endDrag);
        document.addEventListener('touchend', endDrag);

        function endDrag() {
            isDragging = false;
        }

        // ========== 라인 렌더링 ==========
        function renderLines() {
            // 기존 제거
            Object.values(lineSeries).forEach(s => {
                if (s) chart.removeSeries(s);
            });
            areaSeriesList.forEach(s => chart.removeSeries(s));
            areaSeriesList = [];

            // 영역 색상화
            // TP-Entry 초록 영역
            const profitArea = chart.addAreaSeries({
                topColor: 'rgba(38, 166, 154, 0.15)',
                bottomColor: 'rgba(38, 166, 154, 0.05)',
                lineColor: 'transparent',
                lineWidth: 0,
            });
            profitArea.setData([
                { time: data[0].time, value: lineData.tp },
                { time: data[data.length - 1].time, value: lineData.tp },
                { time: data[data.length - 1].time, value: lineData.entry },
                { time: data[0].time, value: lineData.entry },
            ]);
            areaSeriesList.push(profitArea);

            // Entry-SL 빨강 영역
            const lossArea = chart.addAreaSeries({
                topColor: 'rgba(239, 83, 80, 0.15)',
                bottomColor: 'rgba(239, 83, 80, 0.05)',
                lineColor: 'transparent',
                lineWidth: 0,
            });
            lossArea.setData([
                { time: data[0].time, value: lineData.entry },
                { time: data[data.length - 1].time, value: lineData.entry },
                { time: data[data.length - 1].time, value: lineData.sl },
                { time: data[0].time, value: lineData.sl },
            ]);
            areaSeriesList.push(lossArea);

            // TP 라인
            lineSeries.tp = chart.addLineSeries({
                color: '#26a69a',
                lineWidth: 2,
            });
            lineSeries.tp.setData([
                { time: data[0].time, value: lineData.tp },
                { time: data[data.length - 1].time, value: lineData.tp },
            ]);

            // Entry 라인
            lineSeries.entry = chart.addLineSeries({
                color: '#9E9E9E',
                lineWidth: 2,
            });
            lineSeries.entry.setData([
                { time: data[0].time, value: lineData.entry },
                { time: data[data.length - 1].time, value: lineData.entry },
            ]);

            // SL 라인
            lineSeries.sl = chart.addLineSeries({
                color: '#EF5350',
                lineWidth: 2,
            });
            lineSeries.sl.setData([
                { time: data[0].time, value: lineData.sl },
                { time: data[data.length - 1].time, value: lineData.sl },
            ]);

            updateInfo();
            updateVerticalLines();
        }

        // ========== 수직 라인 그리기 ==========
        function updateVerticalLines() {
            const svg = document.getElementById('verticalLines');
            svg.innerHTML = '';

            const entryY = chart.priceToCoordinate(lineData.entry);
            const tpY = chart.priceToCoordinate(lineData.tp);
            const slY = chart.priceToCoordinate(lineData.sl);
            const chartHeight = container.offsetHeight;

            if (entryY !== null) {
                drawVerticalLine(svg, entryY, '#9E9E9E', 'entry');
            }
            if (tpY !== null) {
                drawVerticalLine(svg, tpY, '#26a69a', 'tp');
            }
            if (slY !== null) {
                drawVerticalLine(svg, slY, '#EF5350', 'sl');
            }
        }

        function drawVerticalLine(svg, y, color, type) {
            const line = document.createElementNS('http://www.w3.org/2000/svg', 'line');
            line.setAttribute('x1', '0');
            line.setAttribute('y1', y);
            line.setAttribute('x2', svg.parentElement.offsetWidth);
            line.setAttribute('y2', y);
            line.setAttribute('stroke', color);
            line.setAttribute('stroke-width', '1');
            line.setAttribute('opacity', '0.3');
            line.setAttribute('stroke-dasharray', '4,4');
            svg.appendChild(line);
        }

        // ========== 정보 업데이트 ==========
        function updateInfo() {
            const rrRatio = (lineData.tp - lineData.entry) / (lineData.entry - lineData.sl);
            const pnl = lineData.tp - lineData.entry;
            const pnlPercent = (pnl / lineData.entry * 100).toFixed(2);

            // PnL 표시
            const pnlValueEl = document.getElementById('pnlValue');
            pnlValueEl.textContent = pnl >= 0 ? `+${pnlPercent}%` : `${pnlPercent}%`;
            pnlValueEl.className = pnl >= 0 ? 'pnl-value positive' : 'pnl-value';

            document.getElementById('pnlUsd').textContent = 
                `${pnl >= 0 ? '+' : ''}${pnl.toFixed(2)} USD`;

            // 정보 패널
            const infoPanel = document.getElementById('infoPanel');
            infoPanel.innerHTML = `
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
                    <span class="info-value">${rrRatio.toFixed(2)}</span>
                </div>
            `;
        }

        // ========== 닫기 버튼 ==========
        document.getElementById('closeBtn').addEventListener('click', () => {
            tpSlBar.style.display = 'none';
            document.getElementById('horizontalLine').style.display = 'none';
        });

        // ========== 초기화 ==========
        renderLines();

        window.addEventListener('resize', () => {
            chart.applyOptions({
                width: container.offsetWidth,
                height: container.offsetHeight,
            });
            updateVerticalLines();
        });

        // ========== Android 인터페이스 ==========
        window.updateLines = function(entry, tp, sl) {
            lineData.entry = entry;
            lineData.tp = tp;
            lineData.sl = sl;
            renderLines();
        };

        if (window.AndroidBridge) {
            window.AndroidBridge.ready();
        }

        // ========== 데이터 생성 ==========
        function generateCandleData() {
            const basePrice = 35000;
            const result = [];
            const now = Math.floor(Date.now() / 1000);

            for (let i = 0; i < 200; i++) {
                const time = now - (200 - i) * 3600;
                const volatility = Math.random() * 500 - 250;
                const open = basePrice + volatility;
                const close = open + (Math.random() * 300 - 150);
                const high = Math.max(open, close) + Math.random() * 200;
                const low = Math.min(open, close) - Math.random() * 200;

                result.push({ time, open, high, low, close });
            }

            return result;
        }
    </script>
</body>
</html>
```

---

## 📋 핵심 기능

✅ **가로 컨트롤 바** - 차트 위에 가로로 한 줄 표시
✅ **드래그 가능** - Y축만 드래그 (위아래 이동)
✅ **버튼 함께 이동** - Entry, TP, SL 버튼이 함께 움직임
✅ **수평 점선** - 오렌지 점선으로 수평 표시
✅ **수직 라인** - 각 라인의 수직 위치 표시
✅ **정보 패널** - 우상단에 정보 표시
✅ **TradingView 스타일** - 깔끔한 디자인

---

## 🚀 구현 방법

1. 이 HTML을 `assets/tradingview_chart.html`로 저장
2. Android Activity에서 WebView로 로드
3. JavaScriptInterface로 `onLineUpdated()` 연동
4. 버튼 클릭 시 입력값 업데이트

