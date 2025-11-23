package com.example.rsquare.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.rsquare.ui.BaseActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.example.rsquare.R;

/**
 * 대시보드 설정 Activity
 */
public class DashboardSettingsActivity extends BaseActivity {
    
    private CardView templateBeginner;
    private CardView templateAdvanced;
    private View templateBeginnerIndicator;
    private View templateAdvancedIndicator;
    private SwitchCompat widgetTotalPnl;
    private SwitchCompat widgetRiskScore;
    private SwitchCompat widgetMdd;
    private Button btnSaveSettings;
    
    private String selectedTemplate = "BEGINNER";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_settings);
        
        initViews();
        setupToolbar();
        setupListeners();
        loadSettings();
    }
    
    private void initViews() {
        templateBeginner = findViewById(R.id.template_beginner);
        templateAdvanced = findViewById(R.id.template_advanced);
        templateBeginnerIndicator = findViewById(R.id.template_beginner_indicator);
        templateAdvancedIndicator = findViewById(R.id.template_advanced_indicator);
        widgetTotalPnl = findViewById(R.id.widget_total_pnl);
        widgetRiskScore = findViewById(R.id.widget_risk_score);
        widgetMdd = findViewById(R.id.widget_mdd);
        btnSaveSettings = findViewById(R.id.btn_save_settings);
    }
    
    private void setupToolbar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("대시보드 설정");
        }
    }
    
    private void setupListeners() {
        templateBeginner.setOnClickListener(v -> {
            selectedTemplate = "BEGINNER";
            updateTemplateSelection();
        });
        
        templateAdvanced.setOnClickListener(v -> {
            selectedTemplate = "ADVANCED";
            updateTemplateSelection();
        });
        
        btnSaveSettings.setOnClickListener(v -> {
            saveSettings();
            Toast.makeText(this, "설정이 저장되었습니다", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
    
    private void updateTemplateSelection() {
        if ("BEGINNER".equals(selectedTemplate)) {
            templateBeginner.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(
                getColor(R.color.tds_blue_400)));
            templateAdvanced.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(
                getColor(R.color.tds_bg_neutral)));
            templateBeginnerIndicator.setVisibility(View.VISIBLE);
            templateAdvancedIndicator.setVisibility(View.GONE);
        } else {
            templateBeginner.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(
                getColor(R.color.tds_bg_neutral)));
            templateAdvanced.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(
                getColor(R.color.tds_blue_400)));
            templateBeginnerIndicator.setVisibility(View.GONE);
            templateAdvancedIndicator.setVisibility(View.VISIBLE);
        }
    }
    
    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences("r2_prefs", MODE_PRIVATE);
        selectedTemplate = prefs.getString("dashboard_template", "BEGINNER");
        widgetTotalPnl.setChecked(prefs.getBoolean("widget_total_pnl", true));
        widgetRiskScore.setChecked(prefs.getBoolean("widget_risk_score", true));
        widgetMdd.setChecked(prefs.getBoolean("widget_mdd", false));
        
        updateTemplateSelection();
    }
    
    private void saveSettings() {
        SharedPreferences prefs = getSharedPreferences("r2_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("dashboard_template", selectedTemplate);
        editor.putBoolean("widget_total_pnl", widgetTotalPnl.isChecked());
        editor.putBoolean("widget_risk_score", widgetRiskScore.isChecked());
        editor.putBoolean("widget_mdd", widgetMdd.isChecked());
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

