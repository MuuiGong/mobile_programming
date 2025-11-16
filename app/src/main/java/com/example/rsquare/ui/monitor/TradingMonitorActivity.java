package com.example.rsquare.ui.monitor;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rsquare.R;
import com.example.rsquare.data.local.entity.Position;
import com.example.rsquare.data.repository.TradingRepository;
import com.example.rsquare.ui.adapter.ActivePositionAdapter;

import java.util.List;

/**
 * 실시간 트레이딩 모니터링 Activity
 */
public class TradingMonitorActivity extends AppCompatActivity {
    
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
    
    private TradingRepository tradingRepository;
    private Handler updateHandler = new Handler(Looper.getMainLooper());
    private static final long UPDATE_INTERVAL_MS = 1000;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trading_monitor);
        
        tradingRepository = new TradingRepository(this);
        
        initViews();
        setupRecyclerView();
        setupListeners();
        startRealtimeUpdates();
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
