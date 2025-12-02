package com.example.rsquare.data.remote;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Binance API 서비스 인터페이스
 */
public interface BinanceApiService {
    
    /**
     * 캔들스틱 데이터 조회 (klines)
     * 
     * @param symbol 거래 심볼 (예: BTCUSDT)
     * @param interval 간격 (1m, 5m, 15m, 30m, 1h, 4h, 1d 등)
     * @param limit 데이터 개수 (최대 1000)
     * @param startTime 시작 시간 (밀리초, 선택사항)
     * @param endTime 종료 시간 (밀리초, 선택사항)
     * 
     * 응답 형식: [[openTime, open, high, low, close, volume, closeTime, quoteVolume, trades, takerBuyBaseVolume, takerBuyQuoteVolume, ignore], ...]
     */
    @GET("api/v3/klines")
    Call<List<List<Object>>> getKlines(
        @Query("symbol") String symbol,
        @Query("interval") String interval,
        @Query("limit") Integer limit,
        @Query("startTime") Long startTime,
        @Query("endTime") Long endTime
    );
    /**
     * 거래소 정보 조회 (유효한 심볼 목록 확인용)
     */
    @GET("api/v3/exchangeInfo")
    Call<com.example.rsquare.data.remote.model.ExchangeInfoResponse> getExchangeInfo();
}

