# R² 차트 구현 프롬프트 — 극단적으로 간단한 버전

## 🎯 **핵심 딱 한 가지만**

차트 위에 **가로로 한 줄의 점선** (오렌지색)을 그으세요.
이 점선을 **드래그할 수 있게** 만드세요.
그게 끝입니다.

---

## 📐 요구사항 (정말 간단함)

### 1. 가로 점선 하나
```
차트 위에:

─── ─── ─── ─── ─── (오렌지 점선)

라벨: [1L] [TP] [SL] [-0.06%] [+76.37 USD] [x]
```

### 2. 드래그
- 이 점선을 마우스/터치로 드래그
- **위아래로만 이동** (좌우는 안 됨)
- 드래그하면 라인의 Y좌표 변경

### 3. 라인 표시
- **TP, Entry, SL** 세 개의 **수평선** (차트 가로 전체)
- TP: 초록색 (#26a69a)
- Entry: 회색 (#9E9E9E)  
- SL: 빨강색 (#ef5350)
- 각 라인이 **별도의 수평선** (세로로 배치됨, 겹치지 않음)

### 4. 라벨
- 컨트롤 바 하나에 모든 버튼: `[1L] [TP] [SL]`
- 우측에 정보: `[-0.06%] [+76.37 USD]`
- 우측끝에 X 버튼

### 5. 색상
- 배경: 검정 (#1A1F2E)
- 점선 테두리: 오렌지 (#FF9800)
- 라인: 각각 색상 (초록/회색/빨강)

---

## 🔧 간단한 구현

```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>R² Chart</title>
    <script src="https://unpkg.com/lightweight-charts@4/dist/lightweight-charts.standalone.production.js"></script>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            background-color: #1A1F2E;
            font-family: Arial, sans-serif;
            overflow: hidden;
        }

        #chartContainer {
            width: 100%;
            height: 100%;
            position: relative;
        }

        /* 드래그 가능한 컨트롤 바 */
        .control-bar {
            position: absolute;
            background-color: rgba(0, 0, 0, 0.4);
            border: 2px dashed #FF9800;
            border-radius: 6px;
            padding: 8px 12px;
            display: flex;
            align-items: center;
            gap: 8px;
            z-index: 100;
            cursor: grab;
            user-select: none;
            left: 50%;
            transform: translateX(-50%);
            top: 150px;
        }

        .control-bar:active {
            cursor: grabbing;
        }

        /* 버튼 스타일 */
        .btn {
            padding: 6px 12px;
            border: none;
            border-radius: 4px;
            font-weight: 600;
            font-size: 12px;
            color: white;
            cursor: pointer;
        }

        .btn.entry {
            background-color: #9E9E9E;
        }

        .btn.tp {
            background-color: #26a69a;
        }

        .btn.sl {
            background-color: #ef5350;
        }

        /* 정보 텍스트 */
        .info {
            margin-left: 8px;
            font-size: 12px;
            color: white;
        }

        .info-value {
            color: #26a69a;
        }

        .close-btn {
            background-color: transparent;
            border: none;
            color: white;
            cursor: pointer;
            font-size: 16px;
            margin-left: 8px;
        }
    </style>
</head>
<body>
    <div id="chartContainer">
        <div class="control-bar" id="controlBar">
            <button class="btn entry">1L</button>
            <button class="btn tp">TP</button>
            <button class="btn sl">SL</button>
            <span class="info">-0.06% <span class="info-value">+76.37 USD</span></span>
            <button class="close-btn" id="closeBtn">×</button>
        </div>
    </div>

    <script>
        // 차트 초기화
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
            width: container.offsetWidth,
            height: container.offsetHeight,
        });

        const candleSeries = chart.addCandlestickSeries({
            upColor: '#26a69a',
            downColor: '#ef5350',
        });

        // 샘플 데이터
        const data = [];
        const now = Math.floor(Date.now() / 1000);
        for (let i = 0; i < 100; i++) {
            const time = now - (100 - i) * 3600;
            const open = 95000 + Math.random() * 2000 - 1000;
            const close = open + Math.random() * 1500 - 750;
            data.push({
                time,
                open,
                high: Math.max(open, close) + Math.random() * 500,
                low: Math.min(open, close) - Math.random() * 500,
                close,
            });
        }

        candleSeries.setData(data);
        chart.timeScale().fitContent();

        // 라인 데이터
        let lines = {
            tp: { price: 97000, color: '#26a69a', series: null },
            entry: { price: 95000, color: '#9E9E9E', series: null },
            sl: { price: 93000, color: '#ef5350', series: null },
        };

        // 라인 그리기
        function drawLines() {
            Object.values(lines).forEach(line => {
                if (line.series) chart.removeSeries(line.series);
            });

            Object.entries(lines).forEach(([key, line]) => {
                const series = chart.addLineSeries({
                    color: line.color,
                    lineWidth: 2,
                });
                series.setData([
                    { time: data[0].time, value: line.price },
                    { time: data[data.length - 1].time, value: line.price },
                ]);
                line.series = series;
            });
        }

        drawLines();

        // 드래그 기능
        const controlBar = document.getElementById('controlBar');
        let isDragging = false;
        let dragStartY = 0;
        let dragStartPrice = lines.entry.price;

        controlBar.addEventListener('mousedown', (e) => {
            isDragging = true;
            dragStartY = e.clientY;
        });

        controlBar.addEventListener('touchstart', (e) => {
            isDragging = true;
            dragStartY = e.touches[0].clientY;
        });

        document.addEventListener('mousemove', (e) => {
            if (!isDragging) return;

            const deltaY = e.clientY - dragStartY;
            const priceChange = -deltaY * 10; // 1px = 10 가격 단위

            // 모든 라인을 함께 이동
            lines.entry.price = dragStartPrice + priceChange;
            lines.tp.price = dragStartPrice + priceChange + 2000;
            lines.sl.price = dragStartPrice + priceChange - 2000;

            drawLines();
        });

        document.addEventListener('touchmove', (e) => {
            if (!isDragging) return;

            const deltaY = e.touches[0].clientY - dragStartY;
            const priceChange = -deltaY * 10;

            lines.entry.price = dragStartPrice + priceChange;
            lines.tp.price = dragStartPrice + priceChange + 2000;
            lines.sl.price = dragStartPrice + priceChange - 2000;

            drawLines();
        });

        document.addEventListener('mouseup', () => {
            isDragging = false;
            dragStartPrice = lines.entry.price;
        });

        document.addEventListener('touchend', () => {
            isDragging = false;
            dragStartPrice = lines.entry.price;
        });

        // 닫기 버튼
        document.getElementById('closeBtn').addEventListener('click', () => {
            controlBar.style.display = 'none';
        });

        // 리사이즈
        window.addEventListener('resize', () => {
            chart.applyOptions({
                width: container.offsetWidth,
                height: container.offsetHeight,
            });
        });

        // Android 인터페이스
        window.updateLines = function(entry, tp, sl) {
            lines.entry.price = entry;
            lines.tp.price = tp;
            lines.sl.price = sl;
            drawLines();
        };

        if (window.AndroidBridge) {
            window.AndroidBridge.ready();
        }
    </script>
</body>
</html>
```

---

## ✅ 이 구현으로:

- ✅ 차트에 3개의 수평선 (TP, Entry, SL)
- ✅ 가로 점선 컨트롤 바 (오렌지)
- ✅ 드래그하면 모든 라인이 위아래로 이동
- ✅ 깔끔한 디자인
- ✅ Android JavaScriptInterface 연동 가능

---

## 🚀 AI에게 요청

이 파일을 AI에게 주고 명확히 요청:

**"정확히 이 코드대로만 구현해줘. 더 이상 복잡하게 하지 말고."**

