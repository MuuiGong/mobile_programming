package com.example.rsquare.ui.onboarding;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.rsquare.ui.BaseActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.example.rsquare.R;
import com.example.rsquare.ui.MainActivity;

/**
 * 온보딩 설문조사 Activity
 */
public class SurveyActivity extends BaseActivity {
    
    private ProgressBar progressBar;
    private TextView questionNumber;
    private TextView questionTitle;
    private LinearLayout optionsContainer;
    private Button btnBack;
    private Button btnNext;
    
    private int currentQuestion = 0;
    private int[] answers = new int[5];
    
    private final String[] questions = {
        "투자 경험이 얼마나 되셨나요?",
        "선호하는 거래 종목은 무엇인가요?",
        "리스크 성향을 자가 평가해주세요.",
        "일일 거래 목표 횟수는?",
        "가장 중요하게 생각하는 것은?"
    };
    
    private final String[][] options = {
        {"초보자 (1년 미만)", "중급자 (1-3년)", "고급자 (3년 이상)"},
        {"비트코인", "이더리움", "알트코인", "주식"},
        {"매우 보수적", "보수적", "중립", "공격적", "매우 공격적"},
        {"1-3회", "4-7회", "8-15회", "15회 이상"},
        {"수익률", "리스크 관리", "학습", "재미"}
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey);
        
        initViews();
        setupListeners();
        showQuestion(0);
    }
    
    private void initViews() {
        progressBar = findViewById(R.id.survey_progress);
        questionNumber = findViewById(R.id.question_number);
        questionTitle = findViewById(R.id.question_title);
        optionsContainer = findViewById(R.id.options_container);
        btnBack = findViewById(R.id.btn_back);
        btnNext = findViewById(R.id.btn_next_survey);
    }
    
    private void setupListeners() {
        btnBack.setOnClickListener(v -> {
            if (currentQuestion > 0) {
                showQuestion(currentQuestion - 1);
            } else {
                finish();
            }
        });
        
        btnNext.setOnClickListener(v -> {
            if (answers[currentQuestion] == 0) {
                // 선택하지 않음
                android.widget.Toast.makeText(this, "옵션을 선택해주세요", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (currentQuestion < questions.length - 1) {
                showQuestion(currentQuestion + 1);
            } else {
                // 설문 완료
                saveSurveyResults();
                completeOnboarding();
            }
        });
    }
    
    private void showQuestion(int questionIndex) {
        currentQuestion = questionIndex;
        
        // Progress 업데이트
        int progress = ((questionIndex + 1) * 100) / questions.length;
        progressBar.setProgress(progress);
        questionNumber.setText((questionIndex + 1) + " / " + questions.length);
        questionTitle.setText(questions[questionIndex]);
        
        // Options 업데이트
        optionsContainer.removeAllViews();
        String[] currentOptions = options[questionIndex];
        
        for (int i = 0; i < currentOptions.length; i++) {
            final int optionIndex = i + 1;
            final boolean isSelected = answers[questionIndex] == optionIndex;
            
            CardView cardView = new CardView(this);
            cardView.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(
                getColor(isSelected ? R.color.tds_blue_400 : R.color.tds_bg_dark)));
            cardView.setRadius(12f);
            cardView.setCardElevation(0);
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 12);
            cardView.setLayoutParams(params);
            
            LinearLayout innerLayout = new LinearLayout(this);
            innerLayout.setOrientation(LinearLayout.HORIZONTAL);
            innerLayout.setPadding(16, 16, 16, 16);
            innerLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
            innerLayout.setClickable(true);
            innerLayout.setFocusable(true);
            
            // selectableItemBackground를 위한 TypedValue 사용
            try {
                android.util.TypedValue typedValue = new android.util.TypedValue();
                if (getTheme().resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true) 
                    && typedValue.resourceId != 0) {
                    innerLayout.setBackgroundResource(typedValue.resourceId);
                } else {
                    // Fallback: 투명 배경 (클릭 효과는 CardView에서 처리)
                    innerLayout.setBackground(null);
                }
            } catch (Exception e) {
                // 오류 발생 시 배경 없음
                innerLayout.setBackground(null);
            }
            
            TextView optionText = new TextView(this);
            optionText.setText(currentOptions[i]);
            optionText.setTextSize(16);
            optionText.setTextColor(getColor(isSelected ? R.color.tds_text_primary : R.color.tds_text_primary));
            optionText.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
            ));
            
            View indicator = new View(this);
            indicator.setLayoutParams(new LinearLayout.LayoutParams(20, 20));
            indicator.setBackgroundResource(R.drawable.badge_background);
            indicator.getBackground().setTint(getColor(isSelected ? R.color.tds_text_primary : R.color.tds_text_tertiary));
            indicator.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            
            innerLayout.addView(optionText);
            innerLayout.addView(indicator);
            cardView.addView(innerLayout);
            
            innerLayout.setOnClickListener(v -> {
                answers[questionIndex] = optionIndex;
                showQuestion(questionIndex); // 다시 그리기
            });
            
            optionsContainer.addView(cardView);
        }
        
        // 버튼 상태 업데이트
        btnBack.setVisibility(questionIndex == 0 ? View.GONE : View.VISIBLE);
        btnNext.setText(questionIndex == questions.length - 1 ? "완료" : "다음");
        btnNext.setEnabled(answers[questionIndex] != 0);
    }
    
    private void saveSurveyResults() {
        SharedPreferences prefs = getSharedPreferences("r2_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        editor.putInt("experience_level", answers[0]);
        editor.putInt("preferred_asset", answers[1]);
        editor.putInt("risk_tolerance", answers[2]);
        editor.putInt("daily_trade_target", answers[3]);
        editor.putInt("priority", answers[4]);
        
        editor.apply();
    }
    
    private void completeOnboarding() {
        SharedPreferences prefs = getSharedPreferences("r2_prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("onboarding_completed", true).apply();
        
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
