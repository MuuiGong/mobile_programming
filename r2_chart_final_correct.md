# R² 차트 구현 프롬프트 — 정확한 라인 라벨 오버레이 스타일

## 🎯 핵심 목표

> **중요:** 라인 라벨(**TP, Entry, SL**)이 **차트 위에 크게 오버레이**되어야 합니다.
> 각 라벨은:
> - 라인의 **왼쪽에 큰 텍스트** (배경 박스 포함)
> - 라인의 **오른쪽에 현재 가격** (우측 정렬)
> - 라인 색상과 일치하는 배경 (TP=초록, Entry=회색, SL=빨강)

---

## 📐 이미지 분석 (정확한 스펙)

### 1. 차트 레이아웃
- 전체 화면의 **65-70%가 차트**
- 배경: 검정 (#0D1117)
- 캔들스틱 기본 TradingView 스타일

### 2. 라인 라벨 (매우 중요!)

#### 왼쪽 라벨 박스
- **위치:** 라인의 왼쪽, 차트 영역 내부
- **크기:** 약 120px x 28px (크고 눈에 띄게)
- **배경색:** 
  - TP = `#00C853` (초록)
  - Entry = `#9E9E9E` (회색)
  - SL = `#FF1744` (빨강)
- **텍스트:** 
  - 흰색, **Bold, 13-14sp**
  - 형식: `TP: $97707.79` / `Entry: $95720.00` / `SL: $93876.11`
- **모서리:** 약 4-6dp 둥글게
- **패딩:** 상하 4px, 좌우 8px

#### 오른쪽 가격 표시
- **위치:** 라인의 오른쪽 끝, 차트 우측
- **형식:** 가격만 표시 (예: `97707.79`)
- **크기:** 약 70-80px x 24px
- **배경:** 라인과 같은 색상 (반투명 20%)
- **텍스트:** 흰색, Bold, 12-13sp
- **오른쪽 정렬**

### 3. 라인 자체
- **두께:** 2-3px (선명함)
- **색상:** 
  - TP = `#00C853` (초록)
  - Entry = `#9E9E9E` (회색)
  - SL = `#FF1744` (빨강)
- **스타일:** 실선 (드래그 중 점선)

### 4. 영역 색상화
- **Entry-TP 사이:** `rgba(0, 200, 83, 0.25)` (초록, 25% 투명도)
- **Entry-SL 사이:** `rgba(255, 23, 68, 0.25)` (빨강, 25% 투명도)
- **면적이 크게 표시됨** (R:R 시각화)

### 5. 우상단 정보 패널
- **위치:** 우상단 모서리
- **크기:** 약 140px x 150px (작고 간결)
- **배경:** `rgba(0, 0, 0, 0.8)`
- **내용:**
  ```
  Entry:  $95720.00
  TP:     $97707.79
  SL:     $93876.11
  R:R:    1.08
  P&L:    +78.59 USDT
  위험도:  Low
  ```
- **텍스트 크기:** 10-11sp
- **색상:** 흰색/회색

### 6. 하단 R:R 비율 카드
- **위치:** 차트 아래, 별도 카드
- **크기:** 전체 너비, 약 60dp 높이
- **배경:** `#1C1F26`
- **표시:** 
  - 왼쪽: "R:R 비율" (회색 텍스트)
  - 오른쪽: `R:R 1.08:1` (오렌지색, **24sp Bold**)

---

## 🔧 구현 가이드 (상세)

### A. HTML/JavaScript (차트 라인 라벨 렌더링)

```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>R² Trading Chart - Line Labels</title>
    <script src="https://unpkg.com/lightweight-charts@4/dist/lightweight-charts.standalone.production.js"></script>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            background-color: #0D1117;
            font-family: 'Arial', sans-serif;
            overflow: hidden;
        }

        #chartContainer {
            width: 100%;
            height: 100%;
            position: relative;
        }

        /* 라인 라벨 스타일 (왼쪽 박스) */
        .line-label-left {
            position: absolute;
            padding: 6px 12px;
            border-radius: 4px;
            font-size: 13px;
            font-weight: bold;
            color: white;
            z-index: 100;
            white-space: nowrap;
            user-select: none;
            cursor: grab;
            display: none;
        }

        .line-label-left.tp {
            background-color: #00C853;
        }

        .line-label-left.entry {
            background-color: #9E9E9E;
        }

        .line-label-left.sl {
            background-color: #FF1744;
        }

        /* 오른쪽 가격 표시 */
        .line-label-right {
            position: absolute;
            padding: 4px 8px;
            border-radius: 3px;
            font-size: 12px;
            font-weight: bold;
            color: white;
            z-index: 100;
            white-space: nowrap;
            user-select: none;
            display: none;
        }

        .line-label-right.tp {
            background-color: rgba(0, 200, 83, 0.6);
        }

        .line-label-right.entry {
            background-color: rgba(158, 158, 158, 0.6);
        }

        .line-label-right.sl {
            background-color: rgba(255, 23, 68, 0.6);
        }

        /* 드래그 중 상태 */
        .line-label-left.dragging,
        .line-label-right.dragging {
            opacity: 1;
            transform: scale(1.05);
            box-shadow: 0 0 8px rgba(255, 255, 255, 0.3);
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
            border: 1px solid #666;
        }

        /* 우상단 정보 패널 */
        .info-panel {
            position: absolute;
            top: 10px;
            right: 10px;
            background-color: rgba(0, 0, 0, 0.85);
            border: 1px solid #424242;
            border-radius: 8px;
            padding: 12px;
            color: white;
            font-size: 10px;
            z-index: 95;
            font-family: monospace;
            line-height: 1.8;
            max-width: 160px;
        }

        .info-panel .info-row {
            display: flex;
            justify-content: space-between;
            margin-bottom: 4px;
        }

        .info-panel .label {
            color: #9E9E9E;
            margin-right: 8px;
        }

        .info-panel .value {
            color: white;
            font-weight: bold;
            text-align: right;
            flex-grow: 1;
        }

        .info-panel .value.positive {
            color: #00C853;
        }

        .info-panel .value.negative {
            color: #FF1744;
        }

        .info-panel .value.orange {
            color: #FF9800;
        }
    </style>
</head>
<body>
    <div id="chartContainer">
        <div class="info-panel" id="infoPanel"></div>
        <div class="line-label-left tp" id="tpLabelLeft"></div>
        <div class="line-label-left entry" id="entryLabelLeft"></div>
        <div class="line-label-left sl" id="slLabelLeft"></div>
        <div class="line-label-right tp" id="tpLabelRight"></div>
        <div class="line-label-right entry" id="entryLabelRight"></div>
        <div class="line-label-right sl" id="slLabelRight"></div>
        <div class="price-tooltip" id="priceTooltip"></div>
    </div>

    <script>
        // ========== 차트 초기화 ==========
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
                borderColor: '#2C2F36',
            },
            rightPriceScale: {
                borderColor: '#2C2F36',
            },
            width: container.offsetWidth,
            height: container.offsetHeight,
        });

        const candleSeries = chart.addCandlestickSeries({
            upColor: '#26a69a',
            downColor: '#ef5350',
        });

        // 샘플 데이터
        const data = generateCandleData();
        candleSeries.setData(data);
        chart.timeScale().fitContent();

        // ========== 라인 데이터 ==========
        let lineData = {
            entry: 95720.00,
            tp: 97707.79,
            sl: 93876.11,
        };

        let lineSeries = {
            entry: null,
            tp: null,
            sl: null,
        };

        let areaSeriesList = [];

        // ========== 드래그 상태 ==========
        let isDragging = false;
        let draggingLineType = null;

        // ========== 라인 렌더링 ==========
        function renderLines() {
            // 기존 제거
            if (lineSeries.entry) chart.removeSeries(lineSeries.entry);
            if (lineSeries.tp) chart.removeSeries(lineSeries.tp);
            if (lineSeries.sl) chart.removeSeries(lineSeries.sl);
            areaSeriesList.forEach(s => chart.removeSeries(s));
            areaSeriesList = [];

            // 영역 색상
            const profitArea = chart.addAreaSeries({
                topColor: 'rgba(0, 200, 83, 0.25)',
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

            const lossArea = chart.addAreaSeries({
                topColor: 'rgba(255, 23, 68, 0.25)',
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

            // 라인 그리기
            const lineStyle = isDragging ? 1 : 0; // 0=실선, 1=점선

            lineSeries.entry = chart.addLineSeries({
                color: '#9E9E9E',
                lineWidth: isDragging && draggingLineType === 'entry' ? 3 : 2,
                lineStyle: isDragging && draggingLineType === 'entry' ? 1 : 0,
            });
            lineSeries.entry.setData([
                { time: data[0].time, value: lineData.entry },
                { time: data[data.length - 1].time, value: lineData.entry },
            ]);

            lineSeries.tp = chart.addLineSeries({
                color: '#00C853',
                lineWidth: isDragging && draggingLineType === 'tp' ? 3 : 2,
                lineStyle: isDragging && draggingLineType === 'tp' ? 1 : 0,
            });
            lineSeries.tp.setData([
                { time: data[0].time, value: lineData.tp },
                { time: data[data.length - 1].time, value: lineData.tp },
            ]);

            lineSeries.sl = chart.addLineSeries({
                color: '#FF1744',
                lineWidth: isDragging && draggingLineType === 'sl' ? 3 : 2,
                lineStyle: isDragging && draggingLineType === 'sl' ? 1 : 0,
            });
            lineSeries.sl.setData([
                { time: data[0].time, value: lineData.sl },
                { time: data[data.length - 1].time, value: lineData.sl },
            ]);

            updateLabels();
            updateInfoPanel();
        }

        // ========== 라벨 위치 업데이트 ==========
        function updateLabels() {
            const entryY = chart.priceToCoordinate(lineData.entry);
            const tpY = chart.priceToCoordinate(lineData.tp);
            const slY = chart.priceToCoordinate(lineData.sl);

            // Entry 라벨
            if (entryY !== null) {
                updateLabel('entry', entryY, lineData.entry);
            }

            // TP 라벨
            if (tpY !== null) {
                updateLabel('tp', tpY, lineData.tp);
            }

            // SL 라벨
            if (slY !== null) {
                updateLabel('sl', slY, lineData.sl);
            }
        }

        function updateLabel(type, y, price) {
            const leftLabel = document.getElementById(`${type}LabelLeft`);
            const rightLabel = document.getElementById(`${type}LabelRight`);

            // 왼쪽 라벨
            leftLabel.textContent = `${type.toUpperCase()}: $${price.toFixed(2)}`;
            leftLabel.style.top = (y - 14) + 'px';
            leftLabel.style.left = '10px';
            leftLabel.style.display = 'block';

            // 오른쪽 라벨
            const chartWidth = container.offsetWidth;
            rightLabel.textContent = price.toFixed(2);
            rightLabel.style.top = (y - 12) + 'px';
            rightLabel.style.right = '10px';
            rightLabel.style.display = 'block';
        }

        // ========== 정보 패널 업데이트 ==========
        function updateInfoPanel() {
            const rrRatio = (lineData.tp - lineData.entry) / (lineData.entry - lineData.sl);
            const pnlValue = 100; // 예시

            const html = `
                <div class="info-row">
                    <span class="label">Entry:</span>
                    <span class="value">$${lineData.entry.toFixed(2)}</span>
                </div>
                <div class="info-row">
                    <span class="label">TP:</span>
                    <span class="value">$${lineData.tp.toFixed(2)}</span>
                </div>
                <div class="info-row">
                    <span class="label">SL:</span>
                    <span class="value">$${lineData.sl.toFixed(2)}</span>
                </div>
                <div class="info-row">
                    <span class="label">R:R:</span>
                    <span class="value orange">${rrRatio.toFixed(2)}</span>
                </div>
                <div class="info-row">
                    <span class="label">P&amp;L:</span>
                    <span class="value positive">+${pnlValue.toFixed(2)} USDT</span>
                </div>
                <div class="info-row">
                    <span class="label">위험도:</span>
                    <span class="value">Low</span>
                </div>
            `;

            document.getElementById('infoPanel').innerHTML = html;
        }

        // ========== 드래그 핸들러 ==========
        container.addEventListener('mousedown', startDrag);
        container.addEventListener('touchstart', startDrag);

        function startDrag(e) {
            const y = getEventY(e);
            const price = chart.coordinateToPrice(y);

            if (!price) return;

            const threshold = Math.abs(lineData.entry) * 0.001;

            if (Math.abs(price - lineData.tp) < threshold) {
                isDragging = true;
                draggingLineType = 'tp';
                document.getElementById('tpLabelLeft').classList.add('dragging');
                document.getElementById('tpLabelRight').classList.add('dragging');
            } else if (Math.abs(price - lineData.sl) < threshold) {
                isDragging = true;
                draggingLineType = 'sl';
                document.getElementById('slLabelLeft').classList.add('dragging');
                document.getElementById('slLabelRight').classList.add('dragging');
            } else if (Math.abs(price - lineData.entry) < threshold) {
                isDragging = true;
                draggingLineType = 'entry';
                document.getElementById('entryLabelLeft').classList.add('dragging');
                document.getElementById('entryLabelRight').classList.add('dragging');
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

                // 툴팁
                const tooltip = document.getElementById('priceTooltip');
                tooltip.textContent = `$${price.toFixed(2)}`;
                tooltip.style.top = (y - 30) + 'px';
                const clientX = e.clientX || e.touches?.[0]?.clientX || 0;
                tooltip.style.left = (clientX + 20) + 'px';
                tooltip.style.display = 'block';

                renderLines();

                // Android 알림
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
                document.querySelectorAll('.dragging').forEach(el => el.classList.remove('dragging'));
                document.getElementById('priceTooltip').style.display = 'none';
                draggingLineType = null;
                renderLines();
            }
        }

        function getEventY(e) {
            const rect = container.getBoundingClientRect();
            if (e.touches) return e.touches[0].clientY - rect.top;
            return e.clientY - rect.top;
        }

        // ========== Android 인터페이스 ==========
        window.updateLines = function(entry, tp, sl) {
            lineData.entry = entry;
            lineData.tp = tp;
            lineData.sl = sl;
            renderLines();
        };

        // ========== 초기화 및 리사이즈 ==========
        renderLines();

        window.addEventListener('resize', () => {
            chart.applyOptions({
                width: container.offsetWidth,
                height: container.offsetHeight,
            });
            updateLabels();
        });

        if (window.AndroidBridge) {
            window.AndroidBridge.ready();
        }

        // ========== 샘플 데이터 ==========
        function generateCandleData() {
            const basePrice = 95000;
            const result = [];
            const now = Math.floor(Date.now() / 1000);

            for (let i = 0; i < 200; i++) {
                const time = now - (200 - i) * 3600;
                const volatility = Math.random() * 2000 - 1000;
                const open = basePrice + volatility;
                const close = open + (Math.random() * 1500 - 750);
                const high = Math.max(open, close) + Math.random() * 800;
                const low = Math.min(open, close) - Math.random() * 800;

                result.push({ time, open, high, low, close });
            }

            return result;
        }
    </script>
</body>
</html>
```

---

## 📋 핵심 특징 요약

1. ✅ **왼쪽에 큰 라벨 박스**
   - 형식: `TP: $97707.79`
   - 크기: 120px x 28px
   - 색상: 라인과 일치

2. ✅ **오른쪽에 가격 표시**
   - 형식: `97707.79` (숫자만)
   - 우측 정렬

3. ✅ **영역 색상화**
   - Entry-TP: 초록 반투명 (25%)
   - Entry-SL: 빨강 반투명 (25%)

4. ✅ **우상단 작은 정보 패널**
   - 140x150px 정도
   - 차트를 가리지 않음

5. ✅ **드래그 중 점선 + 툴팁**
   - 부드러운 인터랙션

6. ✅ **하단 R:R 비율 카드**
   - `R:R 1.08:1` (오렌지, 24sp)

