package com.example.rsquare.ui.liquidation;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.rsquare.R;
import com.example.rsquare.data.local.entity.Position;
import com.example.rsquare.data.repository.TradingRepository;
import com.example.rsquare.data.repository.UserRepository;
import com.example.rsquare.domain.LiquidationEngine;
import com.example.rsquare.domain.MarginCalculator;
import com.example.rsquare.util.NumberFormatter;

import java.util.Locale;

/**
 * 청산 경고 Activity
 * 마진 부족 시 표시되는 경고 화면
 */
public class LiquidationWarningActivity extends AppCompatActivity {
    
    public static final String EXTRA_POSITION_ID = "position_id";
    
    private TradingRepository tradingRepository;
    private LiquidationEngine liquidationEngine;
    private Position position;
    
    // Views
    private Toolbar toolbar;
    private TextView marginRatioText;
    private TextView liquidationPriceText;
    private TextView currentPriceText;
    private Button btnEmergencyClose;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_liquidation_warning);
        
        tradingRepository = new TradingRepository(this);
        UserRepository userRepository = new UserRepository(this);
        liquidationEngine = new LiquidationEngine(tradingRepository, userRepository);
        
        initViews();
        setupToolbar();
        loadPosition();
        setupListeners();
    }
    
    /**
     * View 초기화
     */
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        marginRatioText = findViewById(R.id.margin_ratio_alert);
        liquidationPriceText = findViewById(R.id.liquidation_price_alert);
        currentPriceText = findViewById(R.id.current_price_alert);
        btnEmergencyClose = findViewById(R.id.btn_emergency_close);
    }
    
    /**
     * Toolbar 설정
     */
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("마진콜 경고");
        }
    }
    
    /**
     * 포지션 로드
     */
    private void loadPosition() {
        long positionId = getIntent().getLongExtra(EXTRA_POSITION_ID, -1);
        if (positionId == -1) {
            finish();
            return;
        }
        
        // 포지션 조회 (동기)
        position = tradingRepository.getPositionByIdSync(positionId);
        if (position == null || position.isClosed()) {
            finish();
            return;
        }
        
        updateUI();
    }
    
    /**
     * UI 업데이트
     */
    private void updateUI() {
        if (position == null) return;
        
        // 현재 가격 (임시로 진입가 사용, 실제로는 실시간 가격 필요)
        double currentPrice = position.getEntryPrice(); // TODO: 실시간 가격 가져오기
        
        // 사용 마진 계산
        double usedMargin = MarginCalculator.calculateUsedMargin(
            position.getEntryPrice(),
            position.getQuantity(),
            position.getLeverage()
        );
        
        // 미실현 손익 계산
        double unrealizedPnL = position.calculateUnrealizedPnL(currentPrice);
        
        // 가용 마진 계산
        double availableMargin = MarginCalculator.calculateAvailableMargin(
            usedMargin,
            usedMargin,
            unrealizedPnL
        );
        
        // 마진 비율 계산
        double marginRatio = MarginCalculator.calculateMarginRatio(availableMargin, usedMargin);
        
        // 청산 가격 계산
        double liquidationPrice = MarginCalculator.calculateLiquidationPrice(
            position.getEntryPrice(),
            position.getQuantity(),
            position.getLeverage(),
            usedMargin,
            position.isLong()
        );
        
        // UI 업데이트
        marginRatioText.setText(String.format(Locale.US, "%.1f%%", marginRatio));
        if (marginRatio <= 20) {
            marginRatioText.setTextColor(getColor(R.color.risk_danger));
        } else if (marginRatio <= 50) {
            marginRatioText.setTextColor(getColor(R.color.risk_caution));
        } else {
            marginRatioText.setTextColor(getColor(R.color.risk_safe));
        }
        
        liquidationPriceText.setText(NumberFormatter.formatPrice(liquidationPrice));
        currentPriceText.setText(NumberFormatter.formatPrice(currentPrice));
    }
    
    /**
     * 리스너 설정
     */
    private void setupListeners() {
        btnEmergencyClose.setOnClickListener(v -> {
            if (position != null) {
                // 긴급 포지션 종료
                double currentPrice = position.getEntryPrice(); // TODO: 실시간 가격
                liquidationEngine.executeLiquidation(position, currentPrice, "EMERGENCY_CLOSE");
                finish();
            }
        });
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

