package com.example.rsquare.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rsquare.R;
import com.example.rsquare.data.local.entity.Position;
import com.example.rsquare.util.NumberFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 활성 포지션 RecyclerView Adapter
 */
public class ActivePositionAdapter extends RecyclerView.Adapter<ActivePositionAdapter.PositionViewHolder> {
    
    private List<Position> positions = new ArrayList<>();
    private OnPositionClickListener listener;
    
    // 심볼별 현재 가격 (실시간 업데이트 필요)
    private Map<String, Double> priceMap = new HashMap<>();
    
    public interface OnPositionClickListener {
        void onPositionClick(Position position);
    }
    
    public interface OnPositionCloseListener {
        void onPositionClose(Position position, double currentPrice);
    }
    
    private OnPositionCloseListener closeListener;
    
    public void setOnPositionClickListener(OnPositionClickListener listener) {
        this.listener = listener;
    }
    
    public void setOnPositionCloseListener(OnPositionCloseListener listener) {
        this.closeListener = listener;
    }
    
    public void setCurrentPrice(String symbol, double price) {
        priceMap.put(symbol, price);
        notifyDataSetChanged();
    }
    
    public void setCurrentPrice(double price) {
        // 모든 심볼에 동일한 가격 설정 (임시)
        for (Position position : positions) {
            if (!position.isClosed()) {
                priceMap.put(position.getSymbol(), price);
            }
        }
        notifyDataSetChanged();
    }
    
    public void setPositions(List<Position> positions) {
        // 활성 포지션만 필터링
        this.positions = new ArrayList<>();
        for (Position position : positions) {
            if (!position.isClosed()) {
                this.positions.add(position);
            }
        }
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public PositionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_active_position, parent, false);
        return new PositionViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull PositionViewHolder holder, int position) {
        Position pos = positions.get(position);
        double currentPrice = priceMap.getOrDefault(pos.getSymbol(), 0.0);
        holder.bind(pos, currentPrice);
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPositionClick(pos);
            }
        });
        
        // 포지션 종료 버튼 클릭
        holder.btnClosePosition.setOnClickListener(v -> {
            if (closeListener != null) {
                double priceToUse = currentPrice > 0 ? currentPrice : pos.getEntryPrice();
                closeListener.onPositionClose(pos, priceToUse);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return positions.size();
    }
    
    static class PositionViewHolder extends RecyclerView.ViewHolder {
        private TextView symbolText;
        private TextView typeText;
        private TextView leverageText;
        private TextView entryPriceText;
        private TextView currentPriceText;
        private TextView pnlText;
        private TextView pnlPercentText;
        private Button btnClosePosition;
        
        public PositionViewHolder(@NonNull View itemView) {
            super(itemView);
            symbolText = itemView.findViewById(R.id.symbol_text);
            typeText = itemView.findViewById(R.id.type_text);
            leverageText = itemView.findViewById(R.id.leverage_text);
            entryPriceText = itemView.findViewById(R.id.entry_price_text);
            currentPriceText = itemView.findViewById(R.id.current_price_text);
            pnlText = itemView.findViewById(R.id.pnl_text);
            pnlPercentText = itemView.findViewById(R.id.pnl_percent_text);
            btnClosePosition = itemView.findViewById(R.id.btn_close_position);
        }
        
        public void bind(Position position, double currentPrice) {
            // 심볼
            symbolText.setText(position.getSymbol());
            
            // 롱/숏
            typeText.setText(position.isLong() ? "롱" : "숏");
            typeText.setTextColor(itemView.getContext().getColor(
                position.isLong() ? R.color.tds_success_alt : R.color.tds_error_alt));
            
            // 레버리지
            if (position.getLeverage() > 1) {
                leverageText.setText(position.getLeverage() + "x");
                leverageText.setVisibility(View.VISIBLE);
            } else {
                leverageText.setVisibility(View.GONE);
            }
            
            // 진입 가격
            entryPriceText.setText("진입: " + NumberFormatter.formatPrice(position.getEntryPrice()));
            
            // 현재 가격 및 미실현 손익
            // currentPrice가 0이면 진입가 사용
            double priceToUse = currentPrice > 0 ? currentPrice : position.getEntryPrice();
            
            if (currentPrice > 0) {
                currentPriceText.setText("현재: " + NumberFormatter.formatPrice(currentPrice));
                currentPriceText.setVisibility(View.VISIBLE);
            } else {
                currentPriceText.setText("현재: " + NumberFormatter.formatPrice(position.getEntryPrice()) + " (진입가)");
                currentPriceText.setVisibility(View.VISIBLE);
                currentPriceText.setTextColor(itemView.getContext().getColor(R.color.tds_text_secondary));
            }
            
            // 미실현 손익 계산
            double unrealizedPnL = position.calculateUnrealizedPnL(priceToUse);
            double unrealizedPnLPercent = position.calculateUnrealizedPnLPercent(priceToUse);
            
            pnlText.setText(NumberFormatter.formatPnL(unrealizedPnL));
            pnlPercentText.setText(String.format(Locale.US, "%.2f%%", unrealizedPnLPercent));
            
            // 색상 설정
            int color = unrealizedPnL >= 0 ? 
                itemView.getContext().getColor(R.color.tds_success_alt) :
                itemView.getContext().getColor(R.color.tds_error_alt);
            pnlText.setTextColor(color);
            pnlPercentText.setTextColor(color);
        }
    }
}

