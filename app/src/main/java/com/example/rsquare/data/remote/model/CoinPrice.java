package com.example.rsquare.data.remote.model;

import com.google.gson.annotations.SerializedName;

/**
 * CoinGecko API 가격 응답 모델
 */
public class CoinPrice {
    
    @SerializedName("id")
    private String id;
    
    @SerializedName("symbol")
    private String symbol;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("current_price")
    private double currentPrice;
    
    @SerializedName("price_change_24h")
    private double priceChange24h;
    
    @SerializedName("price_change_percentage_24h")
    private double priceChangePercentage24h;
    
    @SerializedName("market_cap")
    private double marketCap;
    
    @SerializedName("total_volume")
    private double totalVolume;
    
    @SerializedName("high_24h")
    private double high24h;
    
    @SerializedName("low_24h")
    private double low24h;
    
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
    
    public double getCurrentPrice() {
        return currentPrice;
    }
    
    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }
    
    public double getPriceChange24h() {
        return priceChange24h;
    }
    
    public void setPriceChange24h(double priceChange24h) {
        this.priceChange24h = priceChange24h;
    }
    
    public double getPriceChangePercentage24h() {
        return priceChangePercentage24h;
    }
    
    public void setPriceChangePercentage24h(double priceChangePercentage24h) {
        this.priceChangePercentage24h = priceChangePercentage24h;
    }
    
    public double getMarketCap() {
        return marketCap;
    }
    
    public void setMarketCap(double marketCap) {
        this.marketCap = marketCap;
    }
    
    public double getTotalVolume() {
        return totalVolume;
    }
    
    public void setTotalVolume(double totalVolume) {
        this.totalVolume = totalVolume;
    }
    
    public double getHigh24h() {
        return high24h;
    }
    
    public void setHigh24h(double high24h) {
        this.high24h = high24h;
    }
    
    public double getLow24h() {
        return low24h;
    }
    
    public void setLow24h(double low24h) {
        this.low24h = low24h;
    }
    @SerializedName("image")
    private String image;

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}

