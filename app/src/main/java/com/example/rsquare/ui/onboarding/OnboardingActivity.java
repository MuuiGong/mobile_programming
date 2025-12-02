package com.example.rsquare.ui.onboarding;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.rsquare.ui.BaseActivity;

import com.example.rsquare.R;
import com.example.rsquare.ui.MainActivity;
import com.example.rsquare.ui.onboarding.SurveyActivity;

/**
 * 온보딩 화면 Activity
 */
public class OnboardingActivity extends BaseActivity {
    
    private ViewPager2 viewPager;
    private Button btnNext;
    private Button btnSkip;
    private View[] indicators;
    
    private static final int TOTAL_SLIDES = 4;
    
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
        setupViewPager();
        setupListeners();
    }
    
    private void initViews() {
        viewPager = findViewById(R.id.view_pager);
        btnNext = findViewById(R.id.btn_next);
        btnSkip = findViewById(R.id.btn_skip);
        
        indicators = new View[]{
            findViewById(R.id.indicator_0),
            findViewById(R.id.indicator_1),
            findViewById(R.id.indicator_2),
            findViewById(R.id.indicator_3)
        };
    }
    
    private void setupViewPager() {
        OnboardingPagerAdapter adapter = new OnboardingPagerAdapter();
        viewPager.setAdapter(adapter);
        
        // Page change callback
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateIndicators(position);
                updateButtons(position);
            }
        });
    }
    
    private void setupListeners() {
        btnNext.setOnClickListener(v -> {
            int currentItem = viewPager.getCurrentItem();
            if (currentItem < TOTAL_SLIDES - 1) {
                viewPager.setCurrentItem(currentItem + 1);
            } else {
                // 마지막 슬라이드에서 시작하기
                completeOnboarding();
                startActivity(new Intent(this, SurveyActivity.class));
                finish();
            }
        });
        
        btnSkip.setOnClickListener(v -> {
            completeOnboarding();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
    
    private void updateIndicators(int position) {
        for (int i = 0; i < indicators.length; i++) {
            if (i == position) {
                indicators[i].setBackgroundResource(R.drawable.indicator_active);
                indicators[i].getLayoutParams().width = (int) (32 * getResources().getDisplayMetrics().density);
            } else {
                indicators[i].setBackgroundResource(R.drawable.indicator_inactive);
                indicators[i].getLayoutParams().width = (int) (8 * getResources().getDisplayMetrics().density);
            }
            indicators[i].requestLayout();
        }
    }
    
    private void updateButtons(int position) {
        if (position == TOTAL_SLIDES - 1) {
            btnNext.setText("시작하기");
            btnSkip.setVisibility(View.GONE);
        } else {
            btnNext.setText("다음");
            btnSkip.setVisibility(View.VISIBLE);
        }
    }
    
    private void completeOnboarding() {
        SharedPreferences prefs = getSharedPreferences("r2_prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("onboarding_completed", true).apply();
    }
}

