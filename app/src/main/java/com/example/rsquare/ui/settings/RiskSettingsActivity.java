package com.example.rsquare.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import com.example.rsquare.R;

/**
 * 고급 리스크 관리 설정 Activity
 */
public class RiskSettingsActivity extends AppCompatActivity {
    
    private EditText dailyLossLimit;
    private SwitchCompat enableDailyLossLimit;
    private EditText maxLossPercentage;
    private SwitchCompat enableMaxLossPercentage;
    private EditText targetProfit;
    private SwitchCompat enableTargetProfit;
    private EditText cooldownDuration;
    private SwitchCompat enableCooldown;
    private Button btnSaveRiskSettings;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_risk_settings);
        
        initViews();
        setupToolbar();
        setupListeners();
        loadSettings();
    }
    
    private void initViews() {
        dailyLossLimit = findViewById(R.id.daily_loss_limit);
        enableDailyLossLimit = findViewById(R.id.enable_daily_loss_limit);
        maxLossPercentage = findViewById(R.id.max_loss_percentage);
        enableMaxLossPercentage = findViewById(R.id.enable_max_loss_percentage);
        targetProfit = findViewById(R.id.target_profit);
        enableTargetProfit = findViewById(R.id.enable_target_profit);
        cooldownDuration = findViewById(R.id.cooldown_duration);
        enableCooldown = findViewById(R.id.enable_cooldown);
        btnSaveRiskSettings = findViewById(R.id.btn_save_risk_settings);
    }
    
    private void setupToolbar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("고급 리스크 관리");
        }
    }
    
    private void setupListeners() {
        btnSaveRiskSettings.setOnClickListener(v -> {
            if (validateInputs()) {
                saveSettings();
                Toast.makeText(this, "설정이 저장되었습니다", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
    
    private boolean validateInputs() {
        if (enableDailyLossLimit.isChecked() && dailyLossLimit.getText().toString().isEmpty()) {
            Toast.makeText(this, "일일 손실 한도를 입력해주세요", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (enableMaxLossPercentage.isChecked() && maxLossPercentage.getText().toString().isEmpty()) {
            Toast.makeText(this, "최대 손실률을 입력해주세요", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (enableTargetProfit.isChecked() && targetProfit.getText().toString().isEmpty()) {
            Toast.makeText(this, "목표 수익을 입력해주세요", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (enableCooldown.isChecked() && cooldownDuration.getText().toString().isEmpty()) {
            Toast.makeText(this, "쿨다운 시간을 입력해주세요", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
    
    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences("r2_prefs", MODE_PRIVATE);
        dailyLossLimit.setText(String.valueOf(prefs.getFloat("daily_loss_limit", 500f)));
        enableDailyLossLimit.setChecked(prefs.getBoolean("enable_daily_loss_limit", false));
        maxLossPercentage.setText(String.valueOf(prefs.getFloat("max_loss_percentage", 10f)));
        enableMaxLossPercentage.setChecked(prefs.getBoolean("enable_max_loss_percentage", false));
        targetProfit.setText(String.valueOf(prefs.getFloat("target_profit", 1000f)));
        enableTargetProfit.setChecked(prefs.getBoolean("enable_target_profit", false));
        cooldownDuration.setText(String.valueOf(prefs.getInt("cooldown_duration", 60)));
        enableCooldown.setChecked(prefs.getBoolean("enable_cooldown", false));
    }
    
    private void saveSettings() {
        SharedPreferences prefs = getSharedPreferences("r2_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        if (!dailyLossLimit.getText().toString().isEmpty()) {
            editor.putFloat("daily_loss_limit", Float.parseFloat(dailyLossLimit.getText().toString()));
        }
        editor.putBoolean("enable_daily_loss_limit", enableDailyLossLimit.isChecked());
        
        if (!maxLossPercentage.getText().toString().isEmpty()) {
            editor.putFloat("max_loss_percentage", Float.parseFloat(maxLossPercentage.getText().toString()));
        }
        editor.putBoolean("enable_max_loss_percentage", enableMaxLossPercentage.isChecked());
        
        if (!targetProfit.getText().toString().isEmpty()) {
            editor.putFloat("target_profit", Float.parseFloat(targetProfit.getText().toString()));
        }
        editor.putBoolean("enable_target_profit", enableTargetProfit.isChecked());
        
        if (!cooldownDuration.getText().toString().isEmpty()) {
            editor.putInt("cooldown_duration", Integer.parseInt(cooldownDuration.getText().toString()));
        }
        editor.putBoolean("enable_cooldown", enableCooldown.isChecked());
        
        editor.apply();
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

