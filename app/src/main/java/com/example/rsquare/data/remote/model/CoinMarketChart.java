package com.example.rsquare.data.remote.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * CoinGecko API 차트 데이터 응답 모델
 */
public class CoinMarketChart {
    
    @SerializedName("prices")
    private List<List<Double>> prices;
    
    @SerializedName("market_caps")
    private List<List<Double>> marketCaps;
    
    @SerializedName("total_volumes")
    private List<List<Double>> totalVolumes;
    
    // Getters and Setters
    public List<List<Double>> getPrices() {
        return prices;
    }
    
    public void setPrices(List<List<Double>> prices) {
        this.prices = prices;
    }
    
    public List<List<Double>> getMarketCaps() {
        return marketCaps;
    }
    
    public void setMarketCaps(List<List<Double>> marketCaps) {
        this.marketCaps = marketCaps;
    }
    
    public List<List<Double>> getTotalVolumes() {
        return totalVolumes;
    }
    
    public void setTotalVolumes(List<List<Double>> totalVolumes) {
        this.totalVolumes = totalVolumes;
    }
    
    /**
     * 차트 데이터 포인트 클래스
     */
    public static class PricePoint {
        private long timestamp;
        private double price;
        
        public PricePoint(long timestamp, double price) {
            this.timestamp = timestamp;
            this.price = price;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
        
        public double getPrice() {
            return price;
        }
        
        public void setPrice(double price) {
            this.price = price;
        }
    }
    
    /**
     * 가격 배열을 PricePoint 리스트로 변환
     */
    public List<PricePoint> getPricePoints() {
        if (prices == null) return null;
        
        List<PricePoint> points = new java.util.ArrayList<>();
        for (List<Double> price : prices) {
            if (price.size() >= 2) {
                points.add(new PricePoint(price.get(0).longValue(), price.get(1)));
            }
        }
        return points;
    }
}

