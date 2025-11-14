package com.example.rsquare.data.remote.model;

import com.google.gson.annotations.SerializedName;

/**
 * CoinGecko API 코인 목록 아이템 모델
 */
public class CoinListItem {
    
    @SerializedName("id")
    private String id;
    
    @SerializedName("symbol")
    private String symbol;
    
    @SerializedName("name")
    private String name;
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
}

