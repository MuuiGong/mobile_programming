package com.example.rsquare.ui.trading;

import android.os.Bundle;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.example.rsquare.R;
import com.example.rsquare.domain.RiskCalculator;
import com.example.rsquare.domain.TradeCalculator;
import com.example.rsquare.domain.TradeExecutor;
import com.example.rsquare.ui.chart.ChartWebViewInterface;
import com.example.rsquare.ui.chart.ChartViewModel;
import com.example.rsquare.ui.trade.TradeViewModel;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * 거래 실행 Activity
 * 제안서의 activity_trading.xml 레이아웃 사용
 */
public class TradingActivity extends AppCompatActivity {
    
    private TradeViewModel viewModel;
    private ChartViewModel chartViewModel;
    private TradeExecutor tradeExecutor;
    
    // Views
    private Toolbar toolbar;
    private WebView tradingChart;
    private TextView symbolText;
    private TextView timeframeText;
    private EditText entryPriceInput;
    private EditText tpPriceInput;
    private EditText slPriceInput;
    private Spinner leverageSpinner;
    private Button btnLong;
    private Button btnShort;
    private TextView rrRatioText;
    private TextView riskScoreTrading;
    private Button btnEnterTrade;
    
    // Data
    private String currentSymbol = "BTCUSDT";
    private String currentTimeframe = "1H";
    private int leverage = 5;
    private boolean isLong = true;
    private double currentPrice = 0.0;
    private boolean isChartReady = false;
    private java.util.List<java.util.List<Object>> pendingKlines = null;
    
    private final DecimalFormat priceFormatter = new DecimalFormat("#,##0.00");
    private final NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trading);
        
        // ViewModel 초기화
        viewModel = new ViewModelProvider(this).get(TradeViewModel.class);
        chartViewModel = new ViewModelProvider(this).get(ChartViewModel.class);
        tradeExecutor = new TradeExecutor(this);
        
        initViews();
        setupToolbar();
        setupWebView();
        setupListeners();
        setupObservers();
        
        // 초기 데이터 로드
        loadInitialData();
    }
    
    /**
     * View 초기화
     */
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tradingChart = findViewById(R.id.trading_chart);
        symbolText = findViewById(R.id.symbol_text);
        timeframeText = findViewById(R.id.timeframe_text);
        entryPriceInput = findViewById(R.id.entry_price_input);
        tpPriceInput = findViewById(R.id.tp_price_input);
        slPriceInput = findViewById(R.id.sl_price_input);
        leverageSpinner = findViewById(R.id.leverage_spinner);
        btnLong = findViewById(R.id.btn_long);
        btnShort = findViewById(R.id.btn_short);
        rrRatioText = findViewById(R.id.rr_ratio_text);
        riskScoreTrading = findViewById(R.id.risk_score_trading);
        btnEnterTrade = findViewById(R.id.btn_enter_trade);
        
        // 심볼 및 타임프레임 설정
        symbolText.setText(currentSymbol + " | " + currentTimeframe);
        timeframeText.setText(leverage + "x 레버리지");
        
        // 레버리지 Spinner 설정
        String[] leverageOptions = {"1x", "2x", "3x", "5x", "10x", "20x"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, leverageOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        leverageSpinner.setAdapter(adapter);
        // 기본값 5x 설정 (인덱스 3)
        leverageSpinner.setSelection(3);
        
        // 롱/숏 버튼 초기 상태 설정
        updatePositionButtons();
    }
    
    /**
     * Toolbar 설정
     */
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }
    
    /**
     * WebView 설정
     */
    private void setupWebView() {
        WebSettings webSettings = tradingChart.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // 캐시 비활성화 (개발 중)
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        tradingChart.clearCache(true);
        
        // WebChromeClient 설정 (콘솔 로그 확인용)
        tradingChart.setWebChromeClient(new android.webkit.WebChromeClient() {
            @Override
            public boolean onConsoleMessage(android.webkit.ConsoleMessage consoleMessage) {
                android.util.Log.d("TradingChart", consoleMessage.message() + " -- From line "
                        + consoleMessage.lineNumber() + " of " + consoleMessage.sourceId());
                return true;
            }
        });
        
        tradingChart.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                android.util.Log.d("TradingActivity", "Chart page loaded: " + url);
            }
        });
        
        // ChartWebViewInterface 연결
        ChartWebViewInterface chartInterface = new ChartWebViewInterface(
            new ChartWebViewInterface.ExtendedChartCallback() {
                @Override
                public void onPriceChanged(double price) {
                    if (price > 0) {
                        // updateRiskMetrics()가 메인 스레드 체크를 수행하므로 직접 호출 가능
                        runOnUiThread(() -> {
                            currentPrice = price;
                            String currentText = entryPriceInput.getText() != null ? 
                                entryPriceInput.getText().toString() : "";
                            if (currentText == null || currentText.trim().isEmpty()) {
                                entryPriceInput.setText(formatPrice(price));
                            }
                            updateRiskMetrics();
                        });
                    }
                }
                
                @Override
                public void onEntryPriceChanged(double price) {
                    if (price > 0) {
                        runOnUiThread(() -> {
                            entryPriceInput.setText(formatPrice(price));
                            updateRiskMetrics();
                        });
                    }
                }
                
                @Override
                public void onTakeProfitChanged(double price) {
                    if (price > 0) {
                        runOnUiThread(() -> {
                            tpPriceInput.setText(formatPrice(price));
                            updateRiskMetrics();
                        });
                    }
                }
                
                @Override
                public void onStopLossChanged(double price) {
                    if (price > 0) {
                        runOnUiThread(() -> {
                            slPriceInput.setText(formatPrice(price));
                            updateRiskMetrics();
                        });
                    }
                }
                
            @Override
            public void onChartReady() {
                android.util.Log.d("TradingActivity", "Chart ready callback received");
                runOnUiThread(() -> {
                    isChartReady = true;

                    // 대기 중인 OHLC 데이터가 있으면 전송 (포지션 타입 설정은 데이터 로드 후 자동으로)
                    if (pendingKlines != null && !pendingKlines.isEmpty()) {
                        android.util.Log.d("TradingActivity", "Sending pending OHLC data");
                        loadBinanceOHLCData(pendingKlines);
                        pendingKlines = null;
                    }
                    // OHLC 데이터가 없어도 chart.html에서 기본 포지션 타입을 설정함
                });
            }
            }
        );
        
        // JavaScript 인터페이스 등록
        tradingChart.addJavascriptInterface(chartInterface, "Android");
        
        // 차트 HTML 로드
        tradingChart.loadUrl("file:///android_asset/chart.html");
    }
    
    /**
     * 리스너 설정
     */
    private void setupListeners() {
        // Entry Price 변경
        entryPriceInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateRiskMetrics();
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // TP Price 변경
        tpPriceInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateRiskMetrics();
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // SL Price 변경
        slPriceInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateRiskMetrics();
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // 레버리지 변경 리스너
        leverageSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                leverage = Integer.parseInt(selected.replace("x", ""));
                timeframeText.setText(leverage + "x 레버리지");
                updateRiskMetrics();
            }
            
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        
        // 롱 버튼
        btnLong.setOnClickListener(v -> {
            android.util.Log.d("TradingActivity", "LONG button clicked, current isLong: " + isLong);
            if (!isLong) {
                android.util.Log.d("TradingActivity", "Switching to LONG position");
                isLong = true;
                updatePositionButtons();
                updateRiskMetrics();
                // 차트에 포지션 타입 변경 알림
                setPositionType("long");
            } else {
                android.util.Log.d("TradingActivity", "Already in LONG position");
            }
        });

        // 숏 버튼
        btnShort.setOnClickListener(v -> {
            android.util.Log.d("TradingActivity", "SHORT button clicked, current isLong: " + isLong);
            if (isLong) {
                android.util.Log.d("TradingActivity", "Switching to SHORT position");
                isLong = false;
                updatePositionButtons();
                updateRiskMetrics();
                // 차트에 포지션 타입 변경 알림
                setPositionType("short");
            } else {
                android.util.Log.d("TradingActivity", "Already in SHORT position");
            }
        });
        
        // 거래 진입 버튼
        btnEnterTrade.setOnClickListener(v -> executeTrade());
    }
    
    /**
     * 포지션 버튼 상태 업데이트
     */
    private void updatePositionButtons() {
        if (isLong) {
            btnLong.setBackgroundResource(R.drawable.btn_primary);
            btnShort.setBackgroundResource(R.drawable.btn_secondary);
        } else {
            btnLong.setBackgroundResource(R.drawable.btn_secondary);
            btnShort.setBackgroundResource(R.drawable.btn_primary);
        }
    }

    /**
     * 포지션 타입을 차트에 설정하고 EP, TP, SL 위치 조정
     */
    private void setPositionType(String positionType) {
        android.util.Log.d("TradingActivity", "=== SET POSITION TYPE: " + positionType + " ===");

        String jsCode = String.format(
            "(function() { " +
            "  try { " +
            "    console.log('=== JAVA CALL: setPositionType ==='); " +
            "    if (typeof adjustLinesForPositionType === 'function') { " +
            "      console.log('Calling adjustLinesForPositionType with:', '%s'); " +
            "      adjustLinesForPositionType('%s'); " +
            "    } else { " +
            "      console.error('adjustLinesForPositionType function not found'); " +
            "    } " +
            "    console.log('=== JAVA CALL COMPLETED ==='); " +
            "  } catch (error) { " +
            "    console.error('Error in setPositionType:', error); " +
            "  } " +
            "})();",
            positionType, positionType
        );

        tradingChart.post(() -> {
            if (tradingChart != null) {
                android.util.Log.d("TradingActivity", "Evaluating JavaScript for position type: " + positionType);
                tradingChart.evaluateJavascript(jsCode, result -> {
                    android.util.Log.d("TradingActivity", "JavaScript evaluation result: " + result);
                });
            } else {
                android.util.Log.e("TradingActivity", "TradingChart is null!");
            }
        });
    }
    
    /**
     * Observer 설정
     */
    private void setupObservers() {
        // ChartViewModel의 현재 가격 관찰 (웹소켓 실시간 업데이트)
        chartViewModel.getCurrentPrice().observe(this, price -> {
            if (price != null && price > 0) {
                android.util.Log.d("TradingActivity", "Current price updated from WebSocket: " + price);
                currentPrice = price;
                
                // JavaScript에 현재가 업데이트 전달
                String jsCode = "if (typeof setCurrentPrice === 'function') { " +
                    "console.log('Setting current price from Android:', " + price + "); " +
                    "setCurrentPrice(" + price + "); }";
                tradingChart.post(() -> {
                    if (tradingChart != null) {
                        tradingChart.evaluateJavascript(jsCode, null);
                    }
                });
                
                // 진입가가 비어있으면 현재 가격으로 설정
                String currentText = entryPriceInput.getText() != null ? 
                    entryPriceInput.getText().toString().trim() : "";
                if (currentText.isEmpty()) {
                    entryPriceInput.setText(formatPrice(price));
                }
                updateRiskMetrics();
            }
        });
        
        // Binance OHLC 데이터 관찰
        chartViewModel.getBinanceKlines().observe(this, klines -> {
            if (klines != null && !klines.isEmpty()) {
                android.util.Log.d("TradingActivity", "Loading Binance OHLC data, klines: " + klines.size());
                loadBinanceOHLCData(klines);
            }
        });
        
        // 실시간 Kline 업데이트 관찰 (WebSocket에서 받은 새로운 캔들)
        chartViewModel.getKlineUpdate().observe(this, klineData -> {
            if (klineData != null && isChartReady) {
                android.util.Log.d("TradingActivity", "Real-time kline update: " + klineData.coinId + " " + klineData.close);
                
                // JavaScript에 실시간 캔들 업데이트 전달
                String jsCode = "if (typeof updateKline === 'function') { " +
                    "updateKline(" + klineData.openTime + ", " + klineData.open + ", " + 
                    klineData.high + ", " + klineData.low + ", " + klineData.close + ", " + 
                    klineData.volume + "); }";
                tradingChart.post(() -> {
                    if (tradingChart != null) {
                        tradingChart.evaluateJavascript(jsCode, null);
                    }
                });
            }
        });
    }
    
    /**
     * 초기 데이터 로드
     */
    private void loadInitialData() {
        android.util.Log.d("TradingActivity", "Loading initial data");
        // ChartViewModel을 통해 웹소켓 연결 및 차트 데이터 로드
        chartViewModel.loadMarketData();
        chartViewModel.loadChartData("bitcoin", 7);
    }
    
    /**
     * Binance OHLC 데이터 로드
     */
    private void loadBinanceOHLCData(java.util.List<java.util.List<Object>> klines) {
        // 차트가 준비되지 않았으면 대기
        if (!isChartReady) {
            android.util.Log.d("TradingActivity", "Chart not ready yet, storing klines for later");
            pendingKlines = klines;
            return;
        }
        
        try {
            // Binance klines 형식: [[openTime, open, high, low, close, volume, ...], ...]
            org.json.JSONArray klinesArray = new org.json.JSONArray();
            
            for (java.util.List<Object> kline : klines) {
                if (kline != null && kline.size() >= 6) {
                    org.json.JSONArray klineArray = new org.json.JSONArray();
                    for (Object value : kline) {
                        if (value instanceof Number) {
                            klineArray.put(((Number) value).doubleValue());
                        } else if (value instanceof String) {
                            try {
                                klineArray.put(Double.parseDouble((String) value));
                            } catch (NumberFormatException e) {
                                klineArray.put(value.toString());
                            }
                        } else {
                            klineArray.put(value.toString());
                        }
                    }
                    klinesArray.put(klineArray);
                }
            }
            
            String klinesString = klinesArray.toString();
            android.util.Log.d("TradingActivity", "Calling setOHLCData with data length: " + klinesString.length());
            
            // JavaScript에 OHLC 데이터 직접 전달 (재시도 로직 포함)
            String jsCode = String.format(
                "(function() { " +
                "  if (typeof setOHLCData === 'function') { " +
                "    console.log('Calling setOHLCData'); " +
                "    setOHLCData(%s); " +
                "  } else { " +
                "    console.error('setOHLCData function not found, retrying...'); " +
                "    setTimeout(function() { " +
                "      if (typeof setOHLCData === 'function') { " +
                "        console.log('Retry: Calling setOHLCData'); " +
                "        setOHLCData(%s); " +
                "      } else { " +
                "        console.error('setOHLCData function still not found after retry'); " +
                "      } " +
                "    }, 500); " +
                "  } " +
                "})();",
                klinesString, klinesString
            );
            tradingChart.post(() -> {
                if (tradingChart != null) {
                    tradingChart.evaluateJavascript(jsCode, null);
                }
            });
        } catch (Exception e) {
            android.util.Log.e("TradingActivity", "Error loading Binance OHLC data", e);
            e.printStackTrace();
        }
    }
    
    /**
     * 리스크 메트릭 업데이트 (R:R 비율, Risk Score)
     */
    private void updateRiskMetrics() {
        // 메인 스레드에서만 실행되도록 보장
        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnUiThread(this::updateRiskMetrics);
            return;
        }
        
        try {
            String entryText = entryPriceInput.getText() != null ? entryPriceInput.getText().toString().trim() : "";
            String tpText = tpPriceInput.getText() != null ? tpPriceInput.getText().toString().trim() : "";
            String slText = slPriceInput.getText() != null ? slPriceInput.getText().toString().trim() : "";
            
            // 빈 문자열이면 기본값 표시
            if (entryText.isEmpty() || tpText.isEmpty() || slText.isEmpty()) {
                rrRatioText.setText("0.00:1");
                riskScoreTrading.setText("0/100 🔴");
                riskScoreTrading.setTextColor(getColor(R.color.risk_danger));
                return;
            }
            
            double entryPrice = parsePrice(entryText);
            double tpPrice = parsePrice(tpText);
            double slPrice = parsePrice(slText);
            
            // 파싱 결과 로깅 (디버깅용)
            android.util.Log.d("TradingActivity", String.format(Locale.US, 
                "가격 파싱: Entry=%s->%.2f, TP=%s->%.2f, SL=%s->%.2f", 
                entryText, entryPrice, tpText, tpPrice, slText, slPrice));
            
            // 모든 가격이 유효한지 확인
            if (entryPrice > 0 && tpPrice > 0 && slPrice > 0) {
                // 롱 포지션: TP > Entry > SL, 숏 포지션: SL > Entry > TP
                boolean isValid = isLong ? 
                    (tpPrice > entryPrice && entryPrice > slPrice) : 
                    (slPrice > entryPrice && entryPrice > tpPrice);
                
                if (isValid) {
                    // R:R 비율 계산
                    double rrRatio = TradeCalculator.calculateRRRatio(
                        entryPrice, tpPrice, slPrice, isLong
                    );
                    if (!Double.isNaN(rrRatio) && !Double.isInfinite(rrRatio) && rrRatio > 0) {
                        rrRatioText.setText(String.format(Locale.US, "%.2f:1", rrRatio));
                        
                        // Risk Score 계산 (간단한 버전)
                        int riskScore = calculateRiskScore(rrRatio, entryPrice, tpPrice, slPrice);
                        riskScoreTrading.setText(riskScore + "/100 " + getRiskEmoji(riskScore));
                        riskScoreTrading.setTextColor(getRiskColor(riskScore));
                    } else {
                        rrRatioText.setText("0.00:1");
                        riskScoreTrading.setText("0/100 🔴");
                        riskScoreTrading.setTextColor(getColor(R.color.risk_danger));
                    }
                } else {
                    // 가격 순서가 잘못됨
                    android.util.Log.w("TradingActivity", String.format(Locale.US,
                        "가격 순서 오류: Entry=%.2f, TP=%.2f, SL=%.2f, isLong=%b",
                        entryPrice, tpPrice, slPrice, isLong));
                    rrRatioText.setText("0.00:1");
                    riskScoreTrading.setText("0/100 🔴");
                    riskScoreTrading.setTextColor(getColor(R.color.risk_danger));
                }
            } else {
                // 가격이 입력되지 않음
                android.util.Log.w("TradingActivity", String.format(Locale.US,
                    "가격 파싱 실패: Entry=%.2f, TP=%.2f, SL=%.2f",
                    entryPrice, tpPrice, slPrice));
                rrRatioText.setText("0.00:1");
                riskScoreTrading.setText("0/100 🔴");
                riskScoreTrading.setTextColor(getColor(R.color.risk_danger));
            }
        } catch (Exception e) {
            android.util.Log.e("TradingActivity", "리스크 메트릭 업데이트 오류", e);
            rrRatioText.setText("0.00:1");
            riskScoreTrading.setText("0/100 🔴");
            riskScoreTrading.setTextColor(getColor(R.color.risk_danger));
        }
    }
    
    /**
     * Risk Score 계산
     */
    private int calculateRiskScore(double rrRatio, double entry, double tp, double sl) {
        // 간단한 Risk Score 계산
        // R:R 비율이 좋을수록 높은 점수
        int baseScore = 50;
        
        if (rrRatio >= 2.0) {
            baseScore = 90;
        } else if (rrRatio >= 1.5) {
            baseScore = 80;
        } else if (rrRatio >= 1.0) {
            baseScore = 70;
        } else if (rrRatio >= 0.5) {
            baseScore = 50;
        } else {
            baseScore = 30;
        }
        
        // 레버리지 페널티
        if (leverage > 10) {
            baseScore -= 20;
        } else if (leverage > 5) {
            baseScore -= 10;
        }
        
        return Math.max(0, Math.min(100, baseScore));
    }
    
    /**
     * Risk Score에 따른 이모지 반환
     */
    private String getRiskEmoji(int score) {
        if (score >= 71) return "🟢";
        if (score >= 31) return "🟡";
        return "🔴";
    }
    
    /**
     * Risk Score에 따른 색상 반환
     */
    private int getRiskColor(int score) {
        if (score >= 71) return getColor(R.color.risk_safe);
        if (score >= 31) return getColor(R.color.risk_caution);
        return getColor(R.color.risk_danger);
    }
    
    /**
     * 거래 실행
     */
    private void executeTrade() {
        try {
            String entryText = entryPriceInput.getText() != null ? 
                entryPriceInput.getText().toString().trim() : "";
            String tpText = tpPriceInput.getText() != null ? 
                tpPriceInput.getText().toString().trim() : "";
            String slText = slPriceInput.getText() != null ? 
                slPriceInput.getText().toString().trim() : "";
            
            if (entryText.isEmpty() || tpText.isEmpty() || slText.isEmpty()) {
                Toast.makeText(this, "모든 가격을 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }
            
            double entryPrice = parsePrice(entryText);
            double tpPrice = parsePrice(tpText);
            double slPrice = parsePrice(slText);
            
            // 파싱 결과 로깅
            android.util.Log.d("TradingActivity", String.format(Locale.US,
                "거래 실행: Entry=%s->%.2f, TP=%s->%.2f, SL=%s->%.2f",
                entryText, entryPrice, tpText, tpPrice, slText, slPrice));
            
            if (entryPrice <= 0 || tpPrice <= 0 || slPrice <= 0) {
                android.util.Log.w("TradingActivity", String.format(Locale.US,
                    "가격 파싱 실패: Entry=%.2f, TP=%.2f, SL=%.2f",
                    entryPrice, tpPrice, slPrice));
                Toast.makeText(this, "가격 형식이 올바르지 않습니다", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 가격 순서 검증
            boolean isValid = isLong ? 
                (tpPrice > entryPrice && entryPrice > slPrice) : 
                (slPrice > entryPrice && entryPrice > tpPrice);
            
            if (!isValid) {
                Toast.makeText(this, "가격 순서가 올바르지 않습니다", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 사용자 ID 가져오기 (임시로 1 사용)
            long userId = 1;
            
            // 거래 실행 (백그라운드 스레드에서 실행)
            new Thread(() -> {
                tradeExecutor.executeTrade(
                    userId,
                    currentSymbol,
                    entryPrice,
                    tpPrice,
                    slPrice,
                    isLong,
                    leverage,
                    new TradeExecutor.OnTradeExecutedListener() {
                        @Override
                        public void onSuccess(long positionId, TradeCalculator.TradeCalculationResult result) {
                            runOnUiThread(() -> {
                                Toast.makeText(TradingActivity.this, 
                                    "거래가 성공적으로 실행되었습니다", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                        }
                        
                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> {
                                Toast.makeText(TradingActivity.this, error, Toast.LENGTH_SHORT).show();
                            });
                        }
                        
                        @Override
                        public void onWarning(String warning) {
                            runOnUiThread(() -> {
                                Toast.makeText(TradingActivity.this, warning, Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                );
            }).start();
            
        } catch (NumberFormatException e) {
            android.util.Log.e("TradingActivity", "가격 파싱 오류", e);
            Toast.makeText(this, "가격 형식이 올바르지 않습니다", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            android.util.Log.e("TradingActivity", "거래 실행 오류", e);
            Toast.makeText(this, "거래 실행 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 가격 파싱 (더 강력한 파싱 로직)
     */
    private double parsePrice(String priceStr) {
        if (priceStr == null) {
            return 0.0;
        }
        
        String trimmed = priceStr.trim();
        if (trimmed.isEmpty()) {
            return 0.0;
        }
        
        try {
            // 모든 비숫자 문자 제거 ($, 쉼표, 공백 등)
            String cleaned = trimmed.replaceAll("[^0-9.]", "");
            if (cleaned.isEmpty()) {
                return 0.0;
            }
            
            // 소수점이 여러 개인 경우 처리 (첫 번째 소수점만 유지)
            int firstDotIndex = cleaned.indexOf('.');
            if (firstDotIndex >= 0) {
                String beforeDot = cleaned.substring(0, firstDotIndex);
                String afterDot = cleaned.substring(firstDotIndex + 1).replace(".", "");
                // 소수점 앞뒤가 모두 비어있으면 0 반환
                if (beforeDot.isEmpty() && afterDot.isEmpty()) {
                    return 0.0;
                }
                cleaned = beforeDot + (afterDot.isEmpty() ? "" : "." + afterDot);
            }
            
            // 빈 문자열 체크
            if (cleaned.isEmpty() || cleaned.equals(".")) {
                return 0.0;
            }
            
            // 숫자만 있는 경우 (소수점 없음)
            if (!cleaned.contains(".")) {
                long longValue = Long.parseLong(cleaned);
                if (longValue <= 0) {
                    return 0.0;
                }
                return (double) longValue;
            }
            
            // 소수점이 있는 경우
            double result = Double.parseDouble(cleaned);
            
            // 유효성 검사
            if (result <= 0 || Double.isNaN(result) || Double.isInfinite(result)) {
                return 0.0;
            }
            
            return result;
        } catch (NumberFormatException e) {
            android.util.Log.e("TradingActivity", "가격 파싱 오류: 원본='" + priceStr + "'", e);
            return 0.0;
        } catch (Exception e) {
            android.util.Log.e("TradingActivity", "가격 파싱 예외: 원본='" + priceStr + "'", e);
            return 0.0;
        }
    }
    
    /**
     * 가격 포맷팅 (천 단위 구분자 없이 숫자만 표시)
     */
    private String formatPrice(double price) {
        if (price <= 0 || Double.isNaN(price) || Double.isInfinite(price)) {
            return "";
        }
        // 소수점 자리수 조정 (가격에 따라, 천 단위 구분자 없이)
        if (price >= 1000) {
            // 큰 가격은 소수점 2자리
            DecimalFormat df = new DecimalFormat("0.00");
            return df.format(price);
        } else if (price >= 1) {
            // 중간 가격은 소수점 4자리
            DecimalFormat df = new DecimalFormat("0.0000");
            return df.format(price);
        } else {
            // 작은 가격은 소수점 8자리
            DecimalFormat df = new DecimalFormat("0.00000000");
            return df.format(price);
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onBackPressed() {
        if (tradingChart.canGoBack()) {
            tradingChart.goBack();
        } else {
            super.onBackPressed();
        }
    }
}

