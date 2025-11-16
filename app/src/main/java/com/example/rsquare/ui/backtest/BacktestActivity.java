package com.example.rsquare.ui.backtest;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.example.rsquare.R;

/**
 * 백테스트 Activity
 */
public class BacktestActivity extends AppCompatActivity {
    
    private Spinner symbolSpinner;
    private EditText startDate;
    private EditText endDate;
    private EditText entryRules;
    private EditText exitRules;
    private Button btnRunBacktest;
    private CardView resultsCard;
    private TextView backtestReturn;
    private TextView backtestWinRate;
    private TextView backtestMdd;
    private TextView backtestTotalTrades;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backtest);
        
        initViews();
        setupToolbar();
        setupListeners();
    }
    
    private void initViews() {
        symbolSpinner = findViewById(R.id.symbol_spinner);
        startDate = findViewById(R.id.start_date);
        endDate = findViewById(R.id.end_date);
        entryRules = findViewById(R.id.entry_rules);
        exitRules = findViewById(R.id.exit_rules);
        btnRunBacktest = findViewById(R.id.btn_run_backtest);
        resultsCard = findViewById(R.id.results_card);
        backtestReturn = findViewById(R.id.backtest_return);
        backtestWinRate = findViewById(R.id.backtest_win_rate);
        backtestMdd = findViewById(R.id.backtest_mdd);
        backtestTotalTrades = findViewById(R.id.backtest_total_trades);
        
        // Spinner 설정
        String[] symbols = {"BTCUSDT", "ETHUSDT", "ADAUSDT", "SOLUSDT"};
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, symbols);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        symbolSpinner.setAdapter(adapter);
    }
    
    private void setupToolbar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("백테스트");
        }
    }
    
    private void setupListeners() {
        btnRunBacktest.setOnClickListener(v -> {
            // 입력 검증
            if (startDate.getText().toString().isEmpty() || endDate.getText().toString().isEmpty()) {
                Toast.makeText(this, "시작일과 종료일을 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (entryRules.getText().toString().isEmpty() || exitRules.getText().toString().isEmpty()) {
                Toast.makeText(this, "진입 규칙과 청산 규칙을 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 백테스트 실행 (시뮬레이션)
            runBacktest();
        });
    }
    
    private void runBacktest() {
        btnRunBacktest.setEnabled(false);
        btnRunBacktest.setText("백테스트 실행 중...");
        
        // 시뮬레이션 결과 (실제로는 백테스트 엔진 호출)
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            resultsCard.setVisibility(View.VISIBLE);
            backtestReturn.setText("+25.5%");
            backtestReturn.setTextColor(getColor(R.color.tds_success_alt));
            backtestWinRate.setText("60%");
            backtestWinRate.setTextColor(getColor(R.color.tds_success_alt));
            backtestMdd.setText("-8.2%");
            backtestMdd.setTextColor(getColor(R.color.tds_error_alt));
            backtestTotalTrades.setText("45회");
            
            btnRunBacktest.setEnabled(true);
            btnRunBacktest.setText("백테스트 실행");
            
            Toast.makeText(this, "백테스트 완료", Toast.LENGTH_SHORT).show();
        }, 2000);
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

