package com.example.rsquare.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rsquare.R;
import com.example.rsquare.data.local.entity.Position;
import com.example.rsquare.util.NumberFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 거래 기록 RecyclerView Adapter
 */
public class TradeHistoryAdapter extends RecyclerView.Adapter<TradeHistoryAdapter.TradeViewHolder> {
    
    private List<Position> trades = new ArrayList<>();
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd", Locale.getDefault());
    
    public void setTrades(List<Position> trades) {
        this.trades = trades != null ? trades : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public TradeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_trade_history, parent, false);
        return new TradeViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull TradeViewHolder holder, int position) {
        Position trade = trades.get(position);
        holder.bind(trade);
    }
    
    @Override
    public int getItemCount() {
        return trades.size();
    }
    
    static class TradeViewHolder extends RecyclerView.ViewHolder {
        private TextView symbolText;
        private TextView timeText;
        private TextView pnlText;
        private TextView statusText;
        
        public TradeViewHolder(@NonNull View itemView) {
            super(itemView);
            symbolText = itemView.findViewById(R.id.symbol_text);
            timeText = itemView.findViewById(R.id.time_text);
            pnlText = itemView.findViewById(R.id.pnl_text);
            statusText = itemView.findViewById(R.id.status_text);
        }
        
        public void bind(Position position) {
            // 심볼
            symbolText.setText(position.getSymbol());
            
            // 시간
            if (position.getCloseTime() != null) {
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                timeText.setText(timeFormat.format(position.getCloseTime()));
            } else {
                timeText.setText("-");
            }
            
            // 손익
            double pnl = position.getPnl();
            pnlText.setText(NumberFormatter.formatPnL(pnl));
            pnlText.setTextColor(itemView.getContext().getColor(
                pnl >= 0 ? R.color.tds_success_alt : R.color.tds_error_alt));
            
            // 상태
            String exitReason = position.getExitReason();
            if (exitReason != null) {
                switch (exitReason) {
                    case "TP_HIT":
                        statusText.setText("✓TP");
                        statusText.setTextColor(itemView.getContext().getColor(R.color.tds_success_alt));
                        break;
                    case "SL_HIT":
                        statusText.setText("✗SL");
                        statusText.setTextColor(itemView.getContext().getColor(R.color.tds_error_alt));
                        break;
                    case "MARGIN_CALL_LIQUIDATION":
                    case "LIQUIDATION":
                        statusText.setText("💥청산");
                        statusText.setTextColor(itemView.getContext().getColor(R.color.tds_error_alt));
                        break;
                    case "TIMEOUT":
                        statusText.setText("⏱타임아웃");
                        statusText.setTextColor(itemView.getContext().getColor(R.color.tds_text_secondary));
                        break;
                    default:
                        statusText.setText("종료");
                        statusText.setTextColor(itemView.getContext().getColor(R.color.tds_text_secondary));
                        break;
                }
            } else {
                statusText.setText("종료");
                statusText.setTextColor(itemView.getContext().getColor(R.color.tds_text_secondary));
            }
        }
    }
}

