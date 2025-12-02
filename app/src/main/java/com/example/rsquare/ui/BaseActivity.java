package com.example.rsquare.ui;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.rsquare.R;

/**
 * Base Activity for all activities in the app
 * Provides common functionality like window insets handling
 */
public class BaseActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // setContentView 후에 자동으로 WindowInsets 설정
        if (findViewById(android.R.id.content) != null) {
            setupWindowInsets();
        }
    }
    
    /**
     * WindowInsets 설정 (상단 노치/카메라, 하단 네비게이션 바 여백 처리)
     * 하위 클래스에서 필요시 오버라이드하여 커스터마이징 가능
     */
    protected void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int navigationBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            
            // Toolbar/Header에 상단 여백 추가 (최소 4dp)
            View toolbar = findToolbar();
            if (toolbar != null) {
                int minPaddingTopDp = 4;
                int minPaddingTopPx = (int) (minPaddingTopDp * getResources().getDisplayMetrics().density);
                toolbar.setPadding(
                    toolbar.getPaddingLeft(),
                    minPaddingTopPx,
                    toolbar.getPaddingRight(),
                    toolbar.getPaddingBottom()
                );
            }
            
            // 하단 컨트롤 패널 찾기 (control_panel, bottom_sheet 등)
            View controlPanel = findBottomPanel();
            if (controlPanel != null) {
                ViewGroup.MarginLayoutParams params = 
                    (ViewGroup.MarginLayoutParams) controlPanel.getLayoutParams();
                if (params != null) {
                    int bottomMarginDp = 16; // 기본 마진 16dp
                    int bottomMarginPx = (int) (bottomMarginDp * getResources().getDisplayMetrics().density);
                    params.bottomMargin = navigationBarHeight + bottomMarginPx;
                    controlPanel.setLayoutParams(params);
                }
            }
            
            return insets;
        });
    }
    
    /**
     * 하단 패널 찾기 (하위 클래스에서 오버라이드 가능)
     */
    protected View findBottomPanel() {
        // 기본적으로 control_panel, bottom_sheet_coordinator 등을 찾으려고 시도
        View panel = findViewById(R.id.control_panel);
        // if (panel == null) {
        //    panel = findViewById(R.id.bottom_sheet_coordinator);
        // }
        return panel;
    }
    
    /**
     * Toolbar 찾기 (하위 클래스에서 오버라이드 가능)
     */
    protected View findToolbar() {
        // 기본적으로 Toolbar를 찾으려고 시도
        View toolbar = findViewById(R.id.toolbar);
        if (toolbar == null) {
            toolbar = findViewById(R.id.header);
        }
        return toolbar;
    }
}

