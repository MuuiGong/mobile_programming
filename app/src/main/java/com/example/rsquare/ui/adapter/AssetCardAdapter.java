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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 자산 카드 RecyclerView Adapter
 */
public class AssetCardAdapter extends RecyclerView.Adapter<AssetCardAdapter.AssetViewHolder> {
    
    private List<Position> assets = new ArrayList<>();
    private OnAssetClickListener listener;
    private int selectedPosition = -1;
    
    public interface OnAssetClickListener {
        void onAssetClick(Position asset);
    }
    
    public void setOnAssetClickListener(OnAssetClickListener listener) {
        this.listener = listener;
    }
    
    public void setAssets(List<Position> assets) {
        this.assets = assets != null ? assets : new ArrayList<>();
        selectedPosition = -1; // 선택 초기화
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public AssetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_asset_card, parent, false);
        return new AssetViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull AssetViewHolder holder, int position) {
        Position asset = assets.get(position);
        boolean isSelected = (selectedPosition == position);
        holder.bind(asset, isSelected);
        
        // CardView를 클릭 가능하게 설정
        androidx.cardview.widget.CardView cardView = (androidx.cardview.widget.CardView) holder.itemView;
        cardView.setClickable(true);
        cardView.setFocusable(true);
        
        holder.itemView.setOnClickListener(v -> {
            int previousPosition = selectedPosition;
            selectedPosition = position;
            
            // 이전 선택 해제
            if (previousPosition != -1 && previousPosition != position) {
                notifyItemChanged(previousPosition);
            }
            // 현재 선택 표시
            notifyItemChanged(position);
            
            if (listener != null) {
                listener.onAssetClick(asset);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return assets.size();
    }
    
    static class AssetViewHolder extends RecyclerView.ViewHolder {
        private androidx.cardview.widget.CardView cardView;
        private TextView assetIcon;
        private TextView assetSymbol;
        private TextView assetName;
        private TextView assetPrice;
        private TextView assetChange;
        
        public AssetViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (androidx.cardview.widget.CardView) itemView;
            assetIcon = itemView.findViewById(R.id.asset_icon);
            assetSymbol = itemView.findViewById(R.id.asset_symbol);
            assetName = itemView.findViewById(R.id.asset_name);
            assetPrice = itemView.findViewById(R.id.asset_price);
            assetChange = itemView.findViewById(R.id.asset_change);
        }
        
        public void bind(Position asset, boolean isSelected) {
            // 선택 상태에 따라 배경색 변경
            if (isSelected) {
                cardView.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(
                    itemView.getContext().getColor(R.color.tds_blue_400)));
            } else {
                cardView.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(
                    itemView.getContext().getColor(R.color.tds_bg_dark)));
            }
            
            // 심볼에 따라 아이콘 설정
            String symbol = asset.getSymbol();
            if (symbol.contains("BTC")) {
                assetIcon.setText("₿");
            } else if (symbol.contains("ETH")) {
                assetIcon.setText("Ξ");
            } else {
                assetIcon.setText("📈");
            }
            
            assetSymbol.setText(symbol);
            assetName.setText(getAssetName(symbol));
            
            // 가격 정보 (실제로는 MarketDataRepository에서 가져와야 함)
            assetPrice.setText(NumberFormatter.formatPrice(asset.getEntryPrice()));
            assetChange.setText("+2.45%");
            assetChange.setTextColor(itemView.getContext().getColor(R.color.tds_success_alt));
        }
        
        private String getAssetName(String symbol) {
            if (symbol.contains("BTC")) return "Bitcoin";
            if (symbol.contains("ETH")) return "Ethereum";
            if (symbol.contains("ADA")) return "Cardano";
            if (symbol.contains("SOL")) return "Solana";
            return "Unknown";
        }
    }
}

