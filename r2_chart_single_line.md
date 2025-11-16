# R² 차트 구현 — 단 하나의 드래그 라인

## 🎯 핵심 (정말 정확함!)

```
차트에:
  1개의 수평선 (회색 또는 흰색)
  그 위에 3개 버튼: [1L] [TP] [SL]
  
  이 선을 드래그하면 → 선택된 버튼의 라인이 결정됨
```

---

## 📐 구조

### 1. 드래그 라인 (1개만!)
- **색상:** 흰색 또는 밝은 회색
- **스타일:** 실선 또는 점선, 2px
- **특징:** 차트 전체 너비를 가로질러 가는 **수평선 하나**

### 2. 버튼 (라인 위에 붙어있음)
```
─────────── [1L] [TP] [SL] ───────────────
```
- **1L 버튼:** 회색 배경, 진입점 결정
- **TP 버튼:** 초록 배경, 익절점 결정  
- **SL 버튼:** 빨강 배경, 손절점 결정
- **현재 선택:** 예를 들어 TP를 드래그하면 **TP 라인이 차트에 나타남**

### 3. 동작 방식
```
1. 사용자가 [TP] 버튼을 누르면 → TP 모드 활성화
2. 라인을 드래그 → TP 라인이 위아래 이동
3. 사용자가 [SL] 버튼을 누르면 → SL 모드로 전환
4. 라인을 드래그 → SL 라인이 위아래 이동
```

### 4. 차트에 표시되는 라인
- **현재 선택된 라인만** 차트에 표시
- 예: TP를 선택 → 초록 라인만 보임
- 예: Entry를 선택 → 회색 라인만 보임

---

## 🔧 완전한 구현

```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>R² Chart - Single Line Control</title>
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

        /* 드래그 가능한 선과 버튼 */
        .drag-line {
            position: absolute;
            height: 40px;
            border: 2px solid #E0E0E0;
            border-radius: 6px;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 6px;
            padding: 0 12px;
            z-index: 100;
            cursor: grab;
            user-select: none;
            left: 50%;
            transform: translateX(-50%);
            top: 200px;
            background-color: rgba(0, 0, 0, 0.3);
        }

        .drag-line:active {
            cursor: grabbing;
        }

        /* 버튼 스타일 */
        .line-btn {
            padding: 8px 14px;
            border: none;
            border-radius: 4px;
            font-weight: 600;
            font-size: 12px;
            color: white;
            cursor: pointer;
            transition: all 0.2s ease;
            border: 2px solid transparent;
        }

        .line-btn:hover {
            transform: scale(1.05);
        }

        .line-btn.active {
            border: 2px solid white;
            box-shadow: 0 0 10px rgba(255, 255, 255, 0.5);
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
        <div class="drag-line" id="dragLine">
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

        // ========== 라인 데이터 ==========
        let linePrices = {
            entry: 95000,
            tp: 97000,
            sl: 93000,
        };

        let currentLineType = 'entry'; // 현재 선택된 라인
        let currentLineSeries = null; // 현재 표시되는 라인

        // ========== 라인 표시 함수 ==========
        function renderLine() {
            // 기존 라인 제거
            if (currentLineSeries) {
                chart.removeSeries(currentLineSeries);
            }

            // 현재 선택된 라인의 색상 결정
            const colors = {
                entry: '#9E9E9E',
                tp: '#26a69a',
                sl: '#ef5350',
            };

            const series = chart.addLineSeries({
                color: colors[currentLineType],
                lineWidth: 3,
            });

            series.setData([
                { time: data[0].time, value: linePrices[currentLineType] },
                { time: data[data.length - 1].time, value: linePrices[currentLineType] },
            ]);

            currentLineSeries = series;
        }

        // 초기 렌더링
        renderLine();

        // ========== 버튼 클릭 ==========
        document.querySelectorAll('.line-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                // 이전 버튼 비활성화
                document.querySelectorAll('.line-btn').forEach(b => b.classList.remove('active'));
                
                // 새 버튼 활성화
                btn.classList.add('active');
                
                // 라인 타입 변경
                currentLineType = btn.dataset.line;
                
                // 라인 다시 그리기
                renderLine();
            });
        });

        // ========== 드래그 기능 ==========
        const dragLine = document.getElementById('dragLine');
        let isDragging = false;
        let dragStartY = 0;
        let dragStartPrice = linePrices[currentLineType];

        dragLine.addEventListener('mousedown', (e) => {
            isDragging = true;
            dragStartY = e.clientY;
            dragStartPrice = linePrices[currentLineType];
        });

        dragLine.addEventListener('touchstart', (e) => {
            isDragging = true;
            dragStartY = e.touches[0].clientY;
            dragStartPrice = linePrices[currentLineType];
        });

        document.addEventListener('mousemove', (e) => {
            if (!isDragging) return;

            const deltaY = e.clientY - dragStartY;
            const priceChange = -deltaY * 10; // 1px = 10 가격 단위

            // 현재 선택된 라인 가격 업데이트
            linePrices[currentLineType] = dragStartPrice + priceChange;

            // 라인 다시 그리기
            renderLine();

            // Android 알림 (선택 사항)
            if (window.AndroidBridge) {
                window.AndroidBridge.onLineUpdated(currentLineType, linePrices[currentLineType]);
            }
        });

        document.addEventListener('touchmove', (e) => {
            if (!isDragging) return;

            const deltaY = e.touches[0].clientY - dragStartY;
            const priceChange = -deltaY * 10;

            linePrices[currentLineType] = dragStartPrice + priceChange;
            renderLine();

            if (window.AndroidBridge) {
                window.AndroidBridge.onLineUpdated(currentLineType, linePrices[currentLineType]);
            }
        });

        document.addEventListener('mouseup', () => {
            isDragging = false;
        });

        document.addEventListener('touchend', () => {
            isDragging = false;
        });

        // ========== 리사이즈 ==========
        window.addEventListener('resize', () => {
            chart.applyOptions({
                width: container.offsetWidth,
                height: container.offsetHeight,
            });
        });

        // ========== Android 인터페이스 ==========
        window.updateLine = function(lineType, price) {
            linePrices[lineType] = price;
            renderLine();
        };

        if (window.AndroidBridge) {
            window.AndroidBridge.ready();
        }
    </script>
</body>
</html>
```

---

## ✅ 동작 방식

### 초기 상태
```
─── [1L*] [TP] [SL] ───  (* 선택됨)
```
- Entry 라인만 차트에 표시 (회색)

### TP 버튼 클릭 후
```
─── [1L] [TP*] [SL] ───  (* 선택됨)
```
- TP 라인이 차트에 표시됨 (초록색)
- 라인을 드래그하면 TP 라인만 이동

### SL 버튼 클릭 후
```
─── [1L] [TP] [SL*] ───  (* 선택됨)
```
- SL 라인이 차트에 표시됨 (빨강색)
- 라인을 드래그하면 SL 라인만 이동

---

## 🎯 핵심 특징

✅ **1개의 드래그 라인만 존재**
✅ **버튼 3개가 같은 선 위에** (1L, TP, SL)
✅ **버튼 클릭으로 라인 선택**
✅ **선택된 라인만 차트에 표시**
✅ **드래그로 라인 위치 변경**
✅ **선택된 버튼에 하이라이트 표시**

---

## 🚀 AI에게 명확한 요청

**"이 코드를 정확히 그대로만 구현해줘. 주석까지 무시하고 그냥 복사해서 쓰면 됨."**

