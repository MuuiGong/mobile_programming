package com.example.rsquare.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rsquare.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 종목 선택 RecyclerView Adapter
 */
public class SymbolAdapter extends RecyclerView.Adapter<SymbolAdapter.SymbolViewHolder> {
    
    public static class SymbolItem {
        public final String symbol;      // BTCUSDT
        public final String name;         // Bitcoin
        public final String code;         // BTC
        public final String icon;         // ₿
        
        public SymbolItem(String symbol, String name, String code, String icon) {
            this.symbol = symbol;
            this.name = name;
            this.code = code;
            this.icon = icon;
        }
    }
    
    private List<SymbolItem> items = new ArrayList<>();
    private List<SymbolItem> filteredItems = new ArrayList<>();
    private String selectedSymbol;
    private OnSymbolClickListener listener;
    
    public interface OnSymbolClickListener {
        void onSymbolClick(String symbol);
    }
    
    public SymbolAdapter(String selectedSymbol, OnSymbolClickListener listener) {
        this.selectedSymbol = selectedSymbol;
        this.listener = listener;
    }
    
    public void setItems(List<SymbolItem> items) {
        this.items = items;
        this.filteredItems = new ArrayList<>(items);
        notifyDataSetChanged();
    }
    
    public void filter(String query) {
        filteredItems.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredItems.addAll(items);
        } else {
            String lowerQuery = query.toLowerCase();
            for (SymbolItem item : items) {
                if (item.name.toLowerCase().contains(lowerQuery) ||
                    item.code.toLowerCase().contains(lowerQuery) ||
                    item.symbol.toLowerCase().contains(lowerQuery)) {
                    filteredItems.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public SymbolViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_symbol, parent, false);
        return new SymbolViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull SymbolViewHolder holder, int position) {
        SymbolItem item = filteredItems.get(position);
        holder.bind(item, item.symbol.equals(selectedSymbol), listener);
    }
    
    @Override
    public int getItemCount() {
        return filteredItems.size();
    }
    
    static class SymbolViewHolder extends RecyclerView.ViewHolder {
        private final RadioButton radioButton;
        private final TextView symbolName;
        private final TextView symbolCode;
        private final TextView symbolIcon;
        
        public SymbolViewHolder(@NonNull View itemView) {
            super(itemView);
            radioButton = itemView.findViewById(R.id.radio_button);
            symbolName = itemView.findViewById(R.id.symbol_name);
            symbolCode = itemView.findViewById(R.id.symbol_code);
            symbolIcon = itemView.findViewById(R.id.symbol_icon);
        }
        
        public void bind(SymbolItem item, boolean isSelected, OnSymbolClickListener listener) {
            symbolName.setText(item.name);
            symbolCode.setText(item.code);
            symbolIcon.setText(item.icon);
            radioButton.setChecked(isSelected);
            
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSymbolClick(item.symbol);
                }
            });
        }
    }
}
