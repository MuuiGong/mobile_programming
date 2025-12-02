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
    
    public interface OnPriceUpdateListener {
        void onPriceUpdated();
    }
    
    private OnPositionCloseListener closeListener;
    private OnPriceUpdateListener priceUpdateListener;
    
    public void setOnPositionClickListener(OnPositionClickListener listener) {
        this.listener = listener;
    }
    
    public void setOnPositionCloseListener(OnPositionCloseListener listener) {
        this.closeListener = listener;
    }
    
    public void setOnPriceUpdateListener(OnPriceUpdateListener listener) {
        this.priceUpdateListener = listener;
    }
    
    public void setCurrentPrice(String symbol, double price) {
        double oldPrice = priceMap.getOrDefault(symbol, 0.0);
        priceMap.put(symbol, price);
        notifyDataSetChanged();
        
        // 가격이 변경되었으면 리스너에 알림
        if (priceUpdateListener != null && oldPrice != price) {
            priceUpdateListener.onPriceUpdated();
        }
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
    
    /**
     * 심볼의 현재 가격 조회
     */
    public double getCurrentPrice(String symbol) {
        return priceMap.getOrDefault(symbol, 0.0);
    }
    
    /**
     * 모든 활성 포지션의 총 미실현 손익 계산
     */
    public double getTotalUnrealizedPnL() {
        double total = 0.0;
        for (Position position : positions) {
            if (!position.isClosed()) {
                String symbol = position.getSymbol();
                // priceMap에 실제 가격이 설정되어 있는지 확인
                if (priceMap.containsKey(symbol)) {
                    double currentPrice = priceMap.get(symbol);
                    // 가격이 0보다 크면 미실현 손익 계산
                    if (currentPrice > 0) {
                        total += position.calculateUnrealizedPnL(currentPrice);
                    }
                }
                // priceMap에 가격이 없으면 미실현 손익은 0 (가격 정보가 없음)
            }
        }
        return total;
    }

    /**
     * 모든 활성 포지션의 총 사용 마진 계산
     */
    public double getTotalUsedMargin() {
        double total = 0.0;
        for (Position position : positions) {
            if (!position.isClosed()) {
                double positionSize = position.getEntryPrice() * position.getQuantity();
                total += positionSize / position.getLeverage();
            }
        }
        return total;
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

    public List<Position> getPositions() {
        return positions;
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
        private TextView quantityText;
        private TextView typeText;
        private TextView leverageText;
        private TextView entryPriceText;
        private TextView currentPriceText;
        private TextView marginText;
        private TextView valueText;
        private TextView pnlText;
        private TextView pnlPercentText;
        private Button btnClosePosition;
        
        public PositionViewHolder(@NonNull View itemView) {
            super(itemView);
            symbolText = itemView.findViewById(R.id.symbol_text);
            quantityText = itemView.findViewById(R.id.quantity_text);
            typeText = itemView.findViewById(R.id.type_text);
            leverageText = itemView.findViewById(R.id.leverage_text);
            entryPriceText = itemView.findViewById(R.id.entry_price_text);
            currentPriceText = itemView.findViewById(R.id.current_price_text);
            marginText = itemView.findViewById(R.id.margin_text);
            valueText = itemView.findViewById(R.id.value_text);
            pnlText = itemView.findViewById(R.id.pnl_text);
            pnlPercentText = itemView.findViewById(R.id.pnl_percent_text);
            btnClosePosition = itemView.findViewById(R.id.btn_close_position);
        }
        
        public void bind(Position position, double currentPrice) {
            // 심볼
            symbolText.setText(position.getSymbol());
            
            // 수량
            quantityText.setText(NumberFormatter.formatQuantity(position.getQuantity()) + " " + position.getSymbol().replace("USDT", ""));

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
            
            // 상태 확인 (PENDING)
            boolean isPending = "PENDING".equals(position.getStatus());
            
            if (isPending) {
                // 대기중 상태 표시
                typeText.setText("대기중");
                typeText.setTextColor(itemView.getContext().getColor(R.color.tds_text_secondary));
                
                entryPriceText.setText("진입가: " + NumberFormatter.formatPrice(position.getEntryPrice()));
                
                // 현재가 표시
                if (currentPrice > 0) {
                    currentPriceText.setText("현재: " + NumberFormatter.formatPrice(currentPrice));
                    currentPriceText.setVisibility(View.VISIBLE);
                } else {
                    currentPriceText.setVisibility(View.GONE);
                }
                
                // PnL은 표시하지 않음 (아직 진입 안함)
                pnlText.setText("-");
                pnlText.setTextColor(itemView.getContext().getColor(R.color.tds_text_secondary));
                pnlPercentText.setText("-");
                pnlPercentText.setTextColor(itemView.getContext().getColor(R.color.tds_text_secondary));
                
                // 버튼 텍스트 변경
                btnClosePosition.setText("취소");
                
            } else {
                // 활성 상태 (기존 로직)
                
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
                
                // 마진 및 가치 계산
                double positionSize = position.getEntryPrice() * position.getQuantity();
                double margin = positionSize / position.getLeverage(); // Required Margin
                double value = priceToUse * position.getQuantity();
                
                marginText.setText("마진: " + NumberFormatter.formatPrice(margin));
                valueText.setText("가치: " + NumberFormatter.formatPrice(value));
                
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
                
                // 버튼 텍스트 복원
                btnClosePosition.setText("종료");
            }
        }
    }
}

