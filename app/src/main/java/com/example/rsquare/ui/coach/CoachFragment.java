package com.example.rsquare.ui.coach;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rsquare.R;
import com.example.rsquare.domain.CoachingMessage;
import com.example.rsquare.ui.coach.CoachViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Coach Fragment
 * AI 코치 피드백 및 행동 패턴 분석
 */
public class CoachFragment extends Fragment {
    
    private CoachViewModel viewModel;
    private RecyclerView rvCoachingMessages;
    private Button btnWeeklyReport, btnAcceptChallenge;
    private TextView tvRecommendedChallengeTitle, tvRecommendedChallengeDesc;
    private CoachingMessageAdapter messageAdapter;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_coach_tds, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewModel = new ViewModelProvider(this).get(CoachViewModel.class);
        
        initViews(view);
        setupRecyclerView();
        setupListeners();
        setupObservers();
        
        // 초기 데이터 로드
        viewModel.loadCoachingMessages();
        viewModel.loadRecommendedChallenge();
    }
    
    private void initViews(View view) {
        rvCoachingMessages = view.findViewById(R.id.rv_coaching_messages);
        btnWeeklyReport = view.findViewById(R.id.btn_weekly_report);
        btnAcceptChallenge = view.findViewById(R.id.btn_accept_challenge);
        tvRecommendedChallengeTitle = view.findViewById(R.id.tv_recommended_challenge_title);
        tvRecommendedChallengeDesc = view.findViewById(R.id.tv_recommended_challenge_desc);
    }
    
    private void setupRecyclerView() {
        messageAdapter = new CoachingMessageAdapter(message -> {
            // 메시지 클릭 시 상세 정보 표시
            Toast.makeText(requireContext(), 
                "피드백: " + message.getMessage(), 
                Toast.LENGTH_SHORT).show();
        });
        rvCoachingMessages.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCoachingMessages.setAdapter(messageAdapter);
    }
    
    private void setupListeners() {
        // 주간 리포트 버튼
        btnWeeklyReport.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "주간 리포트 기능은 준비 중입니다", Toast.LENGTH_SHORT).show();
            // TODO: 주간 리포트 화면으로 이동
        });
        
        // 챌린지 수락 버튼
        btnAcceptChallenge.setOnClickListener(v -> {
            viewModel.acceptRecommendedChallenge();
            Toast.makeText(requireContext(), "챌린지가 수락되었습니다!", Toast.LENGTH_SHORT).show();
        });
    }
    
    private void setupObservers() {
        viewModel.getCoachingMessages().observe(getViewLifecycleOwner(), messages -> {
            if (messages != null) {
                messageAdapter.setMessages(messages);
            }
        });
        
        viewModel.getRecommendedChallenge().observe(getViewLifecycleOwner(), challenge -> {
            if (challenge != null) {
                tvRecommendedChallengeTitle.setText(challenge.getTitle());
                tvRecommendedChallengeDesc.setText(challenge.getDescription());
            }
        });
    }
    
    /**
     * Coaching Message Adapter
     */
    private static class CoachingMessageAdapter extends RecyclerView.Adapter<CoachingMessageAdapter.ViewHolder> {
        
        private List<CoachingMessage> messages = new ArrayList<>();
        private OnMessageClickListener clickListener;
        
        interface OnMessageClickListener {
            void onMessageClick(CoachingMessage message);
        }
        
        public CoachingMessageAdapter(OnMessageClickListener clickListener) {
            this.clickListener = clickListener;
        }
        
        public void setMessages(List<CoachingMessage> messages) {
            this.messages = messages;
            notifyDataSetChanged();
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_coaching_message_tds, parent, false);
            return new ViewHolder(view, clickListener);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CoachingMessage message = messages.get(position);
            holder.bind(message);
        }
        
        @Override
        public int getItemCount() {
            return messages.size();
        }
        
        static class ViewHolder extends RecyclerView.ViewHolder {
            private android.widget.TextView tvIcon, tvType, tvContent;
            private OnMessageClickListener clickListener;
            
            public ViewHolder(@NonNull View itemView, OnMessageClickListener clickListener) {
                super(itemView);
                this.clickListener = clickListener;
                tvIcon = itemView.findViewById(R.id.tv_message_icon);
                tvType = itemView.findViewById(R.id.tv_message_type);
                tvContent = itemView.findViewById(R.id.tv_message_content);
                
                itemView.setOnClickListener(v -> {
                    if (clickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                        RecyclerView.Adapter adapter = ((RecyclerView) itemView.getParent()).getAdapter();
                        if (adapter instanceof CoachingMessageAdapter) {
                            CoachingMessageAdapter msgAdapter = (CoachingMessageAdapter) adapter;
                            int pos = getAdapterPosition();
                            if (pos >= 0 && pos < msgAdapter.messages.size()) {
                                clickListener.onMessageClick(msgAdapter.messages.get(pos));
                            }
                        }
                    }
                });
            }
            
            public void bind(CoachingMessage message) {
                tvType.setText(message.getType().toString());
                tvContent.setText(message.getMessage());
                
                // 타입에 따라 아이콘 설정
                CoachingMessage.MessageType type = message.getType();
                if (type == CoachingMessage.MessageType.SUGGESTION) {
                    tvIcon.setText("💡");
                } else if (type == CoachingMessage.MessageType.WARNING) {
                    tvIcon.setText("⚠️");
                } else if (type == CoachingMessage.MessageType.POSITIVE) {
                    tvIcon.setText("✅");
                } else if (type == CoachingMessage.MessageType.ACHIEVEMENT) {
                    tvIcon.setText("🎉");
                } else {
                    tvIcon.setText("📊");
                }
            }
        }
    }
}
