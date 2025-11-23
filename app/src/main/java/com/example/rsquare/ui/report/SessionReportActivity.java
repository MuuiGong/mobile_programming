package com.example.rsquare.ui.report;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.rsquare.ui.BaseActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rsquare.R;
import com.example.rsquare.data.repository.TradingRepository;
import com.example.rsquare.util.NumberFormatter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 세션 리포트 Activity
 */
public class SessionReportActivity extends BaseActivity {
    
    private TextView sessionDate;
    private TextView totalPnlReport;
    private TextView winRateReport;
    private TextView totalTradesReport;
    private TextView maxConsecutiveLoss;
    private ProgressBar avgRiskGauge;
    private TextView avgRiskScore;
    private TextView avgRiskLevel;
    private TextView riskyPositionSymbol;
    private TextView riskyRiskScore;
    private TextView riskyMarginRatio;
    private RecyclerView emotionalTradesRecycler;
    private TextView dailyFeedbackText;
    private Button btnViewDetails;
    private Button btnBackToDashboard;
    
    private TradingRepository tradingRepository;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_report);
        
        tradingRepository = new TradingRepository(this);
        
        initViews();
        setupListeners();
        loadReportData();
    }
    
    private void initViews() {
        sessionDate = findViewById(R.id.session_date);
        totalPnlReport = findViewById(R.id.total_pnl_report);
        winRateReport = findViewById(R.id.win_rate_report);
        totalTradesReport = findViewById(R.id.total_trades_report);
        maxConsecutiveLoss = findViewById(R.id.max_consecutive_loss);
        avgRiskGauge = findViewById(R.id.avg_risk_gauge);
        avgRiskScore = findViewById(R.id.avg_risk_score);
        avgRiskLevel = findViewById(R.id.avg_risk_level);
        riskyPositionSymbol = findViewById(R.id.risky_position_symbol);
        riskyRiskScore = findViewById(R.id.risky_risk_score);
        riskyMarginRatio = findViewById(R.id.risky_margin_ratio);
        emotionalTradesRecycler = findViewById(R.id.emotional_trades_recycler);
        dailyFeedbackText = findViewById(R.id.daily_feedback_text);
        btnViewDetails = findViewById(R.id.btn_view_details);
        btnBackToDashboard = findViewById(R.id.btn_back_to_dashboard);
        
        // RecyclerView 설정
        emotionalTradesRecycler.setLayoutManager(new LinearLayoutManager(this));
    }
    
    private void setupListeners() {
        btnViewDetails.setOnClickListener(v -> {
            // AnalysisActivity로 이동
            startActivity(new android.content.Intent(this, com.example.rsquare.ui.analysis.AnalysisActivity.class));
        });
        
        btnBackToDashboard.setOnClickListener(v -> {
            finish();
        });
    }
    
    private void loadReportData() {
        // 날짜 설정
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREAN);
        sessionDate.setText(sdf.format(new Date()));
        
        // 샘플 데이터 설정 (실제로는 Repository에서 가져와야 함)
        totalPnlReport.setText(NumberFormatter.formatPnL(250.55));
        totalPnlReport.setTextColor(getColor(R.color.tds_success_alt));
        
        winRateReport.setText("60%");
        winRateReport.setTextColor(getColor(R.color.tds_success_alt));
        
        totalTradesReport.setText("5회");
        
        maxConsecutiveLoss.setText("2회");
        maxConsecutiveLoss.setTextColor(getColor(R.color.tds_error_alt));
        
        // 평균 Risk Score
        int avgRisk = 75;
        avgRiskGauge.setProgress(avgRisk);
        avgRiskScore.setText(avgRisk + "/100");
        avgRiskScore.setTextColor(getColor(R.color.risk_safe));
        avgRiskLevel.setText("안정");
        avgRiskLevel.setTextColor(getColor(R.color.risk_safe));
        
        // 가장 위험했던 순간
        riskyPositionSymbol.setText("ETHUSDT");
        riskyRiskScore.setText("25/100");
        riskyRiskScore.setTextColor(getColor(R.color.risk_danger));
        riskyMarginRatio.setText("15.2%");
        riskyMarginRatio.setTextColor(getColor(R.color.tds_error_alt));
        
        // 오늘의 피드백
        dailyFeedbackText.setText("손실 이후 포지션 크기를 갑자기 키우는 경향이 있습니다. 일관된 포지션 크기를 유지하세요.");
    }
}
