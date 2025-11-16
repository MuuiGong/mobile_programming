package com.example.rsquare.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rsquare.R;
import com.example.rsquare.data.local.entity.Position;
import com.example.rsquare.data.local.entity.User;
import com.example.rsquare.data.remote.model.CoinPrice;
import com.example.rsquare.data.repository.MarketDataRepository;
import com.example.rsquare.data.repository.TradingRepository;
import com.example.rsquare.data.repository.UserRepository;
import com.example.rsquare.domain.RiskMetrics;
import com.example.rsquare.ui.adapter.ActivePositionAdapter;
import com.example.rsquare.ui.dashboard.DashboardViewModel;
import com.example.rsquare.ui.trading.TradingActivity;
import com.example.rsquare.util.NumberFormatter;
import com.example.rsquare.worker.WorkManagerHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Main Activity
 * 제안서의 activity_main_dashboard.xml 레이아웃 사용
 */
public class MainActivity extends AppCompatActivity {
    
    private DashboardViewModel viewModel;
    private MarketDataRepository marketDataRepository;
    private TradingRepository tradingRepository;
    private UserRepository userRepository;
    
    // Views
    private TextView balanceText;
    private TextView netAssetText;
    private TextView changeText;
    private TextView riskScoreText;
    private ProgressBar riskGauge;
    private TextView activePositionsCount;
    private RecyclerView activePositionsRecycler;
    private TextView todayTradesCount;
    private TextView todayWinRate;
    private TextView todayPnl;
    
    // Buttons
    private Button btnNewTrade;
    private Button btnHistory;
    private Button btnAnalysis;
    
    // Adapter
    private ActivePositionAdapter positionAdapter;
    
    // 실시간 업데이트 Handler
    private Handler updateHandler;
    private Runnable updateRunnable;
    private static final long UPDATE_INTERVAL_MS = 1000; // 1초
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 온보딩 체크
        android.content.SharedPreferences prefs = getSharedPreferences("r2_prefs", MODE_PRIVATE);
        if (!prefs.getBoolean("onboarding_completed", false)) {
            startActivity(new android.content.Intent(this, com.example.rsquare.ui.onboarding.OnboardingActivity.class));
            finish();
            return;
        }
        
        setContentView(R.layout.activity_main_dashboard);
        
        // ViewModel 초기화
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        marketDataRepository = new MarketDataRepository();
        tradingRepository = new TradingRepository(this);
        userRepository = new UserRepository(this);
        
        initViews();
        setupRecyclerView();
        setupListeners();
        setupObservers();
        
        // 데이터 새로고침
        viewModel.refresh();
        
        // 트레이딩 모니터 워커 시작
        WorkManagerHelper.scheduleTradingMonitor(this);
        
        // 실시간 업데이트 시작
        startRealtimeUpdates();
    }
    
    /**
     * 실시간 업데이트 시작 (1초마다)
     */
    private void startRealtimeUpdates() {
        updateHandler = new Handler(Looper.getMainLooper());
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                // 데이터 새로고침
                viewModel.refresh();
                
                // 활성 포지션 가격 업데이트
                updateActivePositionsPrices();
                
                // 다음 업데이트 스케줄
                updateHandler.postDelayed(this, UPDATE_INTERVAL_MS);
            }
        };
        updateHandler.postDelayed(updateRunnable, UPDATE_INTERVAL_MS);
    }
    
    /**
     * 활성 포지션 가격 업데이트
     */
    private void updateActivePositionsPrices() {
        if (positionAdapter != null) {
            new Thread(() -> {
                List<Position> activePositions = tradingRepository.getActivePositionsSync(1);
                if (!activePositions.isEmpty()) {
                    runOnUiThread(() -> {
                        updateCurrentPrices(activePositions);
                    });
                }
            }).start();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 실시간 업데이트 중지
        if (updateHandler != null && updateRunnable != null) {
            updateHandler.removeCallbacks(updateRunnable);
        }
    }
    
    /**
     * View 초기화
     */
    private void initViews() {
        balanceText = findViewById(R.id.balance_text);
        netAssetText = findViewById(R.id.net_asset_text);
        changeText = findViewById(R.id.change_text);
        riskScoreText = findViewById(R.id.risk_score_text);
        riskGauge = findViewById(R.id.risk_gauge);
        activePositionsCount = findViewById(R.id.active_positions_count);
        activePositionsRecycler = findViewById(R.id.active_positions_recycler);
        todayTradesCount = findViewById(R.id.today_trades_count);
        todayWinRate = findViewById(R.id.today_win_rate);
        todayPnl = findViewById(R.id.today_pnl);
        
        btnNewTrade = findViewById(R.id.btn_new_trade);
        btnHistory = findViewById(R.id.btn_history);
        btnAnalysis = findViewById(R.id.btn_analysis);
    }
    
    /**
     * RecyclerView 설정
     */
    private void setupRecyclerView() {
        activePositionsRecycler.setLayoutManager(new LinearLayoutManager(this));
        positionAdapter = new ActivePositionAdapter();
        positionAdapter.setOnPositionClickListener(position -> {
            // 포지션 클릭 시 상세 화면으로 이동 (나중에 구현)
            // Intent intent = new Intent(this, PositionDetailActivity.class);
            // intent.putExtra("position_id", position.getId());
            // startActivity(intent);
        });
        positionAdapter.setOnPositionCloseListener((position, currentPrice) -> {
            // 포지션 종료 확인 다이얼로그
            closePosition(position, currentPrice);
        });
        activePositionsRecycler.setAdapter(positionAdapter);
    }
    
    /**
     * 포지션 종료
     */
    private void closePosition(Position position, double currentPrice) {
        // 확인 다이얼로그 표시
        double unrealizedPnL = position.calculateUnrealizedPnL(currentPrice);
        String message = String.format(Locale.US, 
            "%s 포지션을 현재 가격(%.2f)으로 종료하시겠습니까?\n\n예상 손익: %s",
            position.getSymbol(),
            currentPrice,
            NumberFormatter.formatPnL(unrealizedPnL));
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("포지션 종료 확인")
            .setMessage(message)
            .setPositiveButton("종료", (dialog, which) -> {
                // 포지션 종료 실행
                new Thread(() -> {
                    tradingRepository.closePosition(
                        position.getId(),
                        currentPrice,
                        com.example.rsquare.data.local.entity.TradeHistory.TradeType.CLOSE_SL,
                        pnl -> {
                            // 잔고 업데이트
                            userRepository.addToBalance(position.getUserId(), pnl);
                            
                            // UI 업데이트
                            runOnUiThread(() -> {
                                Toast.makeText(this, 
                                    "포지션이 종료되었습니다. 손익: " + NumberFormatter.formatPnL(pnl),
                                    Toast.LENGTH_SHORT).show();
                                
                                // 데이터 새로고침
                                viewModel.refresh();
                            });
                        }
                    );
                }).start();
            })
            .setNegativeButton("취소", null)
            .show();
    }
    
    /**
     * 리스너 설정
     */
    private void setupListeners() {
        btnNewTrade.setOnClickListener(v -> {
            // 자산 선택 화면으로 이동
            Intent intent = new Intent(this, com.example.rsquare.ui.asset.AssetSelectionActivity.class);
            startActivity(intent);
        });
        
        btnHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, com.example.rsquare.ui.history.HistoryActivity.class);
            startActivity(intent);
        });
        
        btnAnalysis.setOnClickListener(v -> {
            Intent intent = new Intent(this, com.example.rsquare.ui.analysis.AnalysisActivity.class);
            startActivity(intent);
        });
    }
    
    /**
     * Observer 설정
     */
    private void setupObservers() {
        // 사용자 정보 관찰
        viewModel.getCurrentUser().observe(this, user -> {
            if (user != null) {
                updateUserInfo(user);
            }
        });
        
        // 총 손익 관찰
        viewModel.getTotalPnL().observe(this, pnl -> {
            if (pnl != null) {
                updatePnL(pnl);
            }
        });
        
        // 리스크 메트릭스 관찰
        viewModel.getRiskMetrics().observe(this, metrics -> {
            if (metrics != null) {
                updateRiskScore(metrics);
            }
        });
        
        // 거래 통계 관찰
        viewModel.getTradeStatistics().observe(this, stats -> {
            if (stats != null) {
                updateTradeStatistics(stats);
            }
        });
        
        // 활성 포지션 관찰
        viewModel.getRecentPositions().observe(this, positions -> {
            if (positions != null) {
                updateActivePositions(positions);
            }
        });
    }
    
    /**
     * 사용자 정보 업데이트
     */
    private void updateUserInfo(User user) {
        double balance = user.getBalance();
        balanceText.setText(NumberFormatter.formatPrice(balance));
        
        // 순자산 계산 (잔고 + 미실현 손익)
        // TODO: 미실현 손익 계산 로직 추가
        double netAsset = balance;
        netAssetText.setText(NumberFormatter.formatPrice(netAsset));
    }
    
    /**
     * 손익 업데이트
     */
    private void updatePnL(double pnl) {
        if (pnl >= 0) {
            changeText.setText("+" + NumberFormatter.formatPrice(pnl) + 
                " (+" + String.format(Locale.US, "%.2f", (pnl / 10000.0) * 100) + "%)");
            changeText.setTextColor(getColor(R.color.tds_success_alt));
        } else {
            changeText.setText(NumberFormatter.formatPrice(pnl) + 
                " (" + String.format(Locale.US, "%.2f", (pnl / 10000.0) * 100) + "%)");
            changeText.setTextColor(getColor(R.color.tds_error_alt));
        }
    }
    
    /**
     * Risk Score 업데이트
     */
    private void updateRiskScore(RiskMetrics metrics) {
        int score = (int) metrics.getRiskScore();
        riskScoreText.setText(score + "/100");
        
        // Risk Score 색상 설정
        if (score >= 71) {
            riskScoreText.setTextColor(getColor(R.color.risk_safe));
        } else if (score >= 31) {
            riskScoreText.setTextColor(getColor(R.color.risk_caution));
        } else {
            riskScoreText.setTextColor(getColor(R.color.risk_danger));
        }
        
        // ProgressBar 업데이트
        riskGauge.setProgress(score);
        
        // ProgressBar 색상 동적 설정
        if (score >= 71) {
            riskGauge.getProgressDrawable().setColorFilter(
                getColor(R.color.risk_safe), android.graphics.PorterDuff.Mode.SRC_IN);
        } else if (score >= 31) {
            riskGauge.getProgressDrawable().setColorFilter(
                getColor(R.color.risk_caution), android.graphics.PorterDuff.Mode.SRC_IN);
        } else {
            riskGauge.getProgressDrawable().setColorFilter(
                getColor(R.color.risk_danger), android.graphics.PorterDuff.Mode.SRC_IN);
        }
    }
    
    /**
     * 거래 통계 업데이트
     */
    private void updateTradeStatistics(DashboardViewModel.TradeStatistics stats) {
        todayTradesCount.setText(stats.totalCount + "회");
        
        if (stats.totalCount > 0) {
            todayWinRate.setText(String.format(Locale.US, "%.0f%%", stats.winRate));
            if (stats.winRate >= 50) {
                todayWinRate.setTextColor(getColor(R.color.tds_success_alt));
            } else {
                todayWinRate.setTextColor(getColor(R.color.tds_error_alt));
            }
        } else {
            todayWinRate.setText("0%");
            todayWinRate.setTextColor(getColor(R.color.tds_text_secondary));
        }
        
        // totalPnL은 double (primitive)이므로 null 체크 불필요
        if (stats.totalPnL >= 0) {
            todayPnl.setText("+" + NumberFormatter.formatPrice(stats.totalPnL));
            todayPnl.setTextColor(getColor(R.color.tds_success_alt));
        } else {
            todayPnl.setText(NumberFormatter.formatPrice(stats.totalPnL));
            todayPnl.setTextColor(getColor(R.color.tds_error_alt));
        }
    }
    
    /**
     * 활성 포지션 업데이트
     */
    private void updateActivePositions(List<Position> positions) {
        // 활성 포지션만 필터링
        List<Position> activePositions = new ArrayList<>();
        for (Position position : positions) {
            if (!position.isClosed()) {
                activePositions.add(position);
            }
        }
        
        activePositionsCount.setText(activePositions.size() + "개");
        
        // RecyclerView Adapter 업데이트
        if (positionAdapter != null) {
            positionAdapter.setPositions(positions);
            
            // 현재 가격 가져오기
            if (!activePositions.isEmpty()) {
                updateCurrentPrices(activePositions);
            }
        }
    }
    
    /**
     * 현재 가격 업데이트
     */
    private void updateCurrentPrices(List<Position> activePositions) {
        // 심볼별로 그룹화
        Map<String, Position> symbolMap = new HashMap<>();
        for (Position position : activePositions) {
            symbolMap.put(position.getSymbol(), position);
        }
        
        // 각 심볼의 현재 가격 가져오기
        for (Map.Entry<String, Position> entry : symbolMap.entrySet()) {
            String symbol = entry.getKey();
            String coinId = getCoinIdFromSymbol(symbol);
            
            double currentPrice = entry.getValue().getEntryPrice(); // 기본값: 진입가
            
            if (coinId != null) {
                CoinPrice cachedPrice = marketDataRepository.getCachedPrice(coinId);
                if (cachedPrice != null && cachedPrice.getCurrentPrice() > 0) {
                    currentPrice = cachedPrice.getCurrentPrice();
                }
            }
            
            // 해당 심볼의 포지션들에 현재 가격 적용
            positionAdapter.setCurrentPrice(symbol, currentPrice);
        }
    }
    
    /**
     * 심볼을 CoinGecko ID로 변환
     */
    private String getCoinIdFromSymbol(String symbol) {
        // Binance 심볼 -> CoinGecko ID 매핑
        Map<String, String> symbolToCoinId = new HashMap<>();
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
}

