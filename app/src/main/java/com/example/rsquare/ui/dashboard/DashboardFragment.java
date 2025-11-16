package com.example.rsquare.ui.dashboard;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rsquare.R;
import com.example.rsquare.data.local.entity.Position;
import com.example.rsquare.ui.common.RiskGaugeView;
import com.example.rsquare.util.NumberFormatter;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * Dashboard Fragment
 * 잔고, 손익, 리스크 스코어 등 핵심 정보 표시
 */
public class DashboardFragment extends Fragment {
    
    private DashboardViewModel viewModel;
    
    private TextView tvBalance, tvTotalPnL, tvWinRate, tvTotalTrades;
    private RiskGaugeView riskGaugeView;
    private LineChart pnlChart;
    private RecyclerView rvRecentPositions;
    private RecentPositionsAdapter positionsAdapter;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard_tds, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        
        initViews(view);
        setupChart();
        setupRecyclerView();
        setupObservers();
    }
    
    private void initViews(View view) {
        tvBalance = view.findViewById(R.id.tv_balance);
        tvTotalPnL = view.findViewById(R.id.tv_total_pnl);
        tvWinRate = view.findViewById(R.id.tv_win_rate);
        tvTotalTrades = view.findViewById(R.id.tv_total_trades);
        riskGaugeView = view.findViewById(R.id.risk_gauge);
        pnlChart = view.findViewById(R.id.pnl_chart);
        rvRecentPositions = view.findViewById(R.id.rv_recent_positions);
    }
    
    private void setupChart() {
        pnlChart.setTouchEnabled(true);
        pnlChart.setDragEnabled(true);
        pnlChart.setScaleEnabled(true);
        pnlChart.getDescription().setEnabled(false);
        pnlChart.getLegend().setEnabled(false);
        
        pnlChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        // TDS 색상 사용
        int textColor = ContextCompat.getColor(requireContext(), R.color.tds_gray_700);
        int gridColor = ContextCompat.getColor(requireContext(), R.color.tds_gray_200);
        int bgColor = ContextCompat.getColor(requireContext(), R.color.tds_background);
        
        pnlChart.getXAxis().setTextColor(textColor);
        pnlChart.getAxisLeft().setTextColor(textColor);
        pnlChart.getAxisRight().setEnabled(false);
        
        pnlChart.setBackgroundColor(bgColor);
        pnlChart.getXAxis().setGridColor(gridColor);
        pnlChart.getAxisLeft().setGridColor(gridColor);
    }
    
    private void setupRecyclerView() {
        positionsAdapter = new RecentPositionsAdapter(position -> {
            // 포지션 클릭 시 상세 정보 표시 (향후 구현)
            android.widget.Toast.makeText(requireContext(), 
                position.getSymbol() + " 포지션 상세 정보", 
                android.widget.Toast.LENGTH_SHORT).show();
        });
        rvRecentPositions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRecentPositions.setAdapter(positionsAdapter);
    }
    
    private void setupObservers() {
        // 사용자 정보 관찰
        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                tvBalance.setText(NumberFormatter.formatPrice(user.getBalance()));
            }
        });
        
        // 총 손익 관찰
        viewModel.getTotalPnL().observe(getViewLifecycleOwner(), pnl -> {
            if (pnl != null) {
                tvTotalPnL.setText(NumberFormatter.formatPnL(pnl));
                
                if (pnl >= 0) {
                    tvTotalPnL.setTextColor(ContextCompat.getColor(requireContext(), R.color.tds_success));
                } else {
                    tvTotalPnL.setTextColor(ContextCompat.getColor(requireContext(), R.color.tds_error));
                }
            }
        });
        
        // 거래 통계 관찰
        viewModel.getTradeStatistics().observe(getViewLifecycleOwner(), stats -> {
            if (stats != null) {
                tvWinRate.setText(String.format("%.1f%%", stats.winRate));
                tvTotalTrades.setText(String.valueOf(stats.totalCount) + " 거래");
                
                if (stats.winRate >= 50) {
                    tvWinRate.setTextColor(ContextCompat.getColor(requireContext(), R.color.tds_success));
                } else {
                    tvWinRate.setTextColor(ContextCompat.getColor(requireContext(), R.color.tds_error));
                }
            }
        });
        
        // 리스크 메트릭스 관찰
        viewModel.getRiskMetrics().observe(getViewLifecycleOwner(), metrics -> {
            if (metrics != null) {
                riskGaugeView.setRiskScore((float) metrics.getRiskScore());
            }
        });
        
        // 최근 포지션 관찰
        viewModel.getRecentPositions().observe(getViewLifecycleOwner(), positions -> {
            if (positions != null) {
                positionsAdapter.setPositions(positions);
                updatePnLChart(positions);
            }
        });
    }
    
    private void updatePnLChart(List<Position> positions) {
        List<Entry> entries = new ArrayList<>();
        float cumulativePnL = 0f;
        
        int index = 0;
        for (Position position : positions) {
            if (position.isClosed()) {
                cumulativePnL += (float) position.getPnl();
                entries.add(new Entry(index++, cumulativePnL));
            }
        }
        
        if (entries.isEmpty()) {
            entries.add(new Entry(0, 0));
        }
        
        LineDataSet dataSet = new LineDataSet(entries, "누적 손익");
        // TDS 블루 색상 사용
        int tdsBlue = ContextCompat.getColor(pnlChart.getContext(), R.color.tds_blue_500);
        dataSet.setColor(tdsBlue);
        dataSet.setLineWidth(2f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(tdsBlue);
        dataSet.setFillAlpha(50);
        
        LineData lineData = new LineData(dataSet);
        pnlChart.setData(lineData);
        pnlChart.invalidate();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        viewModel.refresh();
    }
    
    /**
     * Recent Positions Adapter
     */
    private static class RecentPositionsAdapter extends RecyclerView.Adapter<RecentPositionsAdapter.ViewHolder> {
        
        private List<Position> positions = new ArrayList<>();
        private OnPositionClickListener clickListener;
        
        interface OnPositionClickListener {
            void onPositionClick(Position position);
        }
        
        public RecentPositionsAdapter(OnPositionClickListener clickListener) {
            this.clickListener = clickListener;
        }
        
        public void setPositions(List<Position> positions) {
            this.positions = positions;
            notifyDataSetChanged();
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_position_tds, parent, false);
            return new ViewHolder(view, clickListener);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Position pos = positions.get(position);
            holder.bind(pos);
        }
        
        @Override
        public int getItemCount() {
            return positions.size();
        }
        
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvSymbol, tvType, tvPnL, tvStatus;
            OnPositionClickListener clickListener;
            
            public ViewHolder(@NonNull View itemView, OnPositionClickListener clickListener) {
                super(itemView);
                this.clickListener = clickListener;
                tvSymbol = itemView.findViewById(R.id.tv_position_symbol);
                tvType = itemView.findViewById(R.id.tv_position_type);
                tvPnL = itemView.findViewById(R.id.tv_position_pnl);
                tvStatus = itemView.findViewById(R.id.tv_position_status);
                
                // 클릭 리스너 설정
                itemView.setOnClickListener(v -> {
                    if (clickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                        // 어댑터에서 position 가져오기
                        RecyclerView.Adapter adapter = ((RecyclerView) itemView.getParent()).getAdapter();
                        if (adapter instanceof RecentPositionsAdapter) {
                            RecentPositionsAdapter posAdapter = (RecentPositionsAdapter) adapter;
                            int pos = getAdapterPosition();
                            if (pos >= 0 && pos < posAdapter.positions.size()) {
                                clickListener.onPositionClick(posAdapter.positions.get(pos));
                            }
                        }
                    }
                });
            }
            
            public void bind(Position position) {
                tvSymbol.setText(position.getSymbol());
                tvType.setText(position.isLong() ? "롱" : "숏");
                
                if (position.isClosed()) {
                    tvPnL.setText(NumberFormatter.formatPnL(position.getPnl()));
                    tvStatus.setText("· 종료");
                    
                    // TDS 색상 사용
                    if (position.getPnl() >= 0) {
                        tvPnL.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.tds_profit));
                    } else {
                        tvPnL.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.tds_loss));
                    }
                } else {
                    tvPnL.setText("-");
                    tvStatus.setText("· 활성");
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.tds_blue_400));
                }
            }
        }
    }
}

