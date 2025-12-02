package com.example.rsquare.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

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
    
    private java.util.Map<String, Double> changePercents = new java.util.HashMap<>();
    
    // 심볼 -> 인덱스 매핑을 위한 맵
    private final java.util.Map<String, Integer> symbolToIndexMap = new java.util.HashMap<>();

    public void setAssets(List<Position> newAssets) {
        final List<Position> oldAssets = new ArrayList<>(this.assets);
        this.assets = newAssets != null ? newAssets : new ArrayList<>();
        
        // 인덱스 맵 업데이트
        symbolToIndexMap.clear();
        for (int i = 0; i < this.assets.size(); i++) {
            String symbol = this.assets.get(i).getSymbol();
            if (symbol != null) {
                symbolToIndexMap.put(symbol.toUpperCase(), i);
            }
        }
        
        androidx.recyclerview.widget.DiffUtil.DiffResult diffResult = androidx.recyclerview.widget.DiffUtil.calculateDiff(new androidx.recyclerview.widget.DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldAssets.size();
            }

            @Override
            public int getNewListSize() {
                return assets.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                Position oldItem = oldAssets.get(oldItemPosition);
                Position newItem = assets.get(newItemPosition);
                return oldItem.getSymbol() != null && oldItem.getSymbol().equals(newItem.getSymbol());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Position oldItem = oldAssets.get(oldItemPosition);
                Position newItem = assets.get(newItemPosition);
                return oldItem.getEntryPrice() == newItem.getEntryPrice() &&
                       (oldItem.getLogoUrl() == null ? newItem.getLogoUrl() == null : oldItem.getLogoUrl().equals(newItem.getLogoUrl()));
            }
        });
        
        diffResult.dispatchUpdatesTo(this);
    }
    
    /**
     * 초기 가격 데이터 설정 (알림 없음)
     */
    public void setInitialPrices(java.util.Map<String, Double> initialChanges) {
        if (initialChanges != null) {
            this.changePercents.putAll(initialChanges);
        }
    }

    /**
     * 실시간 가격 업데이트 (최적화됨)
     */
    public void updatePrice(String symbol, double price, double changePercent) {
        if (symbol == null) return;
        
        changePercents.put(symbol, changePercent);
        
        // O(1) 조회
        Integer index = symbolToIndexMap.get(symbol.toUpperCase());
        if (index != null && index < assets.size()) {
            Position asset = assets.get(index);
            // 심볼이 일치하는지 이중 확인 (안전장치)
            if (asset.getSymbol().equalsIgnoreCase(symbol)) {
                asset.setEntryPrice(price);
                notifyItemChanged(index, "PRICE_UPDATE");
            }
        }
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
        onBindViewHolder(holder, position, java.util.Collections.emptyList());
    }
    
    @Override
    public void onBindViewHolder(@NonNull AssetViewHolder holder, int position, @NonNull List<Object> payloads) {
        try {
            Position asset = assets.get(position);
            boolean isSelected = (selectedPosition == position);
            Double changePercent = changePercents.get(asset.getSymbol());
            
            if (!payloads.isEmpty()) {
                holder.bindPrice(asset, changePercent);
            } else {
                holder.bind(asset, isSelected, changePercent);
            }
            
            androidx.cardview.widget.CardView cardView = (androidx.cardview.widget.CardView) holder.itemView;
            cardView.setClickable(true);
            cardView.setFocusable(true);
            
            holder.itemView.setOnClickListener(v -> {
                int previousPosition = selectedPosition;
                selectedPosition = position;
                if (previousPosition != -1 && previousPosition != position) {
                    notifyItemChanged(previousPosition);
                }
                notifyItemChanged(position);
                if (listener != null) {
                    listener.onAssetClick(asset);
                }
            });
        } catch (Exception e) {
            android.util.Log.e("AssetCardAdapter", "Error binding view holder", e);
        }
    }
    
    @Override
    public int getItemCount() {
        return assets.size();
    }
    
    static class AssetViewHolder extends RecyclerView.ViewHolder {
        private androidx.cardview.widget.CardView cardView;
        private ImageView assetIcon;
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
        
        public void bind(Position asset, boolean isSelected, Double changePercent) {
            if (isSelected) {
                cardView.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(
                    itemView.getContext().getColor(R.color.tds_blue_400)));
            } else {
                cardView.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(
                    itemView.getContext().getColor(R.color.tds_bg_dark)));
            }
            
            String logoUrl = asset.getLogoUrl();
            if (logoUrl != null && !logoUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                    .load(logoUrl)
                    .placeholder(R.drawable.badge_background)
                    .error(R.drawable.badge_background)
                    .into(assetIcon);
            } else {
                assetIcon.setImageResource(R.drawable.badge_background);
            }
            
            assetSymbol.setText(asset.getSymbol());
            
            // 디버깅 로그 추가
            String name = getAssetName(asset.getSymbol());
            // android.util.Log.d("AssetCardAdapter", "Symbol: " + asset.getSymbol() + ", Name: " + name);
            assetName.setText(name);
            
            bindPrice(asset, changePercent);
        }
        
        public void bindPrice(Position asset, Double changePercent) {
            assetPrice.setText(NumberFormatter.formatPrice(asset.getEntryPrice()));
            
            if (changePercent != null) {
                String sign = changePercent >= 0 ? "+" : "";
                assetChange.setText(String.format(Locale.US, "%s%.2f%%", sign, changePercent));
                
                if (changePercent >= 0) {
                    assetChange.setTextColor(itemView.getContext().getColor(R.color.tds_success_alt));
                } else {
                    assetChange.setTextColor(itemView.getContext().getColor(R.color.tds_error));
                }
            } else {
                assetChange.setText("-");
                assetChange.setTextColor(itemView.getContext().getColor(R.color.tds_text_secondary));
            }
        }
        
        private String getAssetName(String symbol) {
            if (symbol == null) return "";
            String s = symbol.toUpperCase();
            if (s.contains("BTC")) return "Bitcoin";
            if (s.contains("ETH")) return "Ethereum";
            if (s.contains("ADA")) return "Cardano";
            if (s.contains("SOL")) return "Solana";
            if (s.contains("XRP")) return "Ripple";
            if (s.contains("DOGE")) return "Dogecoin";
            if (s.contains("DOT")) return "Polkadot";
            if (s.contains("AVAX")) return "Avalanche";
            if (s.contains("MATIC")) return "Polygon";
            if (s.contains("LTC")) return "Litecoin";
            if (s.contains("LINK")) return "Chainlink";
            if (s.contains("UNI")) return "Uniswap";
            if (s.contains("ATOM")) return "Cosmos";
            return s.replace("USDT", "");
        }
    }
}

