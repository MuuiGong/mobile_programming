package com.example.rsquare.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.rsquare.ui.BaseActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rsquare.R;
import com.example.rsquare.data.local.entity.Position;
import com.example.rsquare.data.local.entity.User;
import com.example.rsquare.domain.RiskMetrics;
import com.example.rsquare.ui.trading.TradingActivity;
import com.example.rsquare.util.NumberFormatter;

import java.util.List;
import java.util.Locale;

/**
 * 대시보드 Activity
 * 제안서의 activity_main_dashboard.xml 레이아웃 사용
 */
public class DashboardActivity extends BaseActivity {
    
    private DashboardViewModel viewModel;
    
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
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_dashboard);
        
        // ViewModel 초기화
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        
        initViews();
        setupRecyclerView();
        setupListeners();
        setupObservers();
        
        // 데이터 새로고침
        viewModel.refresh();
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
        // Adapter는 나중에 구현
    }
    
    /**
     * 리스너 설정
     */
    private void setupListeners() {
        btnNewTrade.setOnClickListener(v -> {
            Intent intent = new Intent(this, TradingActivity.class);
            startActivity(intent);
        });
        
        btnHistory.setOnClickListener(v -> {
            Toast.makeText(this, "기록 화면으로 이동", Toast.LENGTH_SHORT).show();
            // TODO: 기록 화면으로 이동
        });
        
        btnAnalysis.setOnClickListener(v -> {
            Toast.makeText(this, "분석 화면으로 이동", Toast.LENGTH_SHORT).show();
            // TODO: 분석 화면으로 이동
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
        int activeCount = 0;
        for (Position position : positions) {
            if (!position.isClosed()) {
                activeCount++;
            }
        }
        
        activePositionsCount.setText(activeCount + "개");
        
        // TODO: RecyclerView Adapter 업데이트
    }
}

