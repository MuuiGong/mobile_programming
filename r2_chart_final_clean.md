# R² 차트 — 최종 완벽 버전

## 코드 (이게 최종!)

```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
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

        /* 드래그 라인과 버튼 (네모 없음!) */
        .control-line {
            position: absolute;
            display: flex;
            align-items: center;
            gap: 4px;
            z-index: 100;
            cursor: grab;
            user-select: none;
            left: 50%;
            transform: translateX(-50%);
            top: 300px;
        }

        .control-line:active {
            cursor: grabbing;
        }

        /* 버튼만 있음 (배경 없음) */
        .btn {
            padding: 8px 12px;
            border: none;
            border-radius: 3px;
            font-weight: 600;
            font-size: 12px;
            color: white;
            cursor: pointer;
            transition: all 0.1s ease;
            flex-shrink: 0;
        }

        .btn:hover {
            transform: scale(1.08);
        }

        .btn.active {
            box-shadow: 0 0 8px rgba(255, 255, 255, 0.6);
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

        /* 수평 선 */
        .line-bg {
            position: absolute;
            height: 2px;
            background: linear-gradient(to right, transparent, #E0E0E0 10%, #E0E0E0 90%, transparent);
            width: 400px;
            left: 50%;
            transform: translateX(-50%);
            top: 308px;
            z-index: 50;
            pointer-events: none;
        }
    </style>
</head>
<body>
    <div id="chartContainer">
        <div class="line-bg"></div>
        <div class="control-line" id="controlLine">
            <button class="btn entry active" data-line="entry">1L</button>
            <button class="btn tp" data-line="tp">TP</button>
            <button class="btn sl" data-line="sl">SL</button>
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

        // 데이터 생성
        const data = [];
        const now = Math.floor(Date.now() / 1000);
        for (let i = 0; i < 150; i++) {
            const time = now - (150 - i) * 3600;
            const open = 95000 + Math.random() * 3000 - 1500;
            const close = open + Math.random() * 2000 - 1000;
            data.push({
                time,
                open,
                high: Math.max(open, close) + Math.random() * 1000,
                low: Math.min(open, close) - Math.random() * 1000,
                close,
            });
        }

        candleSeries.setData(data);
        chart.timeScale().fitContent();

        // 라인 가격 데이터
        let prices = {
            entry: 95836.00,
            tp: 97752.72,
            sl: 93919.28,
        };

        let currentLine = 'entry';
        let currentSeries = null;

        // 라인 그리기
        function drawLine() {
            if (currentSeries) chart.removeSeries(currentSeries);

            const colors = { entry: '#9E9E9E', tp: '#26a69a', sl: '#ef5350' };
            const series = chart.addLineSeries({ color: colors[currentLine], lineWidth: 2 });

            series.setData([
                { time: data[0].time, value: prices[currentLine] },
                { time: data[data.length - 1].time, value: prices[currentLine] },
            ]);

            currentSeries = series;
        }

        drawLine();

        // 버튼 클릭
        document.querySelectorAll('.btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                document.querySelectorAll('.btn').forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                currentLine = btn.dataset.line;
                drawLine();
            });
        });

        // 드래그
        const controlLine = document.getElementById('controlLine');
        let isDragging = false;
        let dragStartY = 0;
        let dragStartPrice = prices[currentLine];

        controlLine.addEventListener('mousedown', (e) => {
            isDragging = true;
            dragStartY = e.clientY;
            dragStartPrice = prices[currentLine];
        });

        controlLine.addEventListener('touchstart', (e) => {
            isDragging = true;
            dragStartY = e.touches[0].clientY;
            dragStartPrice = prices[currentLine];
        });

        document.addEventListener('mousemove', (e) => {
            if (!isDragging) return;
            const deltaY = e.clientY - dragStartY;
            prices[currentLine] = dragStartPrice - deltaY * 10;
            drawLine();

            if (window.AndroidBridge) {
                window.AndroidBridge.onLineUpdated(currentLine, prices[currentLine]);
            }
        });

        document.addEventListener('touchmove', (e) => {
            if (!isDragging) return;
            const deltaY = e.touches[0].clientY - dragStartY;
            prices[currentLine] = dragStartPrice - deltaY * 10;
            drawLine();

            if (window.AndroidBridge) {
                window.AndroidBridge.onLineUpdated(currentLine, prices[currentLine]);
            }
        });

        document.addEventListener('mouseup', () => {
            isDragging = false;
        });

        document.addEventListener('touchend', () => {
            isDragging = false;
        });

        // 리사이즈
        window.addEventListener('resize', () => {
            chart.applyOptions({
                width: container.offsetWidth,
                height: container.offsetHeight,
            });
        });

        // Android 인터페이스
        window.updateLine = function(lineType, price) {
            prices[lineType] = price;
            if (currentLine === lineType) drawLine();
        };

        if (window.AndroidBridge) {
            window.AndroidBridge.ready();
        }
    </script>
</body>
</html>
```

---

## ✅ 특징

- ✅ **네모 박스 없음** (라인만 있음)
- ✅ **버튼 3개** 가로로 정렬
- ✅ **드래그 가능** (위/아래)
- ✅ **버튼 클릭** 으로 라인 선택
- ✅ **선택된 라인만 표시**
- ✅ **깔끔한 디자인**

