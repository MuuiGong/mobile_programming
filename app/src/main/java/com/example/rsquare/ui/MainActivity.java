package com.example.rsquare.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.rsquare.R;
import com.example.rsquare.ui.challenge.ChallengeFragment;
import com.example.rsquare.ui.chart.ChartFragment;
import com.example.rsquare.ui.coach.CoachFragment;
import com.example.rsquare.ui.dashboard.DashboardFragment;
import com.example.rsquare.ui.journal.JournalFragment;
import com.example.rsquare.worker.WorkManagerHelper;

/**
 * Main Activity
 * 앱의 메인 화면 및 네비게이션 관리
 */
public class MainActivity extends AppCompatActivity {
    
    private LinearLayout bottomNavigationView;
    private LinearLayout navItemDashboard, navItemChart, navItemJournal, navItemChallenge, navItemCoach;
    private int selectedColor = Color.parseColor("#3182FF");
    private int unselectedColor = Color.parseColor("#787b86");
    private int currentSelectedId = R.id.nav_item_dashboard;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_tds);
        
        // Bottom Navigation 설정
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        navItemDashboard = findViewById(R.id.nav_item_dashboard);
        navItemChart = findViewById(R.id.nav_item_chart);
        navItemJournal = findViewById(R.id.nav_item_journal);
        navItemChallenge = findViewById(R.id.nav_item_challenge);
        navItemCoach = findViewById(R.id.nav_item_coach);
        
        // 네비게이션 아이템 클릭 리스너 설정
        navItemDashboard.setOnClickListener(v -> handleNavigationItemSelected(R.id.nav_item_dashboard));
        navItemChart.setOnClickListener(v -> handleNavigationItemSelected(R.id.nav_item_chart));
        navItemJournal.setOnClickListener(v -> handleNavigationItemSelected(R.id.nav_item_journal));
        navItemChallenge.setOnClickListener(v -> handleNavigationItemSelected(R.id.nav_item_challenge));
        navItemCoach.setOnClickListener(v -> handleNavigationItemSelected(R.id.nav_item_coach));
        
        // 초기 프래그먼트 표시
        if (savedInstanceState == null) {
            handleNavigationItemSelected(R.id.nav_item_dashboard);
        }
        
        // 트레이딩 모니터 워커 시작
        WorkManagerHelper.scheduleTradingMonitor(this);
    }
    
    /**
     * 네비게이션 아이템 선택 처리
     */
    private void handleNavigationItemSelected(int itemId) {
        Fragment fragment = null;
        
        if (itemId == R.id.nav_item_dashboard) {
            fragment = new DashboardFragment();
        } else if (itemId == R.id.nav_item_chart) {
            fragment = new ChartFragment();
        } else if (itemId == R.id.nav_item_journal) {
            fragment = new JournalFragment();
        } else if (itemId == R.id.nav_item_challenge) {
            fragment = new ChallengeFragment();
        } else if (itemId == R.id.nav_item_coach) {
            fragment = new CoachFragment();
        }
        
        if (fragment != null) {
            loadFragment(fragment);
            updateNavigationSelection(itemId);
        }
    }
    
    /**
     * 네비게이션 선택 상태 업데이트
     */
    private void updateNavigationSelection(int selectedId) {
        // 모든 아이템을 비선택 상태로 변경
        setNavigationItemSelected(navItemDashboard, selectedId == R.id.nav_item_dashboard);
        setNavigationItemSelected(navItemChart, selectedId == R.id.nav_item_chart);
        setNavigationItemSelected(navItemJournal, selectedId == R.id.nav_item_journal);
        setNavigationItemSelected(navItemChallenge, selectedId == R.id.nav_item_challenge);
        setNavigationItemSelected(navItemCoach, selectedId == R.id.nav_item_coach);
        
        currentSelectedId = selectedId;
    }
    
    /**
     * 네비게이션 아이템 선택 상태 설정
     */
    private void setNavigationItemSelected(LinearLayout item, boolean selected) {
        ImageView icon = (ImageView) item.getChildAt(0);
        TextView text = (TextView) item.getChildAt(1);
        
        int color = selected ? selectedColor : unselectedColor;
        if (icon != null) {
            icon.setColorFilter(color);
        }
        if (text != null) {
            text.setTextColor(color);
        }
    }
    
    /**
     * 프래그먼트 로드
     */
    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
            return true;
        }
        return false;
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 필요 시 워커 정리 (일반적으로는 계속 실행)
    }
    
}

