package com.example.rsquare.ui.onboarding;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.rsquare.R;
import com.example.rsquare.ui.MainActivity;
import com.example.rsquare.ui.onboarding.SurveyActivity;

/**
 * 온보딩 화면 Activity
 */
public class OnboardingActivity extends AppCompatActivity {
    
    private TextView titleText;
    private TextView descriptionText;
    private Button btnNext;
    private Button btnSkip;
    private View progressDot1, progressDot2, progressDot3;
    
    private int currentStep = 1;
    private static final int TOTAL_STEPS = 3;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);
        
        // 이미 온보딩을 완료한 경우 메인으로 이동
        SharedPreferences prefs = getSharedPreferences("r2_prefs", MODE_PRIVATE);
        if (prefs.getBoolean("onboarding_completed", false)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        
        initViews();
        setupListeners();
        updateStep(1);
    }
    
    private void initViews() {
        titleText = findViewById(R.id.onboarding_title);
        descriptionText = findViewById(R.id.onboarding_description);
        btnNext = findViewById(R.id.btn_next);
        btnSkip = findViewById(R.id.btn_skip);
        progressDot1 = findViewById(R.id.progress_dot_1);
        progressDot2 = findViewById(R.id.progress_dot_2);
        progressDot3 = findViewById(R.id.progress_dot_3);
    }
    
    private void setupListeners() {
        btnNext.setOnClickListener(v -> {
            if (currentStep < TOTAL_STEPS) {
                updateStep(currentStep + 1);
            } else {
                // 설문조사 화면으로 이동
                startActivity(new Intent(this, SurveyActivity.class));
            }
        });
        
        btnSkip.setOnClickListener(v -> {
            // 온보딩 완료 표시
            SharedPreferences prefs = getSharedPreferences("r2_prefs", MODE_PRIVATE);
            prefs.edit().putBoolean("onboarding_completed", true).apply();
            
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
    
    private void updateStep(int step) {
        currentStep = step;
        
        // Progress dots 업데이트
        progressDot1.setBackgroundTintList(getColorStateList(
            step >= 1 ? R.color.tds_blue_400 : R.color.tds_text_tertiary));
        progressDot2.setBackgroundTintList(getColorStateList(
            step >= 2 ? R.color.tds_blue_400 : R.color.tds_text_tertiary));
        progressDot3.setBackgroundTintList(getColorStateList(
            step >= 3 ? R.color.tds_blue_400 : R.color.tds_text_tertiary));
        
        // Content 업데이트
        switch (step) {
            case 1:
                titleText.setText("R²에 오신 것을 환영합니다");
                descriptionText.setText("R²는 수익률보다 리스크 관리 훈련에 초점을 둔 모의투자 앱입니다.\n\n실제 돈 없이도 거래 감각을 키우고, 위험을 관리하는 방법을 배울 수 있습니다.");
                btnNext.setText("다음");
                break;
            case 2:
                titleText.setText("실시간 리스크 모니터링");
                descriptionText.setText("모든 거래에서 실시간으로 위험도를 평가하고, 위험한 상황을 미리 감지합니다.\n\nRisk Score를 통해 안전한 거래를 유지하세요.");
                btnNext.setText("다음");
                break;
            case 3:
                titleText.setText("AI 코치와 함께 성장하세요");
                descriptionText.setText("거래 패턴을 분석하고 개선점을 제안하는 AI 코치가 함께합니다.\n\n반복되는 실수를 피하고, 더 나은 트레이더가 되세요.");
                btnNext.setText("시작하기");
                break;
        }
    }
}

