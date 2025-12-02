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
public class MainActivity extends BaseActivity {
    
    private DashboardViewModel viewModel;
    private MarketDataRepository marketDataRepository;
    private TradingRepository tradingRepository;
    private UserRepository userRepository;
    
    // Views
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
    private TextView btnCloseAll; // TextView로 변경 (레이아웃에 맞춤)
    private android.widget.ImageButton btnSettings;
    
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
        setupWindowInsets();
        setupPriceUpdateListener();
        
        // 데이터 새로고침
        viewModel.refreshData();
        
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
                viewModel.refreshData();
                
                // 활성 포지션 가격 업데이트
                updateActivePositionsPrices();
                
                // 다음 업데이트 스케줄
                updateHandler.postDelayed(this, UPDATE_INTERVAL_MS);
            }
        };
        updateHandler.postDelayed(updateRunnable, UPDATE_INTERVAL_MS);
    }
    
    // 현재 활성 포지션 리스트 (DB 쿼리 최소화용)
    private List<Position> currentActivePositions = new ArrayList<>();

    /**
     * 활성 포지션 가격 업데이트
     */
    private void updateActivePositionsPrices() {
        if (currentActivePositions == null || currentActivePositions.isEmpty()) {
            // 활성 포지션이 없으면 잔고만 표시
            updateNetAssetFromAdapter();
            return;
        }
        
        // 각 활성 포지션의 현재 가격을 가져와서 Adapter에 설정 및 TP/SL 체크
        for (Position position : currentActivePositions) {
            // 이미 종료된 포지션은 스킵
            if (position.isClosed()) {
                continue;
            }
            
            String symbol = position.getSymbol();
            String coinId = getCoinIdFromSymbol(symbol);
            
            if (coinId != null && positionAdapter != null) {
                // 1순위: Adapter에 이미 설정된 가격
                double currentPrice = positionAdapter.getCurrentPrice(symbol);
                
                // 2순위: 캐시에서 가져오기
                if (currentPrice <= 0) {
                    CoinPrice cachedPrice = marketDataRepository.getCachedPrice(coinId);
                    if (cachedPrice != null && cachedPrice.getCurrentPrice() > 0) {
                        currentPrice = cachedPrice.getCurrentPrice();
                        // Adapter에 설정
                        positionAdapter.setCurrentPrice(symbol, currentPrice);
                    }
                }
                
                // 3순위: 진입가 (가격 정보가 없을 때만)
                if (currentPrice <= 0) {
                    currentPrice = position.getEntryPrice();
                    positionAdapter.setCurrentPrice(symbol, currentPrice);
                }
                
                // TP/SL 도달 체크 (실시간)
                if (currentPrice > 0) {
                    checkTPAndSL(position, currentPrice);
                }
            }
        }
        
        // Adapter에서 직접 순자산 업데이트 (가격 변화에 직접 연동)
        updateNetAssetFromAdapter();
    }
    
    /**
     * TP/SL 도달 체크 및 자동 종료
     */
    private void checkTPAndSL(Position position, double currentPrice) {
        if (position.isClosed()) {
            return;
        }
        
        // TP 도달 체크
        if (position.isTakeProfitReached(currentPrice)) {
            android.util.Log.d("MainActivity", "TP reached for position " + position.getId() + 
                ", Current: " + currentPrice + ", TP: " + position.getTakeProfit());
            
            new Thread(() -> {
                tradingRepository.closePosition(
                    position.getId(),
                    currentPrice,
                    com.example.rsquare.data.local.entity.TradeHistory.TradeType.CLOSE_TP,
                    pnl -> {
                        runOnUiThread(() -> {
                            Toast.makeText(this, 
                                "익절! TP 도달 - 수익: " + NumberFormatter.formatPnL(pnl),
                                Toast.LENGTH_LONG).show();
                            
                            // 데이터 새로고침
                            viewModel.refreshData();
                        });
                    }
                );
            }).start();
            return;
        }
        
        // SL 도달 체크
        if (position.isStopLossReached(currentPrice)) {
            android.util.Log.d("MainActivity", "SL reached for position " + position.getId() + 
                ", Current: " + currentPrice + ", SL: " + position.getStopLoss());
            
            new Thread(() -> {
                tradingRepository.closePosition(
                    position.getId(),
                    currentPrice,
                    com.example.rsquare.data.local.entity.TradeHistory.TradeType.CLOSE_SL,
                    pnl -> {
                        runOnUiThread(() -> {
                            Toast.makeText(this, 
                                "손절! SL 도달 - 손실: " + NumberFormatter.formatPnL(pnl),
                                Toast.LENGTH_LONG).show();
                            
                            // 데이터 새로고침
                            viewModel.refreshData();
                        });
                    }
                );
            }).start();
            return;
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
        
        this.currentActivePositions = activePositions;
        activePositionsCount.setText(activePositions.size() + "개");
        
        // RecyclerView Adapter 업데이트
        if (positionAdapter != null) {
            positionAdapter.setPositions(positions);
        }
        
        // 활성 포지션이 있으면 초기 가격 로드 시도
        if (!activePositions.isEmpty()) {
            loadInitialPrices(activePositions);
            // 초기 가격 로드 후 순자산 업데이트
            updateNetAssetFromAdapter();
            // 모두 종료 버튼 표시
            if (btnCloseAll != null) {
                btnCloseAll.setVisibility(android.view.View.VISIBLE);
            }
        } else {
            // 활성 포지션이 없으면 잔고만 표시
            updateNetAssetFromAdapter();
            // 모두 종료 버튼 숨김
            if (btnCloseAll != null) {
                btnCloseAll.setVisibility(android.view.View.GONE);
            }
        }
    }
    
    /**
     * 초기 가격 로드 (캐시에서 가져오기)
     */
    private void loadInitialPrices(List<Position> activePositions) {
        // 각 심볼의 가격을 캐시에서 가져와서 Adapter에 설정
        for (Position position : activePositions) {
            String symbol = position.getSymbol();
            String coinId = getCoinIdFromSymbol(symbol);
            
            if (coinId != null && positionAdapter != null) {
                CoinPrice cachedPrice = marketDataRepository.getCachedPrice(coinId);
                if (cachedPrice != null && cachedPrice.getCurrentPrice() > 0) {
                    // 캐시에 가격이 있으면 Adapter에 설정
                    positionAdapter.setCurrentPrice(symbol, cachedPrice.getCurrentPrice());
                    android.util.Log.d("MainActivity", 
                        String.format(Locale.US, "Loaded initial price from cache: %s = %.2f", 
                            symbol, cachedPrice.getCurrentPrice()));
                }
            }
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
        btnCloseAll = findViewById(R.id.btn_close_all);
        btnSettings = findViewById(R.id.btn_settings);
    }
    
    /**
     * RecyclerView 설정
     */
    private void setupRecyclerView() {
        activePositionsRecycler.setLayoutManager(new LinearLayoutManager(this));
        positionAdapter = new ActivePositionAdapter();
        positionAdapter.setOnPositionClickListener(position -> {
            // 포지션 클릭 시 상세 화면으로 이동
            Intent intent = new Intent(this, com.example.rsquare.ui.position.PositionDetailActivity.class);
            intent.putExtra("position_id", position.getId());
            startActivity(intent);
        });
        positionAdapter.setOnPositionCloseListener((position, currentPrice) -> {
            // 포지션 종료 확인 다이얼로그
            closePosition(position, currentPrice);
        });
        // 가격 업데이트 리스너: 가격이 변경될 때마다 순자산 업데이트
        positionAdapter.setOnPriceUpdateListener(() -> {
            updateNetAssetFromAdapter();
        });
        activePositionsRecycler.setAdapter(positionAdapter);
    }
    
    /**
     * 포지션 종료
     */
    /**
     * 포지션 종료
     */
    private void closePosition(Position position, double currentPrice) {
        // 커스텀 다이얼로그 레이아웃 인플레이트
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_confirm_close, null);
        
        // 뷰 참조
        android.widget.TextView messageText = dialogView.findViewById(R.id.dialog_message);
        android.widget.TextView priceText = dialogView.findViewById(R.id.dialog_price);
        android.widget.TextView pnlText = dialogView.findViewById(R.id.dialog_pnl);
        android.widget.Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        android.widget.Button btnConfirm = dialogView.findViewById(R.id.btn_confirm);
        
        // 데이터 설정
        double unrealizedPnL = position.calculateUnrealizedPnL(currentPrice);
        
        messageText.setText(String.format(Locale.US, 
            "%s 포지션을 현재 가격으로 종료하시겠습니까?", position.getSymbol()));
            
        priceText.setText(NumberFormatter.formatPrice(currentPrice));
        
        pnlText.setText(NumberFormatter.formatPnL(unrealizedPnL));
        int pnlColor = unrealizedPnL >= 0 ? 
            getColor(R.color.tds_success_alt) : getColor(R.color.tds_error_alt);
        pnlText.setTextColor(pnlColor);
        
        // 다이얼로그 생성
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setView(dialogView);
        
        final androidx.appcompat.app.AlertDialog dialog = builder.create();
        
        // 배경 투명하게 설정 (CardView의 둥근 모서리 적용을 위해)
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        
        // 버튼 리스너
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            // 포지션 종료 실행
            new Thread(() -> {
                tradingRepository.closePosition(
                    position.getId(),
                    currentPrice,
                    com.example.rsquare.data.local.entity.TradeHistory.TradeType.CLOSE_SL,
                    pnl -> {
                        // UI 업데이트
                        runOnUiThread(() -> {
                            Toast.makeText(this, 
                                "포지션이 종료되었습니다. 손익: " + NumberFormatter.formatPnL(pnl),
                                Toast.LENGTH_SHORT).show();
                            
                            // 데이터 새로고침
                            viewModel.refreshData();
                        });
                    }
                );
            }).start();
        });
        
        dialog.show();
    }
    
    /**
     * 모든 포지션 종료
     */
    private void closeAllPositions() {
        List<Position> positions = positionAdapter.getPositions();
        if (positions == null || positions.isEmpty()) {
            Toast.makeText(this, "종료할 활성 포지션이 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 총 예상 손익 계산
        double totalEstimatedPnL = 0;
        Map<String, Double> currentPrices = new java.util.HashMap<>();
        
        for (Position pos : positions) {
            double currentPrice = positionAdapter.getCurrentPrice(pos.getSymbol());
            if (currentPrice > 0) {
                currentPrices.put(pos.getSymbol(), currentPrice);
                totalEstimatedPnL += pos.calculateUnrealizedPnL(currentPrice);
            } else {
                // 현재 가격 정보가 없는 경우 진입가로 가정 (PnL 0)
                currentPrices.put(pos.getSymbol(), pos.getEntryPrice());
            }
        }
        
        // 커스텀 다이얼로그 레이아웃 인플레이트
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_confirm_close, null);
        
        // 뷰 참조
        android.widget.TextView titleText = dialogView.findViewById(R.id.dialog_title); // 제목 변경 필요
        android.widget.TextView messageText = dialogView.findViewById(R.id.dialog_message);
        android.widget.TextView priceLabel = dialogView.findViewById(R.id.price_label); // 라벨 변경 필요
        android.widget.TextView priceText = dialogView.findViewById(R.id.dialog_price);
        android.widget.TextView pnlText = dialogView.findViewById(R.id.dialog_pnl);
        android.widget.Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        android.widget.Button btnConfirm = dialogView.findViewById(R.id.btn_confirm);
        
        // 데이터 설정
        // dialog_confirm_close.xml의 ID들이 일부 다를 수 있으므로 확인 필요
        // 기존 dialog_confirm_close.xml에는 dialog_title ID가 없음. TextView가 직접 있음.
        // 레이아웃을 재사용하되 텍스트만 변경
        
        // 제목 변경 (직접 접근이 어려우면 레이아웃 수정 필요하지만, 여기서는 메시지로 처리)
        messageText.setText(String.format(Locale.US, 
            "총 %d개의 포지션을 모두 종료하시겠습니까?", positions.size()));
            
        // 가격 표시 대신 포지션 수 표시
        // priceLabel이 없으므로 priceText 앞의 텍스트를 변경할 수 없음.
        // 레이아웃 구조상 "종료 가격" 텍스트뷰를 찾아서 변경해야 함.
        // 하지만 ID가 없으므로 priceText에 정보를 담아서 표시
        
        priceText.setText(positions.size() + "개 포지션");
        
        pnlText.setText(NumberFormatter.formatPnL(totalEstimatedPnL));
        int pnlColor = totalEstimatedPnL >= 0 ? 
            getColor(R.color.tds_success_alt) : getColor(R.color.tds_error_alt);
        pnlText.setTextColor(pnlColor);
        
        btnConfirm.setText("모두 종료");
        btnConfirm.setTextColor(getColor(R.color.tds_error)); // 위험 색상
        
        // 다이얼로그 생성
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setView(dialogView);
        
        final androidx.appcompat.app.AlertDialog dialog = builder.create();
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            // 일괄 종료 실행
            new Thread(() -> {
                tradingRepository.closeAllPositions(1, currentPrices, totalPnL -> {
                    runOnUiThread(() -> {
                        Toast.makeText(this, 
                            "모든 포지션이 종료되었습니다. 총 손익: " + NumberFormatter.formatPnL(totalPnL),
                            Toast.LENGTH_SHORT).show();
                        viewModel.refreshData();
                    });
                });
            }).start();
        });
        
        dialog.show();
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
        
        btnCloseAll.setOnClickListener(v -> closeAllPositions());
        
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                Intent intent = new Intent(this, com.example.rsquare.ui.settings.RiskSettingsActivity.class);
                startActivity(intent);
            });
        }
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
        viewModel.getActivePositions().observe(this, positions -> {
            if (positions != null) {
                updateActivePositions(positions);
                updateWebSocketSubscription(positions);
            }
        });
    }
    
    /**
     * 실시간 가격 업데이트 리스너 설정
     */
    private void setupPriceUpdateListener() {
        marketDataRepository.setPriceUpdateListener(new MarketDataRepository.OnPriceUpdateListener() {
            @Override
            public void onPriceUpdate(String coinId, double price) {
                // UI 스레드에서 실행
                runOnUiThread(() -> {
                    // CoinGecko ID를 Binance 심볼로 변환
                    String symbol = getSymbolFromCoinId(coinId);
                    if (symbol != null && positionAdapter != null) {
                        // Adapter에 가격 업데이트 (리스너가 순자산 업데이트를 트리거함)
                        positionAdapter.setCurrentPrice(symbol, price);
                    }
                });
            }
            
            @Override
            public void onConnectionStatusChanged(boolean connected) {
                android.util.Log.d("MainActivity", "WebSocket connection status: " + connected);
            }
        });
    }
    
    /**
     * Adapter에서 순자산 업데이트 (활성 포지션 가격 변화에 직접 연동)
     */
    private void updateNetAssetFromAdapter() {
        if (positionAdapter == null) {
            // Adapter가 없으면 잔고만 표시
            User user = viewModel.getCurrentUser().getValue();
            double balance = 0.0;
            if (user != null) {
                balance = user.getBalance();
            }
            // user가 null이면 balance는 0.0
            updateNetAsset(balance, 0.0);
            return;
        }
        
        // Adapter에서 총 미실현 손익 가져오기
        double totalUnrealizedPnL = positionAdapter.getTotalUnrealizedPnL();
        // 총 사용 마진 가져오기
        double totalUsedMargin = positionAdapter.getTotalUsedMargin();
        
        // 잔고 가져오기
        User user = viewModel.getCurrentUser().getValue();
        double balance = 0.0;
        if (user != null) {
            balance = user.getBalance();
        }
        // user가 null이면 balance는 0.0
        
        // 디버깅 로그
        android.util.Log.d("MainActivity", String.format(Locale.US, 
            "updateNetAssetFromAdapter: balance=%.2f, margin=%.2f, pnl=%.2f, netAsset=%.2f", 
            balance, totalUsedMargin, totalUnrealizedPnL, balance + totalUsedMargin + totalUnrealizedPnL));
        
        // 순자산 업데이트 (잔고 + 마진 + PnL)
        updateNetAsset(balance + totalUsedMargin, totalUnrealizedPnL);
    }
    
    /**
     * 사용자 정보 업데이트
     */
    private void updateUserInfo(User user) {
        // 순자산 업데이트 (Adapter에서 직접 계산)
        updateNetAssetFromAdapter();
    }
    
    /**
     * 손익 업데이트
     */
    private void updatePnL(double pnl) {
        // 현재 잔고 가져오기 (포지션이 없으면 잔고 = 초기 자본)
        User user = viewModel.getCurrentUser().getValue();
        double balance = (user != null) ? user.getBalance() : 0.0;
        
        // 비율은 잔고 대비 손익
        double percentage = (balance > 0) ? (pnl / balance) * 100 : 0;
        
        if (pnl >= 0) {
            changeText.setText("+" + NumberFormatter.formatPrice(pnl) + 
                " (+" + String.format(Locale.US, "%.2f", percentage) + "%)");
            changeText.setTextColor(getColor(R.color.tds_success_alt));
        } else {
            changeText.setText(NumberFormatter.formatPrice(pnl) + 
                " (" + String.format(Locale.US, "%.2f", percentage) + "%)");
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
     * 현재 가격 업데이트
     */
    private void updateCurrentPrices(List<Position> activePositions) {
        // 활성 포지션이 없으면 미실현 손익은 0, 순자산 = 잔고
        if (activePositions == null || activePositions.isEmpty()) {
            // 잔고 가져오기
            User user = viewModel.getCurrentUser().getValue();
            double balance = 0.0;
            if (user != null) {
                balance = user.getBalance();
            }
            // user가 null이면 balance는 0.0
            updateNetAsset(balance, 0.0);
            // Adapter에도 빈 상태 알림
            if (positionAdapter != null) {
                positionAdapter.setPositions(new ArrayList<>());
            }
            return;
        }
        
        // 심볼별 현재 가격 저장 (같은 심볼의 여러 포지션을 위해)
        Map<String, Double> symbolPriceMap = new HashMap<>();
        
        // 먼저 각 심볼의 현재 가격을 가져옴
        for (Position position : activePositions) {
            String symbol = position.getSymbol();
            
            // 이미 가격을 가져온 심볼은 스킵
            if (symbolPriceMap.containsKey(symbol)) {
                continue;
            }
            
            // 1순위: Adapter에 이미 설정된 가격 (WebSocket 실시간 가격)
            double currentPrice = 0.0;
            if (positionAdapter != null) {
                double adapterPrice = positionAdapter.getCurrentPrice(symbol);
                if (adapterPrice > 0) {
                    currentPrice = adapterPrice;
                }
            }
            
            // 2순위: 캐시에서 가져오기 (MarketDataRepository의 캐시)
            if (currentPrice <= 0) {
                String coinId = getCoinIdFromSymbol(symbol);
                if (coinId != null) {
                    CoinPrice cachedPrice = marketDataRepository.getCachedPrice(coinId);
                    if (cachedPrice != null && cachedPrice.getCurrentPrice() > 0) {
                        currentPrice = cachedPrice.getCurrentPrice();
                    }
                }
            }
            
            // 3순위: 진입가 사용 (가격 정보가 없을 때만)
            if (currentPrice <= 0) {
                currentPrice = position.getEntryPrice();
            }
            
            symbolPriceMap.put(symbol, currentPrice);
        }
        
        // 해당 심볼의 포지션들에 현재 가격 적용 (Adapter에 반영)
        if (positionAdapter != null) {
            for (Map.Entry<String, Double> entry : symbolPriceMap.entrySet()) {
                positionAdapter.setCurrentPrice(entry.getKey(), entry.getValue());
            }
        }
        
        // 모든 활성 포지션의 미실현 손익 및 마진 합산
        double totalUnrealizedPnL = 0;
        double totalUsedMargin = 0;
        
        for (Position position : activePositions) {
            String symbol = position.getSymbol();
            double currentPrice = symbolPriceMap.get(symbol);
            
            // 각 포지션의 미실현 손익 합산
            double pnl = position.calculateUnrealizedPnL(currentPrice);
            totalUnrealizedPnL += pnl;
            
            // 마진 합산
            double positionSize = position.getEntryPrice() * position.getQuantity();
            totalUsedMargin += positionSize / position.getLeverage();
        }
        
        // 순자산 업데이트 (잔고 + 마진 + 미실현 손익)
        User user = viewModel.getCurrentUser().getValue();
        double balance = 0.0;
        if (user != null) {
            balance = user.getBalance();
        }
        // user가 null이면 balance는 0.0
        
        // 디버깅 로그
        android.util.Log.d("MainActivity", String.format(Locale.US, 
            "updateCurrentPrices: balance=%.2f, margin=%.2f, pnl=%.2f, netAsset=%.2f", 
            balance, totalUsedMargin, totalUnrealizedPnL, balance + totalUsedMargin + totalUnrealizedPnL));
        
        updateNetAsset(balance + totalUsedMargin, totalUnrealizedPnL);
    }
    
    /**
     * 순자산 업데이트
     */
    private void updateNetAsset(double balance, double totalUnrealizedPnL) {
        double netAsset = balance + totalUnrealizedPnL;
        netAssetText.setText(NumberFormatter.formatPrice(netAsset));
        
        // 디버깅 로그
        android.util.Log.d("MainActivity", String.format(Locale.US, 
            "updateNetAsset: balance=%.2f, totalUnrealizedPnL=%.2f, netAsset=%.2f", 
            balance, totalUnrealizedPnL, netAsset));
        
        // 미실현 손익이 있을 경우 색상 표시
        if (totalUnrealizedPnL > 0) {
            netAssetText.setTextColor(getColor(R.color.tds_success_alt));
        } else if (totalUnrealizedPnL < 0) {
            netAssetText.setTextColor(getColor(R.color.tds_error_alt));
        } else {
            netAssetText.setTextColor(getColor(R.color.tds_text_primary));
        }
        
        // 변화 텍스트 업데이트
        updatePnL(totalUnrealizedPnL);
    }
    
    /**
     * 순자산 업데이트 (오버로드 - 미실현 손익만 전달)
     */
    private void updateNetAsset(double totalUnrealizedPnL) {
        User user = viewModel.getCurrentUser().getValue();
        double balance = 0.0;
        if (user != null) {
            balance = user.getBalance();
        }
        updateNetAsset(balance, totalUnrealizedPnL);
    }
    
    /**
     * WebSocket 구독 업데이트
     */
    private void updateWebSocketSubscription(List<Position> positions) {
        List<String> coinIds = new ArrayList<>();
        for (Position position : positions) {
            String coinId = getCoinIdFromSymbol(position.getSymbol());
            if (coinId != null && !coinIds.contains(coinId)) {
                coinIds.add(coinId);
            }
        }
        
        if (!coinIds.isEmpty()) {
            // 1초봉 데이터도 함께 구독하여 정밀도 향상 (선택사항)
            marketDataRepository.startWebSocket(coinIds, "1m");
        } else {
            marketDataRepository.stopWebSocket();
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
    
    /**
     * CoinGecko ID를 Binance 심볼로 변환
     */
    private String getSymbolFromCoinId(String coinId) {
        // CoinGecko ID -> Binance 심볼 매핑
        Map<String, String> coinIdToSymbol = new HashMap<>();
        coinIdToSymbol.put("bitcoin", "BTCUSDT");
        coinIdToSymbol.put("ethereum", "ETHUSDT");
        coinIdToSymbol.put("cardano", "ADAUSDT");
        coinIdToSymbol.put("solana", "SOLUSDT");
        coinIdToSymbol.put("ripple", "XRPUSDT");
        coinIdToSymbol.put("polkadot", "DOTUSDT");
        coinIdToSymbol.put("dogecoin", "DOGEUSDT");
        coinIdToSymbol.put("avalanche-2", "AVAXUSDT");
        
        return coinIdToSymbol.get(coinId);
    }
}

