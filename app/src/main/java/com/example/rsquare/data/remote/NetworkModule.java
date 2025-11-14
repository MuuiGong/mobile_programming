package com.example.rsquare.data.remote;

import com.example.rsquare.util.Constants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * 네트워크 모듈 - Retrofit 인스턴스 생성 및 관리
 */
public class NetworkModule {
    
    private static Retrofit retrofit = null;
    private static CoinGeckoApiService apiService = null;
    
    /**
     * Retrofit 인스턴스 생성
     */
    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            // HTTP 로깅 인터셉터
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            
            // OkHttp 클라이언트 설정
            OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
            
            // Gson 설정
            Gson gson = new GsonBuilder()
                .setLenient()
                .create();
            
            // Retrofit 빌드
            retrofit = new Retrofit.Builder()
                .baseUrl(Constants.COINGECKO_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        }
        return retrofit;
    }
    
    /**
     * CoinGecko API 서비스 인스턴스
     */
    public static CoinGeckoApiService getApiService() {
        if (apiService == null) {
            apiService = getRetrofitInstance().create(CoinGeckoApiService.class);
        }
        return apiService;
    }
}

