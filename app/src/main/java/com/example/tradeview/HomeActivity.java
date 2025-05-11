package com.example.tradeview;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {

    private CryptoWallet cryptoWallet;
    private TextView walletBalanceText;
    private RecyclerView walletRecyclerView;
    private WalletAdapter walletAdapter;
    private EditText cryptoWalletInput, amountInput, cryptoSearchInput;
    private TextView priceTextView;
    private ProgressBar progressBar;
    private Button searchButton, chartButton, addCryptoButton, logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initViews();
        cryptoWallet = new CryptoWallet();
        setupWalletUI();
        setupButtons();
        updateWalletData(); // Первоначальная загрузка данных
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateWalletData(); // Обновляем при возвращении на экран
    }

    private void initViews() {
        walletBalanceText = findViewById(R.id.walletBalanceText);
        walletRecyclerView = findViewById(R.id.walletRecyclerView);
        cryptoWalletInput = findViewById(R.id.cryptoWalletInput);
        amountInput = findViewById(R.id.amountInput);
        cryptoSearchInput = findViewById(R.id.cryptoSearchInput);
        priceTextView = findViewById(R.id.priceTextView);
        progressBar = findViewById(R.id.progressBar);

        searchButton = findViewById(R.id.searchButton);
        chartButton = findViewById(R.id.chartButton);
        addCryptoButton = findViewById(R.id.addCryptoButton);
        logoutButton = findViewById(R.id.logoutButton);
    }

    private void setupWalletUI() {
        walletRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        walletAdapter = new WalletAdapter(new ArrayList<>());
        walletRecyclerView.setAdapter(walletAdapter);
    }

    private void setupButtons() {
        addCryptoButton.setOnClickListener(v -> handleAddCrypto());
        searchButton.setOnClickListener(v -> handleSearchPrice());
        chartButton.setOnClickListener(v -> handleOpenChart());
        logoutButton.setOnClickListener(v -> logoutUser());
    }

    private void handleAddCrypto() {
        String symbol = cryptoWalletInput.getText().toString().trim().toUpperCase();
        String amountStr = amountInput.getText().toString().trim();

        if (validateInput(symbol, amountStr)) {
            try {
                double amount = Double.parseDouble(amountStr);
                addCryptoToWallet(symbol, amount);
            } catch (NumberFormatException e) {
                showToast("Некорректная сумма");
            }
        } else {
            showToast("Введите криптовалюту и количество");
        }
    }

    private void addCryptoToWallet(String symbol, double amount) {
        showProgress(true);
        cryptoWallet.addCrypto(symbol, amount, new CryptoWallet.WalletUpdateCallback() {
            @Override
            public void onUpdated(double newBalance) {
                runOnUiThread(() -> {
                    updateWalletData(); // Полное обновление данных после добавления
                    showToast(symbol + " успешно добавлен");
                    clearInputFields();
                });
            }
        });
    }

    private void updateWalletData() {
        if (cryptoWallet == null) {
            return;
        }

        showProgress(true);
        Map<String, Double> holdings = cryptoWallet.getHoldings();

        if (holdings.isEmpty()) {
            walletAdapter.updateData(new ArrayList<>());
            walletBalanceText.setText("Balance: $0.00");
            showProgress(false);
            return;
        }

        List<WalletItem> walletItems = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger(holdings.size());
        final double[] totalBalance = {0};

        for (Map.Entry<String, Double> entry : holdings.entrySet()) {
            String symbol = entry.getKey();
            double amount = entry.getValue();

            fetchCurrentPrice(symbol, (price, error) -> {
                double value = 0;
                if (error == null) {
                    value = price * amount;
                    Log.d("WalletUpdate", symbol + ": " + amount + " x $" + price + " = $" + value);
                } else {
                    Log.e("WalletError", "Ошибка получения цены для " + symbol, error);
                }

                walletItems.add(new WalletItem(symbol, amount, value));
                totalBalance[0] += value;

                if (counter.decrementAndGet() == 0) {
                    runOnUiThread(() -> {
                        walletAdapter.updateData(walletItems);
                        walletBalanceText.setText(String.format(Locale.US, "Balance: $%.2f", totalBalance[0]));
                        showProgress(false);
                        Log.d("WalletUpdate", "Итоговый баланс: $" + totalBalance[0]);
                    });
                }
            });
        }
    }

    private void fetchCurrentPrice(String symbol, PriceCallback callback) {
        String tradingPair = symbol + "USDT";
        BinanceApiService apiService = RetrofitClient.getClient().create(BinanceApiService.class);

        apiService.getCryptoPrice(tradingPair).enqueue(new Callback<CryptoModel>() {
            @Override
            public void onResponse(Call<CryptoModel> call, Response<CryptoModel> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        double price = Double.parseDouble(response.body().getPrice());
                        callback.onPriceLoaded(price, null);
                    } catch (NumberFormatException e) {
                        callback.onPriceLoaded(0, e);
                    }
                } else {
                    callback.onPriceLoaded(0, new Exception("API error: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<CryptoModel> call, Throwable t) {
                callback.onPriceLoaded(0, t);
            }
        });
    }

    private void handleSearchPrice() {
        String cryptoName = cryptoSearchInput.getText().toString().trim().toUpperCase();
        if (!cryptoName.isEmpty()) {
            showProgress(true);
            String symbol = cryptoName.endsWith("USDT") ? cryptoName : cryptoName + "USDT";
            fetchCryptoPrice(symbol);
        } else {
            showToast("Введите название криптовалюты");
        }
    }

    private void fetchCryptoPrice(String symbol) {
        BinanceApiService apiService = RetrofitClient.getClient().create(BinanceApiService.class);
        Call<CryptoModel> call = apiService.getCryptoPrice(symbol);

        call.enqueue(new Callback<CryptoModel>() {
            @Override
            public void onResponse(Call<CryptoModel> call, Response<CryptoModel> response) {
                showProgress(false);
                if (response.isSuccessful() && response.body() != null) {
                    displayCryptoPrice(response.body());
                } else {
                    showToast("Ошибка при получении данных");
                }
            }

            @Override
            public void onFailure(Call<CryptoModel> call, Throwable t) {
                showProgress(false);
                showToast("Ошибка сети: " + t.getMessage());
            }
        });
    }

    private void displayCryptoPrice(CryptoModel crypto) {
        String displaySymbol = crypto.getSymbol().replace("USDT", "");
        priceTextView.setText(String.format(Locale.US,
                "%s: $%s", displaySymbol, crypto.getPrice()));
    }

    private void handleOpenChart() {
        String cryptoName = cryptoSearchInput.getText().toString().trim().toUpperCase();
        if (!cryptoName.isEmpty()) {
            Intent intent = new Intent(this, ChartActivity.class);
            intent.putExtra("CRYPTO_NAME", cryptoName);
            startActivity(intent);
        } else {
            showToast("Введите название криптовалюты");
        }
    }

    private boolean validateInput(String symbol, String amount) {
        return !symbol.isEmpty() && !amount.isEmpty();
    }

    private void clearInputFields() {
        cryptoWalletInput.setText("");
        amountInput.setText("");
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    interface PriceCallback {
        void onPriceLoaded(double price, Throwable error);
    }
}