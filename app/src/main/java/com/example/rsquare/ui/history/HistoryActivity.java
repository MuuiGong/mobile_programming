package com.example.rsquare.ui.history;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.rsquare.ui.BaseActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rsquare.R;
import com.example.rsquare.data.local.entity.Position;
import com.example.rsquare.data.repository.TradingRepository;
import com.example.rsquare.ui.adapter.TradeHistoryAdapter;
import com.example.rsquare.ui.dashboard.DashboardViewModel;
import com.example.rsquare.util.NumberFormatter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 기록 화면 Activity
 * 거래 이력 및 저널 조회
 */
public class HistoryActivity extends BaseActivity {
    
    private DashboardViewModel viewModel;
    private TradingRepository tradingRepository;
    
    // Views
    private Toolbar toolbar;
    private LinearLayout filterLayout;
    private Button btnToday, btnWeek, btnMonth, btnAll;
    private Button btnAllResult, btnProfit, btnLoss;
    private RecyclerView historyRecycler;
    private TextView emptyText;
    
    // Statistics
    private TextView totalTradesText;
    private TextView winRateText;
    private TextView totalPnLText;
    private TextView avgRRText;
    
    // Adapter
    private TradeHistoryAdapter adapter;
    
    // Filter state
    private String dateFilter = "ALL"; // TODAY, WEEK, MONTH, ALL
    private String resultFilter = "ALL"; // ALL, PROFIT, LOSS
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        
        // ViewModel 초기화
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        tradingRepository = new TradingRepository(this);
        
        initViews();
        setupToolbar();
        setupRecyclerView();
        setupFilters();
        setupObservers();
        
        // 데이터 새로고침
        viewModel.refresh();
        loadHistory();
    }
    
    /**
     * View 초기화
     */
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        filterLayout = findViewById(R.id.filter_layout);
        btnToday = findViewById(R.id.btn_today);
        btnWeek = findViewById(R.id.btn_week);
        btnMonth = findViewById(R.id.btn_month);
        btnAll = findViewById(R.id.btn_all);
        btnAllResult = findViewById(R.id.btn_all_result);
        btnProfit = findViewById(R.id.btn_profit);
        btnLoss = findViewById(R.id.btn_loss);
        historyRecycler = findViewById(R.id.history_recycler);
        emptyText = findViewById(R.id.empty_text);
        totalTradesText = findViewById(R.id.total_trades_text);
        winRateText = findViewById(R.id.win_rate_text);
        totalPnLText = findViewById(R.id.total_pnl_text);
        avgRRText = findViewById(R.id.avg_rr_text);
    }
    
    /**
     * Toolbar 설정
     */
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("거래 기록");
        }
    }
    
    /**
     * RecyclerView 설정
     */
    private void setupRecyclerView() {
        historyRecycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TradeHistoryAdapter();
        historyRecycler.setAdapter(adapter);
    }
    
    /**
     * 필터 설정
     */
    private void setupFilters() {
        // 날짜 필터
        btnToday.setOnClickListener(v -> {
            dateFilter = "TODAY";
            updateFilterButtons();
            loadHistory();
        });
        btnWeek.setOnClickListener(v -> {
            dateFilter = "WEEK";
            updateFilterButtons();
            loadHistory();
        });
        btnMonth.setOnClickListener(v -> {
            dateFilter = "MONTH";
            updateFilterButtons();
            loadHistory();
        });
        btnAll.setOnClickListener(v -> {
            dateFilter = "ALL";
            updateFilterButtons();
            loadHistory();
        });
        
        // 결과 필터
        btnAllResult.setOnClickListener(v -> {
            resultFilter = "ALL";
            updateResultFilterButtons();
            loadHistory();
        });
        btnProfit.setOnClickListener(v -> {
            resultFilter = "PROFIT";
            updateResultFilterButtons();
            loadHistory();
        });
        btnLoss.setOnClickListener(v -> {
            resultFilter = "LOSS";
            updateResultFilterButtons();
            loadHistory();
        });
        
        updateFilterButtons();
        updateResultFilterButtons();
    }
    
    /**
     * 필터 버튼 상태 업데이트
     */
    private void updateFilterButtons() {
        btnToday.setSelected("TODAY".equals(dateFilter));
        btnWeek.setSelected("WEEK".equals(dateFilter));
        btnMonth.setSelected("MONTH".equals(dateFilter));
        btnAll.setSelected("ALL".equals(dateFilter));
    }
    
    /**
     * 결과 필터 버튼 상태 업데이트
     */
    private void updateResultFilterButtons() {
        btnAllResult.setSelected("ALL".equals(resultFilter));
        btnProfit.setSelected("PROFIT".equals(resultFilter));
        btnLoss.setSelected("LOSS".equals(resultFilter));
    }
    
    /**
     * 거래 기록 로드
     */
    private void loadHistory() {
        new Thread(() -> {
            List<Position> allPositions = tradingRepository.getClosedPositionsSync(1);
            
            // 날짜 필터링
            List<Position> filteredByDate = filterByDate(allPositions);
            
            // 결과 필터링
            final List<Position> filtered = filterByResult(filteredByDate);
            
            // 통계 계산
            calculateStatistics(filtered);
            
            // UI 업데이트
            runOnUiThread(() -> {
                adapter.setTrades(filtered);
                if (filtered.isEmpty()) {
                    emptyText.setVisibility(View.VISIBLE);
                    historyRecycler.setVisibility(View.GONE);
                } else {
                    emptyText.setVisibility(View.GONE);
                    historyRecycler.setVisibility(View.VISIBLE);
                }
            });
        }).start();
    }
    
    /**
     * 날짜별 필터링
     */
    private List<Position> filterByDate(List<Position> positions) {
        if ("ALL".equals(dateFilter)) {
            return positions;
        }
        
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        
        List<Position> filtered = new ArrayList<>();
        for (Position pos : positions) {
            if (pos.getCloseTime() == null) continue;
            
            Date closeTime = pos.getCloseTime();
            boolean include = false;
            
            switch (dateFilter) {
                case "TODAY":
                    cal.setTime(now);
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    include = closeTime.after(cal.getTime());
                    break;
                case "WEEK":
                    cal.setTime(now);
                    cal.add(Calendar.DAY_OF_YEAR, -7);
                    include = closeTime.after(cal.getTime());
                    break;
                case "MONTH":
                    cal.setTime(now);
                    cal.add(Calendar.MONTH, -1);
                    include = closeTime.after(cal.getTime());
                    break;
            }
            
            if (include) {
                filtered.add(pos);
            }
        }
        
        return filtered;
    }
    
    /**
     * 결과별 필터링
     */
    private List<Position> filterByResult(List<Position> positions) {
        if ("ALL".equals(resultFilter)) {
            return positions;
        }
        
        List<Position> filtered = new ArrayList<>();
        for (Position pos : positions) {
            boolean include = false;
            if ("PROFIT".equals(resultFilter) && pos.getPnl() > 0) {
                include = true;
            } else if ("LOSS".equals(resultFilter) && pos.getPnl() < 0) {
                include = true;
            }
            
            if (include) {
                filtered.add(pos);
            }
        }
        
        return filtered;
    }
    
    /**
     * 통계 계산
     */
    private void calculateStatistics(List<Position> positions) {
        int totalCount = positions.size();
        int winCount = 0;
        double totalPnL = 0.0;
        double totalRR = 0.0;
        int rrCount = 0;
        
        for (Position pos : positions) {
            if (pos.getPnl() > 0) {
                winCount++;
            }
            totalPnL += pos.getPnl();
            
            // R:R 비율 계산 (간단히)
            if (pos.getTakeProfit() > 0 && pos.getStopLoss() > 0) {
                double tpDistance = Math.abs(pos.getTakeProfit() - pos.getEntryPrice());
                double slDistance = Math.abs(pos.getEntryPrice() - pos.getStopLoss());
                if (slDistance > 0) {
                    totalRR += tpDistance / slDistance;
                    rrCount++;
                }
            }
        }
        
        final int finalTotalCount = totalCount;
        final int finalWinCount = winCount;
        final double finalTotalPnL = totalPnL;
        final double finalAvgRR = rrCount > 0 ? totalRR / rrCount : 0.0;
        final double finalWinRate = totalCount > 0 ? ((double) winCount / totalCount) * 100 : 0.0;
        
        runOnUiThread(() -> {
            totalTradesText.setText(String.format(Locale.US, "%d회", finalTotalCount));
            winRateText.setText(String.format(Locale.US, "%.0f%%", finalWinRate));
            totalPnLText.setText(NumberFormatter.formatPrice(finalTotalPnL));
            avgRRText.setText(String.format(Locale.US, "%.2f:1", finalAvgRR));
            
            if (finalTotalPnL >= 0) {
                totalPnLText.setTextColor(getColor(R.color.tds_success_alt));
            } else {
                totalPnLText.setTextColor(getColor(R.color.tds_error_alt));
            }
            
            if (finalWinRate >= 50) {
                winRateText.setTextColor(getColor(R.color.tds_success_alt));
            } else {
                winRateText.setTextColor(getColor(R.color.tds_error_alt));
            }
        });
    }
    
    /**
     * Observer 설정
     */
    private void setupObservers() {
        // 종료된 포지션 목록 관찰
        viewModel.getRecentPositions().observe(this, positions -> {
            if (positions != null) {
                updateHistoryList(positions);
            }
        });
    }
    
    /**
     * 거래 기록 목록 업데이트
     */
    private void updateHistoryList(List<Position> positions) {
        // 종료된 포지션만 필터링
        int closedCount = 0;
        for (Position position : positions) {
            if (position.isClosed()) {
                closedCount++;
            }
        }
        
        if (closedCount == 0) {
            emptyText.setVisibility(android.view.View.VISIBLE);
            historyRecycler.setVisibility(android.view.View.GONE);
        } else {
            emptyText.setVisibility(android.view.View.GONE);
            historyRecycler.setVisibility(android.view.View.VISIBLE);
            // TODO: Adapter에 데이터 설정
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

