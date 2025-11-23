package com.example.rsquare.ui.export;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.example.rsquare.ui.BaseActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import com.example.rsquare.R;

/**
 * 데이터 내보내기 Activity
 */
public class ExportActivity extends BaseActivity {
    
    private SwitchCompat exportTradeHistory;
    private SwitchCompat exportSessionReports;
    private SwitchCompat exportRiskAnalysis;
    private RadioGroup formatRadioGroup;
    private EditText exportStartDate;
    private EditText exportEndDate;
    private Button btnExport;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);
        
        initViews();
        setupToolbar();
        setupListeners();
    }
    
    private void initViews() {
        exportTradeHistory = findViewById(R.id.export_trade_history);
        exportSessionReports = findViewById(R.id.export_session_reports);
        exportRiskAnalysis = findViewById(R.id.export_risk_analysis);
        formatRadioGroup = findViewById(R.id.format_radio_group);
        exportStartDate = findViewById(R.id.export_start_date);
        exportEndDate = findViewById(R.id.export_end_date);
        btnExport = findViewById(R.id.btn_export);
    }
    
    private void setupToolbar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("데이터 내보내기");
        }
    }
    
    private void setupListeners() {
        btnExport.setOnClickListener(v -> {
            // 선택된 데이터 확인
            if (!exportTradeHistory.isChecked() && !exportSessionReports.isChecked() && !exportRiskAnalysis.isChecked()) {
                Toast.makeText(this, "내보낼 데이터를 최소 하나 선택해주세요", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 날짜 확인
            if (exportStartDate.getText().toString().isEmpty() || exportEndDate.getText().toString().isEmpty()) {
                Toast.makeText(this, "시작일과 종료일을 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 형식 확인
            int selectedFormatId = formatRadioGroup.getCheckedRadioButtonId();
            String format = "CSV";
            if (selectedFormatId == R.id.format_excel) {
                format = "Excel";
            } else if (selectedFormatId == R.id.format_json) {
                format = "JSON";
            }
            
            // 내보내기 실행 (시뮬레이션)
            exportData(format);
        });
    }
    
    private void exportData(String format) {
        btnExport.setEnabled(false);
        btnExport.setText("내보내는 중...");
        
        // 시뮬레이션 (실제로는 파일 생성 및 저장)
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            Toast.makeText(this, 
                format + " 형식으로 데이터가 내보내졌습니다.\n(시뮬레이션)", 
                Toast.LENGTH_LONG).show();
            
            btnExport.setEnabled(true);
            btnExport.setText("내보내기");
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

