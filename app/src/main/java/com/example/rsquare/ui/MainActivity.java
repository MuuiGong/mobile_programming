package com.example.rsquare.ui;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.rsquare.R;
import com.example.rsquare.ui.challenge.ChallengeFragment;
import com.example.rsquare.ui.chart.ChartFragment;
import com.example.rsquare.ui.coach.CoachFragment;
import com.example.rsquare.ui.dashboard.DashboardFragment;
import com.example.rsquare.ui.journal.JournalFragment;
import com.example.rsquare.worker.WorkManagerHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

/**
 * Main Activity
 * 앱의 메인 화면 및 네비게이션 관리
 */
public class MainActivity extends AppCompatActivity {
    
    private BottomNavigationView bottomNavigationView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_tds);
        
        // Bottom Navigation 설정
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                return handleNavigationItemSelected(item.getItemId());
            }
        });
        
        // 초기 프래그먼트 표시
        if (savedInstanceState == null) {
            loadFragment(new DashboardFragment());
        }
        
        // 트레이딩 모니터 워커 시작
        WorkManagerHelper.scheduleTradingMonitor(this);
    }
    
    /**
     * 네비게이션 아이템 선택 처리
     */
    private boolean handleNavigationItemSelected(int itemId) {
        Fragment fragment = null;
        
        if (itemId == R.id.nav_dashboard) {
            fragment = new DashboardFragment();
        } else if (itemId == R.id.nav_chart) {
            fragment = new ChartFragment();
        } else if (itemId == R.id.nav_journal) {
            fragment = new JournalFragment();
        } else if (itemId == R.id.nav_challenge) {
            fragment = new ChallengeFragment();
        } else if (itemId == R.id.nav_coach) {
            fragment = new CoachFragment();
        }
        
        return loadFragment(fragment);
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

