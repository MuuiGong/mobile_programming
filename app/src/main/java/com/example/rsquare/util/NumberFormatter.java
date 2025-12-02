package com.example.rsquare.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * 숫자 포맷팅 유틸리티
 */
public class NumberFormatter {
    
    private static final DecimalFormat priceFormat = new DecimalFormat("#,##0.00");
    private static final DecimalFormat percentFormat = new DecimalFormat("#,##0.00");
    private static final DecimalFormat ratioFormat = new DecimalFormat("0.00");
    
    /**
     * 가격 포맷 (소수점 2자리)
     */
    public static String formatPrice(double price) {
        return "$" + priceFormat.format(price);
    }
    
    /**
     * 퍼센트 포맷
     */
    public static String formatPercent(double value) {
        return percentFormat.format(value) + "%";
    }
    
    /**
     * 비율 포맷 (R:R)
     */
    public static String formatRatio(double ratio) {
        return ratioFormat.format(ratio);
    }
    
    /**
     * 손익 포맷 (색상용 부호 포함)
     */
    public static String formatPnL(double pnl) {
        String sign = pnl >= 0 ? "+" : "";
        return sign + priceFormat.format(pnl);
    }
    
    /**
     * 큰 숫자 포맷 (K, M 단위)
     */
    public static String formatLargeNumber(double number) {
        if (number >= 1_000_000) {
            return String.format(Locale.US, "%.1fM", number / 1_000_000);
        } else if (number >= 1_000) {
            return String.format(Locale.US, "%.1fK", number / 1_000);
        } else {
            return priceFormat.format(number);
        }
    }
    
    /**
     * 수량 포맷 (소수점 최대 8자리, 불필요한 0 제거)
     */
    public static String formatQuantity(double quantity) {
        // 소수점 이하 0 제거를 위해 포맷팅
        if (quantity == (long) quantity) {
            return String.format(Locale.US, "%.0f", quantity);
        } else {
            // 소수점이 있으면 최대 8자리까지 표시하되, 끝의 0은 제거
            String formatted = String.format(Locale.US, "%.8f", quantity);
            // 끝의 0 제거
            formatted = formatted.replaceAll("0+$", "");
            formatted = formatted.replaceAll("\\.$", "");
            return formatted;
        }
    }
}

