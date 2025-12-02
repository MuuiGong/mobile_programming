package com.example.rsquare.ui.analysis;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import com.example.rsquare.ui.BaseActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.example.rsquare.R;
import com.example.rsquare.domain.RiskMetrics;
import com.example.rsquare.ui.coach.CoachViewModel;
import com.example.rsquare.ui.dashboard.DashboardViewModel;

import java.util.Locale;

/**
 * 분석 화면 Activity
 * 통계 분석 및 AI 코치 피드백
 */
public class AnalysisActivity extends BaseActivity {
    
    private DashboardViewModel dashboardViewModel;
    private CoachViewModel coachViewModel;
    
    // Views
    private Toolbar toolbar;
    private TextView riskScoreText;
    private TextView riskLevelText;
    private TextView totalTradesText;
    private TextView winRateText;
    private TextView totalPnLText;
    private TextView coachFeedbackText;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);
        
        // ViewModel 초기화
        dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        coachViewModel = new ViewModelProvider(this).get(CoachViewModel.class);
        
        initViews();
        setupToolbar();
        setupObservers();
        
        // 데이터 새로고침
        dashboardViewModel.refreshData();
        coachViewModel.analyzeTradingSession();
    }
    
    /**
     * View 초기화
     */
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        riskScoreText = findViewById(R.id.risk_score_text);
        riskLevelText = findViewById(R.id.risk_level_text);
        totalTradesText = findViewById(R.id.total_trades_text);
        winRateText = findViewById(R.id.win_rate_text);
        totalPnLText = findViewById(R.id.total_pnl_text);
        coachFeedbackText = findViewById(R.id.coach_feedback_text);
    }
    
    /**
     * Toolbar 설정
     */
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("거래 분석");
        }
    }
    
    /**
     * Observer 설정
     */
    private void setupObservers() {
        // 리스크 메트릭스 관찰
        dashboardViewModel.getRiskMetrics().observe(this, metrics -> {
            if (metrics != null) {
                updateRiskMetrics(metrics);
            }
        });
        
        // 거래 통계 관찰
        dashboardViewModel.getTradeStatistics().observe(this, stats -> {
            if (stats != null) {
                updateTradeStatistics(stats);
            }
        });
        
        // AI 코치 피드백 관찰
        coachViewModel.getCoachingMessages().observe(this, messages -> {
            if (messages != null && !messages.isEmpty()) {
                // 첫 번째 메시지의 텍스트 표시
                StringBuilder feedback = new StringBuilder();
                for (int i = 0; i < Math.min(messages.size(), 3); i++) {
                    if (i > 0) feedback.append("\n\n");
                    feedback.append(messages.get(i).getMessage());
                }
                coachFeedbackText.setText(feedback.toString());
            } else {
                coachFeedbackText.setText("아직 충분한 거래 데이터가 없습니다.\n더 많은 거래를 통해 분석을 받아보세요.");
            }
        });
    }
    
    /**
     * 리스크 메트릭스 업데이트
     */
    private void updateRiskMetrics(RiskMetrics metrics) {
        int score = (int) metrics.getRiskScore();
        riskScoreText.setText(String.format(Locale.US, "%d/100", score));
        
        String level;
        int color;
        if (score >= 71) {
            level = "안정";
            color = getColor(R.color.risk_safe);
        } else if (score >= 31) {
            level = "주의";
            color = getColor(R.color.risk_caution);
        } else {
            level = "위험";
            color = getColor(R.color.risk_danger);
        }
        
        riskLevelText.setText(level);
        riskLevelText.setTextColor(color);
        riskScoreText.setTextColor(color);
    }
    
    /**
     * 거래 통계 업데이트
     */
    private void updateTradeStatistics(DashboardViewModel.TradeStatistics stats) {
        totalTradesText.setText(String.format(Locale.US, "%d회", stats.totalCount));
        
        if (stats.totalCount > 0) {
            winRateText.setText(String.format(Locale.US, "%.1f%%", stats.winRate));
            if (stats.winRate >= 50) {
                winRateText.setTextColor(getColor(R.color.tds_success_alt));
            } else {
                winRateText.setTextColor(getColor(R.color.tds_error_alt));
            }
        } else {
            winRateText.setText("0%");
            winRateText.setTextColor(getColor(R.color.tds_text_secondary));
        }
        
        if (stats.totalPnL >= 0) {
            totalPnLText.setText("+" + com.example.rsquare.util.NumberFormatter.formatPrice(stats.totalPnL));
            totalPnLText.setTextColor(getColor(R.color.tds_success_alt));
        } else {
            totalPnLText.setText(com.example.rsquare.util.NumberFormatter.formatPrice(stats.totalPnL));
            totalPnLText.setTextColor(getColor(R.color.tds_error_alt));
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
}

