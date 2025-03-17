package com.example.tradeview.api;

import com.example.tradeview.CryptoModel;
import com.example.tradeview.ExchangeInfo;
import com.example.tradeview.KlineData;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import java.util.List;

public interface BinanceApiService {
    @GET("api/v3/ticker/price")
    Call<CryptoModel> getCryptoPrice(@Query("symbol") String symbol);
    @GET("api/v3/ticker/price")
    Call<List<CryptoModel>> getAllCryptoPrices();
    @GET("api/v3/klines")
    Call<List<List<String>>> getKlineData(
            @Query("symbol") String symbol,
            @Query("interval") String interval,
            @Query("limit") int limit
    );
    @GET("api/v3/ticker/24hr")
    Call<CryptoModel> get24hrPriceChange(@Query("symbol") String symbol);
    @GET("api/v3/exchangeInfo")
    Call<ExchangeInfo> getExchangeInfo();
}