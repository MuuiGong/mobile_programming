# R² 차트 구현 프롬프트 — TradingView Advanced Chart + TP/SL 라인 기능

## 🎯 목표 개요

> 당신은 Android 차트 통합 전문가입니다.  
> TradingView Lightweight Charts(또는 Advanced Chart)를 WebView로 임베드하고,  
> 그 위에 **엔트리(Entry/1L), 익절(TP), 손절(SL) 라인**을 오버레이하는 기능을 구현해야 합니다.  
> 사용자가 라인을 드래그하거나 값을 숫자로 입력할 때, 실시간으로 **위험-보상 비율(R:R), 손익(P&L), 리스크 스코어**가 표시되어야 합니다.

---

## 📋 요구사항 상세 분석

### 1. 차트 기본 구조

- **차트 렌더링:** TradingView Lightweight Chart (JS) via WebView
- **언어:** Java / Android Native
- **아키텍처:** MVVM 패턴 (ViewModel ↔ ChartViewModel)
- **양방향 통신:** JavaScript Bridge (JS ↔ Android Native)
- **성능:** 60 FPS 유지, 저사양 기기(API 26+) 지원

### 2. 첨부 이미지 분석 및 요구사항

#### 이미지 1 (상단):
- 차트 상단에 **엔트리(1L), TP, SL** 라벨
- 오른쪽 하단에 **R:R 비율 정보** ("17.03")
- 포지션 방향 표시 ("Long -0.0420" 레드 표시)
- 현재 가격과 TP/SL 라인이 차트 위에 시각적으로 표시

#### 이미지 2 (하단):
- **SL 라인을 드래그 중** 상태
- SL 값 변경에 따라 **리스크 정보 실시간 갱신** ("-1.70 USDT (200.06%)" 표시)
- 드래그 중에도 손익과 위험 수치가 동적으로 업데이트

### 3. 핵심 기능 목록

| 기능 | 상세 요구사항 |
|------|-------------|
| **라인 3개 렌더링** | Entry(1L), TP, SL을 차트 위에 수평선으로 표시 (색상: Entry=회색, TP=초록, SL=빨강) |
| **라인 드래그** | 사용자가 화면을 터치하여 TP/SL 라인 상하 이동 가능 (Entry는 고정) |
| **숫자 입력** | 인풋 필드에서 TP/SL 가격을 직접 입력 시 라인 위치 실시간 업데이트 |
| **양방향 JS 브릿지** | 라인 변경 → JS 감지 → Android로 전달 / Android 값 변경 → JS에서 라인 재렌더링 |
| **R:R 계산** | `R:R = (TP - Entry) / (Entry - SL)` 실시간 계산 및 표시 |
| **P&L 계산** | `P&L = (현재가 - Entry) × 포지션수량`, SL에서 가능한 최대손실 계산 |
| **리스크 스코어** | 변동성, MDD, Sharpe 기반 0-100 실시간 계산 및 색상 피드백 |
| **포지션 정보 오버레이** | 차트 우측/우상단에 Entry, TP, SL, R:R, P&L 정보 표시 (반투명 박스) |
| **포지션 방향 표시** | Long/Short 방향 및 색상(Long=초록, Short=빨강) 명확히 표시 |

---

## 🔧 구현 아키텍처

### 전체 데이터 흐름

```
┌─────────────────────────────────────────────────────┐
│            ChartViewModel (MVVM)                     │
│  - entryPrice, tpPrice, slPrice (LiveData)          │
│  - riskRewardRatio, pnl, riskScore (MutableLiveData)│
│  - currentPrice (실시간 업데이트)                    │
└────────────┬────────────────────────────────────────┘
             │
      ┌──────┴──────┐
      │             │
      ▼             ▼
 ┌─────────┐  ┌──────────────────────┐
 │ Native  │  │  WebView (TradingView)│
 │ Android │◄─┤  JS Bridge Handler   │
 │ Code    ├─►│  - onLineUpdated()   │
 └─────────┘  │  - updateChart()     │
              └──────────────────────┘
                      │
                      ▼
              ┌──────────────────┐
              │ Chart UI Overlay │
              │ (Entry/TP/SL)    │
              └──────────────────┘
```

---

## 📐 상세 구현 가이드

### A. WebView 설정 및 TradingView 차트 로드

#### Step 1: Activity/Fragment 레이아웃 XML

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <!-- 차트 컨테이너 (WebView) -->
    <FrameLayout
        android:id="@+id/chartContainer"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="0.65"
        android:background="@android:color/black" />

    <!-- 컨트롤 패널 (Entry/TP/SL 숫자 입력) -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="0.35"
        android:orientation="vertical"
        android:padding="16dp"
        android:background="@drawable/dark_gradient">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:spacing="8dp">

            <EditText
                android:id="@+id/entryInput"
                android:layout_width="0dp"
                android:layout_height="48dp"
                android:layout_weight="1"
                android:hint="Entry"
                android:inputType="numberDecimal"
                android:textColorHint="@android:color/darker_gray"
                android:textColor="@android:color/white"
                android:background="@drawable/input_background" />

            <EditText
                android:id="@+id/tpInput"
                android:layout_width="0dp"
                android:layout_height="48dp"
                android:layout_weight="1"
                android:hint="TP"
                android:inputType="numberDecimal"
                android:textColorHint="@android:color/darker_gray"
                android:textColor="@android:color/white"
                android:background="@drawable/input_background" />

            <EditText
                android:id="@+id/slInput"
                android:layout_width="0dp"
                android:layout_height="48dp"
                android:layout_weight="1"
                android:hint="SL"
                android:inputType="numberDecimal"
                android:textColorHint="@android:color/darker_gray"
                android:textColor="@android:color/white"
                android:background="@drawable/input_background" />
        </LinearLayout>

        <!-- 위험-보상, P&L, 리스크 스코어 정보 표시 -->
        <LinearLayout
            android:id="@+id/infoPanel"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            android:orientation="vertical">

            <TextView
                android:id="@+id/riskRewardText"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="R:R = --"
                android:textColor="@android:color/white"
                android:textSize="16sp"
                android:layout_marginBottom="8dp" />

            <TextView
                android:id="@+id/pnlText"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="P&L = -- USDT"
                android:textColor="@android:color/white"
                android:textSize="16sp"
                android:layout_marginBottom="8dp" />

            <TextView
                android:id="@+id/riskScoreText"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="Risk Score = --"
                android:textColor="@android:color/white"
                android:textSize="16sp" />
        </LinearLayout>
    </LinearLayout>
</LinearLayout>
```

#### Step 2: Activity/Fragment 코드 (WebView 초기화)

```java
public class ChartActivity extends AppCompatActivity {
    private WebView chartWebView;
    private ChartViewModel viewModel;
    private EditText entryInput, tpInput, slInput;
    private TextView riskRewardText, pnlText, riskScoreText;
    private JavaScriptInterface jsInterface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        // 뷰 바인딩
        chartWebView = findViewById(R.id.chartContainer);
        entryInput = findViewById(R.id.entryInput);
        tpInput = findViewById(R.id.tpInput);
        slInput = findViewById(R.id.slInput);
        riskRewardText = findViewById(R.id.riskRewardText);
        pnlText = findViewById(R.id.pnlText);
        riskScoreText = findViewById(R.id.riskScoreText);

        // ViewModel 초기화
        viewModel = new ViewModelProvider(this).get(ChartViewModel.class);

        // WebView 설정
        setupWebView();

        // ViewModel 옵저버 설정
        observeViewModel();

        // 입력 필드 리스너 설정
        setupInputListeners();
    }

    private void setupWebView() {
        WebSettings settings = chartWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        // 하드웨어 가속
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            chartWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }

        // JS 브릿지 등록
        jsInterface = new JavaScriptInterface(viewModel);
        chartWebView.addJavascriptInterface(jsInterface, "AndroidBridge");

        // TradingView 차트 HTML 로드
        String htmlUrl = "file:///android_asset/tradingview_chart.html";
        chartWebView.loadUrl(htmlUrl);

        chartWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.d("WebView", consoleMessage.message() + " @ " + consoleMessage.sourceId());
            }
        });

        chartWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, String.valueOf(url));
                Log.d("WebView", "Chart page loaded");
                // 초기 가격 데이터 로드
                viewModel.initializeChart();
            }
        });
    }

    private void observeViewModel() {
        // Entry 가격 변경
        viewModel.getEntryPrice().observe(this, entryPrice -> {
            entryInput.setText(String.valueOf(entryPrice));
            updateChartLines();
            updateRiskMetrics();
        });

        // TP 가격 변경
        viewModel.getTpPrice().observe(this, tpPrice -> {
            tpInput.setText(String.valueOf(tpPrice));
            updateChartLines();
            updateRiskMetrics();
        });

        // SL 가격 변경
        viewModel.getSlPrice().observe(this, slPrice -> {
            slInput.setText(String.valueOf(slPrice));
            updateChartLines();
            updateRiskMetrics();
        });

        // R:R 비율 변경
        viewModel.getRiskRewardRatio().observe(this, rrRatio -> {
            riskRewardText.setText(String.format("R:R = %.2f", rrRatio));
        });

        // P&L 변경
        viewModel.getPnl().observe(this, pnl -> {
            String pnlColor = pnl >= 0 ? "#00C853" : "#FF1744";
            pnlText.setText(String.format("P&L = <font color='%s'>%.2f USDT</font>", pnlColor, pnl));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                pnlText.setText(Html.fromHtml(pnlText.getText().toString(), Html.FROM_HTML_MODE_LEGACY));
            }
        });

        // 리스크 스코어 변경
        viewModel.getRiskScore().observe(this, score -> {
            String scoreColor = getScoreColor(score);
            riskScoreText.setText(String.format("Risk Score = <font color='%s'>%d</font>", scoreColor, score));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                riskScoreText.setText(Html.fromHtml(riskScoreText.getText().toString(), Html.FROM_HTML_MODE_LEGACY));
            }
        });
    }

    private void setupInputListeners() {
        entryInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                try {
                    double entry = Double.parseDouble(entryInput.getText().toString());
                    viewModel.setEntryPrice(entry);
                } catch (NumberFormatException e) {
                    Log.e("Input", "Invalid entry price");
                }
            }
        });

        tpInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                try {
                    double tp = Double.parseDouble(tpInput.getText().toString());
                    viewModel.setTpPrice(tp);
                } catch (NumberFormatException e) {
                    Log.e("Input", "Invalid TP price");
                }
            }
        });

        slInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                try {
                    double sl = Double.parseDouble(slInput.getText().toString());
                    viewModel.setSlPrice(sl);
                } catch (NumberFormatException e) {
                    Log.e("Input", "Invalid SL price");
                }
            }
        });
    }

    private void updateChartLines() {
        // Android → JS: 라인 업데이트 호출
        String jsCode = String.format(
            "window.updateLines(%f, %f, %f);",
            viewModel.getEntryPrice().getValue(),
            viewModel.getTpPrice().getValue(),
            viewModel.getSlPrice().getValue()
        );
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            chartWebView.evaluateJavascript(jsCode, null);
        } else {
            chartWebView.loadUrl("javascript:" + jsCode);
        }
    }

    private void updateRiskMetrics() {
        viewModel.calculateRiskMetrics();
    }

    private String getScoreColor(int score) {
        if (score >= 75) return "#00C853"; // 초록
        if (score >= 50) return "#FDD835"; // 노랑
        if (score >= 25) return "#FF9100"; // 주황
        return "#FF1744"; // 빨강
    }
}
```

---

### B. JavaScript Bridge (양방향 통신)

#### Step 1: Java 브릿지 인터페이스

```java
public class JavaScriptInterface {
    private ChartViewModel viewModel;

    public JavaScriptInterface(ChartViewModel viewModel) {
        this.viewModel = viewModel;
    }

    /**
     * JS에서 호출: 라인이 드래그되었을 때
     */
    @JavascriptInterface
    public void onLineUpdated(String lineType, double price) {
        // lineType: "entry", "tp", "sl"
        Log.d("JSBridge", String.format("Line %s updated to %f", lineType, price));

        switch (lineType) {
            case "entry":
                viewModel.setEntryPrice(price);
                break;
            case "tp":
                viewModel.setTpPrice(price);
                break;
            case "sl":
                viewModel.setSlPrice(price);
                break;
        }

        // ViewModel에서 자동으로 R:R, P&L, 리스크스코어 계산 (LiveData 업데이트)
    }

    /**
     * JS에서 호출: 현재 가격 업데이트
     */
    @JavascriptInterface
    public void onPriceUpdated(double price) {
        viewModel.setCurrentPrice(price);
        viewModel.calculateRiskMetrics();
    }

    /**
     * Android에서 호출 가능 (JS 함수 준비 확인)
     */
    @JavascriptInterface
    public void ready() {
        Log.d("JSBridge", "JavaScript is ready");
        viewModel.initializeChart();
    }
}
```

#### Step 2: HTML/JavaScript (TradingView 차트 + 라인 렌더링)

저장 위치: `assets/tradingview_chart.html`

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
            background-color: #000;
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
        }

        #chart {
            width: 100%;
            height: 100%;
        }

        .line-info {
            position: absolute;
            top: 10px;
            right: 10px;
            background-color: rgba(0, 0, 0, 0.7);
            color: #fff;
            padding: 10px 15px;
            border-radius: 6px;
            font-size: 12px;
            font-family: monospace;
            z-index: 100;
        }

        .entry-line { color: #999; }
        .tp-line { color: #00C853; }
        .sl-line { color: #FF1744; }
    </style>
</head>
<body>
    <div id="chart"></div>
    <div class="line-info" id="lineInfo"></div>

    <script>
        // TradingView Lightweight Charts 초기화
        const chartContainer = document.getElementById('chart');
        const chart = LightweightCharts.createChart(chartContainer, {
            layout: {
                background: { color: '#000' },
                textColor: '#DDD',
            },
            timeScale: {
                timeVisible: true,
                secondsVisible: false,
            },
            width: chartContainer.offsetWidth,
            height: chartContainer.offsetHeight,
        });

        // 캔들스틱 시리즈 생성
        const candleSeries = chart.addCandlestickSeries({
            upColor: '#26a69a',
            downColor: '#ef5350',
            borderDownColor: '#ef5350',
            borderUpColor: '#26a69a',
            wickDownColor: '#ef5350',
            wickUpColor: '#26a69a',
        });

        // 초기 OHLCV 데이터 (예시)
        const data = [
            { time: '2025-11-16', open: 56800, high: 57200, low: 56500, close: 57000 },
            { time: '2025-11-17', open: 57000, high: 57500, low: 56900, close: 57200 },
            { time: '2025-11-18', open: 57200, high: 57800, low: 57000, close: 57500 },
            // ... 더 많은 데이터
        ];
        candleSeries.setData(data);

        // 라인 데이터 저장
        let lineData = {
            entry: 56925.9,
            tp: 56785.9,
            sl: 51231.5,
        };

        let isDragging = false;
        let draggingLineType = null;

        /**
         * Android에서 호출: 라인 위치 업데이트
         */
        window.updateLines = function(entry, tp, sl) {
            lineData.entry = entry;
            lineData.tp = tp;
            lineData.sl = sl;
            updateLineSeries();
            updateLineInfo();
        };

        /**
         * 라인 시리즈 생성/업데이트
         */
        function updateLineSeries() {
            // 기존 라인 제거
            chart.removeAllSeries();

            // 캔들스틱 다시 추가
            const candleSeries = chart.addCandlestickSeries({
                upColor: '#26a69a',
                downColor: '#ef5350',
                borderDownColor: '#ef5350',
                borderUpColor: '#26a69a',
                wickDownColor: '#ef5350',
                wickUpColor: '#26a69a',
            });
            candleSeries.setData(data);

            // Entry 라인 (회색)
            const entryLine = chart.addLineSeries({ color: '#999', lineWidth: 2 });
            entryLine.setData([
                { time: data[0].time, value: lineData.entry },
                { time: data[data.length - 1].time, value: lineData.entry },
            ]);

            // TP 라인 (초록)
            const tpLine = chart.addLineSeries({ color: '#00C853', lineWidth: 2 });
            tpLine.setData([
                { time: data[0].time, value: lineData.tp },
                { time: data[data.length - 1].time, value: lineData.tp },
            ]);

            // SL 라인 (빨강)
            const slLine = chart.addLineSeries({ color: '#FF1744', lineWidth: 2 });
            slLine.setData([
                { time: data[0].time, value: lineData.sl },
                { time: data[data.length - 1].time, value: lineData.sl },
            ]);

            // 마우스 이벤트: 라인 드래그
            chart.applyOptions({ handleScale: false, handleScroll: false });
        }

        /**
         * 라인 정보 표시 (우상단)
         */
        function updateLineInfo() {
            const rrRatio = (lineData.tp - lineData.entry) / (lineData.entry - lineData.sl);
            const lineInfoDiv = document.getElementById('lineInfo');
            lineInfoDiv.innerHTML = `
                <div class="entry-line">Entry: $${lineData.entry.toFixed(2)}</div>
                <div class="tp-line">TP: $${lineData.tp.toFixed(2)}</div>
                <div class="sl-line">SL: $${lineData.sl.toFixed(2)}</div>
                <div style="color: #FDD835;">R:R: ${rrRatio.toFixed(2)}</div>
            `;
        }

        /**
         * 마우스/터치 핸들러: 라인 드래그
         */
        chartContainer.addEventListener('mousedown', (e) => {
            const rect = chartContainer.getBoundingClientRect();
            const y = e.clientY - rect.top;
            const price = chart.coordinateToPrice(y);

            if (Math.abs(price - lineData.entry) < 100) {
                isDragging = true;
                draggingLineType = 'entry';
            } else if (Math.abs(price - lineData.tp) < 100) {
                isDragging = true;
                draggingLineType = 'tp';
            } else if (Math.abs(price - lineData.sl) < 100) {
                isDragging = true;
                draggingLineType = 'sl';
            }
        });

        chartContainer.addEventListener('mousemove', (e) => {
            if (!isDragging || !draggingLineType) return;

            const rect = chartContainer.getBoundingClientRect();
            const y = e.clientY - rect.top;
            const price = chart.coordinateToPrice(y);

            lineData[draggingLineType] = price;

            // Android로 통지
            if (window.AndroidBridge) {
                window.AndroidBridge.onLineUpdated(draggingLineType, price);
            }

            updateLineInfo();
        });

        chartContainer.addEventListener('mouseup', () => {
            isDragging = false;
            draggingLineType = null;
        });

        /**
         * 초기 로드
         */
        window.addEventListener('load', () => {
            updateLineSeries();
            updateLineInfo();

            // Android에 준비 완료 신호
            if (window.AndroidBridge) {
                window.AndroidBridge.ready();
            }
        });

        // 터치 이벤트도 지원 (모바일)
        chartContainer.addEventListener('touchstart', (e) => {
            const touch = e.touches[0];
            const rect = chartContainer.getBoundingClientRect();
            const y = touch.clientY - rect.top;
            const price = chart.coordinateToPrice(y);

            if (Math.abs(price - lineData.entry) < 100) {
                isDragging = true;
                draggingLineType = 'entry';
            } else if (Math.abs(price - lineData.tp) < 100) {
                isDragging = true;
                draggingLineType = 'tp';
            } else if (Math.abs(price - lineData.sl) < 100) {
                isDragging = true;
                draggingLineType = 'sl';
            }
        });

        chartContainer.addEventListener('touchmove', (e) => {
            if (!isDragging || !draggingLineType) return;

            const touch = e.touches[0];
            const rect = chartContainer.getBoundingClientRect();
            const y = touch.clientY - rect.top;
            const price = chart.coordinateToPrice(y);

            lineData[draggingLineType] = price;

            if (window.AndroidBridge) {
                window.AndroidBridge.onLineUpdated(draggingLineType, price);
            }

            updateLineInfo();
        });

        chartContainer.addEventListener('touchend', () => {
            isDragging = false;
            draggingLineType = null;
        });

        // 창 크기 변경 시 차트 리사이즈
        window.addEventListener('resize', () => {
            chart.applyOptions({
                width: chartContainer.offsetWidth,
                height: chartContainer.offsetHeight,
            });
        });
    </script>
</body>
</html>
```

---

### C. ViewModel 구현

```java
public class ChartViewModel extends ViewModel {
    private MutableLiveData<Double> entryPrice = new MutableLiveData<>(56925.9);
    private MutableLiveData<Double> tpPrice = new MutableLiveData<>(56785.9);
    private MutableLiveData<Double> slPrice = new MutableLiveData<>(51231.5);
    private MutableLiveData<Double> currentPrice = new MutableLiveData<>(56925.9);

    private MutableLiveData<Double> riskRewardRatio = new MutableLiveData<>(0.0);
    private MutableLiveData<Double> pnl = new MutableLiveData<>(0.0);
    private MutableLiveData<Integer> riskScore = new MutableLiveData<>(75);

    private double positionSize = 1.0; // 포지션 수량

    public LiveData<Double> getEntryPrice() { return entryPrice; }
    public LiveData<Double> getTpPrice() { return tpPrice; }
    public LiveData<Double> getSlPrice() { return slPrice; }
    public LiveData<Double> getRiskRewardRatio() { return riskRewardRatio; }
    public LiveData<Double> getPnl() { return pnl; }
    public LiveData<Integer> getRiskScore() { return riskScore; }

    public void setEntryPrice(double price) {
        entryPrice.setValue(price);
        calculateRiskMetrics();
    }

    public void setTpPrice(double price) {
        tpPrice.setValue(price);
        calculateRiskMetrics();
    }

    public void setSlPrice(double price) {
        slPrice.setValue(price);
        calculateRiskMetrics();
    }

    public void setCurrentPrice(double price) {
        currentPrice.setValue(price);
        calculateRiskMetrics();
    }

    public void calculateRiskMetrics() {
        double entry = entryPrice.getValue() != null ? entryPrice.getValue() : 0;
        double tp = tpPrice.getValue() != null ? tpPrice.getValue() : 0;
        double sl = slPrice.getValue() != null ? slPrice.getValue() : 0;
        double current = currentPrice.getValue() != null ? currentPrice.getValue() : entry;

        // R:R 계산
        if (entry != sl && entry != 0) {
            double profit = tp - entry;
            double loss = entry - sl;
            double rr = profit / loss;
            riskRewardRatio.setValue(Math.max(rr, 0.01)); // 최소값 0.01
        }

        // P&L 계산
        double pnlValue = (current - entry) * positionSize;
        pnl.setValue(pnlValue);

        // 리스크 스코어 계산 (간단 예시)
        int score = calculateRiskScore(riskRewardRatio.getValue(), pnl.getValue());
        riskScore.setValue(score);
    }

    public void initializeChart() {
        calculateRiskMetrics();
    }

    private int calculateRiskScore(double rrRatio, double pnlValue) {
        // 공식: score = 100 - (0.4*Vol + 0.4*MDD + 0.2*negSharpe)
        // 간단화: RR > 2 = 75점, RR 1-2 = 50점, RR < 1 = 25점
        if (rrRatio >= 2.0) return 85;
        if (rrRatio >= 1.5) return 75;
        if (rrRatio >= 1.0) return 65;
        if (rrRatio >= 0.5) return 45;
        return 25;
    }
}
```

---

## 🎨 UI/UX 세부 사항

### 색상 스키마

- **Entry 라인:** 회색 (#999)
- **TP 라인:** 초록색 (#00C853)
- **SL 라인:** 빨강색 (#FF1744)
- **배경:** 검정색 (#000)
- **텍스트:** 흰색 (#FFF)

### 드래그 피드백

- 드래그 중: 라인 색상 명도 증가, 불투명도 100%
- 드래그 종료: 라인 다시 원래 색상
- 라인 감지 범위: ±100 가격 단위

### 실시간 업데이트

- Entry/TP/SL 입력 → 0ms 업데이트
- 드래그 중 → 매 프레임 업데이트 (60 FPS)
- R:R/P&L/리스크스코어 → 50ms 디바운스

---

## 🔧 성능 최적화

1. **WebView 메모리:**
   - 하드웨어 가속 활성화
   - 불필요한 DOM 제거
   - 데이터 버퍼링

2. **JS 브릿지:**
   - evaluateJavascript 사용 (loadUrl 대신)
   - 배치 업데이트 (빈번한 개별 호출 피하기)

3. **ViewModel:**
   - LiveData 옵저버 분리 (필요한 것만 구독)
   - 배경 스레드에서 계산

---

## ✅ 테스트 체크리스트

- [ ] 차트 로드 후 라인 3개 표시
- [ ] 라인 드래그 시 부드럽게 이동 (60 FPS 유지)
- [ ] R:R 실시간 계산 및 표시
- [ ] P&L 변경 반영
- [ ] 리스크 스코어 색상 변경
- [ ] 숫자 입력 시 라인 위치 갱신
- [ ] 저사양 기기(API 26) 테스트
- [ ] 메모리 누수 없음

---

## 🚀 확장 기능 (선택)

- 포지션 수량 조절 UI 추가
- 다중 차트 지원
- 포지션 히스토리 저장/로드
- 커스텀 라인 색상 설정
- 실시간 시세 CoinGecko API 연동

