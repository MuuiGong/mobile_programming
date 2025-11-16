package com.example.rsquare.ui.portfolio;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rsquare.R;
import com.example.rsquare.data.repository.TradingRepository;
import com.example.rsquare.ui.adapter.ActivePositionAdapter;
import com.example.rsquare.util.NumberFormatter;

/**
 * 포트폴리오 관리 Activity
 */
public class PortfolioActivity extends AppCompatActivity {
    
    private TradingRepository tradingRepository;
    
    private TextView totalAssets;
    private TextView totalPnlPortfolio;
    private TextView volatility;
    private TextView portfolioRiskScore;
    private RecyclerView assetAllocationRecycler;
    private RecyclerView positionsRecycler;
    
    private ActivePositionAdapter positionAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_portfolio);
        
        tradingRepository = new TradingRepository(this);
        
        initViews();
        setupToolbar();
        setupRecyclerView();
        loadPortfolioData();
    }
    
    private void initViews() {
        totalAssets = findViewById(R.id.total_assets);
        totalPnlPortfolio = findViewById(R.id.total_pnl_portfolio);
        volatility = findViewById(R.id.volatility);
        portfolioRiskScore = findViewById(R.id.portfolio_risk_score);
        assetAllocationRecycler = findViewById(R.id.asset_allocation_recycler);
        positionsRecycler = findViewById(R.id.positions_recycler);
    }
    
    private void setupToolbar() {
        // Toolbar는 XML에 없으므로 ActionBar만 설정
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("포트폴리오");
        }
    }
    
    private void setupRecyclerView() {
        assetAllocationRecycler.setLayoutManager(new LinearLayoutManager(this));
        positionsRecycler.setLayoutManager(new LinearLayoutManager(this));
        positionAdapter = new ActivePositionAdapter();
        positionsRecycler.setAdapter(positionAdapter);
    }
    
    private void loadPortfolioData() {
        // 샘플 데이터 설정
        totalAssets.setText("$10,250.55");
        totalPnlPortfolio.setText(NumberFormatter.formatPnL(250.55));
        totalPnlPortfolio.setTextColor(getColor(R.color.tds_success_alt));
        volatility.setText("15.2%");
        volatility.setTextColor(getColor(R.color.tds_warning_alt));
        portfolioRiskScore.setText("68/100");
        portfolioRiskScore.setTextColor(getColor(R.color.risk_caution));
        
        // 활성 포지션 로드
        new Thread(() -> {
            var positions = tradingRepository.getActivePositionsSync(1);
            runOnUiThread(() -> {
                positionAdapter.setPositions(positions);
            });
        }).start();
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

