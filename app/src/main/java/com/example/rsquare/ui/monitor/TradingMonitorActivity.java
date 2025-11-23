package com.example.rsquare.ui.monitor;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rsquare.R;
import com.example.rsquare.data.local.entity.Position;
import com.example.rsquare.data.repository.TradingRepository;
import com.example.rsquare.ui.BaseActivity;
import com.example.rsquare.ui.adapter.ActivePositionAdapter;
import com.example.rsquare.ui.chart.ChartViewModel;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;

/**
 * 실시간 트레이딩 모니터링 Activity
 */
public class TradingMonitorActivity extends BaseActivity {
    
    private WebView monitorChart;
    private TextView symbolHeader;
    private TextView currentPriceHeader;
    private TextView entryPriceMonitor;
    private TextView unrealizedPnlMonitor;
    private TextView leverageMonitor;
    private TextView marginRatioMonitor;
    private TextView mddText;
    private TextView dailyPnlText;
    private RecyclerView positionsRecycler;
    private ActivePositionAdapter positionAdapter;
    private Button btnAdjustPosition;
    
    // 타임프레임 버튼들
    private MaterialButton btnTime1m, btnTime5m, btnTime15m, btnTime30m, btnTime1h, btnTime4h, btnTime1d;
    private String currentTimeframe = "1h";
    
    private TradingRepository tradingRepository;
    private ChartViewModel chartViewModel;
    private Handler updateHandler = new Handler(Looper.getMainLooper());
    private static final long UPDATE_INTERVAL_MS = 1000;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trading_monitor);
        
        tradingRepository = new TradingRepository(this);
        chartViewModel = new ViewModelProvider(this).get(ChartViewModel.class);
        
        initViews();
        setupRecyclerView();
        setupListeners();
        setupObservers();
        setupWindowInsets();
        startRealtimeUpdates();
        
        // 시장 데이터 로드 (WebSocket 연결 시작)
        chartViewModel.loadMarketData();
    }
    
    private void initViews() {
        monitorChart = findViewById(R.id.monitor_chart);
        symbolHeader = findViewById(R.id.symbol_header);
        currentPriceHeader = findViewById(R.id.current_price_header);
        entryPriceMonitor = findViewById(R.id.entry_price_monitor);
        unrealizedPnlMonitor = findViewById(R.id.unrealized_pnl_monitor);
        leverageMonitor = findViewById(R.id.leverage_monitor);
        marginRatioMonitor = findViewById(R.id.margin_ratio_monitor);
        mddText = findViewById(R.id.mdd_text);
        dailyPnlText = findViewById(R.id.daily_pnl_text);
        positionsRecycler = findViewById(R.id.positions_recycler);
        btnAdjustPosition = findViewById(R.id.btn_adjust_position);
        
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
        
        // WebView 설정
        monitorChart.getSettings().setJavaScriptEnabled(true);
        monitorChart.setWebViewClient(new WebViewClient());
        monitorChart.loadUrl("file:///android_asset/chart.html");
        
        // 기본값 설정
        symbolHeader.setText("BTCUSDT");
        currentPriceHeader.setText("$95,836.00");
    }
    
    private void setupRecyclerView() {
        positionsRecycler.setLayoutManager(new LinearLayoutManager(this));
        positionAdapter = new ActivePositionAdapter();
        positionAdapter.setOnPositionCloseListener((position, currentPrice) -> {
            // 포지션 종료 로직
            Toast.makeText(this, "포지션 종료 기능", Toast.LENGTH_SHORT).show();
        });
        positionsRecycler.setAdapter(positionAdapter);
        
        // 활성 포지션 로드
        loadActivePositions();
    }
    
    private void setupListeners() {
        btnAdjustPosition.setOnClickListener(v -> {
            Toast.makeText(this, "포지션 조정 화면으로 이동", Toast.LENGTH_SHORT).show();
            // TODO: 포지션 조정 화면 구현
        });
        
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
     * 타임프레임 변경
     */
    private void setTimeframe(String timeframe, MaterialButton button) {
        currentTimeframe = timeframe;
        setTimeframeButtonActive(button);
        
        // ViewModel에 타임프레임 변경 알림
        chartViewModel.setTimeframe(timeframe);
        
        // JavaScript에 타임프레임 변경 알림
        String jsCode = "if (typeof setTimeframe === 'function') { setTimeframe('" + timeframe + "'); }";
        monitorChart.post(() -> {
            if (monitorChart != null) {
                monitorChart.evaluateJavascript(jsCode, null);
            }
        });
    }
    
    private void loadActivePositions() {
        new Thread(() -> {
            List<Position> positions = tradingRepository.getActivePositionsSync(1);
            runOnUiThread(() -> {
                positionAdapter.setPositions(positions);
            });
        }).start();
    }
    
    private void startRealtimeUpdates() {
        Runnable updateRunnable = new Runnable() {
            @Override
            public void run() {
                // 실시간 데이터 업데이트
                updateUI();
                updateHandler.postDelayed(this, UPDATE_INTERVAL_MS);
            }
        };
        updateHandler.postDelayed(updateRunnable, UPDATE_INTERVAL_MS);
    }
    
    private void setupObservers() {
        // 실시간 가격 업데이트 관찰 (WebSocket)
        chartViewModel.getCurrentPrice().observe(this, price -> {
            if (price != null && price > 0) {
                android.util.Log.d("TradingMonitorActivity", "Current price updated: " + price);
                currentPriceHeader.setText("$" + String.format(Locale.US, "%.2f", price));
                
                // WebView 차트에도 가격 업데이트
                String jsCode = String.format(Locale.US,
                    "if (typeof setCurrentPrice === 'function') { " +
                    "setCurrentPrice(%f); }",
                    price
                );
                monitorChart.post(() -> {
                    if (monitorChart != null) {
                        monitorChart.evaluateJavascript(jsCode, null);
                    }
                });
            }
        });
    }
    
    private void updateUI() {
        // 실제로는 Repository에서 데이터를 가져와서 업데이트
        // 여기서는 간단히 구현
        loadActivePositions();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        updateHandler.removeCallbacksAndMessages(null);
    }
}
