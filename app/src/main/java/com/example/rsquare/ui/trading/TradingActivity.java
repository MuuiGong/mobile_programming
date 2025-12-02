package com.example.rsquare.ui.trading;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.rsquare.R;
import com.example.rsquare.domain.MarginCalculator;
import com.example.rsquare.domain.PositionSizeCalculator;
import com.example.rsquare.domain.RiskCalculator;
import com.example.rsquare.domain.TradeCalculator;
import com.example.rsquare.domain.TradeExecutor;
import com.example.rsquare.ui.BaseActivity;
import com.example.rsquare.ui.chart.ChartWebViewInterface;
import com.example.rsquare.ui.chart.ChartViewModel;
import com.example.rsquare.ui.trade.TradeViewModel;
import com.google.android.material.button.MaterialButton;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.example.rsquare.data.local.entity.Position;

/**
 * 거래 실행 Activity
 * 제안서의 activity_trading.xml 레이아웃 사용
 */
public class TradingActivity extends BaseActivity {
    
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
    private EditText riskAmountInput;
    private TextView riskPercentageHint;
    private TextView modeIndicator;
    private Button btnMarginIsolated;
    private Button btnMarginCross;
    private Spinner leverageSpinner;
    private Button btnLong;
    private Button btnShort;
    private TextView rrRatioText;
    private TextView riskScoreTrading;
    private TextView liquidationPriceText;
    
    // 데이터 캐싱
    private double currentBalance = 0.0;
    private List<Position> activePositions = new ArrayList<>();
    private double lastLiquidationPrice = -1.0; // 캐싱된 청산가
    private Button btnEnterTrade;
    
    // 타임프레임 버튼들
    private MaterialButton btnTime1m, btnTime5m, btnTime15m, btnTime30m, btnTime1h, btnTime4h, btnTime1d;
    
    // Data
    private String currentSymbol = "BTCUSDT";
    private String currentTimeframe = "1h";
    private String tradeMode = "FUTURES"; // "FUTURES" or "SPOT"
    private int leverage = 5;
    private boolean isLong = true;
    private String marginMode = "CROSS"; // 기본값: Cross 마진 모드
    private double currentPrice = 0.0;
    private double defaultRiskAmount = 0.0; // UserSettings 기본값
    private double riskPercentage = 2.0; // 기본 리스크 비율
    // private double currentBalance = 0.0; // 현재 잔고 (DB에서 로드됨) - Removed duplicate
    private boolean isChartReady = false;
    private boolean isChangingSymbol = false; // 종목 변경 중 플래그
    private boolean userHasSetEntryPrice = false; // 사용자가 직접 Entry Price 설정했는지
    private boolean userHasSetTP = false; // 사용자가 직접 TP 설정했는지
    private boolean userHasSetSL = false; // 사용자가 직접 SL 설정했는지
    private boolean isUpdatingFromChart = false; // 차트에서 업데이트 중인지 (순환 참조 방지)
    private boolean needsAutoPriceSetup = false; // 종목 변경 후 자동 가격 설정이 필요한지
    private java.util.List<java.util.List<Object>> pendingKlines = null;
    
    // Throttling variables
    private long lastPriceUpdateTimestamp = 0;
    private long lastKlineUpdateTimestamp = 0;
    private static final long UPDATE_THROTTLE_MS = 100; // 100ms throttling (10fps)
    
    private final DecimalFormat priceFormatter = new DecimalFormat("#,##0.00");
    private final NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trading);
        
        // Intent에서 모드와 심볼 가져오기
        Intent intent = getIntent();
        if (intent != null) {
            String mode = intent.getStringExtra("mode");
            if (mode != null) {
                tradeMode = mode; // "FUTURES" or "SPOT"
            }
            String symbol = intent.getStringExtra("symbol");
            if (symbol != null) {
                currentSymbol = symbol;
            }
        }
        
        // SPOT 모드일 때는 레버리지 1x로 고정
        if ("SPOT".equals(tradeMode)) {
            leverage = 1;
            isLong = true; // 현물은 항상 매수만 가능
        }
        
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
        
        // 인텐트 데이터 처리 (포트폴리오 등에서 넘어온 경우)
        handleIntentExtras();
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
        riskAmountInput = findViewById(R.id.risk_amount_input);
        riskPercentageHint = findViewById(R.id.risk_percentage_hint);
        modeIndicator = findViewById(R.id.mode_indicator);
        btnMarginIsolated = findViewById(R.id.btn_margin_isolated);
        btnMarginCross = findViewById(R.id.btn_margin_cross);
        leverageSpinner = findViewById(R.id.leverage_spinner);
        btnLong = findViewById(R.id.btn_long);
        btnShort = findViewById(R.id.btn_short);
        rrRatioText = findViewById(R.id.rr_ratio_text);
        riskScoreTrading = findViewById(R.id.risk_score_trading);
        liquidationPriceText = findViewById(R.id.liquidation_price_text);
        btnEnterTrade = findViewById(R.id.btn_enter_trade);
        
        // 타임프레임 버튼 초기화
        btnTime1m = findViewById(R.id.btn_time_1m);
        btnTime5m = findViewById(R.id.btn_time_5m);
        btnTime15m = findViewById(R.id.btn_time_15m);
        btnTime30m = findViewById(R.id.btn_time_30m);
        btnTime1h = findViewById(R.id.btn_time_1h);
        btnTime4h = findViewById(R.id.btn_time_4h);
        btnTime1d = findViewById(R.id.btn_time_1d);
        
        // 기본 타임프레임 선택 (1h)
        setTimeframeButtonActive(btnTime1h);
        
        // 심볼 및 타임프레임 설정
        symbolText.setText(currentSymbol + " | " + currentTimeframe.toUpperCase());
        
        // SPOT 모드일 때는 레버리지/마진/포지션 방향 UI 숨기기
        if ("SPOT".equals(tradeMode)) {
            timeframeText.setText("현물 거래");
            timeframeText.setVisibility(View.VISIBLE);
            
            // 레버리지 & 포지션 컨테이너 숨기기
            View leveragePositionContainer = findViewById(R.id.leverage_position_container);
            if (leveragePositionContainer != null) {
                leveragePositionContainer.setVisibility(View.GONE);
            }
            
            // 마진 모드 컨테이너 숨기기
            View marginModeContainer = findViewById(R.id.margin_mode_container);
            if (marginModeContainer != null) {
                marginModeContainer.setVisibility(View.GONE);
            }
            
            // 청산가 컨테이너 숨기기
            View liquidationContainer = findViewById(R.id.liquidation_container);
            if (liquidationContainer != null) {
                liquidationContainer.setVisibility(View.GONE);
            }
        } else {
            // FUTURES 모드
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
    }
    
    /**
     * 타임프레임 버튼 활성화 상태 설정
     */
    private void setTimeframeButtonActive(MaterialButton activeButton) {
        int inactiveBgColor = ContextCompat.getColor(this, android.R.color.transparent);
        int inactiveTextColor = ContextCompat.getColor(this, R.color.tds_gray_500);
        int activeBgColor = ContextCompat.getColor(this, R.color.tds_blue_500);
        int activeTextColor = ContextCompat.getColor(this, android.R.color.white);
        
        MaterialButton[] buttons = {btnTime1m, btnTime5m, btnTime15m, btnTime30m, btnTime1h, btnTime4h, btnTime1d};
        for (MaterialButton btn : buttons) {
            if (btn != null) {
                btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(inactiveBgColor));
                btn.setTextColor(inactiveTextColor);
            }
        }
        
        if (activeButton != null) {
            activeButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(activeBgColor));
            activeButton.setTextColor(activeTextColor);
        }
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
    private long lastPriceUpdateTime = 0;
    private static final long PRICE_UPDATE_INTERVAL_MS = 100; // 100ms debounce

    /**
     * WebView 설정
     */
    private void setupWebView() {
        // 하드웨어 가속 활성화
        tradingChart.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        
        WebSettings webSettings = tradingChart.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        
        // 렌더링 우선순위 높임
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);

        // 캐시 최적화 (성능 향상)
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        
        // WebChromeClient 설정 (콘솔 로그 확인용)
        tradingChart.setWebChromeClient(new android.webkit.WebChromeClient() {
            @Override
            public boolean onConsoleMessage(android.webkit.ConsoleMessage consoleMessage) {
                // 로그 양을 줄이기 위해 필요한 경우에만 활성화
                // android.util.Log.d("TradingChart", consoleMessage.message());
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
                        long currentTime = System.currentTimeMillis();
                        // Debounce: 너무 잦은 업데이트 방지 (100ms 간격)
                        if (currentTime - lastPriceUpdateTime < PRICE_UPDATE_INTERVAL_MS) {
                            return;
                        }
                        lastPriceUpdateTime = currentTime;
                        
                        // updateRiskMetrics()가 메인 스레드 체크를 수행하므로 직접 호출 가능
                        runOnUiThread(() -> {
                            currentPrice = price;
                            
                            // 종목 변경 중이면 자동 가격 설정하지 않음
                            if (isChangingSymbol) {
                                // 종목 변경 완료 후 플래그 해제 (차트가 준비되면)
                                android.util.Log.d("TradingActivity", "Price changed during symbol change, skipping auto-set");
                                updateRiskMetrics();
                                return;
                            }
                            
                            // 사용자가 이미 가격을 설정했다면 자동 업데이트하지 않음
                            if (userHasSetEntryPrice || userHasSetTP || userHasSetSL) {
                                // android.util.Log.d("TradingActivity", "User has set prices, skipping auto-update");
                                updateRiskMetrics();
                                return;
                            }
                            
                            // Entry Price Input이 비어있고 사용자가 설정하지 않았을 때만 자동 설정
                            String currentText = entryPriceInput.getText() != null ? 
                                entryPriceInput.getText().toString() : "";
                            if (currentText == null || currentText.trim().isEmpty() || "0.00".equals(currentText.trim())) {
                                String priceStr = formatPrice(price);
                                entryPriceInput.setText(priceStr);
                                // TP와 SL도 EP와 같은 값으로 초기화 (겹쳐진 상태)
                                tpPriceInput.setText(priceStr);
                                slPriceInput.setText(priceStr);
                                
                                    tradingChart.postDelayed(() -> {
                                        tradingChart.evaluateJavascript(
                                            "if (typeof setEntryPrice === 'function') { setEntryPrice(" + price + "); }", null);
                                        tradingChart.evaluateJavascript(
                                            "if (typeof setTakeProfit === 'function') { setTakeProfit(" + price + "); }", null);
                                        tradingChart.evaluateJavascript(
                                            "if (typeof setStopLoss === 'function') { setStopLoss(" + price + "); }", null);
                                    }, 300); // 300ms 딜레이
                                // 자동 설정 시에만 리스크 메트릭 업데이트
                                updateRiskMetrics();
                            }
                            // 일반적인 가격 변동 시에는 리스크 메트릭 업데이트 호출하지 않음 (불필요한 연산 방지)
                        });
                    }
                }
                
                @Override
                public void onEntryPriceChanged(double price) {
                    if (price > 0) {
                        runOnUiThread(() -> {
                            // 사용자가 차트에서 드래그하여 설정한 것으로 표시
                            userHasSetEntryPrice = true;
                            // 차트에서 업데이트 중임을 표시 (순환 참조 방지)
                            isUpdatingFromChart = true;
                            entryPriceInput.setText(formatPrice(price));
                            isUpdatingFromChart = false;
                            updateRiskMetrics();
                        });
                    }
                }
                
                @Override
                public void onTakeProfitChanged(double price) {
                    if (price > 0) {
                        runOnUiThread(() -> {
                            // 사용자가 차트에서 드래그하여 설정한 것으로 표시
                            userHasSetTP = true;
                            // 차트에서 업데이트 중임을 표시 (순환 참조 방지)
                            isUpdatingFromChart = true;
                            tpPriceInput.setText(formatPrice(price));
                            isUpdatingFromChart = false;
                            updateRiskMetrics();
                        });
                    }
                }
                
                @Override
                public void onStopLossChanged(double price) {
                    if (price > 0) {
                        runOnUiThread(() -> {
                            // 사용자가 차트에서 드래그하여 설정한 것으로 표시
                            userHasSetSL = true;
                            // 차트에서 업데이트 중임을 표시 (순환 참조 방지)
                            isUpdatingFromChart = true;
                            slPriceInput.setText(formatPrice(price));
                            isUpdatingFromChart = false;
                            updateRiskMetrics();
                        });
                    }
                }
                
            @Override
            public void onChartReady() {
                android.util.Log.d("TradingActivity", "Chart ready callback received");
                runOnUiThread(() -> {
                    isChartReady = true;
                    
                    // 차트가 새로 로드되었으므로 캐시된 값 초기화하여 강제 업데이트 유도
                    lastLiquidationPrice = -1.0;

                    // 대기 중인 OHLC 데이터가 있으면 전송 (포지션 타입 설정은 데이터 로드 후 자동으로)
                    if (pendingKlines != null && !pendingKlines.isEmpty()) {
                        android.util.Log.d("TradingActivity", "Sending pending OHLC data");
                        loadBinanceOHLCData(pendingKlines);
                        pendingKlines = null;
                    }
                    // OHLC 데이터가 없어도 chart.html에서 기본 포지션 타입을 설정함
                    
                    // 종목 변경 중이었다면 플래그 해제 및 가격 자동 설정
                    if (isChangingSymbol) {
                        android.util.Log.d("TradingActivity", "Chart ready after symbol change, resetting flag");
                        // 약간의 딜레이 후 플래그 해제 (차트가 완전히 렌더링될 때까지 대기)
                        tradingChart.postDelayed(() -> {
                            isChangingSymbol = false;
                            needsAutoPriceSetup = true; // 자동 가격 설정 필요 플래그 설정
                            android.util.Log.d("TradingActivity", "Symbol change flag reset, needsAutoPriceSetup = true");
                            
                            // 현재 가격이 있으면 즉시 설정 시도
                            tryAutoSetupPrices();
                        }, 500);
                    }
                    
                    // 차트 심볼 업데이트 (확실하게 설정)
                    if (currentSymbol != null) {
                        tradingChart.evaluateJavascript("if (typeof setSymbol === 'function') { setSymbol('" + currentSymbol + "'); }", null);
                    }
                    
                    // 모든 라인(EP, TP, SL, LP) 다시 그리기
                    updateRiskMetrics();
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
                // 차트에서 업데이트 중이면 차트로 다시 업데이트하지 않음 (순환 참조 방지)
                if (isUpdatingFromChart) {
                    updateRiskMetrics();
                    return;
                }
                
                // 사용자가 직접 입력한 것으로 표시
                String text = s.toString().trim();
                if (!text.isEmpty()) {
                    userHasSetEntryPrice = true;
                }
                updateRiskMetrics();
                // 차트 업데이트
                if (!text.isEmpty() && tradingChart != null) {
                    try {
                        double price = parsePrice(text);
                        tradingChart.post(() -> {
                            tradingChart.evaluateJavascript(
                                "if (typeof setEntryPrice === 'function') { setEntryPrice(" + price + "); }", null);
                        });
                    } catch (Exception e) {}
                }
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
                // 차트에서 업데이트 중이면 차트로 다시 업데이트하지 않음 (순환 참조 방지)
                if (isUpdatingFromChart) {
                    updateRiskMetrics();
                    return;
                }
                
                // 사용자가 직접 입력한 것으로 표시
                String text = s.toString().trim();
                if (!text.isEmpty()) {
                    userHasSetTP = true;
                }
                updateRiskMetrics();
                // 차트 업데이트
                if (!text.isEmpty() && tradingChart != null) {
                    try {
                        double price = parsePrice(text);
                        tradingChart.post(() -> {
                            tradingChart.evaluateJavascript(
                                "if (typeof setTakeProfit === 'function') { setTakeProfit(" + price + "); }", null);
                        });
                    } catch (Exception e) {}
                }
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
                // 차트에서 업데이트 중이면 차트로 다시 업데이트하지 않음 (순환 참조 방지)
                if (isUpdatingFromChart) {
                    updateRiskMetrics();
                    return;
                }
                
                // 사용자가 직접 입력한 것으로 표시
                String text = s.toString().trim();
                if (!text.isEmpty()) {
                    userHasSetSL = true;
                }
                updateRiskMetrics();
                // 차트 업데이트
                if (!text.isEmpty() && tradingChart != null) {
                    try {
                        double price = parsePrice(text);
                        tradingChart.post(() -> {
                            tradingChart.evaluateJavascript(
                                "if (typeof setStopLoss === 'function') { setStopLoss(" + price + "); }", null);
                        });
                    } catch (Exception e) {}
                }
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Risk Amount 변경
        riskAmountInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateModeIndicator(); // 모드 변경 감지
                updateRiskMetrics();
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // FUTURES 모드일 때만 마진/레버리지/포지션 방향 리스너 설정
        if ("FUTURES".equals(tradeMode)) {
            // 마진 모드 버튼 리스너
            btnMarginIsolated.setOnClickListener(v -> setMarginMode("ISOLATED", btnMarginIsolated));
            btnMarginCross.setOnClickListener(v -> setMarginMode("CROSS", btnMarginCross));
            
            // 기본 마진 모드 설정 (Cross)
            setMarginMode("CROSS", btnMarginCross);
            
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
                    
                    // 차트에 포지션 타입 먼저 변경 (드래그 제약 조건 업데이트)
                    setPositionType("long");
                    
                    // TP와 SL 값 교환 (Short->Long 전환 시)
                    swapTPandSL();
                    
                    updatePositionButtons();
                    updateRiskMetrics();
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
                    
                    // 차트에 포지션 타입 먼저 변경 (드래그 제약 조건 업데이트)
                    setPositionType("short");
                    
                    // TP와 SL 값 교환 (Long->Short 전환 시)
                    swapTPandSL();
                    
                    updatePositionButtons();
                    updateRiskMetrics();
                } else {
                    android.util.Log.d("TradingActivity", "Already in SHORT position");
                }
            });
        } else {
            // SPOT 모드: 차트에 항상 롱 포지션으로 설정
            setPositionType("long");
        }
        
        // 거래 진입 버튼
        
        // 종목 선택 클릭 리스너
        symbolText.setOnClickListener(v -> showSymbolSelectionDialog());
        
        btnEnterTrade.setOnClickListener(v -> executeTrade());
        
        // 타임프레임 버튼 리스너
        if (btnTime1m != null) {
            btnTime1m.setOnClickListener(v -> setTimeframe("1m", btnTime1m));
        }
        if (btnTime5m != null) {
            btnTime5m.setOnClickListener(v -> setTimeframe("5m", btnTime5m));
        }
        if (btnTime15m != null) {
            btnTime15m.setOnClickListener(v -> setTimeframe("15m", btnTime15m));
        }
        if (btnTime30m != null) {
            btnTime30m.setOnClickListener(v -> setTimeframe("30m", btnTime30m));
        }
        if (btnTime1h != null) {
            btnTime1h.setOnClickListener(v -> setTimeframe("1h", btnTime1h));
        }
        if (btnTime4h != null) {
            btnTime4h.setOnClickListener(v -> setTimeframe("4h", btnTime4h));
        }
        if (btnTime1d != null) {
            btnTime1d.setOnClickListener(v -> setTimeframe("1d", btnTime1d));
        }
    }
    
    /**
     * 타임프레임 변경
     */
    private void setTimeframe(String timeframe, MaterialButton button) {
        currentTimeframe = timeframe;
        setTimeframeButtonActive(button);
        
        // 심볼 텍스트 업데이트
        symbolText.setText(currentSymbol + " | " + timeframe.toUpperCase());
        
        // ViewModel에 타임프레임 변경 알림
        chartViewModel.setTimeframe(timeframe);
        
        // JavaScript에 타임프레임 변경 알림
        String jsCode = "if (typeof setTimeframe === 'function') { setTimeframe('" + timeframe + "'); }";
        tradingChart.post(() -> {
            if (tradingChart != null) {
                tradingChart.evaluateJavascript(jsCode, null);
            }
        });
    }
    
    /**
     * 포지션 버튼 상태 업데이트
     */
    private void updatePositionButtons() {
        if (isLong) {
            btnLong.setBackgroundResource(R.drawable.btn_success);
            btnLong.setTextColor(getColor(R.color.white));
            btnShort.setBackgroundResource(R.drawable.btn_outline);
            btnShort.setTextColor(getColor(R.color.tds_text_secondary));
        } else {
            btnLong.setBackgroundResource(R.drawable.btn_outline);
            btnLong.setTextColor(getColor(R.color.tds_text_secondary));
            btnShort.setBackgroundResource(R.drawable.btn_error);
            btnShort.setTextColor(getColor(R.color.white));
        }
    }
    
    /**
     * 마진 모드 설정
     */
    private void setMarginMode(String mode, Button activeButton) {
        marginMode = mode;
        
        // 버튼 상태 업데이트
        if (mode.equals("ISOLATED")) {
            btnMarginIsolated.setBackgroundResource(R.drawable.btn_primary);
            btnMarginIsolated.setTextColor(getColor(R.color.white));
            btnMarginCross.setBackgroundResource(R.drawable.btn_outline);
            btnMarginCross.setTextColor(getColor(R.color.tds_text_secondary));
        } else {
            btnMarginIsolated.setBackgroundResource(R.drawable.btn_outline);
            btnMarginIsolated.setTextColor(getColor(R.color.tds_text_secondary));
            btnMarginCross.setBackgroundResource(R.drawable.btn_primary);
            btnMarginCross.setTextColor(getColor(R.color.white));
        }
        
        // 청산 예산가 재계산
        updateRiskMetrics();
    }

    /**
     * 포지션 타입을 차트에 설정하고 EP, TP, SL 위치 조정
     */
    private void setPositionType(String positionType) {
        android.util.Log.d("TradingActivity", "=== SET POSITION TYPE: " + positionType + " ===");

        String jsCode = String.format(
            "(function() { " +
            "  try { " +
            "    console.log('=== JAVA CALL: setPositionType(\"%s\") ==='); " +
            "    if (typeof setPositionType === 'function') { " +
            "      setPositionType('%s'); " +
            "      console.log('Position type updated successfully'); " +
            "    } else { " +
            "      console.error('setPositionType function not found'); " +
            "    } " +
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
                
                // JavaScript에 현재가 업데이트 전달 (Throttling 적용)
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastPriceUpdateTimestamp >= UPDATE_THROTTLE_MS) {
                    lastPriceUpdateTimestamp = currentTime;
                    String jsCode = "if (typeof setCurrentPrice === 'function') { " +
                        "console.log('Setting current price from Android:', " + price + "); " +
                        "setCurrentPrice(" + price + "); }";
                    tradingChart.post(() -> {
                        if (tradingChart != null) {
                            tradingChart.evaluateJavascript(jsCode, null);
                        }
                    });
                }
                
                // 종목 변경 중이면 자동 가격 설정하지 않음 (나중에 needsAutoPriceSetup으로 처리)
                if (isChangingSymbol) {
                    android.util.Log.d("TradingActivity", "Price updated during symbol change, will setup later");
                    updateRiskMetrics();
                    return;
                }
                
                // 종목 변경 후 자동 가격 설정이 필요한 경우
                if (needsAutoPriceSetup) {
                    android.util.Log.d("TradingActivity", "Auto price setup needed, attempting setup");
                    tryAutoSetupPrices();
                    return;
                }
                
                // 사용자가 이미 가격을 설정했다면 자동 업데이트하지 않음
                if (userHasSetEntryPrice || userHasSetTP || userHasSetSL) {
                    android.util.Log.d("TradingActivity", "User has set prices, skipping auto-update");
                    updateRiskMetrics();
                    return;
                }
                
                // 진입가가 비어있으면 현재 가격으로 설정하고, TP와 SL도 같은 값으로 설정
                String currentEntryText = entryPriceInput.getText() != null ? 
                    entryPriceInput.getText().toString().trim() : "";
                String currentTPText = tpPriceInput.getText() != null ? 
                    tpPriceInput.getText().toString().trim() : "";
                String currentSLText = slPriceInput.getText() != null ? 
                    slPriceInput.getText().toString().trim() : "";
                
                if (currentEntryText.isEmpty() || "0.00".equals(currentEntryText)) {
                    String priceStr = formatPrice(price);
                    entryPriceInput.setText(priceStr);
                    // 차트에도 EP 설정
                    if (tradingChart != null) {
                        String jsCodeEP = "if (typeof setEntryPrice === 'function') { setEntryPrice(" + price + "); }";
                        tradingChart.evaluateJavascript(jsCodeEP, null);
                    }
                }
                
                // TP/SL이 비어있으면 기본값 설정 (Entry Price 기준 ±1%)
                // 이렇게 하면 Entry = TP = SL 상태를 방지하여 유효성 검사 경고를 피할 수 있음

                
                if (currentTPText.isEmpty() || "0.00".equals(currentTPText)) {
                    double tpPrice = isLong ? price * 1.01 : price * 0.99; // 1% 이익
                    String priceStr = formatPrice(tpPrice);
                    tpPriceInput.setText(priceStr);
                    // 차트에도 TP 설정
                    if (tradingChart != null) {
                        String jsCodeTP = "if (typeof setTakeProfit === 'function') { setTakeProfit(" + tpPrice + "); }";
                        tradingChart.evaluateJavascript(jsCodeTP, null);
                    }
                }
                
                if (currentSLText.isEmpty() || "0.00".equals(currentSLText)) {
                    double slPrice = isLong ? price * 0.99 : price * 1.01; // 1% 손실
                    String priceStr = formatPrice(slPrice);
                    slPriceInput.setText(priceStr);
                    // 차트에도 SL 설정
                    if (tradingChart != null) {
                        String jsCodeSL = "if (typeof setStopLoss === 'function') { setStopLoss(" + slPrice + "); }";
                        tradingChart.evaluateJavascript(jsCodeSL, null);
                    }
                }
                
                updateRiskMetrics();
            }
        });
        
        // 24시간 변동률 관찰 (웹소켓 실시간 업데이트)
        chartViewModel.getPriceChangePercent().observe(this, percent -> {
            if (percent != null) {
                // JavaScript에 변동률 업데이트 전달 (Throttling 적용 - 1초에 한번 정도면 충분하지만 안전하게 100ms)
                long currentTime = System.currentTimeMillis();
                // 변동률은 덜 중요하므로 500ms로 제한
                if (currentTime - lastPriceUpdateTimestamp >= 500) { 
                    String jsCode = "if (typeof setPriceChangePercent === 'function') { " +
                        "setPriceChangePercent(" + percent + "); }";
                    tradingChart.post(() -> {
                        if (tradingChart != null) {
                            tradingChart.evaluateJavascript(jsCode, null);
                        }
                    });
                }
            }
        });
        
        // 24시간 시가 관찰 (등락폭 계산 기준)
        chartViewModel.getOpenPrice24h().observe(this, openPrice -> {
            if (openPrice != null && openPrice > 0 && isChartReady) {
                tradingChart.post(() -> {
                    if (tradingChart != null) {
                        tradingChart.evaluateJavascript("if (typeof setBasePrice === 'function') { setBasePrice(" + openPrice + "); }", null);
                    }
                });
            }
        });
        
        // 에러 메시지 관찰
        chartViewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                // 데이터가 이미 로드된 상태라면 에러 메시지 무시 (일시적 네트워크 오류 등)
                if (chartViewModel.getBinanceKlines().getValue() != null && !chartViewModel.getBinanceKlines().getValue().isEmpty()) {
                    android.util.Log.w("TradingActivity", "Suppressing error toast because data is loaded: " + error);
                } else {
                    android.widget.Toast.makeText(this, "차트 데이터 로드 실패: " + error, android.widget.Toast.LENGTH_LONG).show();
                }
            }
        });

        // 차트 준비 상태 관찰
        chartViewModel.getBinanceKlines().observe(this, klines -> {
            if (klines != null && !klines.isEmpty()) {
                loadBinanceOHLCData(klines);
                
                // 24시간 시가가 있으면 그것을 사용하고, 없으면 첫 번째 캔들 사용
                Double open24h = chartViewModel.getOpenPrice24h().getValue();
                if (open24h != null && open24h > 0 && isChartReady) {
                    final double finalPrice = open24h;
                    tradingChart.post(() -> {
                        tradingChart.evaluateJavascript("if (typeof setBasePrice === 'function') { setBasePrice(" + finalPrice + "); }", null);
                    });
                } else if (klines.size() > 0) {
                    // 데이터가 있는데 24h 시가가 아직 없으면 첫 캔들 기준 (임시)
                    java.util.List<Object> firstKline = klines.get(0); 
                    if (firstKline.size() >= 2) {
                        Object openObj = firstKline.get(1);
                        Double openPrice = 0.0;
                        if (openObj instanceof Number) {
                            openPrice = ((Number) openObj).doubleValue();
                        } else {
                            try {
                                openPrice = Double.parseDouble(openObj.toString());
                            } catch (Exception e) {}
                        }
                        
                        if (openPrice > 0 && isChartReady) {
                            final double finalPrice = openPrice;
                            tradingChart.post(() -> {
                                tradingChart.evaluateJavascript("if (typeof setBasePrice === 'function') { setBasePrice(" + finalPrice + "); }", null);
                            });
                        }
                    }
                }
            }
        });
        
        // 차트 데이터 관찰
        chartViewModel.getChartData().observe(this, chartData -> {
            if (chartData != null && isChartReady) {
                // 차트 데이터 로드
                // ... (기존 코드)
            }
        });
        
        // 실시간 Kline 업데이트 관찰 (WebSocket에서 받은 새로운 캔들)
        chartViewModel.getKlineUpdate().observe(this, klineData -> {
            if (klineData != null && isChartReady) {
                android.util.Log.d("TradingActivity", "Real-time kline update: " + klineData.coinId + " " + klineData.close);
                
                // JavaScript에 실시간 캔들 업데이트 전달 (Throttling 적용)
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastKlineUpdateTimestamp >= UPDATE_THROTTLE_MS) {
                    lastKlineUpdateTimestamp = currentTime;
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
            }
        });
        
        // 잔고 관찰
        com.example.rsquare.data.repository.UserRepository userRepository = new com.example.rsquare.data.repository.UserRepository(getApplication());
        userRepository.getUserById(com.example.rsquare.data.repository.UserRepository.TEST_USER_ID).observe(this, user -> {
            if (user != null) {
                currentBalance = user.getBalance();
                updateRiskMetrics();
            }
        });

        // 활성 포지션 관찰 (청산가 계산용)
        com.example.rsquare.data.repository.TradingRepository tradingRepository = 
            new com.example.rsquare.data.repository.TradingRepository(getApplication());
        tradingRepository.getActivePositions(com.example.rsquare.data.repository.UserRepository.TEST_USER_ID).observe(this, positions -> {
            if (positions != null) {
                activePositions = positions;
                // 포지션 변경 시 리스크 메트릭 업데이트
                updateRiskMetrics();
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
        
        // 코인 선택 (현재 심볼 사용)
        String coinId = getCoinIdFromSymbol(currentSymbol);
        if (coinId == null) coinId = "bitcoin"; // 기본값
        
        chartViewModel.selectCoin(coinId);
        // 현재 타임프레임으로 차트 데이터 로드
        chartViewModel.loadChartData(coinId, 7, currentTimeframe);
        
        // 차트 심볼 업데이트
        if (tradingChart != null) {
            final String finalSymbol = currentSymbol;
            tradingChart.post(() -> {
                String jsCode = "if (typeof setSymbol === 'function') { setSymbol('" + finalSymbol + "'); }";
                tradingChart.evaluateJavascript(jsCode, null);
            });
        }
        
        // UserSettings에서 기본 위험 자금 및 마진 모드 로드
        new Thread(() -> {
            try {
                com.example.rsquare.data.repository.UserSettingsRepository settingsRepository = 
                    new com.example.rsquare.data.repository.UserSettingsRepository(TradingActivity.this);
                com.example.rsquare.data.repository.UserRepository userRepository = 
                    new com.example.rsquare.data.repository.UserRepository(TradingActivity.this);
                
                com.example.rsquare.data.local.entity.UserSettings settings = 
                    settingsRepository.getSettingsSync(1);
                com.example.rsquare.data.local.entity.User user = userRepository.getUserSync(1);
                
                if (settings != null && user != null) {
                    defaultRiskAmount = com.example.rsquare.domain.TradeCalculator.calculateRiskAmount(
                        settings, user.getBalance()
                    );
                    
                    // 사용자 잔고 및 리스크 비율 로드
                    currentBalance = user.getBalance();
                    riskPercentage = settings.getRiskPercentage();
                    
                    // 기본 마진 모드 설정
                    if (settings.getDefaultMarginMode() != null && !settings.getDefaultMarginMode().isEmpty()) {
                        marginMode = settings.getDefaultMarginMode();
                    }
                    
                    runOnUiThread(() -> {
                        // 초기 리스크 힌트 업데이트
                        updateRiskDisplay(
                            PositionSizeCalculator.calculateRiskAmount(currentBalance, riskPercentage),
                            true
                        );
                        
                        // 초기 모드 인디케이터 설정
                        updateModeIndicator();
                        
                        // 마진 모드 버튼 상태 업데이트
                        if ("ISOLATED".equals(marginMode)) {
                            setMarginMode("ISOLATED", btnMarginIsolated);
                        } else {
                            setMarginMode("CROSS", btnMarginCross);
                        }
                        
                        updateRiskMetrics();
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("TradingActivity", "Error loading initial data", e);
            }
        }).start();
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
     * 리스크 메트릭 업데이트 (R:R 비율, Risk Score, 청산 예산가)
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
            String riskAmountText = riskAmountInput.getText() != null ? riskAmountInput.getText().toString().trim() : "";
            
            // 자동 모드일 때 자동 계산 수행
            if (!isManualMode()) {
                calculateAutoPositionSize();
            }
            
            // 빈 문자열이면 기본값 표시
            if (entryText.isEmpty() || tpText.isEmpty() || slText.isEmpty()) {
                rrRatioText.setText("0.00:1");
                riskScoreTrading.setText("0/100 🔴");
                riskScoreTrading.setTextColor(getColor(R.color.risk_danger));
                if (liquidationPriceText != null) {
                    liquidationPriceText.setText("--");
                    liquidationPriceText.setTextColor(getColor(R.color.tds_text_secondary));
                }
                return;
            }
            
            double entryPrice = parsePrice(entryText);
            double tpPrice = parsePrice(tpText);
            double slPrice = parsePrice(slText);
            
            // 거래 금액 파싱
        double riskAmount = 0.0;
        if (riskAmountText.isEmpty()) {
            // Auto mode: Calculate based on current balance and risk percentage
            if (currentBalance > 0 && riskPercentage > 0) {
                riskAmount = (currentBalance * riskPercentage) / 100.0;
            } else {
                riskAmount = defaultRiskAmount; // Fallback
            }
        } else {
            try {
                riskAmount = Double.parseDouble(riskAmountText);
                if (riskAmount <= 0) {
                    if (currentBalance > 0 && riskPercentage > 0) {
                        riskAmount = (currentBalance * riskPercentage) / 100.0;
                    } else {
                        riskAmount = defaultRiskAmount;
                    }
                }
            } catch (NumberFormatException e) {
                if (currentBalance > 0 && riskPercentage > 0) {
                    riskAmount = (currentBalance * riskPercentage) / 100.0;
                } else {
                    riskAmount = defaultRiskAmount;
                }
            }
        }
            
            // 파싱 결과 로깅 (디버깅용)
            android.util.Log.d("TradingActivity", String.format(Locale.US, 
                "가격 파싱: Entry=%s->%.2f, TP=%s->%.2f, SL=%s->%.2f, RiskAmount=%.2f", 
                entryText, entryPrice, tpText, tpPrice, slText, slPrice, riskAmount));
            
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
                    
                    // 청산 예산가 계산 (선물 거래이고 레버리지가 1x보다 클 때만)
                    updateLiquidationPrice(entryPrice, riskAmount, slPrice);
                } else {
                    // 가격 순서가 잘못됨
                    android.util.Log.w("TradingActivity", String.format(Locale.US,
                        "가격 순서 오류: Entry=%.2f, TP=%.2f, SL=%.2f, isLong=%b",
                        entryPrice, tpPrice, slPrice, isLong));
                    rrRatioText.setText("0.00:1");
                    riskScoreTrading.setText("0/100 🔴");
                    riskScoreTrading.setTextColor(getColor(R.color.risk_danger));
                    if (liquidationPriceText != null) {
                        liquidationPriceText.setText("--");
                        liquidationPriceText.setTextColor(getColor(R.color.tds_text_secondary));
                    }
                }
            } else {
                // 가격이 입력되지 않음
                android.util.Log.w("TradingActivity", String.format(Locale.US,
                    "가격 파싱 실패: Entry=%.2f, TP=%.2f, SL=%.2f",
                    entryPrice, tpPrice, slPrice));
                rrRatioText.setText("0.00:1");
                riskScoreTrading.setText("0/100 🔴");
                riskScoreTrading.setTextColor(getColor(R.color.risk_danger));
                if (liquidationPriceText != null) {
                    liquidationPriceText.setText("--");
                    liquidationPriceText.setTextColor(getColor(R.color.tds_text_secondary));
                }
            }
        } catch (Exception e) {
            android.util.Log.e("TradingActivity", "리스크 메트릭 업데이트 오류", e);
            rrRatioText.setText("0.00:1");
            riskScoreTrading.setText("0/100 🔴");
            riskScoreTrading.setTextColor(getColor(R.color.risk_danger));
            if (liquidationPriceText != null) {
                liquidationPriceText.setText("--");
                liquidationPriceText.setTextColor(getColor(R.color.tds_text_secondary));
            }
        }
    }
    
    /**
     * 청산 예산가 업데이트
     */
    /**
     * 청산 예산가 업데이트 (동기 방식)
     * 기존의 비동기 스레드 생성을 제거하고 캐시된 데이터 사용
     */
    private void updateLiquidationPrice(double entryPrice, double riskAmount, double slPrice) {
        // SPOT 모드일 때는 청산가 계산하지 않음
        if ("SPOT".equals(tradeMode)) {
            if (liquidationPriceText != null) {
                liquidationPriceText.setVisibility(View.GONE);
            }
            return;
        }
        if (liquidationPriceText == null) {
            return;
        }
        
        // 선물 거래이고 레버리지가 1x보다 클 때만 청산가 계산
        if (!"FUTURES".equals(tradeMode) || leverage <= 1 || riskAmount <= 0) {
            liquidationPriceText.setText("없음 (1x)");
            liquidationPriceText.setTextColor(getColor(R.color.tds_text_secondary));
            
            // 차트에서 청산 예산가 제거
            if (tradingChart != null) {
                String jsCode = "if (typeof setLiquidationPrice === 'function') { setLiquidationPrice(null); }";
                tradingChart.post(() -> {
                    if (tradingChart != null) {
                        tradingChart.evaluateJavascript(jsCode, null);
                    }
                });
            }
            return;
        }
        
        // 캐시된 데이터로 즉시 계산
        calculateAndDisplayLiquidationPrice(entryPrice, riskAmount, slPrice, 
            activePositions, currentBalance);
    }
    
    /**
     * 청산 예산가 계산 및 표시
     */
    private void calculateAndDisplayLiquidationPrice(double entryPrice, double riskAmount, 
                                                     double slPrice,
                                                     java.util.List<com.example.rsquare.data.local.entity.Position> existingPositions,
                                                     double totalBalance) {
        try {
            double estimatedLiquidationPrice = MarginCalculator.calculateEstimatedLiquidationPrice(
                entryPrice, riskAmount, leverage, marginMode, isLong, existingPositions, totalBalance
            );
            
            // 청산가가 0 이하인 경우 (현실적으로 청산되지 않음)
            if (estimatedLiquidationPrice <= 0) {
                if (lastLiquidationPrice != 0) { // 변경된 경우에만 업데이트
                    lastLiquidationPrice = 0;
                    liquidationPriceText.setText("없음");
                    liquidationPriceText.setTextColor(getColor(R.color.tds_text_secondary));
                    
                    // 차트에서 청산 예산가 제거
                    if (tradingChart != null) {
                        String jsCode = "if (typeof setLiquidationPrice === 'function') { setLiquidationPrice(null); }";
                        tradingChart.post(() -> {
                            if (tradingChart != null) {
                                tradingChart.evaluateJavascript(jsCode, null);
                            }
                        });
                    }
                }
                return;
            }
            
            // 이전 값과 차이가 미미하면 업데이트 건너뜀 (0.01 미만 차이)
            if (Math.abs(estimatedLiquidationPrice - lastLiquidationPrice) < 0.01) {
                return;
            }
            lastLiquidationPrice = estimatedLiquidationPrice;
            
            String liquidationText = String.format(Locale.US, 
                "$%.2f", estimatedLiquidationPrice);
            liquidationPriceText.setText(liquidationText);
            
            // 청산가가 SL보다 가까우면 위험 표시
            boolean isDangerous = false;
            if (isLong) {
                // 롱 포지션: 청산가가 SL보다 높으면 위험 (청산가가 더 가까움)
                isDangerous = estimatedLiquidationPrice > slPrice;
            } else {
                // 숏 포지션: 청산가가 SL보다 낮으면 위험 (청산가가 더 가까움)
                isDangerous = estimatedLiquidationPrice < slPrice;
            }
            
            if (isDangerous) {
                liquidationPriceText.setTextColor(getColor(R.color.risk_danger));
                liquidationPriceText.append(" ⚠️ SL보다 가까움");
            } else {
                liquidationPriceText.setTextColor(getColor(R.color.tds_warning));
            }
            
            // 차트에 청산 예산가 전달
            if (tradingChart != null) {
                String jsCode = String.format(Locale.US,
                    "if (typeof setLiquidationPrice === 'function') { setLiquidationPrice(%.2f); }",
                    estimatedLiquidationPrice);
                tradingChart.post(() -> {
                    if (tradingChart != null) {
                        tradingChart.evaluateJavascript(jsCode, null);
                    }
                });
            }
        } catch (Exception e) {
            android.util.Log.e("TradingActivity", "청산 예산가 계산 오류", e);
            liquidationPriceText.setText("계산 오류");
            liquidationPriceText.setTextColor(getColor(R.color.risk_danger));
            lastLiquidationPrice = -1.0; // 오류 시 초기화
            
            // 오류 시 차트에서 청산 예산가 제거
            if (tradingChart != null) {
                String jsCode = "if (typeof setLiquidationPrice === 'function') { setLiquidationPrice(null); }";
                tradingChart.post(() -> {
                    if (tradingChart != null) {
                        tradingChart.evaluateJavascript(jsCode, null);
                    }
                });
            }
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
            // SPOT 모드는 항상 롱 (매수만 가능)
            boolean actualIsLong = "SPOT".equals(tradeMode) ? true : isLong;
            boolean isValid = actualIsLong ? 
                (tpPrice > entryPrice && entryPrice > slPrice) : 
                (slPrice > entryPrice && entryPrice > tpPrice);
            
            if (!isValid) {
                Toast.makeText(this, "가격 순서가 올바르지 않습니다", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 거래 금액 파싱
            String riskAmountText = riskAmountInput.getText() != null ? 
                riskAmountInput.getText().toString().trim() : "";
            Double riskAmount = null;
            
            if (!riskAmountText.isEmpty()) {
                try {
                    double parsedAmount = Double.parseDouble(riskAmountText);
                    if (parsedAmount > 0) {
                        riskAmount = parsedAmount;
                    }
                } catch (NumberFormatException e) {
                    android.util.Log.w("TradingActivity", "거래 금액 파싱 실패: " + riskAmountText);
                }
            }
            
            // 람다 표현식에서 사용하기 위해 final 변수로 복사
            final Double finalRiskAmount = riskAmount;
            final String finalMarginMode = "SPOT".equals(tradeMode) ? null : marginMode; // SPOT은 마진 모드 없음
            final String finalSymbol = currentSymbol;
            final boolean finalIsLong = "SPOT".equals(tradeMode) ? true : isLong; // SPOT은 항상 롱
            final int finalLeverage = "SPOT".equals(tradeMode) ? 1 : leverage; // SPOT은 레버리지 1x
            
            // 사용자 ID 가져오기 (임시로 1 사용)
            final long userId = 1;
            
            // 거래 실행 (백그라운드 스레드에서 실행)
            new Thread(() -> {
                tradeExecutor.executeTrade(
                    userId,
                    finalSymbol,
                    entryPrice,
                    tpPrice,
                    slPrice,
                    finalIsLong,
                    finalLeverage,
                    finalRiskAmount,
                    finalMarginMode,
                    currentPrice,
                    new TradeExecutor.OnTradeExecutedListener() {
                        @Override
                        public void onSuccess(long positionId, TradeCalculator.TradeCalculationResult result) {
                            runOnUiThread(() -> {
                                Toast.makeText(TradingActivity.this, 
                                    "거래가 성공적으로 실행되었습니다", Toast.LENGTH_SHORT).show();
                                // 홈 화면으로 이동
                                Intent intent = new Intent(TradingActivity.this, com.example.rsquare.ui.MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                startActivity(intent);
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
    
    
    /**
     * 수동 모드인지 확인 (입력 필드가 비어있지 않음)
     */
    private boolean isManualMode() {
        String riskText = riskAmountInput.getText() != null ? 
            riskAmountInput.getText().toString().trim() : "";
        return !riskText.isEmpty();
    }
    
    /**
     * 자동 포지션 크기 계산
     */
    private void calculateAutoPositionSize() {
        String entryText = entryPriceInput.getText() != null ? entryPriceInput.getText().toString().trim() : "";
        String slText = slPriceInput.getText() != null ? slPriceInput.getText().toString().trim() : "";
        
        if (entryText.isEmpty() || slText.isEmpty()) {
            // 진입가나 SL이 없으면 기본 리스크 금액만 계산
            double calculatedRisk = PositionSizeCalculator.calculateRiskAmount(currentBalance, riskPercentage);
            updateRiskDisplay(calculatedRisk, true);
            return;
        }
        
        try {
            double entryPrice = parsePrice(entryText);
            double slPrice = parsePrice(slText);
            
            if (entryPrice <= 0 || slPrice <= 0) {
                updateRiskDisplay(0, false);
                return;
            }
            
            // 자동 계산 수행
            PositionSizeCalculator.CalculationResult result = 
                PositionSizeCalculator.calculateOptimalPositionSize(
                    currentBalance, 
                    riskPercentage, 
                    entryPrice, 
                    slPrice, 
                    leverage, 
                    isLong
                );
            
            if (result.isValid) {
                updateRiskDisplay(result.calculatedRiskAmount, true);
            } else {
                // 오류 발생 시 기본 리스크만 표시
                double calculatedRisk = PositionSizeCalculator.calculateRiskAmount(currentBalance, riskPercentage);
                updateRiskDisplay(calculatedRisk, true);
                android.util.Log.w("TradingActivity", "Auto calculation failed: " + result.errorMessage);
            }
        } catch (Exception e) {
            android.util.Log.e("TradingActivity", "Error in auto calculation", e);
            updateRiskDisplay(0, false);
        }
    }
    
    /**
     * 리스크 표시 업데이트
     *  
     * @param calculatedAmount 계산된 금액
     * @param isValid 유효 여부
     */
    private void updateRiskDisplay(double calculatedAmount, boolean isValid) {
        if (isValid && calculatedAmount > 0) {
            riskPercentageHint.setText(String.format(Locale.US, 
                "자동: %.1f%% = $%.2f", riskPercentage, calculatedAmount));
            // 플레이스홀더 업데이트
            riskAmountInput.setHint(String.format(Locale.US, "$%.2f", calculatedAmount));
        } else {
            riskPercentageHint.setText(String.format(Locale.US, 
                "자동: %.1f%%", riskPercentage));
            riskAmountInput.setHint("자동");
        }
    }
    
    /**
     * 모드 인디케이터 업데이트
     */
    private void updateModeIndicator() {
        if (isManualMode()) {
            modeIndicator.setText("✏️ 수동");
            modeIndicator.setTextColor(getColor(R.color.tds_text_secondary));
        } else {
            modeIndicator.setText("🤖 자동");
            modeIndicator.setTextColor(getColor(R.color.tds_blue_500));
        }
    }
    
    
    /**
     * 종목 선택 Dialog 표시
     */
    private void showSymbolSelectionDialog() {
        // BottomSheet 레이아웃 inflate
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_symbol_selection, null);
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = 
            new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        bottomSheetDialog.setContentView(bottomSheetView);
        
        // View 초기화
        EditText searchInput = bottomSheetView.findViewById(R.id.search_input);
        androidx.recyclerview.widget.RecyclerView symbolList = bottomSheetView.findViewById(R.id.symbol_list);
        
        // RecyclerView 설정
        symbolList.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        
        // 종목 데이터 생성
        java.util.List<com.example.rsquare.ui.adapter.SymbolAdapter.SymbolItem> symbols = new java.util.ArrayList<>();
        symbols.add(new com.example.rsquare.ui.adapter.SymbolAdapter.SymbolItem("BTCUSDT", "Bitcoin", "BTC", "₿"));
        symbols.add(new com.example.rsquare.ui.adapter.SymbolAdapter.SymbolItem("ETHUSDT", "Ethereum", "ETH", "Ξ"));
        symbols.add(new com.example.rsquare.ui.adapter.SymbolAdapter.SymbolItem("BNBUSDT", "Binance Coin", "BNB", "⬡"));
        symbols.add(new com.example.rsquare.ui.adapter.SymbolAdapter.SymbolItem("SOLUSDT", "Solana", "SOL", "◎"));
        symbols.add(new com.example.rsquare.ui.adapter.SymbolAdapter.SymbolItem("XRPUSDT", "Ripple", "XRP", "✕"));
        symbols.add(new com.example.rsquare.ui.adapter.SymbolAdapter.SymbolItem("ADAUSDT", "Cardano", "ADA", "₳"));
        symbols.add(new com.example.rsquare.ui.adapter.SymbolAdapter.SymbolItem("DOGEUSDT", "Dogecoin", "DOGE", "Ð"));
        symbols.add(new com.example.rsquare.ui.adapter.SymbolAdapter.SymbolItem("MATICUSDT", "Polygon", "MATIC", "⬡"));
        symbols.add(new com.example.rsquare.ui.adapter.SymbolAdapter.SymbolItem("DOTUSDT", "Polkadot", "DOT", "●"));
        symbols.add(new com.example.rsquare.ui.adapter.SymbolAdapter.SymbolItem("AVAXUSDT", "Avalanche", "AVAX", "▲"));
        symbols.add(new com.example.rsquare.ui.adapter.SymbolAdapter.SymbolItem("LTCUSDT", "Litecoin", "LTC", "Ł"));
        symbols.add(new com.example.rsquare.ui.adapter.SymbolAdapter.SymbolItem("LINKUSDT", "Chainlink", "LINK", "⬡"));
        symbols.add(new com.example.rsquare.ui.adapter.SymbolAdapter.SymbolItem("UNIUSDT", "Uniswap", "UNI", "🦄"));
        symbols.add(new com.example.rsquare.ui.adapter.SymbolAdapter.SymbolItem("ATOMUSDT", "Cosmos", "ATOM", "⚛"));
        symbols.add(new com.example.rsquare.ui.adapter.SymbolAdapter.SymbolItem("ETCUSDT", "Ethereum Classic", "ETC", "Ξ"));
        
        //  Adapter 설정
        com.example.rsquare.ui.adapter.SymbolAdapter adapter = 
            new com.example.rsquare.ui.adapter.SymbolAdapter(currentSymbol, symbol -> {
                if (!symbol.equals(currentSymbol)) {
                    changeSymbol(symbol);
                }
                bottomSheetDialog.dismiss();
            });
        adapter.setItems(symbols);
        symbolList.setAdapter(adapter);
        
        // 검색 기능
        searchInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
        
        bottomSheetDialog.show();
    }
    
    /**
     * 종목 변경
     */
    private void changeSymbol(String newSymbol) {
        android.util.Log.d("TradingActivity", "Changing symbol from " + currentSymbol + " to " + newSymbol);
        
        // 종목 변경 중 플래그 설정
        isChangingSymbol = true;
        
        currentSymbol = newSymbol;
        
        // UI 업데이트
        symbolText.setText(currentSymbol + " | " + currentTimeframe.toUpperCase());
        
        // 차트 심볼 업데이트
        if (tradingChart != null) {
            final String finalSymbol = currentSymbol;
            tradingChart.post(() -> {
                String jsCode = "if (typeof setSymbol === 'function') { setSymbol('" + finalSymbol + "'); }";
                tradingChart.evaluateJavascript(jsCode, null);
            });
        }
        
        // Entry, TP, SL 입력 필드 클리어
        entryPriceInput.setText("");
        tpPriceInput.setText("");
        slPriceInput.setText("");
        
        // 사용자 설정 플래그 리셋 (새 종목이므로)
        userHasSetEntryPrice = false;
        userHasSetTP = false;
        userHasSetSL = false;
        needsAutoPriceSetup = false; // 자동 설정 플래그도 리셋
        
        // 차트의 선들 완전히 제거 (updateTradeData 사용)
        if (tradingChart != null) {
            tradingChart.post(() -> {
                // updateTradeData를 사용하여 모든 값을 null로 설정
                String jsCode = 
                    "if (typeof updateTradeData === 'function') { " +
                    "updateTradeData(null, null, null); " +
                    "} else if (typeof updateLines === 'function') { " +
                    "updateLines(null, null, null); " +
                    "}";
                tradingChart.evaluateJavascript(jsCode, null);
                
                // 개별 함수로도 시도 (이중 안전장치)
                tradingChart.postDelayed(() -> {
                    String jsCode2 = 
                        "if (typeof setEntryPrice === 'function') { setEntryPrice(null); } " +
                        "if (typeof setTakeProfit === 'function') { setTakeProfit(null); } " +
                        "if (typeof setStopLoss === 'function') { setStopLoss(null); }";
                    tradingChart.evaluateJavascript(jsCode2, null);
                }, 100);
            });
        }
        
        // CoinGecko ID 매핑 (간단한 버전)
        String coinId = getCoinIdFromSymbol(newSymbol);
        
        // 차트 데이터 다시 로드
        if (coinId != null) {
            chartViewModel.selectCoin(coinId);
            chartViewModel.loadChartData(coinId, 7, currentTimeframe);
            
            // 토스트 메시지
            Toast.makeText(this, newSymbol + " 차트 로딩 중...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "지원하지 않는 종목입니다", Toast.LENGTH_SHORT).show();
            isChangingSymbol = false; // 실패 시 플래그 해제
            needsAutoPriceSetup = false; // 실패 시 플래그 해제
        }
    }
    
    /**
     * 종목 변경 후 EP/TP/SL 자동 설정 시도
     */
    private void tryAutoSetupPrices() {
        if (!needsAutoPriceSetup) {
            return;
        }
        
        if (currentPrice <= 0) {
            android.util.Log.d("TradingActivity", "Cannot auto-setup prices: currentPrice is 0");
            return;
        }
        
        if (!isChartReady) {
            android.util.Log.d("TradingActivity", "Cannot auto-setup prices: chart not ready");
            return;
        }
        
        String currentEntryText = entryPriceInput.getText() != null ? 
            entryPriceInput.getText().toString().trim() : "";
        String currentTPText = tpPriceInput.getText() != null ? 
            tpPriceInput.getText().toString().trim() : "";
        String currentSLText = slPriceInput.getText() != null ? 
            slPriceInput.getText().toString().trim() : "";
        
        boolean allEmpty = (currentEntryText.isEmpty() || "0.00".equals(currentEntryText)) && 
                       (currentTPText.isEmpty() || "0.00".equals(currentTPText)) && 
                       (currentSLText.isEmpty() || "0.00".equals(currentSLText));
        boolean noneSetByUser = !userHasSetEntryPrice && !userHasSetTP && !userHasSetSL;
        
        if (!allEmpty || !noneSetByUser) {
            android.util.Log.d("TradingActivity", "Cannot auto-setup prices: fields not empty or user has set");
            needsAutoPriceSetup = false;
            return;
        }
        
        android.util.Log.d("TradingActivity", "Auto-setting EP/TP/SL to: " + currentPrice);
        
        String priceStr = formatPrice(currentPrice);
        
        // EP 설정
        entryPriceInput.setText(priceStr);
        if (tradingChart != null && isChartReady) {
            String jsCodeEP = "if (typeof setEntryPrice === 'function') { setEntryPrice(" + currentPrice + "); }";
            tradingChart.post(() -> {
                if (tradingChart != null) {
                    tradingChart.evaluateJavascript(jsCodeEP, null);
                }
            });
        }
        
        // TP 설정
        tpPriceInput.setText(priceStr);
        if (tradingChart != null && isChartReady) {
            String jsCodeTP = "if (typeof setTakeProfit === 'function') { setTakeProfit(" + currentPrice + "); }";
            tradingChart.post(() -> {
                if (tradingChart != null) {
                    tradingChart.evaluateJavascript(jsCodeTP, null);
                }
            });
        }
        
        // SL 설정
        slPriceInput.setText(priceStr);
        if (tradingChart != null && isChartReady) {
            String jsCodeSL = "if (typeof setStopLoss === 'function') { setStopLoss(" + currentPrice + "); }";
            tradingChart.post(() -> {
                if (tradingChart != null) {
                    tradingChart.evaluateJavascript(jsCodeSL, null);
                }
            });
        }
        
        needsAutoPriceSetup = false; // 설정 완료
        updateRiskMetrics();
        
        android.util.Log.d("TradingActivity", "Auto price setup completed");
    }
    
    /**
     * 심볼에서 CoinGecko ID 가져오기
     */
    private String getCoinIdFromSymbol(String symbol) {
        // USDT 제거하고 소문자로 변환
        String base = symbol.replace("USDT", "").toLowerCase();
        
        // 특수 케이스 처리
        switch (base) {
            case "btc": return "bitcoin";
            case "eth": return "ethereum";
            case "bnb": return "binancecoin";
            case "sol": return "solana";
            case "xrp": return "ripple";
            case "ada": return "cardano";
            case "doge": return "dogecoin";
            case "matic": return "matic-network";
            case "dot": return "polkadot";
            case "avax": return "avalanche-2";
            case "ltc": return "litecoin";
            case "link": return "chainlink";
            case "uni": return "uniswap";
            case "atom": return "cosmos";
            case "etc": return "ethereum-classic";
            default: return base;
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

    /**
     * 인텐트로 전달된 데이터 처리
     */
    private void handleIntentExtras() {
        Intent intent = getIntent();
        if (intent == null) return;

        if (intent.hasExtra("SYMBOL")) {
            String symbol = intent.getStringExtra("SYMBOL");
            if (symbol != null) {
                currentSymbol = symbol;
                if (symbolText != null) symbolText.setText(currentSymbol + " | " + currentTimeframe.toUpperCase());
                
                // 차트 데이터 다시 로드
                String coinId = getCoinIdFromSymbol(currentSymbol);
                chartViewModel.selectCoin(coinId);
                chartViewModel.loadChartData(coinId, 7, currentTimeframe);
            }
        }
        
        if (intent.hasExtra("TIMEFRAME")) {
            String tf = intent.getStringExtra("TIMEFRAME");
            if (tf != null) {
                currentTimeframe = tf;
                // 타임프레임 버튼 업데이트
                switch (tf.toLowerCase()) {
                    case "1m": setTimeframeButtonActive(btnTime1m); break;
                    case "5m": setTimeframeButtonActive(btnTime5m); break;
                    case "15m": setTimeframeButtonActive(btnTime15m); break;
                    case "30m": setTimeframeButtonActive(btnTime30m); break;
                    case "1h": setTimeframeButtonActive(btnTime1h); break;
                    case "4h": setTimeframeButtonActive(btnTime4h); break;
                    case "1d": setTimeframeButtonActive(btnTime1d); break;
                }
            }
        }

        if (intent.hasExtra("ENTRY_PRICE")) {
            double price = intent.getDoubleExtra("ENTRY_PRICE", 0.0);
            if (entryPriceInput != null && price > 0) entryPriceInput.setText(String.valueOf(price));
        }
        
        if (intent.hasExtra("TP_PRICE")) {
            double price = intent.getDoubleExtra("TP_PRICE", 0.0);
            if (tpPriceInput != null && price > 0) tpPriceInput.setText(String.valueOf(price));
        }
        
        if (intent.hasExtra("SL_PRICE")) {
            double price = intent.getDoubleExtra("SL_PRICE", 0.0);
            if (slPriceInput != null && price > 0) slPriceInput.setText(String.valueOf(price));
        }
        
        if (intent.hasExtra("RISK_AMOUNT")) {
            double amount = intent.getDoubleExtra("RISK_AMOUNT", 0.0);
            if (riskAmountInput != null && amount > 0) riskAmountInput.setText(String.valueOf(amount));
        }
        
        if (intent.hasExtra("LEVERAGE")) {
            leverage = intent.getIntExtra("LEVERAGE", 1);
            // 스피너 업데이트는 복잡할 수 있으므로 변수만 설정하고 텍스트 뷰가 있다면 업데이트
            if (timeframeText != null && "FUTURES".equals(tradeMode)) {
                timeframeText.setText(leverage + "x 레버리지");
            }
        }
        
        if (intent.hasExtra("IS_LONG")) {
            isLong = intent.getBooleanExtra("IS_LONG", true);
            updatePositionButtons();
        }
        
        if (intent.hasExtra("MARGIN_MODE")) {
            String mode = intent.getStringExtra("MARGIN_MODE");
            if (mode != null) {
                marginMode = mode;
                // 올바른 버튼 참조와 함께 setMarginMode 호출
                Button activeButton = "ISOLATED".equals(mode) ? btnMarginIsolated : btnMarginCross;
                setMarginMode(mode, activeButton);
            }
        }
        
        // 값들이 설정된 후 리스크 메트릭 강제 업데이트
        updateRiskMetrics();
    }
    
    /**
     * TP와 SL 값 교환 (롱↔숏 전환 시)
     */
    private void swapTPandSL() {
        if (tpPriceInput == null || slPriceInput == null || entryPriceInput == null) return;
        
        String tpText = tpPriceInput.getText().toString().trim();
        String slText = slPriceInput.getText().toString().trim();
        String entryText = entryPriceInput.getText().toString().trim();
        
        // 둘 다 값이 있을 때만 교환
        if (!tpText.isEmpty() && !slText.isEmpty() && !entryText.isEmpty()) {
            try {
                double entry = parsePrice(entryText);
                double tp = parsePrice(tpText);
                double sl = parsePrice(slText);
                
                // 단순히 swap하는 게 아니라, 포지션 타입에 맞게 재배치
                // Long: TP > Entry > SL
                // Short: SL > Entry > TP
                
                double newTP, newSL;
                
                if (isLong) {
                    // Short -> Long 전환
                    // 현재 Short였을 때: SL이 위(큰 값), TP가 아래(작은 값)
                    // Long으로 바꾸면: TP가 위(큰 값), SL이 아래(작은 값)
                    newTP = Math.max(tp, sl); // 더 큰 값이 TP
                    newSL = Math.min(tp, sl); // 더 작은 값이 SL
                } else {
                    // Long -> Short 전환
                    // 현재 Long이었을 때: TP가 위(큰 값), SL이 아래(작은 값)
                    // Short로 바꾸면: SL이 위(큰 값), TP가 아래(작은 값)
                    newSL = Math.max(tp, sl); // 더 큰 값이 SL
                    newTP = Math.min(tp, sl); // 더 작은 값이 TP
                }
                
                // UI에 반영
                tpPriceInput.setText(String.format(Locale.US, "%.2f", newTP));
                slPriceInput.setText(String.format(Locale.US, "%.2f", newSL));
                
                // 차트에도 반영
               if (tradingChart != null) {
                    tradingChart.post(() -> {
                        tradingChart.evaluateJavascript(
                            "if (typeof setTakeProfit === 'function') { setTakeProfit(" + newTP + "); }", null);
                        tradingChart.evaluateJavascript(
                            "if (typeof setStopLoss === 'function') { setStopLoss(" + newSL + "); }", null);
                    });
                }
                
                android.util.Log.d("TradingActivity", String.format(Locale.US,
                    "Position swap: isLong=%b, Old TP=%.2f SL=%.2f -> New TP=%.2f SL=%.2f",
                    isLong, tp, sl, newTP, newSL));
                    
            } catch (Exception e) {
                android.util.Log.e("TradingActivity", "Error swapping TP/SL", e);
            }
        }
    }
    @Override
    protected void onDestroy() {
        if (tradingChart != null) {
            tradingChart.loadUrl("about:blank");
            tradingChart.destroy();
        }
        super.onDestroy();
    }
}
