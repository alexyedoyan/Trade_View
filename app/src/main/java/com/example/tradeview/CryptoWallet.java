package com.example.tradeview;

import android.util.Log;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CryptoWallet {
    private final Map<String, Double> holdings;
    private double totalBalanceUSD;
    private final BinanceApiService apiService;

    public CryptoWallet() {
        holdings = new HashMap<>();
        apiService = RetrofitClient.getClient().create(BinanceApiService.class);
    }

    public interface WalletUpdateCallback {
        void onUpdated(double newBalance);
    }

    public void addCrypto(String symbol, double amount, WalletUpdateCallback callback) {
        symbol = symbol.toUpperCase();
        holdings.merge(symbol, amount, Double::sum);
        Log.d("Wallet", "Added " + amount + " " + symbol);
        updateTotalBalance(callback);
    }

    private void updateTotalBalance(WalletUpdateCallback callback) {
        totalBalanceUSD = 0;

        if (holdings.isEmpty()) {
            if (callback != null) callback.onUpdated(totalBalanceUSD);
            return;
        }

        AtomicInteger counter = new AtomicInteger(holdings.size());

        for (Map.Entry<String, Double> entry : holdings.entrySet()) {
            String symbol = entry.getKey() + "USDT";

            apiService.getCryptoPrice(symbol).enqueue(new Callback<CryptoModel>() {
                @Override
                public void onResponse(Call<CryptoModel> call, Response<CryptoModel> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            double price = Double.parseDouble(response.body().getPrice());
                            synchronized (this) {
                                totalBalanceUSD += price * entry.getValue();
                            }
                            Log.d("Wallet", "Updated price for " + symbol + ": " + price);
                        } catch (NumberFormatException e) {
                            Log.e("Wallet", "Error parsing price", e);
                        }
                    }

                    if (counter.decrementAndGet() == 0 && callback != null) {
                        callback.onUpdated(totalBalanceUSD);
                    }
                }

                @Override
                public void onFailure(Call<CryptoModel> call, Throwable t) {
                    Log.e("Wallet", "API error for " + symbol, t);
                    if (counter.decrementAndGet() == 0 && callback != null) {
                        callback.onUpdated(totalBalanceUSD);
                    }
                }
            });
        }
    }

    public Map<String, Double> getHoldings() {
        return new HashMap<>(holdings);
    }

    public double getTotalBalanceUSD() {
        return totalBalanceUSD;
    }
}