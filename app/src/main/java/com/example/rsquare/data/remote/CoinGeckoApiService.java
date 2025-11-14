package com.example.rsquare.data.remote;

import com.example.rsquare.data.remote.model.CoinListItem;
import com.example.rsquare.data.remote.model.CoinMarketChart;
import com.example.rsquare.data.remote.model.CoinPrice;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * CoinGecko API 서비스 인터페이스
 */
public interface CoinGeckoApiService {
    
    /**
     * 코인 시장 데이터 조회 (가격, 시가총액 등)
     * 
     * @param vsCurrency 기준 통화 (usd, krw 등)
     * @param ids 코인 ID 목록 (쉼표로 구분)
     * @param order 정렬 방식
     * @param perPage 페이지당 결과 수
     * @param page 페이지 번호
     * @param sparkline 스파크라인 포함 여부
     */
    @GET("coins/markets")
    Call<List<CoinPrice>> getCoinsMarket(
        @Query("vs_currency") String vsCurrency,
        @Query("ids") String ids,
        @Query("order") String order,
        @Query("per_page") int perPage,
        @Query("page") int page,
        @Query("sparkline") boolean sparkline
    );
    
    /**
     * 특정 코인의 시장 차트 데이터 조회
     * 
     * @param coinId 코인 ID (예: bitcoin, ethereum)
     * @param vsCurrency 기준 통화
     * @param days 조회 기간 (일)
     */
    @GET("coins/{id}/market_chart")
    Call<CoinMarketChart> getMarketChart(
        @Path("id") String coinId,
        @Query("vs_currency") String vsCurrency,
        @Query("days") int days
    );
    
    /**
     * 코인 목록 조회
     */
    @GET("coins/list")
    Call<List<CoinListItem>> getCoinList();
    
    /**
     * 단순 가격 조회 (여러 코인)
     * 
     * @param ids 코인 ID 목록 (쉼표로 구분)
     * @param vsCurrencies 기준 통화 목록
     * @param includeMarketCap 시가총액 포함 여부
     * @param include24hrVol 24시간 거래량 포함 여부
     * @param include24hrChange 24시간 변동률 포함 여부
     */
    @GET("simple/price")
    Call<Object> getSimplePrice(
        @Query("ids") String ids,
        @Query("vs_currencies") String vsCurrencies,
        @Query("include_market_cap") boolean includeMarketCap,
        @Query("include_24hr_vol") boolean include24hrVol,
        @Query("include_24hr_change") boolean include24hrChange
    );
}

