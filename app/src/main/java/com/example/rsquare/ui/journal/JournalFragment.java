package com.example.rsquare.ui.journal;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rsquare.R;
import com.example.rsquare.data.local.entity.Journal;
import com.example.rsquare.ui.journal.JournalViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Journal Fragment
 * 거래 저널 및 감정 기록
 */
public class JournalFragment extends Fragment {
    
    private JournalViewModel viewModel;
    private RecyclerView rvJournalList;
    private ChipGroup chipGroupEmotions;
    private Button btnAddJournal;
    private JournalAdapter journalAdapter;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_journal_tds, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewModel = new ViewModelProvider(this).get(JournalViewModel.class);
        
        initViews(view);
        setupRecyclerView();
        setupListeners();
        setupObservers();
        
        // 초기 데이터 로드
        viewModel.loadJournals();
    }
    
    private void initViews(View view) {
        rvJournalList = view.findViewById(R.id.rv_journal_list);
        chipGroupEmotions = view.findViewById(R.id.chip_group_emotions);
        btnAddJournal = view.findViewById(R.id.btn_add_journal);
    }
    
    private void setupRecyclerView() {
        journalAdapter = new JournalAdapter(journal -> {
            // 저널 클릭 시 상세 정보 표시
            Toast.makeText(requireContext(), 
                "저널 상세 정보: " + journal.getNote(), 
                Toast.LENGTH_SHORT).show();
        });
        rvJournalList.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvJournalList.setAdapter(journalAdapter);
    }
    
    private void setupListeners() {
        // 저널 추가 버튼
        btnAddJournal.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "저널 작성 기능은 준비 중입니다", Toast.LENGTH_SHORT).show();
            // TODO: 저널 작성 다이얼로그 표시
        });
        
        // 감정 필터 칩
        chipGroupEmotions.setOnCheckedChangeListener((group, checkedId) -> {
            Chip chip = group.findViewById(checkedId);
            if (chip != null) {
                String filter = chip.getText().toString();
                viewModel.filterByEmotion(filter);
            }
        });
    }
    
    private void setupObservers() {
        viewModel.getJournals().observe(getViewLifecycleOwner(), journals -> {
            if (journals != null) {
                journalAdapter.setJournals(journals);
            }
        });
    }
    
    /**
     * Journal Adapter
     */
    private static class JournalAdapter extends RecyclerView.Adapter<JournalAdapter.ViewHolder> {
        
        private List<Journal> journals = new ArrayList<>();
        private OnJournalClickListener clickListener;
        
        interface OnJournalClickListener {
            void onJournalClick(Journal journal);
        }
        
        public JournalAdapter(OnJournalClickListener clickListener) {
            this.clickListener = clickListener;
        }
        
        public void setJournals(List<Journal> journals) {
            this.journals = journals;
            notifyDataSetChanged();
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_journal_tds, parent, false);
            return new ViewHolder(view, clickListener);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Journal journal = journals.get(position);
            holder.bind(journal);
        }
        
        @Override
        public int getItemCount() {
            return journals.size();
        }
        
        static class ViewHolder extends RecyclerView.ViewHolder {
            private android.widget.TextView tvSymbol, tvEmotion, tvNote, tvTime;
            private OnJournalClickListener clickListener;
            
            public ViewHolder(@NonNull View itemView, OnJournalClickListener clickListener) {
                super(itemView);
                this.clickListener = clickListener;
                tvSymbol = itemView.findViewById(R.id.tv_journal_symbol);
                tvEmotion = itemView.findViewById(R.id.tv_journal_emotion);
                tvNote = itemView.findViewById(R.id.tv_journal_note);
                tvTime = itemView.findViewById(R.id.tv_journal_time);
                
                itemView.setOnClickListener(v -> {
                    if (clickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                        RecyclerView.Adapter adapter = ((RecyclerView) itemView.getParent()).getAdapter();
                        if (adapter instanceof JournalAdapter) {
                            JournalAdapter journalAdapter = (JournalAdapter) adapter;
                            int pos = getAdapterPosition();
                            if (pos >= 0 && pos < journalAdapter.journals.size()) {
                                clickListener.onJournalClick(journalAdapter.journals.get(pos));
                            }
                        }
                    }
                });
            }
            
            public void bind(Journal journal) {
                // TODO: 실제 데이터 바인딩 구현
                tvNote.setText(journal.getNote());
                tvTime.setText("방금 전");
            }
        }
    }
}
