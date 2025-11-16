package com.example.rsquare.ui.chart;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.rsquare.R;
import com.example.rsquare.data.local.entity.Position;
import com.example.rsquare.data.remote.model.CoinMarketChart;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Chart Fragment
 * WebView 기반 인터랙티브 차트 및 거래 UI
 */
public class ChartFragment extends Fragment implements ChartWebViewInterface.ChartCallback {
    
    private ChartViewModel viewModel;
    private WebView chartWebView;
    private EditText etQuantity, etEntryPrice, etTakeProfit, etStopLoss;
    private TextView tvRiskReward, tvSymbol, tvCurrentPrice;
    private LinearLayout togglePositionType;
    private MaterialButton btnLong, btnShort;
    private Button btnOpenPosition;
    private android.widget.ImageView btnCoinSelector;
    private MaterialCardView tradingPanel;
    private BottomSheetBehavior<MaterialCardView> bottomSheetBehavior;
    
    // 차트 컨트롤
    private MaterialButton btnTime1m, btnTime5m, btnTime15m, btnTime30m, btnTime1h, btnTime4h, btnTime1d;
    private MaterialButton btnChartType, btnIndicators, btnDrawingTools;
    private String currentTimeframe = "1h"; // 기본 시간 단위
    private String currentChartType = "candlestick"; // 기본 차트 타입
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chart_tds, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewModel = new ViewModelProvider(this).get(ChartViewModel.class);
        
        initViews(view);
        setupWebView();
        setupObservers();
        setupListeners();
        
        // 초기 데이터 로드
        viewModel.loadMarketData();
    }
    
    /**
     * View 초기화
     */
    private void initViews(View view) {
        chartWebView = view.findViewById(R.id.chart_webview);
        etQuantity = view.findViewById(R.id.et_quantity);
        etEntryPrice = view.findViewById(R.id.et_entry_price);
        etTakeProfit = view.findViewById(R.id.et_take_profit);
        etStopLoss = view.findViewById(R.id.et_stop_loss);
        tvRiskReward = view.findViewById(R.id.tv_risk_reward);
        tvSymbol = view.findViewById(R.id.tv_symbol);
        tvCurrentPrice = view.findViewById(R.id.tv_current_price);
        togglePositionType = view.findViewById(R.id.toggle_position_type);
        btnLong = view.findViewById(R.id.btn_long);
        btnShort = view.findViewById(R.id.btn_short);
        btnOpenPosition = view.findViewById(R.id.btn_open_position);
        btnCoinSelector = view.findViewById(R.id.btn_coin_selector);
        
        // 거래 패널 BottomSheet 설정
        tradingPanel = view.findViewById(R.id.trading_panel);
        bottomSheetBehavior = BottomSheetBehavior.from(tradingPanel);
        
        // dp를 px로 변환
        float density = getResources().getDisplayMetrics().density;
        int peekHeightPx = (int) (200 * density); // 최소 높이 200dp
        int maxHeightPx = (int) (600 * density); // 최대 높이 600dp
        
        bottomSheetBehavior.setPeekHeight(peekHeightPx);
        bottomSheetBehavior.setMaxHeight(maxHeightPx);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED); // 초기 상태: 접힘
        bottomSheetBehavior.setHideable(false); // 완전히 숨길 수 없음
        bottomSheetBehavior.setDraggable(true); // 드래그 가능
        
        // 차트 컨트롤 초기화
        btnTime1m = view.findViewById(R.id.btn_time_1m);
        btnTime5m = view.findViewById(R.id.btn_time_5m);
        btnTime15m = view.findViewById(R.id.btn_time_15m);
        btnTime30m = view.findViewById(R.id.btn_time_30m);
        btnTime1h = view.findViewById(R.id.btn_time_1h);
        btnTime4h = view.findViewById(R.id.btn_time_4h);
        btnTime1d = view.findViewById(R.id.btn_time_1d);
        btnChartType = view.findViewById(R.id.btn_chart_type);
        btnIndicators = view.findViewById(R.id.btn_indicators);
        btnDrawingTools = view.findViewById(R.id.btn_drawing_tools);
        
        // 초기 선택 상태 설정 (롱이 기본 선택)
        btnLong.setChecked(true);
        // 시간 단위 기본 선택 (1h) - TradingView 스타일로 활성화 표시
        setTimeframeButtonActive(btnTime1h);
    }
    
    /**
     * 시간 단위 버튼 활성화 상태 설정 (TradingView 스타일)
     */
    private void setTimeframeButtonActive(MaterialButton activeButton) {
        // 모든 버튼 비활성화 스타일
        int inactiveBgColor = ContextCompat.getColor(requireContext(), android.R.color.transparent);
        int inactiveTextColor = ContextCompat.getColor(requireContext(), R.color.tds_gray_500);
        int activeBgColor = ContextCompat.getColor(requireContext(), R.color.tds_blue_500);
        int activeTextColor = ContextCompat.getColor(requireContext(), android.R.color.white);
        
        // 모든 버튼 비활성화
        MaterialButton[] buttons = {btnTime1m, btnTime5m, btnTime15m, btnTime30m, btnTime1h, btnTime4h, btnTime1d};
        for (MaterialButton btn : buttons) {
            if (btn != null) {
                btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(inactiveBgColor));
                btn.setTextColor(inactiveTextColor);
            }
        }
        
        // 활성화된 버튼만 강조
        if (activeButton != null) {
            activeButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(activeBgColor));
            activeButton.setTextColor(activeTextColor);
        }
    }
    
    /**
     * WebView 설정 (TradingView 공식 예제 베스트 프랙티스 적용)
     */
    private void setupWebView() {
        WebSettings webSettings = chartWebView.getSettings();
        
        // JavaScript 활성화
        webSettings.setJavaScriptEnabled(true);
        
        // DOM Storage 활성화 (로컬 스토리지 사용)
        webSettings.setDomStorageEnabled(true);
        
        // 파일 접근 허용
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        
        // 네트워크 접근 허용 (외부 리소스 로드용)
        webSettings.setBlockNetworkLoads(false);
        webSettings.setBlockNetworkImage(false);
        
        // 캐시 설정 (개발 중에는 NO_CACHE, 프로덕션에서는 DEFAULT)
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        
        // 뷰포트 설정
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        
        // 추가 성능 최적화 설정
        webSettings.setDatabaseEnabled(true);
        // setAppCacheEnabled는 API 33에서 제거됨 (deprecated)
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        
        // WebChromeClient 설정 (JavaScript 콘솔 로그 확인용)
        chartWebView.setWebChromeClient(new android.webkit.WebChromeClient() {
            @Override
            public boolean onConsoleMessage(android.webkit.ConsoleMessage consoleMessage) {
                android.util.Log.d("ChartWebView", consoleMessage.message() + " -- From line "
                        + consoleMessage.lineNumber() + " of "
                        + consoleMessage.sourceId());
                return true;
            }
        });
        
        // 하드웨어 가속 비활성화 (WebView 크래시 방지)
        // 일부 기기에서 하드웨어 가속이 WebView 크래시를 유발할 수 있음
        chartWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        
        // JavaScript Interface 추가 (API 레벨 17 이상에서 @JavascriptInterface 필요)
        chartWebView.addJavascriptInterface(new ChartWebViewInterface(this), "Android");
        
        // WebViewClient 설정 (페이지 로드 완료 후 데이터 설정)
        chartWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                android.util.Log.d("ChartFragment", "WebView page finished, URL: " + url);
                
                // 차트 초기화 완료 후 데이터 설정
                view.postDelayed(() -> {
                    android.util.Log.d("ChartFragment", "Setting up chart after page load");
                    
                    // 심볼 설정
                    if (viewModel.getSelectedCoinId().getValue() != null) {
                        String coinId = viewModel.getSelectedCoinId().getValue();
                        callJavaScript("if (typeof setSymbol === 'function') { setSymbol('" + coinId + "'); }");
                    }
                    // 현재 가격 설정
                    if (viewModel.getCurrentPrice().getValue() != null) {
                        Double price = viewModel.getCurrentPrice().getValue();
                        callJavaScript("if (typeof setCurrentPrice === 'function') { setCurrentPrice(" + price + "); }");
                        // 진입가가 설정되지 않았으면 현재 가격으로 설정
                        if (etEntryPrice.getText().toString().isEmpty()) {
                            etEntryPrice.setText(String.valueOf(price));
                            callJavaScript("if (typeof setEntryPrice === 'function') { setEntryPrice(" + price + "); }");
                        }
                        
                        // TP/SL 초기값 설정 (현재가 기준으로 자동 계산)
                        if (etTakeProfit.getText().toString().isEmpty()) {
                            double tp = price * 1.02; // 현재가의 2% 위
                            etTakeProfit.setText(String.valueOf(tp));
                            callJavaScript("if (typeof setTakeProfit === 'function') { setTakeProfit(" + tp + "); }");
                        } else {
                            // 이미 값이 있으면 JavaScript에 전달
                            try {
                                double tp = Double.parseDouble(etTakeProfit.getText().toString());
                                callJavaScript("if (typeof setTakeProfit === 'function') { setTakeProfit(" + tp + "); }");
                            } catch (NumberFormatException e) {
                                // 무시
                            }
                        }
                        
                        if (etStopLoss.getText().toString().isEmpty()) {
                            double sl = price * 0.98; // 현재가의 2% 아래
                            etStopLoss.setText(String.valueOf(sl));
                            callJavaScript("if (typeof setStopLoss === 'function') { setStopLoss(" + sl + "); }");
                        } else {
                            // 이미 값이 있으면 JavaScript에 전달
                            try {
                                double sl = Double.parseDouble(etStopLoss.getText().toString());
                                callJavaScript("if (typeof setStopLoss === 'function') { setStopLoss(" + sl + "); }");
                            } catch (NumberFormatException e) {
                                // 무시
                            }
                        }
                    }
                    
                    // Binance OHLC 데이터가 있으면 우선 사용
                    if (viewModel.getBinanceKlines().getValue() != null && !viewModel.getBinanceKlines().getValue().isEmpty()) {
                        android.util.Log.d("ChartFragment", "Loading Binance OHLC data after page load");
                        loadBinanceOHLCData(viewModel.getBinanceKlines().getValue());
                    } else if (viewModel.getChartData().getValue() != null) {
                        // 차트 데이터 로드 (fallback)
                        android.util.Log.d("ChartFragment", "Loading fallback chart data after page load");
                        loadChartData(viewModel.getChartData().getValue());
                    }
                }, 500);
            }
            
            @Override
            public void onReceivedError(WebView view, android.webkit.WebResourceRequest request, 
                                      android.webkit.WebResourceError error) {
                super.onReceivedError(view, request, error);
                android.util.Log.e("ChartFragment", "WebView error: " + error.getDescription());
            }
        });
        
        // HTML 로드 (버전 파라미터로 캐시 무시)
        chartWebView.loadUrl("file:///android_asset/chart.html?v=" + System.currentTimeMillis());
    }
    
    /**
     * Observer 설정
     */
    private void setupObservers() {
        // Binance OHLC 데이터 관찰 (우선 사용)
        viewModel.getBinanceKlines().observe(getViewLifecycleOwner(), klines -> {
            if (klines != null && !klines.isEmpty()) {
                android.util.Log.d("ChartFragment", "Binance klines observer triggered, size: " + klines.size());
                loadBinanceOHLCData(klines);
            } else {
                android.util.Log.d("ChartFragment", "Binance klines observer triggered but data is null or empty");
            }
        });
        
        // 차트 데이터 관찰 (기존 호환성)
        viewModel.getChartData().observe(getViewLifecycleOwner(), chartData -> {
            if (chartData != null) {
                // Binance 데이터가 없을 때만 기존 방식 사용
                if (viewModel.getBinanceKlines().getValue() == null || viewModel.getBinanceKlines().getValue().isEmpty()) {
                    android.util.Log.d("ChartFragment", "Using fallback chart data (CoinGecko)");
                    loadChartData(chartData);
                } else {
                    android.util.Log.d("ChartFragment", "Skipping chart data (using Binance OHLC instead)");
                }
            }
        });
        
        // 현재 가격 관찰
        viewModel.getCurrentPrice().observe(getViewLifecycleOwner(), price -> {
            if (price != null) {
                tvCurrentPrice.setText("$" + String.format(Locale.US, "%.2f", price));
                
                // JavaScript에 현재가 업데이트 알림
                callJavaScript("if (typeof setCurrentPrice === 'function') { setCurrentPrice(" + price + "); }");
                
                // 진입가가 설정되지 않았으면 현재 가격으로 설정
                if (etEntryPrice.getText().toString().isEmpty()) {
                    etEntryPrice.setText(String.valueOf(price));
                    callJavaScript("if (typeof setEntryPrice === 'function') { setEntryPrice(" + price + "); }");
                }
            }
        });
        
        // R:R 비율 관찰
        viewModel.getRiskRewardRatio().observe(getViewLifecycleOwner(), rr -> {
            if (rr != null) {
                tvRiskReward.setText("R:R " + String.format(Locale.US, "%.2f", rr) + ":1");
                
                // 색상 설정 (TDS 색상 사용)
                if (rr >= 2.0) {
                    tvRiskReward.setTextColor(ContextCompat.getColor(requireContext(), R.color.tds_success));
                } else if (rr >= 1.0) {
                    tvRiskReward.setTextColor(ContextCompat.getColor(requireContext(), R.color.tds_warning));
                } else {
                    tvRiskReward.setTextColor(ContextCompat.getColor(requireContext(), R.color.tds_error));
                }
            }
        });
        
        // 선택된 코인 관찰
        viewModel.getSelectedCoinId().observe(getViewLifecycleOwner(), coinId -> {
            if (coinId != null) {
                tvSymbol.setText(coinId.toUpperCase());
                // 차트 초기화 후에만 호출
                callJavaScript("if (typeof setSymbol === 'function') { setSymbol('" + coinId.toUpperCase() + "'); }");
            }
        });
        
        // 에러 메시지 관찰
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
        
        // 실시간 캔들스틱 업데이트 관찰
        viewModel.getKlineUpdate().observe(getViewLifecycleOwner(), klineData -> {
            if (klineData != null) {
                // JavaScript에 실시간 캔들 업데이트 전달
                String jsCode = String.format(Locale.US,
                    "if (typeof updateKline === 'function') { " +
                    "updateKline(%d, %f, %f, %f, %f, %f); }",
                    klineData.openTime / 1000, // 초 단위로 변환
                    klineData.open,
                    klineData.high,
                    klineData.low,
                    klineData.close,
                    klineData.volume
                );
                callJavaScript(jsCode);
            }
        });
    }
    
    /**
     * Listener 설정
     */
    private void setupListeners() {
        // 포지션 타입 토글
        btnLong.setOnClickListener(v -> {
            if (!btnLong.isChecked()) {
                btnLong.setChecked(true);
                btnShort.setChecked(false);
                viewModel.setIsLong(true);
                callJavaScript("if (typeof setPositionType === 'function') { setPositionType(true); }");
            }
        });
        
        btnShort.setOnClickListener(v -> {
            if (!btnShort.isChecked()) {
                btnShort.setChecked(true);
                btnLong.setChecked(false);
                viewModel.setIsLong(false);
                callJavaScript("if (typeof setPositionType === 'function') { setPositionType(false); }");
            }
        });
        
        // 가격 입력 리스너 (실시간 업데이트)
        etEntryPrice.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(android.text.Editable s) {
                updateEntryPrice();
                calculateRiskReward();
            }
        });
        
        etTakeProfit.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(android.text.Editable s) {
                updateTakeProfit();
                calculateRiskReward();
            }
        });
        
        etStopLoss.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(android.text.Editable s) {
                updateStopLoss();
                calculateRiskReward();
            }
        });
        
        // 포지션 열기 버튼
        btnOpenPosition.setOnClickListener(v -> openPosition());
        
        // 시간 단위 선택 리스너
        btnTime1m.setOnClickListener(v -> setTimeframe("1m", btnTime1m));
        btnTime5m.setOnClickListener(v -> setTimeframe("5m", btnTime5m));
        btnTime15m.setOnClickListener(v -> setTimeframe("15m", btnTime15m));
        if (btnTime30m != null) {
            btnTime30m.setOnClickListener(v -> setTimeframe("30m", btnTime30m));
        }
        btnTime1h.setOnClickListener(v -> setTimeframe("1h", btnTime1h));
        btnTime4h.setOnClickListener(v -> setTimeframe("4h", btnTime4h));
        btnTime1d.setOnClickListener(v -> setTimeframe("1d", btnTime1d));
        
        // 차트 타입 선택 리스너
        btnChartType.setOnClickListener(v -> showChartTypeDialog());
        
        // 인디케이터 버튼 리스너
        btnIndicators.setOnClickListener(v -> showIndicatorsDialog());
        
        // 드로잉 도구 버튼 리스너
        btnDrawingTools.setOnClickListener(v -> showDrawingToolsDialog());
        
        // 코인 선택 버튼 리스너
        btnCoinSelector.setOnClickListener(v -> showCoinSelectorDialog());
    }
    
    /**
     * 시간 단위 변경 (TradingView 스타일)
     */
    private void setTimeframe(String timeframe, MaterialButton button) {
        currentTimeframe = timeframe;
        
        // 활성화된 버튼 표시
        setTimeframeButtonActive(button);
        
        // JavaScript에 시간 단위 변경 알림
        callJavaScript("if (typeof setTimeframe === 'function') { setTimeframe('" + timeframe + "'); }");
        
        // TradingView Advanced Chart는 자체적으로 데이터를 가져오므로
        // 별도의 차트 데이터 로드 불필요
    }
    
    /**
     * 차트 타입 선택 다이얼로그 (TradingView 스타일 - 더 많은 옵션)
     */
    private void showChartTypeDialog() {
        String[] chartTypes = {"캔들", "라인", "영역", "바", "하이킨 아시", "렌코"};
        String[] chartTypeValues = {"candlestick", "line", "area", "bar", "heikinashi", "renko"};
        
        new android.app.AlertDialog.Builder(requireContext())
            .setTitle("차트 타입 선택")
            .setItems(chartTypes, (dialog, which) -> {
                currentChartType = chartTypeValues[which];
                btnChartType.setText(chartTypes[which]);
                callJavaScript("if (typeof setChartType === 'function') { setChartType('" + currentChartType + "'); }");
            })
            .show();
    }
    
    /**
     * 인디케이터 선택 다이얼로그
     */
    private void showIndicatorsDialog() {
        String[] indicators = {"이동평균 (MA)", "RSI", "MACD", "볼린저 밴드", "스토캐스틱"};
        
        new android.app.AlertDialog.Builder(requireContext())
            .setTitle("인디케이터 추가")
            .setItems(indicators, (dialog, which) -> {
                String indicator = indicators[which];
                Toast.makeText(requireContext(), indicator + " 추가 기능은 준비 중입니다", Toast.LENGTH_SHORT).show();
                // TODO: 인디케이터 추가 기능 구현
                callJavaScript("if (typeof addIndicator === 'function') { addIndicator('" + indicator + "'); }");
            })
            .show();
    }
    
    /**
     * 드로잉 도구 선택 다이얼로그
     */
    private void showDrawingToolsDialog() {
        String[] tools = {"선", "화살표", "텍스트", "사각형", "원"};
        
        new android.app.AlertDialog.Builder(requireContext())
            .setTitle("드로잉 도구")
            .setItems(tools, (dialog, which) -> {
                String tool = tools[which];
                Toast.makeText(requireContext(), tool + " 도구 기능은 준비 중입니다", Toast.LENGTH_SHORT).show();
                // TODO: 드로잉 도구 기능 구현
                callJavaScript("if (typeof enableDrawingTool === 'function') { enableDrawingTool('" + tool + "'); }");
            })
            .show();
    }
    
    /**
     * 코인 선택 다이얼로그
     */
    private void showCoinSelectorDialog() {
        String[] coins = {"비트코인 (BTC)", "이더리움 (ETH)", "카르다노 (ADA)", "솔라나 (SOL)", "리플 (XRP)"};
        String[] coinIds = {"bitcoin", "ethereum", "cardano", "solana", "ripple"};
        
        new android.app.AlertDialog.Builder(requireContext())
            .setTitle("코인 선택")
            .setItems(coins, (dialog, which) -> {
                String coinId = coinIds[which];
                viewModel.selectCoin(coinId);
                tvSymbol.setText(coinId.toUpperCase() + "/USD");
            })
            .show();
    }
    
    /**
     * 차트 데이터 로드
     */
    private void loadChartData(CoinMarketChart chartData) {
        try {
            JSONArray dataArray = new JSONArray();
            List<CoinMarketChart.PricePoint> points = chartData.getPricePoints();

            if (points != null && !points.isEmpty()) {
                // CoinGecko 형식: [timestamp, price] 배열
                for (CoinMarketChart.PricePoint point : points) {
                    JSONArray pricePoint = new JSONArray();
                    pricePoint.put(point.getTimestamp()); // 밀리초
                    pricePoint.put(point.getPrice());
                    dataArray.put(pricePoint);
                }
                
                // JSON 문자열 생성
                String dataString = dataArray.toString();
                android.util.Log.d("ChartFragment", "Loading chart data, points: " + points.size());
                
                // JSON을 Base64로 인코딩하여 전달 (더 안전함)
                // 또는 직접 JSON 문자열을 전달하되 올바르게 이스케이프
                String jsCode = String.format(
                    "if (typeof setChartData === 'function') { setChartData(%s); }",
                    dataString
                );
                callJavaScript(jsCode);
            } else {
                android.util.Log.w("ChartFragment", "No chart data points available");
                callJavaScript("if (typeof setChartData === 'function') { setChartData([]); }");
            }
        } catch (Exception e) {
            android.util.Log.e("ChartFragment", "Error loading chart data", e);
            // 에러 발생 시 빈 배열 전달
            callJavaScript("if (typeof setChartData === 'function') { setChartData([]); }");
        }
    }
    
    /**
     * Binance OHLC 데이터 로드
     */
    private void loadBinanceOHLCData(List<List<Object>> klines) {
        try {
            // Binance klines 형식: [[openTime, open, high, low, close, volume, ...], ...]
            org.json.JSONArray klinesArray = new org.json.JSONArray();
            
            for (List<Object> kline : klines) {
                if (kline != null && kline.size() >= 6) {
                    org.json.JSONArray klineArray = new org.json.JSONArray();
                    for (Object value : kline) {
                        if (value instanceof Number) {
                            klineArray.put(((Number) value).doubleValue());
                        } else if (value instanceof String) {
                            // 문자열인 경우 숫자로 변환 시도
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
            android.util.Log.d("ChartFragment", "Loading Binance OHLC data, klines: " + klines.size());
            android.util.Log.d("ChartFragment", "Calling setOHLCData with data length: " + klinesString.length());
            
            // JavaScript에 OHLC 데이터 직접 전달
            String jsCode = String.format(
                "if (typeof setOHLCData === 'function') { console.log('Calling setOHLCData'); setOHLCData(%s); } else { console.error('setOHLCData function not found'); }",
                klinesString
            );
            callJavaScript(jsCode);
        } catch (Exception e) {
            android.util.Log.e("ChartFragment", "Error loading Binance OHLC data", e);
            e.printStackTrace();
            // 에러 발생 시 빈 배열 전달
            callJavaScript("if (typeof setOHLCData === 'function') { setOHLCData([]); }");
        }
    }
    
    /**
     * R:R 비율 계산
     */
    private void calculateRiskReward() {
        try {
            double entryPrice = Double.parseDouble(etEntryPrice.getText().toString());
            double takeProfit = Double.parseDouble(etTakeProfit.getText().toString());
            double stopLoss = Double.parseDouble(etStopLoss.getText().toString());
            
            boolean isLong = viewModel.getIsLong().getValue() != null && viewModel.getIsLong().getValue();
            
            double risk, reward;
            if (isLong) {
                risk = entryPrice - stopLoss;
                reward = takeProfit - entryPrice;
            } else {
                risk = stopLoss - entryPrice;
                reward = entryPrice - takeProfit;
            }
            
            if (risk > 0) {
                double rrRatio = reward / risk;
                tvRiskReward.setText(String.format(Locale.US, "%.2f", rrRatio));
                
                // 색상 설정
                if (rrRatio >= 2.0) {
                    tvRiskReward.setTextColor(ContextCompat.getColor(requireContext(), R.color.tds_success));
                } else if (rrRatio >= 1.0) {
                    tvRiskReward.setTextColor(ContextCompat.getColor(requireContext(), R.color.tds_warning));
                } else {
                    tvRiskReward.setTextColor(ContextCompat.getColor(requireContext(), R.color.tds_error));
                }
            } else {
                tvRiskReward.setText("-");
            }
        } catch (NumberFormatException e) {
            tvRiskReward.setText("-");
        }
    }
    
    /**
     * 진입가 업데이트
     */
    private void updateEntryPrice() {
        try {
            double price = Double.parseDouble(etEntryPrice.getText().toString());
            viewModel.updateEntryPrice(price);
            callJavaScript("if (typeof setEntryPrice === 'function') { setEntryPrice(" + price + "); }");
        } catch (NumberFormatException e) {
            // 잘못된 입력 무시
        }
    }
    
    /**
     * 익절가 업데이트
     */
    private void updateTakeProfit() {
        try {
            double price = Double.parseDouble(etTakeProfit.getText().toString());
            viewModel.updateTakeProfit(price);
            callJavaScript("if (typeof setTakeProfit === 'function') { setTakeProfit(" + price + "); }");
        } catch (NumberFormatException e) {
            // 잘못된 입력 무시
        }
    }
    
    /**
     * 손절가 업데이트
     */
    private void updateStopLoss() {
        try {
            double price = Double.parseDouble(etStopLoss.getText().toString());
            viewModel.updateStopLoss(price);
            callJavaScript("if (typeof setStopLoss === 'function') { setStopLoss(" + price + "); }");
        } catch (NumberFormatException e) {
            // 잘못된 입력 무시
        }
    }
    
    /**
     * 포지션 열기
     */
    private void openPosition() {
        try {
            double quantity = Double.parseDouble(etQuantity.getText().toString());
            double entryPrice = Double.parseDouble(etEntryPrice.getText().toString());
            double takeProfit = Double.parseDouble(etTakeProfit.getText().toString());
            double stopLoss = Double.parseDouble(etStopLoss.getText().toString());
            
            Position position = new Position();
            position.setUserId(1);
            position.setSymbol(viewModel.getSelectedCoinId().getValue());
            position.setQuantity(quantity);
            position.setEntryPrice(entryPrice);
            position.setTakeProfit(takeProfit);
            position.setStopLoss(stopLoss);
            position.setLong(viewModel.getIsLong().getValue());
            
            // ViewModel을 통해 포지션 열기 (TradeViewModel 사용 필요)
            Toast.makeText(requireContext(), "포지션이 열렸습니다!", Toast.LENGTH_SHORT).show();
            
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "모든 필드를 올바르게 입력하세요", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * JavaScript 호출 (TradingView 공식 예제 베스트 프랙티스)
     * 안전성 체크 및 에러 처리 포함
     */
    private void callJavaScript(String script) {
        if (chartWebView == null || getActivity() == null || !isAdded()) {
            android.util.Log.w("ChartFragment", "Cannot call JavaScript: WebView or Fragment not ready");
            return;
        }
        
        try {
            // 디버깅: setOHLCData 호출인 경우 로그 출력
            if (script.contains("setOHLCData")) {
                android.util.Log.d("ChartFragment", "Calling setOHLCData, script length: " + script.length());
            }
            
            chartWebView.post(() -> {
                if (chartWebView != null) {
                    chartWebView.evaluateJavascript(script, value -> {
                        if (script.contains("setOHLCData") && value != null) {
                            android.util.Log.d("ChartFragment", "setOHLCData JavaScript result: " + value);
                        }
                        // JavaScript 실행 결과 로깅 (디버깅용)
                        if (value != null && !value.equals("null")) {
                            android.util.Log.d("ChartFragment", "JavaScript result: " + value);
                        }
                    });
                }
            });
        } catch (Exception e) {
            android.util.Log.e("ChartFragment", "Error calling JavaScript", e);
        }
    }
    
    // ChartCallback 구현
    @Override
    public void onPriceChanged(double price) {
        requireActivity().runOnUiThread(() -> {
            tvCurrentPrice.setText("$" + String.format(Locale.US, "%.2f", price));
        });
    }
    
    @Override
    public void onEntryPriceChanged(double price) {
        requireActivity().runOnUiThread(() -> {
            etEntryPrice.setText(String.valueOf(price));
            viewModel.updateEntryPrice(price);
        });
    }
    
    @Override
    public void onTakeProfitChanged(double price) {
        requireActivity().runOnUiThread(() -> {
            etTakeProfit.setText(String.valueOf(price));
            viewModel.updateTakeProfit(price);
        });
    }
    
    @Override
    public void onStopLossChanged(double price) {
        requireActivity().runOnUiThread(() -> {
            etStopLoss.setText(String.valueOf(price));
            viewModel.updateStopLoss(price);
        });
    }
    
    /**
     * Fragment 생명주기 관리 (TradingView 공식 예제 베스트 프랙티스)
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // WebView 정리 (메모리 누수 방지)
        if (chartWebView != null) {
            chartWebView.loadUrl("about:blank");
            chartWebView.clearHistory();
            chartWebView.clearCache(true);
            chartWebView.onPause();
            chartWebView.removeJavascriptInterface("Android");
            chartWebView.destroy();
            chartWebView = null;
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        if (chartWebView != null) {
            chartWebView.onPause();
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (chartWebView != null) {
            chartWebView.onResume();
        }
    }
}

