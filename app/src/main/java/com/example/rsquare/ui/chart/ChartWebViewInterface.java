package com.example.rsquare.ui.chart;

import android.webkit.JavascriptInterface;

/**
 * Chart WebView JavaScript Interface
 * WebView와 Android 간 통신 브릿지
 */
public class ChartWebViewInterface {
    
    private final ChartCallback callback;
    
    public ChartWebViewInterface(ChartCallback callback) {
        this.callback = callback;
    }
    
    /**
     * 현재 가격 변경 시 호출
     */
    @JavascriptInterface
    public void onPriceChanged(double price) {
        if (callback != null) {
            callback.onPriceChanged(price);
        }
    }
    
    /**
     * 진입 가격 변경 시 호출
     */
    @JavascriptInterface
    public void onEntryPriceChanged(double price) {
        if (callback != null) {
            callback.onEntryPriceChanged(price);
        }
    }
    
    /**
     * 익절 가격 변경 시 호출
     */
    @JavascriptInterface
    public void onTakeProfitChanged(double price) {
        if (callback != null) {
            callback.onTakeProfitChanged(price);
        }
    }
    
    /**
     * 손절 가격 변경 시 호출
     */
    @JavascriptInterface
    public void onStopLossChanged(double price) {
        if (callback != null) {
            callback.onStopLossChanged(price);
        }
    }
    
    /**
     * 콜백 인터페이스
     */
    public interface ChartCallback {
        void onPriceChanged(double price);
        void onEntryPriceChanged(double price);
        void onTakeProfitChanged(double price);
        void onStopLossChanged(double price);
    }
}

