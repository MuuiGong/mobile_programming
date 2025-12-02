package com.example.rsquare.ui.position;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.rsquare.R;
import com.example.rsquare.data.local.entity.Position;
import com.example.rsquare.data.repository.MarketDataRepository;
import com.example.rsquare.data.repository.TradingRepository;
import com.example.rsquare.ui.BaseActivity;
import com.example.rsquare.ui.chart.ChartViewModel;
import com.example.rsquare.ui.chart.ChartWebViewInterface;
import com.example.rsquare.util.NumberFormatter;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;

/**
 * 포지션 상세 화면
 * TradingActivity의 차트를 재사용하되, EP는 읽기 전용
 */
public class PositionDetailActivity extends BaseActivity {
    
    private PositionDetailViewModel viewModel;
    private ChartViewModel chartViewModel;
    private TradingRepository tradingRepository;
    private MarketDataRepository marketDataRepository;
    
    // Views (TradingActivity와 동일)
    private Toolbar toolbar;
    private WebView tradingChart;
    private EditText entryPriceInput;
    private EditText tpPriceInput;
    private EditText slPriceInput;
    private Button btnSave;
    
    // Info Panel Views
    private android.widget.LinearLayout timeframeContainer;
    private android.widget.TextView timeframeText;
    private android.widget.TextView riskScoreText;
    private android.widget.TextView rrRatioText;
    
    // Data
    private Position position;
    private long positionId;
    private boolean isUpdatingFromChart = false;
    private boolean isChartReady = false;
    private List<List<Object>> pendingKlines = null;
    private double currentPrice = 0.0;
    
    private final DecimalFormat priceFormatter = new DecimalFormat("#,##0.00");
    
    private double parsePrice(String text) {
        try {
            return Double.parseDouble(text.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    private String formatPrice(double price) {
        return priceFormatter.format(price);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_position_detail);
        
        // Position ID 가져오기
        positionId = getIntent().getLongExtra("position_id", -1);
        if (positionId == -1) {
            Toast.makeText(this, "포지션 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // ViewModel 초기화
        viewModel = new ViewModelProvider(this).get(PositionDetailViewModel.class);
        chartViewModel = new ViewModelProvider(this).get(ChartViewModel.class);
        tradingRepository = new TradingRepository(this);
        marketDataRepository = new MarketDataRepository();
        
        initViews();
        setupToolbar();
        setupWebView();
        setupListeners();
        setupObservers();
        
        // 포지션 데이터 로드
        viewModel.loadPosition(positionId);
    }
    
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tradingChart = findViewById(R.id.trading_chart);
        entryPriceInput = findViewById(R.id.entry_price_input);
        tpPriceInput = findViewById(R.id.tp_price_input);
        slPriceInput = findViewById(R.id.sl_price_input);
        btnSave = findViewById(R.id.btn_save);
        
        // Info Panel Views
        timeframeContainer = findViewById(R.id.timeframe_container);
        timeframeText = findViewById(R.id.timeframe_text);
        riskScoreText = findViewById(R.id.risk_score_text);
        rrRatioText = findViewById(R.id.rr_ratio_text);
        
        // EP는 읽기 전용
        entryPriceInput.setEnabled(false);
        entryPriceInput.setFocusable(false);
        entryPriceInput.setFocusableInTouchMode(false);
    }
    
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
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
    
    /**
     * WebView 설정 (TradingActivity와 동일)
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
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        tradingChart.clearCache(true);
        
        // WebChromeClient 설정
        tradingChart.setWebChromeClient(new android.webkit.WebChromeClient() {
            @Override
            public boolean onConsoleMessage(android.webkit.ConsoleMessage consoleMessage) {
                android.util.Log.d("PositionDetailChart", consoleMessage.message() + " -- From line "
                        + consoleMessage.lineNumber() + " of " + consoleMessage.sourceId());
                return true;
            }
        });
        
        tradingChart.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                android.util.Log.d("PositionDetailActivity", "Chart page loaded: " + url);
            }
        });
        
        // ChartWebViewInterface 연결
        ChartWebViewInterface chartInterface = new ChartWebViewInterface(
            new ChartWebViewInterface.ExtendedChartCallback() {
                @Override
                public void onPriceChanged(double price) {
                    if (price > 0) {
                        runOnUiThread(() -> {
                            currentPrice = price;
                            // 차트에 현재가 업데이트 전달 (TradingActivity와 동일)
                            String jsCode = "if (typeof setCurrentPrice === 'function') { " +
                                "console.log('Setting current price from Android:', " + price + "); " +
                                "setCurrentPrice(" + price + "); }";
                            tradingChart.post(() -> {
                                if (tradingChart != null) {
                                    tradingChart.evaluateJavascript(jsCode, null);
                                }
                            });
                        });
                    }
                }
                
                @Override
                public void onEntryPriceChanged(double price) {
                    // EP는 변경 불가능하므로 무시
                }
                
                @Override
                public void onTakeProfitChanged(double price) {
                    if (price > 0) {
                        runOnUiThread(() -> {
                            // 차트에서 업데이트 중임을 표시 (순환 참조 방지)
                            isUpdatingFromChart = true;
                            tpPriceInput.setText(formatPrice(price));
                            isUpdatingFromChart = false;
                            
                            // 차트에서 변경 시에도 Risk/Reward 재계산
                            calculateRiskReward();
                        });
                    }
                }
                
                @Override
                public void onStopLossChanged(double price) {
                    if (price > 0) {
                        runOnUiThread(() -> {
                            // 차트에서 업데이트 중임을 표시 (순환 참조 방지)
                            isUpdatingFromChart = true;
                            slPriceInput.setText(formatPrice(price));
                            isUpdatingFromChart = false;
                            
                            // 차트에서 변경 시에도 Risk/Reward 재계산
                            calculateRiskReward();
                        });
                    }
                }
                
                @Override
                public void onChartReady() {
                    android.util.Log.d("PositionDetailActivity", "Chart ready");
                    runOnUiThread(() -> {
                        isChartReady = true;
                        
                        // 대기 중인 OHLC 데이터가 있으면 전송
                        if (pendingKlines != null && !pendingKlines.isEmpty()) {
                            loadBinanceOHLCData(pendingKlines);
                            pendingKlines = null;
                        }
                        
                        // 포지션 데이터 설정
                        if (position != null) {
                            setupChartData();
                        }
                    });
                }
            });
        
        tradingChart.addJavascriptInterface(chartInterface, "Android");
        
        // 차트 HTML 로드
        tradingChart.loadUrl("file:///android_asset/chart.html");
    }
    
    private void setupChartData() {
        if (position == null || tradingChart == null) return;
        
        String symbol = position.getSymbol();
        double entryPrice = position.getEntryPrice();
        double tp = position.getTakeProfit();
        double sl = position.getStopLoss();
        boolean isLong = position.isLong();
        
        // 차트에 포지션 데이터 설정 (EP는 읽기 전용)
        String jsCode = String.format(Locale.US,
            "if (typeof setPositionData === 'function') { " +
            "setPositionData('%s', %f, %f, %f, '%s', true); " +
            "}",
            symbol, entryPrice, tp, sl, isLong ? "long" : "short");
        
        tradingChart.post(() -> {
            tradingChart.evaluateJavascript(jsCode, null);
        });
    }
    
    private void setupListeners() {
        // TP 입력 변경 (TradingActivity와 동일)
        tpPriceInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 차트에서 업데이트 중이면 차트로 다시 업데이트하지 않음 (순환 참조 방지)
                if (isUpdatingFromChart) {
                    return;
                }
                
                // 차트 업데이트
                String text = s.toString().trim();
                if (!text.isEmpty() && tradingChart != null) {
                    try {
                        double price = parsePrice(text);
                        tradingChart.post(() -> {
                            tradingChart.evaluateJavascript(
                                "if (typeof setTakeProfit === 'function') { setTakeProfit(" + price + "); }", null);
                        });
                    } catch (Exception e) {
                        // 파싱 오류 무시
                    }
                }
                
                // Risk/Reward 재계산
                calculateRiskReward();
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // SL 입력 변경 (TradingActivity와 동일)
        slPriceInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 차트에서 업데이트 중이면 차트로 다시 업데이트하지 않음 (순환 참조 방지)
                if (isUpdatingFromChart) {
                    return;
                }
                
                // 차트 업데이트
                String text = s.toString().trim();
                if (!text.isEmpty() && tradingChart != null) {
                    try {
                        double price = parsePrice(text);
                        tradingChart.post(() -> {
                            tradingChart.evaluateJavascript(
                                "if (typeof setStopLoss === 'function') { setStopLoss(" + price + "); }", null);
                        });
                    } catch (Exception e) {
                        // 파싱 오류 무시
                    }
                }
                
                // Risk/Reward 재계산
                calculateRiskReward();
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // 저장 버튼
        btnSave.setOnClickListener(v -> savePosition());
        
        // Timeframe Selector
        timeframeContainer.setOnClickListener(v -> showTimeframeMenu());
    }
    
    private void showTimeframeMenu() {
        // 커스텀 팝업 레이아웃 인플레이트
        View popupView = getLayoutInflater().inflate(R.layout.popup_timeframe_menu, null);
        
        // PopupWindow 생성
        final android.widget.PopupWindow popupWindow = new android.widget.PopupWindow(
            popupView, 
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 
            true
        );
        
        // 배경 투명하게 설정 (CardView의 둥근 모서리 적용을 위해)
        popupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        popupWindow.setElevation(10);
        
        // 메뉴 아이템 클릭 리스너 설정
        View.OnClickListener listener = v -> {
            String timeframe = "";
            int id = v.getId();
            
            if (id == R.id.menu_15m) timeframe = "15m";
            else if (id == R.id.menu_1h) timeframe = "1h";
            else if (id == R.id.menu_4h) timeframe = "4h";
            else if (id == R.id.menu_1d) timeframe = "1d";
            
            if (!timeframe.isEmpty()) {
                timeframeText.setText(timeframe.toUpperCase());
                
                // 차트 데이터 다시 로드
                if (position != null) {
                    String coinId = getCoinIdFromSymbol(position.getSymbol());
                    if (coinId != null) {
                        chartViewModel.loadChartData(coinId, 7, timeframe);
                    }
                }
                popupWindow.dismiss();
            }
        };
        
        popupView.findViewById(R.id.menu_15m).setOnClickListener(listener);
        popupView.findViewById(R.id.menu_1h).setOnClickListener(listener);
        popupView.findViewById(R.id.menu_4h).setOnClickListener(listener);
        popupView.findViewById(R.id.menu_1d).setOnClickListener(listener);
        
        // 팝업 표시 (Timeframe 컨테이너 아래에)
        popupWindow.showAsDropDown(timeframeContainer, 0, 10);
    }
    
    private void setupObservers() {
        // 포지션 데이터 관찰
        viewModel.getPosition().observe(this, pos -> {
            if (pos != null) {
                position = pos;
                updateUI();
                
                // 차트 데이터 로드
                loadChartData();
                
                // 차트가 준비되었으면 데이터 설정
                if (isChartReady) {
                    setupChartData();
                }
            }
        });
        
        // ChartViewModel의 현재 가격 관찰 (웹소켓 실시간 업데이트) - TradingActivity와 동일
        chartViewModel.getCurrentPrice().observe(this, price -> {
            if (price != null && price > 0) {
                android.util.Log.d("PositionDetailActivity", "Current price updated from WebSocket: " + price);
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
            }
        });
        
        // 차트 데이터 관찰
        chartViewModel.getBinanceKlines().observe(this, klines -> {
            if (klines != null && !klines.isEmpty()) {
                if (isChartReady) {
                    loadBinanceOHLCData(klines);
                } else {
                    pendingKlines = klines;
                }
            }
        });
    }
    
    private void updateUI() {
        if (position == null) return;
        
        // EP 표시 (읽기 전용)
        entryPriceInput.setText(NumberFormatter.formatPrice(position.getEntryPrice()));
        
        // TP 표시
        if (position.getTakeProfit() > 0) {
            tpPriceInput.setText(priceFormatter.format(position.getTakeProfit()));
        }
        
        // SL 표시
        if (position.getStopLoss() > 0) {
            slPriceInput.setText(priceFormatter.format(position.getStopLoss()));
        }
        
        // 툴바 제목 업데이트
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(position.getSymbol() + " 포지션");
        }
        
        // 초기 Risk/Reward 계산
        calculateRiskReward();
    }
    
    /**
     * Risk, Reward, R:R 계산 및 표시
     */
    private void calculateRiskReward() {
        if (position == null) return;
        
        try {
            double entryPrice = position.getEntryPrice();
            double quantity = position.getQuantity();
            boolean isLong = position.isLong();
            
            // TP/SL 파싱
            String tpStr = tpPriceInput.getText().toString().trim().replace(",", "");
            String slStr = slPriceInput.getText().toString().trim().replace(",", "");
            
            double tp = tpStr.isEmpty() ? 0 : Double.parseDouble(tpStr);
            double sl = slStr.isEmpty() ? 0 : Double.parseDouble(slStr);
            
            double riskAmount = 0;
            double rewardAmount = 0;
            double riskPercent = 0;
            double rewardPercent = 0;
            double rrRatio = 0;
            
            if (isLong) {
                // Long: Risk = Entry - SL, Reward = TP - Entry
                if (sl > 0) {
                    riskAmount = (entryPrice - sl) * quantity;
                    riskPercent = (entryPrice - sl) / entryPrice * 100;
                }
                if (tp > 0) {
                    rewardAmount = (tp - entryPrice) * quantity;
                    rewardPercent = (tp - entryPrice) / entryPrice * 100;
                }
            } else {
                // Short: Risk = SL - Entry, Reward = Entry - TP
                if (sl > 0) {
                    riskAmount = (sl - entryPrice) * quantity;
                    riskPercent = (sl - entryPrice) / entryPrice * 100;
                }
                if (tp > 0) {
                    rewardAmount = (entryPrice - tp) * quantity;
                    rewardPercent = (entryPrice - tp) / entryPrice * 100;
                }
            }
            
            // R:R Ratio 계산
            if (riskAmount > 0 && rewardAmount > 0) {
                rrRatio = rewardAmount / riskAmount;
            }
            
            // UI 업데이트 (Risk Score)
            final double finalRiskAmount = riskAmount;
            final double finalRrRatio = rrRatio;
            new Thread(() -> {
                double balance = tradingRepository.getBalanceSync(position.getUserId());
                double riskScore = com.example.rsquare.domain.RiskCalculator.calculatePositionRiskScore(finalRiskAmount, balance, finalRrRatio);
                
                runOnUiThread(() -> {
                    riskScoreText.setText(String.format(Locale.US, "%.0f", riskScore));
                    
                    // 점수에 따른 색상
                    if (riskScore >= 80) {
                        riskScoreText.setTextColor(getColor(R.color.tds_success_alt));
                    } else if (riskScore >= 50) {
                        riskScoreText.setTextColor(getColor(R.color.tds_warning_alt));
                    } else {
                        riskScoreText.setTextColor(getColor(R.color.tds_error_alt));
                    }
                });
            }).start();
            
            // R:R Ratio
            if (rrRatio > 0) {
                rrRatioText.setText(String.format(Locale.US, "1:%.1f", rrRatio));
                
                // R:R에 따른 색상 (2.0 이상이면 좋음)
                if (rrRatio >= 2.0) {
                    rrRatioText.setTextColor(getColor(R.color.tds_success_alt));
                } else if (rrRatio >= 1.0) {
                    rrRatioText.setTextColor(getColor(R.color.tds_warning_alt));
                } else {
                    rrRatioText.setTextColor(getColor(R.color.tds_error_alt));
                }
            } else {
                rrRatioText.setText("-");
                rrRatioText.setTextColor(getColor(R.color.tds_text_tertiary));
            }
            
        } catch (NumberFormatException e) {
            // 파싱 오류 시 초기화
            riskScoreText.setText("-");
            rrRatioText.setText("-");
        }
    }
    
    private void loadChartData() {
        if (position == null) return;
        
        // ChartViewModel을 통해 데이터 로드 (TradingActivity와 동일)
        String symbol = position.getSymbol();
        String coinId = getCoinIdFromSymbol(symbol);
        
        if (coinId != null) {
            // 코인 선택 및 차트 데이터 로드
            chartViewModel.selectCoin(coinId);
            chartViewModel.loadChartData(coinId, 7, "1h");
        }
    }
    
    private void loadBinanceOHLCData(List<List<Object>> klines) {
        if (klines == null || klines.isEmpty() || tradingChart == null) return;
        
        try {
            org.json.JSONArray klinesArray = new org.json.JSONArray();
            
            for (List<Object> kline : klines) {
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
                "      } " +
                "    }, 500); " +
                "  } " +
                "})();",
                klinesString, klinesString
            );
            
            tradingChart.post(() -> {
                tradingChart.evaluateJavascript(jsCode, null);
            });
        } catch (Exception e) {
            android.util.Log.e("PositionDetailActivity", "Error loading OHLC data", e);
        }
    }
    
    private String getCoinIdFromSymbol(String symbol) {
        // Binance 심볼 -> CoinGecko ID 매핑
        java.util.Map<String, String> symbolToCoinId = new java.util.HashMap<>();
        symbolToCoinId.put("BTCUSDT", "bitcoin");
        symbolToCoinId.put("ETHUSDT", "ethereum");
        symbolToCoinId.put("ADAUSDT", "cardano");
        symbolToCoinId.put("SOLUSDT", "solana");
        symbolToCoinId.put("XRPUSDT", "ripple");
        symbolToCoinId.put("DOTUSDT", "polkadot");
        symbolToCoinId.put("DOGEUSDT", "dogecoin");
        symbolToCoinId.put("AVAXUSDT", "avalanche-2");
        
        return symbolToCoinId.get(symbol);
    }
    
    private void savePosition() {
        if (position == null) return;
        
        try {
            // TP 가져오기
            String tpText = tpPriceInput.getText().toString().trim().replace(",", "");
            double newTP = tpText.isEmpty() ? 0 : Double.parseDouble(tpText);
            
            // SL 가져오기
            String slText = slPriceInput.getText().toString().trim().replace(",", "");
            double newSL = slText.isEmpty() ? 0 : Double.parseDouble(slText);
            
            // 유효성 검사
            if (newTP <= 0 || newSL <= 0) {
                Toast.makeText(this, "TP와 SL은 0보다 큰 값이어야 합니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 롱/숏에 따른 유효성 검사
            if (position.isLong()) {
                // 롱: SL < EP < TP
                if (newSL >= position.getEntryPrice()) {
                    Toast.makeText(this, "롱 포지션의 SL은 진입가보다 낮아야 합니다.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (newTP <= position.getEntryPrice()) {
                    Toast.makeText(this, "롱 포지션의 TP는 진입가보다 높아야 합니다.", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                // 숏: TP < EP < SL
                if (newTP >= position.getEntryPrice()) {
                    Toast.makeText(this, "숏 포지션의 TP는 진입가보다 낮아야 합니다.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (newSL <= position.getEntryPrice()) {
                    Toast.makeText(this, "숏 포지션의 SL은 진입가보다 높아야 합니다.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            
            // 포지션 업데이트
            position.setTakeProfit(newTP);
            position.setStopLoss(newSL);
            
            // DB 업데이트
            new Thread(() -> {
                tradingRepository.updatePosition(position);
                runOnUiThread(() -> {
                    Toast.makeText(this, "저장되었습니다.", Toast.LENGTH_SHORT).show();
                });
            }).start();
            
        } catch (NumberFormatException e) {
            Toast.makeText(this, "올바른 숫자를 입력해주세요.", Toast.LENGTH_SHORT).show();
        }
    }
}
