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

        /* 수평 라인 (얇은 회색) */
        .horizontal-line {
            position: absolute;
            height: 1px;
            background-color: #E0E0E0;
            width: 300px;
            left: 50%;
            transform: translateX(-50%);
            z-index: 50;
            pointer-events: none;
        }

        /* 버튼 컨테이너 (네모 없음!) */
        .buttons-container {
            position: absolute;
            display: flex;
            gap: 6px;
            left: 50%;
            transform: translateX(-50%);
            z-index: 100;
            cursor: grab;
            user-select: none;
        }

        .buttons-container:active {
            cursor: grabbing;
        }

        /* 버튼 스타일 */
        .line-btn {
            padding: 8px 14px;
            border: none;
            border-radius: 3px;
            font-weight: 700;
            font-size: 12px;
            color: white;
            cursor: pointer;
            transition: all 0.15s ease;
            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
        }

        .line-btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 4px 8px rgba(0, 0, 0, 0.4);
        }

        .line-btn.active {
            box-shadow: 0 0 12px rgba(255, 255, 255, 0.8);
            transform: scale(1.1);
        }

        .line-btn.entry {
            background-color: #9E9E9E;
        }

        .line-btn.tp {
            background-color: #26a69a;
        }

        .line-btn.sl {
            background-color: #ef5350;
        }
    </style>
</head>
<body>
    <div id="chartContainer">
        <div class="horizontal-line" id="horizontalLine"></div>
        <div class="buttons-container" id="buttonsContainer">
            <button class="line-btn entry active" data-line="entry">1L</button>
            <button class="line-btn tp" data-line="tp">TP</button>
            <button class="line-btn sl" data-line="sl">SL</button>
        </div>
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
            borderUpColor: '#26a69a',
            borderDownColor: '#ef5350',
        });

        // ========== 차트 데이터 ==========
        const data = [];
        const now = Math.floor(Date.now() / 1000);
        const basePrice = 95900;

        for (let i = 0; i < 150; i++) {
            const time = now - (150 - i) * 3600;
            const volatility = Math.random() * 3000 - 1500;
            const open = basePrice + volatility;
            const close = open + (Math.random() * 2000 - 1000);
            const high = Math.max(open, close) + Math.random() * 1000;
            const low = Math.min(open, close) - Math.random() * 1000;

            data.push({ time, open, high, low, close });
        }

        candleSeries.setData(data);
        chart.timeScale().fitContent();

        // ========== 라인 가격 데이터 ==========
        let prices = {
            entry: 95880.00,
            tp: 97810.00,
            sl: 93950.00,
        };

        let currentLine = 'entry';
        let currentSeries = null;

        // ========== 라인 렌더링 ==========
        function renderLine() {
            if (currentSeries) {
                chart.removeSeries(currentSeries);
            }

            const lineColors = {
                entry: '#9E9E9E',
                tp: '#26a69a',
                sl: '#ef5350',
            };

            const series = chart.addLineSeries({
                color: lineColors[currentLine],
                lineWidth: 2,
            });

            series.setData([
                { time: data[0].time, value: prices[currentLine] },
                { time: data[data.length - 1].time, value: prices[currentLine] },
            ]);

            currentSeries = series;
        }

        renderLine();

        // ========== 버튼 클릭 ==========
        document.querySelectorAll('.line-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                document.querySelectorAll('.line-btn').forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                currentLine = btn.dataset.line;
                renderLine();
            });
        });

        // ========== 드래그 기능 ==========
        const buttonsContainer = document.getElementById('buttonsContainer');
        const horizontalLine = document.getElementById('horizontalLine');

        let isDragging = false;
        let dragStartY = 0;
        let dragStartPrice = prices[currentLine];

        buttonsContainer.addEventListener('mousedown', startDrag);
        buttonsContainer.addEventListener('touchstart', startDrag);

        function startDrag(e) {
            isDragging = true;
            dragStartY = e.clientY || e.touches[0].clientY;
            dragStartPrice = prices[currentLine];
        }

        document.addEventListener('mousemove', handleDrag);
        document.addEventListener('touchmove', handleDrag);

        function handleDrag(e) {
            if (!isDragging) return;

            const currentY = e.clientY || e.touches[0].clientY;
            const deltaY = currentY - dragStartY;

            // 라인 위치 업데이트 (1px = 10 가격 단위)
            prices[currentLine] = dragStartPrice - deltaY * 10;

            // 라인 다시 그리기
            renderLine();

            // 버튼 위치도 함께 이동
            const newTop = parseInt(buttonsContainer.style.top) + deltaY;
            buttonsContainer.style.top = newTop + 'px';
            horizontalLine.style.top = newTop + 'px';

            // Android 알림
            if (window.AndroidBridge) {
                window.AndroidBridge.onLineUpdated(currentLine, prices[currentLine]);
            }
        }

        document.addEventListener('mouseup', endDrag);
        document.addEventListener('touchend', endDrag);

        function endDrag() {
            isDragging = false;
        }

        // ========== 초기 위치 설정 ==========
        const initialTop = 300;
        buttonsContainer.style.top = initialTop + 'px';
        horizontalLine.style.top = initialTop + 'px';

        // ========== 리사이즈 ==========
        window.addEventListener('resize', () => {
            chart.applyOptions({
                width: container.offsetWidth,
                height: container.offsetHeight,
            });
        });

        // ========== Android 인터페이스 ==========
        window.updateLine = function(lineType, price) {
            prices[lineType] = price;
            if (currentLine === lineType) {
                renderLine();
            }
        };

        if (window.AndroidBridge) {
            window.AndroidBridge.ready();
        }
    </script>
</body>
</html>