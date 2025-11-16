package com.example.rsquare.ui.challenge;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.rsquare.R;
import com.example.rsquare.data.local.entity.Challenge;
import com.example.rsquare.ui.challenge.ChallengeViewModel;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

/**
 * Challenge Fragment
 * 챌린지 및 뱃지 시스템
 */
public class ChallengeFragment extends Fragment {
    
    private ChallengeViewModel viewModel;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private ChallengePagerAdapter pagerAdapter;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_challenge_tds, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewModel = new ViewModelProvider(this).get(ChallengeViewModel.class);
        
        initViews(view);
        setupViewPager();
        
        // 초기 데이터 로드
        viewModel.loadChallenges();
    }
    
    private void initViews(View view) {
        tabLayout = view.findViewById(R.id.tab_layout);
        viewPager = view.findViewById(R.id.view_pager);
    }
    
    private void setupViewPager() {
        pagerAdapter = new ChallengePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("진행 중");
            } else {
                tab.setText("완료");
            }
        }).attach();
    }
    
    /**
     * Challenge Pager Adapter (ViewPager2용)
     */
    public static class ChallengePagerAdapter extends FragmentStateAdapter {
        
        public ChallengePagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }
        
        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return ChallengeListFragment.newInstance(position == 0);
        }
        
        @Override
        public int getItemCount() {
            return 2; // 진행 중, 완료
        }
    }
    
    /**
     * Challenge List Fragment (각 탭용)
     */
    public static class ChallengeListFragment extends Fragment {
        private static final String ARG_IS_ACTIVE = "is_active";
        private boolean isActive;
        private RecyclerView recyclerView;
        private ChallengeRecyclerAdapter adapter;
        private ChallengeViewModel viewModel;
        
        public static ChallengeListFragment newInstance(boolean isActive) {
            ChallengeListFragment fragment = new ChallengeListFragment();
            Bundle args = new Bundle();
            args.putBoolean(ARG_IS_ACTIVE, isActive);
            fragment.setArguments(args);
            return fragment;
        }
        
        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (getArguments() != null) {
                isActive = getArguments().getBoolean(ARG_IS_ACTIVE);
            }
        }
        
        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_challenge_list, container, false);
            recyclerView = view.findViewById(R.id.recycler_view);
            return view;
        }
        
        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            
            viewModel = new ViewModelProvider(requireActivity()).get(ChallengeViewModel.class);
            
            adapter = new ChallengeRecyclerAdapter();
            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            recyclerView.setAdapter(adapter);
            
            if (isActive) {
                viewModel.getActiveChallenges().observe(getViewLifecycleOwner(), challenges -> {
                    if (challenges != null) {
                        adapter.setChallenges(challenges);
                    }
                });
            } else {
                viewModel.getCompletedChallenges().observe(getViewLifecycleOwner(), challenges -> {
                    if (challenges != null) {
                        adapter.setChallenges(challenges);
                    }
                });
            }
        }
    }
    
    /**
     * Challenge RecyclerView Adapter
     */
    private static class ChallengeRecyclerAdapter extends RecyclerView.Adapter<ChallengeRecyclerAdapter.ViewHolder> {
        
        private List<Challenge> challenges = new ArrayList<>();
        
        public void setChallenges(List<Challenge> challenges) {
            this.challenges = challenges != null ? challenges : new ArrayList<>();
            notifyDataSetChanged();
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_challenge_tds, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (position < challenges.size()) {
                Challenge challenge = challenges.get(position);
                holder.bind(challenge);
            }
        }
        
        @Override
        public int getItemCount() {
            return challenges.size();
        }
        
        static class ViewHolder extends RecyclerView.ViewHolder {
            private TextView tvTitle, tvDescription, tvProgress, tvStatus;
            private ProgressBar progressBar;
            
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tv_challenge_title);
                tvDescription = itemView.findViewById(R.id.tv_challenge_description);
                tvProgress = itemView.findViewById(R.id.tv_challenge_progress);
                tvStatus = itemView.findViewById(R.id.tv_challenge_status);
                progressBar = itemView.findViewById(R.id.progress_challenge);
                
                itemView.setOnClickListener(v -> {
                    Toast.makeText(itemView.getContext(), "챌린지 상세 정보", Toast.LENGTH_SHORT).show();
                });
            }
            
            public void bind(Challenge challenge) {
                tvTitle.setText(challenge.getTitle());
                tvDescription.setText(challenge.getDescription());
                
                double progressPercent = (challenge.getProgress() / challenge.getTargetValue()) * 100;
                int progress = (int) Math.min(100, Math.max(0, progressPercent));
                progressBar.setProgress(progress);
                tvProgress.setText(progress + "%");
                tvStatus.setText(String.format("%.1f / %.1f 완료", challenge.getProgress(), challenge.getTargetValue()));
            }
        }
    }
}
