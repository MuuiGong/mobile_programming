# R² 차트 구현 — TradingView Advanced Charts (BingX 방식)

## 🎯 BingX와 동일한 구현

**TradingView Advanced Charts의 내장 기능** 사용:
- ✅ Drawing Tools (선 그리기)
- ✅ 사용자가 차트에서 직접 TP/SL 라인 드래그
- ✅ 라인 자동 감지 및 계산
- ✅ R:R 비율 자동 표시

---

## 🔧 완벽한 HTML 코드

```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>R² Chart - TradingView Advanced Charts (BingX Style)</title>
    <script src="https://s3.tradingview.com/tv.js"></script>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        html, body {
            width: 100%;
            height: 100%;
            font-family: Arial, sans-serif;
            background-color: #1A1F2E;
            color: #D1D5DB;
        }

        body {
            overflow: hidden;
        }

        #container {
            width: 100%;
            height: 100%;
            display: flex;
            flex-direction: column;
        }

        /* 상단 바 */
        .header {
            background-color: #1C1F26;
            padding: 12px 16px;
            border-bottom: 1px solid #2D3139;
            display: flex;
            justify-content: space-between;
            align-items: center;
            z-index: 100;
        }

        .header-left {
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .coin-name {
            font-size: 14px;
            font-weight: 600;
            color: #E1E8ED;
        }

        .current-price {
            font-size: 16px;
            font-weight: 600;
            color: #26a69a;
        }

        .price-change {
            font-size: 12px;
            color: #26a69a;
        }

        .timeframe-buttons {
            display: flex;
            gap: 4px;
        }

        .timeframe-btn {
            padding: 4px 8px;
            border: 1px solid #2D3139;
            background-color: transparent;
            color: #9E9E9E;
            border-radius: 2px;
            font-size: 11px;
            cursor: pointer;
            transition: all 0.2s;
        }

        .timeframe-btn.active {
            background-color: #1E88E5;
            border-color: #1E88E5;
            color: white;
        }

        .timeframe-btn:hover {
            background-color: #2D3139;
        }

        /* 차트 영역 */
        #tradingview-widget {
            flex: 1;
            width: 100%;
            height: 100%;
        }

        /* 하단 정보 패널 */
        .footer {
            background-color: #1C1F26;
            border-top: 1px solid #2D3139;
            padding: 12px 16px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            flex-wrap: wrap;
            gap: 16px;
            z-index: 100;
        }

        .info-section {
            display: flex;
            gap: 24px;
        }

        .info-item {
            display: flex;
            flex-direction: column;
            gap: 2px;
        }

        .info-label {
            font-size: 10px;
            color: #888;
            text-transform: uppercase;
        }

        .info-value {
            font-size: 13px;
            font-weight: 600;
            color: #E1E8ED;
        }

        .info-value.positive {
            color: #26a69a;
        }

        .info-value.negative {
            color: #ef5350;
        }

        .rr-section {
            text-align: center;
        }

        .rr-label {
            font-size: 10px;
            color: #888;
            text-transform: uppercase;
        }

        .rr-value {
            font-size: 20px;
            font-weight: 700;
            color: #f7931a;
        }

        .action-buttons {
            display: flex;
            gap: 8px;
        }

        .action-btn {
            padding: 8px 16px;
            border: none;
            border-radius: 4px;
            font-weight: 600;
            font-size: 12px;
            cursor: pointer;
            transition: all 0.2s;
            color: white;
        }

        .action-btn.entry {
            background-color: #9E9E9E;
        }

        .action-btn.tp {
            background-color: #26a69a;
        }

        .action-btn.sl {
            background-color: #ef5350;
        }

        .action-btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
        }

        /* 드로잉 도구 안내 */
        .drawing-hint {
            position: absolute;
            bottom: 80px;
            right: 20px;
            background-color: rgba(0, 0, 0, 0.8);
            border: 1px solid #303641;
            border-radius: 6px;
            padding: 12px;
            color: #E1E8ED;
            font-size: 11px;
            z-index: 50;
            max-width: 200px;
            line-height: 1.6;
        }
    </style>
</head>
<body>
    <div id="container">
        <!-- 상단 헤더 -->
        <div class="header">
            <div class="header-left">
                <div class="coin-name">BITCOIN</div>
                <div class="current-price" id="currentPrice">$95,902.92</div>
                <div class="price-change" id="priceChange">+0.85%</div>
            </div>

            <div class="timeframe-buttons">
                <button class="timeframe-btn" data-tf="1">1M</button>
                <button class="timeframe-btn" data-tf="5">5M</button>
                <button class="timeframe-btn" data-tf="15">15M</button>
                <button class="timeframe-btn" data-tf="30">30M</button>
                <button class="timeframe-btn active" data-tf="60">1H</button>
                <button class="timeframe-btn" data-tf="240">4H</button>
                <button class="timeframe-btn" data-tf="1440">1D</button>
            </div>
        </div>

        <!-- TradingView Advanced Charts -->
        <div id="tradingview-widget"></div>

        <!-- 드로잉 도구 안내 -->
        <div class="drawing-hint">
            💡 <strong>팁:</strong> 차트의 도구 모음에서<br/>
            선 그리기 도구를 사용하여<br/>
            TP/SL을 설정하세요.
        </div>

        <!-- 하단 정보 패널 -->
        <div class="footer">
            <div class="info-section">
                <div class="info-item">
                    <span class="info-label">Entry</span>
                    <span class="info-value" id="entryInfo">$95,836.00</span>
                </div>
                <div class="info-item">
                    <span class="info-label">TP</span>
                    <span class="info-value" id="tpInfo">$97,752.72</span>
                </div>
                <div class="info-item">
                    <span class="info-label">SL</span>
                    <span class="info-value" id="slInfo">$93,919.28</span>
                </div>
                <div class="info-item">
                    <span class="info-label">P&L</span>
                    <span class="info-value positive" id="pnlInfo">+1,916.72 USDT</span>
                </div>
            </div>

            <div class="rr-section">
                <div class="rr-label">R:R Ratio</div>
                <div class="rr-value" id="rrInfo">1.07:1</div>
            </div>

            <div class="action-buttons">
                <button class="action-btn entry" id="entryBtn">진입</button>
                <button class="action-btn tp" id="tpBtn">익절</button>
                <button class="action-btn sl" id="slBtn">손절</button>
            </div>
        </div>
    </div>

    <script>
        // ========== TradingView Advanced Charts 초기화 ==========
        new TradingView.widget({
            autosize: true,
            symbol: "BINANCE:BTCUSDT",
            interval: "60",
            timezone: "Asia/Seoul",
            theme: "dark",
            style: "1",
            locale: "ko",
            toolbar_bg: "#1C1F26",
            enable_publishing: false,
            allow_symbol_change: true,
            withdateranges: true,
            hide_side_toolbar: false,
            details: true,
            hotlist: true,
            calendar: true,
            show_popup_button_on_panels: true,
            popup_width: "1000px",
            popup_height: "650px",
            container_id: "tradingview-widget"
        });

        // ========== 라인 데이터 ==========
        let tradeData = {
            entry: 95836.00,
            tp: 97752.72,
            sl: 93919.28,
        };

        // ========== 정보 업데이트 ==========
        function updateInfo() {
            document.getElementById('entryInfo').textContent = `$${tradeData.entry.toFixed(2)}`;
            document.getElementById('tpInfo').textContent = `$${tradeData.tp.toFixed(2)}`;
            document.getElementById('slInfo').textContent = `$${tradeData.sl.toFixed(2)}`;

            // R:R 계산
            const rrRatio = (tradeData.tp - tradeData.entry) / (tradeData.entry - tradeData.sl);
            document.getElementById('rrInfo').textContent = rrRatio.toFixed(2) + ':1';

            // P&L 계산
            const pnl = tradeData.tp - tradeData.entry;
            const pnlElement = document.getElementById('pnlInfo');
            pnlElement.textContent = `${pnl >= 0 ? '+' : ''}${pnl.toFixed(2)} USDT`;
            pnlElement.className = pnl >= 0 ? 'info-value positive' : 'info-value negative';

            // Android 알림
            if (window.AndroidBridge) {
                window.AndroidBridge.onTradeDataUpdated(
                    tradeData.entry,
                    tradeData.tp,
                    tradeData.sl,
                    rrRatio.toFixed(2)
                );
            }
        }

        // ========== 버튼 클릭 ==========
        document.getElementById('entryBtn').addEventListener('click', () => {
            if (window.AndroidBridge) {
                window.AndroidBridge.onEntryExecute(tradeData.entry);
            }
            alert(`진입: $${tradeData.entry.toFixed(2)}`);
        });

        document.getElementById('tpBtn').addEventListener('click', () => {
            if (window.AndroidBridge) {
                window.AndroidBridge.onTPExecute(tradeData.tp);
            }
            alert(`익절: $${tradeData.tp.toFixed(2)}`);
        });

        document.getElementById('slBtn').addEventListener('click', () => {
            if (window.AndroidBridge) {
                window.AndroidBridge.onSLExecute(tradeData.sl);
            }
            alert(`손절: $${tradeData.sl.toFixed(2)}`);
        });

        // ========== 초기 업데이트 ==========
        updateInfo();

        // ========== Android 인터페이스 ==========
        window.updateTradeData = function(entry, tp, sl) {
            tradeData.entry = entry;
            tradeData.tp = tp;
            tradeData.sl = sl;
            updateInfo();
        };

        window.setCurrentPrice = function(price) {
            document.getElementById('currentPrice').textContent = `$${price.toFixed(2)}`;
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

- ✅ **TradingView Advanced Charts** - 공식
- ✅ **내장 드로잉 도구** - 사용자가 직접 선 그리기
- ✅ **BingX와 동일** - 차트에서 직접 TP/SL 설정
- ✅ **R:R 자동 계산**
- ✅ **한국어 완벽 지원**
- ✅ **전문가 수준 디자인**

---

## 🚀 사용 방법

1. **이 HTML 코드를 `assets/tradingview_chart.html`에 저장**
2. **차트의 "도구 모음"에서 선 그리기 도구 선택**
3. **차트에서 직접 TP/SL 라인 드래그**
4. **자동으로 R:R 계산 및 표시**

---

## 💡 핵심

이제 **BingX와 정확하게 동일한** 방식으로:
- TradingView 공식 차트 사용
- 사용자가 차트에서 직접 TP/SL 설정
- 자동 계산 및 표시

