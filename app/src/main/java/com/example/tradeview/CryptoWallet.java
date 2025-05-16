package com.example.tradeview;
import com.google.common.util.concurrent.AtomicDouble;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import retrofit2.*;

public class CryptoWallet {
    private final Map<String, Double> holdings;
    private double totalBalanceUSD;
    private final BinanceApiService apiService;
    private final FirebaseFirestore db;
    private final String userId;
    private final SharedPreferences sharedPrefs;
    private final Gson gson = new Gson();

    public interface WalletUpdateCallback {
        void onUpdated(double newBalance);
    }

    public CryptoWallet(Context context) {
        this.apiService = RetrofitClient.getClient().create(BinanceApiService.class);
        this.db = FirebaseFirestore.getInstance();
        this.userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        this.sharedPrefs = context.getSharedPreferences("CryptoWallet", Context.MODE_PRIVATE);

        // Настройки Firestore
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        db.setFirestoreSettings(settings);

        // Загружаем данные из SharedPreferences
        this.holdings = loadLocalData();
    }

    private Map<String, Double> loadLocalData() {
        String json = sharedPrefs.getString("holdings", "");
        if (!json.isEmpty()) {
            Type type = new TypeToken<Map<String, Double>>(){}.getType();
            return gson.fromJson(json, type);
        }
        return new HashMap<>();
    }

    private void saveLocalData() {
        String json = gson.toJson(holdings);
        sharedPrefs.edit().putString("holdings", json).apply();
    }

    public void loadAssets(WalletUpdateCallback callback) {
        // Сначала пробуем загрузить из Firestore
        db.collection("users").document(userId).collection("wallet")
                .get()
                .addOnSuccessListener(query -> {
                    Map<String, Double> firestoreData = new HashMap<>();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        String symbol = doc.getId();
                        Double amount = doc.getDouble("amount");
                        if (amount != null) {
                            firestoreData.put(symbol, amount);
                        }
                    }

                    // Объединяем с локальными данными
                    holdings.putAll(firestoreData);
                    saveLocalData();
                    updateTotalBalance(callback);
                })
                .addOnFailureListener(e -> {
                    Log.e("Wallet", "Firestore load error, using local data", e);
                    // Если ошибка Firestore, используем локальные данные
                    updateTotalBalance(callback);
                });
    }

    public void addCrypto(String symbol, double amount, WalletUpdateCallback callback) {
        final String finalSymbol = symbol.toUpperCase();

        // Обновляем локально
        holdings.merge(finalSymbol, amount, Double::sum);
        saveLocalData();

        // Синхронизируем с Firestore
        Map<String, Object> data = new HashMap<>();
        data.put("amount", holdings.get(finalSymbol));
        data.put("timestamp", System.currentTimeMillis());

        db.collection("users").document(userId)
                .collection("wallet").document(finalSymbol)
                .set(data)
                .addOnSuccessListener(v -> updateTotalBalance(callback))
                .addOnFailureListener(e -> {
                    Log.e("Wallet", "Firestore add error", e);
                    updateTotalBalance(callback); // Все равно обновляем баланс
                });
    }

    public void removeCrypto(String symbol, WalletUpdateCallback callback) {
        final String finalSymbol = symbol.toUpperCase();

        if (!holdings.containsKey(finalSymbol)) {
            if (callback != null) callback.onUpdated(totalBalanceUSD);
            return;
        }

        // Удаляем локально
        holdings.remove(finalSymbol);
        saveLocalData();

        // Удаляем из Firestore
        db.collection("users").document(userId)
                .collection("wallet").document(finalSymbol)
                .delete()
                .addOnSuccessListener(v -> updateTotalBalance(callback))
                .addOnFailureListener(e -> {
                    Log.e("Wallet", "Firestore delete error", e);
                    updateTotalBalance(callback); // Все равно обновляем баланс
                });
    }

    public void updateTotalBalance(WalletUpdateCallback callback) {
        if (holdings.isEmpty()) {
            if (callback != null) callback.onUpdated(0);
            return;
        }

        AtomicInteger counter = new AtomicInteger(holdings.size());
        AtomicDouble totalBalance = new AtomicDouble(0);

        for (Map.Entry<String, Double> entry : holdings.entrySet()) {
            String symbol = entry.getKey() + "USDT";
            double amount = entry.getValue();

            apiService.getCryptoPrice(symbol).enqueue(new Callback<CryptoModel>() {
                @Override
                public void onResponse(Call<CryptoModel> call, Response<CryptoModel> response) {
                    double value = 0;
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            value = Double.parseDouble(response.body().getPrice()) * amount;
                        } catch (NumberFormatException e) {
                            Log.e("Wallet", "Error parsing price", e);
                        }
                    }

                    totalBalance.addAndGet(value);
                    if (counter.decrementAndGet() == 0 && callback != null) {
                        callback.onUpdated(totalBalance.get());
                    }
                }

                @Override
                public void onFailure(Call<CryptoModel> call, Throwable t) {
                    if (counter.decrementAndGet() == 0 && callback != null) {
                        callback.onUpdated(totalBalance.get());
                    }
                    Log.e("Wallet", "API error for " + symbol, t);
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