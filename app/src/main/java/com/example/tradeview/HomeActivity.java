package com.example.tradeview;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import android.view.Menu;
import android.view.MenuItem;

public class HomeActivity extends AppCompatActivity implements WalletAdapter.OnItemClickListener {

    // UI Components
    private ProgressBar progressBar;
    private TextView walletBalanceText;
    private RecyclerView walletRecyclerView;
    private Button newsButton;
    private EditText cryptoSearchInput, cryptoWalletInput, amountInput;
    private TextView priceTextView;
    private Button searchButton, chartButton, addCryptoButton;
    private Button predictButton;
    private Button logoutButton;

    // Adapter
    private WalletAdapter walletAdapter;

    // Data
    private CryptoWallet cryptoWallet;
    private PricePredictor pricePredictor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Button newsButton = findViewById(R.id.newsButton);
        newsButton.setOnClickListener(v -> {
            startActivity(new Intent(this, CryptoNewsActivity.class));
        });

        // Initialize wallet with context
        cryptoWallet = new CryptoWallet(this);
        pricePredictor = new PricePredictor();

        // Initialize views
        initViews();

        // Setup RecyclerView
        setupRecyclerView();

        // Setup button listeners
        setupButtonListeners();

        // Load initial data
        loadInitialData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_news) {
            startActivity(new Intent(this, CryptoNewsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initViews() {
        progressBar = findViewById(R.id.progressBar);
        walletBalanceText = findViewById(R.id.walletBalanceText);
        walletRecyclerView = findViewById(R.id.walletRecyclerView);
        cryptoSearchInput = findViewById(R.id.cryptoSearchInput);
        priceTextView = findViewById(R.id.priceTextView);
        cryptoWalletInput = findViewById(R.id.cryptoWalletInput);
        amountInput = findViewById(R.id.amountInput);
        searchButton = findViewById(R.id.searchButton);
        chartButton = findViewById(R.id.chartButton);
        addCryptoButton = findViewById(R.id.addCryptoButton);
        predictButton = findViewById(R.id.predictButton);
        logoutButton = findViewById(R.id.logoutButton);
        newsButton = findViewById(R.id.newsButton);
    }

    private void setupRecyclerView() {
        walletAdapter = new WalletAdapter(new ArrayList<>());
        walletAdapter.setOnItemClickListener(this);
        walletRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        walletRecyclerView.setAdapter(walletAdapter);
    }

    @Override
    public void onItemClick(int position, String symbol) {
        showDeleteConfirmationDialog(symbol);
    }

    private void showDeleteConfirmationDialog(String symbol) {
        new AlertDialog.Builder(this)
                .setTitle("Удаление актива")
                .setMessage("Вы уверены, что хотите удалить " + symbol + " из кошелька?")
                .setPositiveButton("Удалить", (dialog, which) -> deleteCryptoAsset(symbol))
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void deleteCryptoAsset(String symbol) {
        showProgress(true);
        cryptoWallet.removeCrypto(symbol, newBalance -> {
            runOnUiThread(() -> {
                updateWalletDisplay();
                showToast(symbol + " успешно удален");
                showProgress(false);
            });
        });
    }

    private void processPredictionData(List<List<String>> klines) {
        try {
            List<CandleStick> candles = new ArrayList<>();
            for (List<String> kline : klines) {
                float open = Float.parseFloat(kline.get(1));
                float high = Float.parseFloat(kline.get(2));
                float low = Float.parseFloat(kline.get(3));
                float close = Float.parseFloat(kline.get(4));
                candles.add(new CandleStick(open, high, low, close));
            }

            float predictedPrice = pricePredictor.predictNextPrice(candles);
            String result = String.format(Locale.US, "Прогноз: %.2f USD", predictedPrice);
            showToast(result);

        } catch (Exception e) {
            showToast("Ошибка обработки данных");
            Log.e("Prediction", "Error processing data", e);
        }
    }

    private void makePrediction(String cryptoName) {
        showToast("Прогнозируем для: " + cryptoName);
        showProgress(true);

        String symbol = cryptoName.endsWith("USDT") ? cryptoName : cryptoName + "USDT";
        BinanceApiService apiService = RetrofitClient.getClient().create(BinanceApiService.class);

        Call<List<List<String>>> call = apiService.getKlineData(symbol, "1d", 100);
        call.enqueue(new Callback<List<List<String>>>() {
            @Override
            public void onResponse(Call<List<List<String>>> call, Response<List<List<String>>> response) {
                showProgress(false);
                if (response.isSuccessful() && response.body() != null) {
                    processPredictionData(response.body());
                } else {
                    showToast("Ошибка получения данных");
                }
            }

            @Override
            public void onFailure(Call<List<List<String>>> call, Throwable t) {
                showProgress(false);
                showToast("Ошибка: " + t.getMessage());
            }
        });
    }

    private void setupButtonListeners() {
        searchButton.setOnClickListener(v -> handleSearchPrice());
        chartButton.setOnClickListener(v -> handleOpenChart());
        addCryptoButton.setOnClickListener(v -> handleAddCrypto());
        logoutButton.setOnClickListener(v -> logoutUser());
        newsButton = findViewById(R.id.newsButton);
        predictButton.setOnClickListener(v -> {
            String cryptoName = cryptoSearchInput.getText().toString().trim().toUpperCase();
            if (cryptoName.isEmpty()) {
                showToast("Введите название криптовалюты");
                return;
            }
            makePrediction(cryptoName);
        });
    }
    private void openNewsActivity() {
        startActivity(new Intent(this, CryptoNewsActivity.class));
    }

    private void loadInitialData() {
        showProgress(true);
        cryptoWallet.loadAssets(newBalance -> {
            runOnUiThread(() -> {
                if (newBalance >= 0) {
                    updateWalletDisplay();
                } else {
                    showToast("Ошибка загрузки данных");
                }
                showProgress(false);
            });
        });
    }

    private void handleSearchPrice() {
        String cryptoName = cryptoSearchInput.getText().toString().trim().toUpperCase();
        if (cryptoName.isEmpty()) {
            showToast("Введите название криптовалюты");
            return;
        }

        showProgress(true);
        String symbol = cryptoName.endsWith("USDT") ? cryptoName : cryptoName + "USDT";

        BinanceApiService apiService = RetrofitClient.getClient().create(BinanceApiService.class);
        apiService.getCryptoPrice(symbol).enqueue(new Callback<CryptoModel>() {
            @Override
            public void onResponse(@NonNull Call<CryptoModel> call, @NonNull Response<CryptoModel> response) {
                showProgress(false);
                if (response.isSuccessful() && response.body() != null) {
                    displayCryptoPrice(response.body());
                } else {
                    showToast("Ошибка получения данных");
                }
            }

            @Override
            public void onFailure(@NonNull Call<CryptoModel> call, @NonNull Throwable t) {
                showProgress(false);
                showToast("Ошибка сети: " + t.getMessage());
                Log.e("HomeActivity", "API Error", t);
            }
        });
    }

    private void displayCryptoPrice(CryptoModel crypto) {
        String displaySymbol = crypto.getSymbol().replace("USDT", "");
        String priceText = String.format(Locale.US, "%s: $%s", displaySymbol, crypto.getPrice());
        priceTextView.setText(priceText);
    }

    private void handleOpenChart() {
        String cryptoName = cryptoSearchInput.getText().toString().trim().toUpperCase();
        if (cryptoName.isEmpty()) {
            showToast("Введите название криптовалюты");
            return;
        }

        Intent intent = new Intent(this, ChartActivity.class);
        intent.putExtra("CRYPTO_NAME", cryptoName);
        startActivity(intent);
    }

    private void handleAddCrypto() {
        String symbol = cryptoWalletInput.getText().toString().trim().toUpperCase();
        String amountStr = amountInput.getText().toString().trim();

        if (symbol.isEmpty() || amountStr.isEmpty()) {
            showToast("Введите символ и количество");
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            showProgress(true);

            cryptoWallet.addCrypto(symbol, amount, newBalance -> {
                runOnUiThread(() -> {
                    updateWalletDisplay();
                    showToast(symbol + " добавлено в кошелек");
                    clearInputFields();
                    showProgress(false);
                });
            });
        } catch (NumberFormatException e) {
            showToast("Некорректное количество");
        }
    }

    private void updateWalletDisplay() {
        List<WalletItem> walletItems = new ArrayList<>();
        final int[] counter = {0};
        int totalItems = cryptoWallet.getHoldings().size();

        if (totalItems == 0) {
            walletAdapter.updateData(walletItems);
            updateTotalBalance();
            return;
        }

        for (Map.Entry<String, Double> entry : cryptoWallet.getHoldings().entrySet()) {
            String symbol = entry.getKey();
            double amount = entry.getValue();

            getCurrentPrice(symbol, currentPrice -> {
                double value = amount * currentPrice;
                walletItems.add(new WalletItem(symbol, amount, value, currentPrice));
                counter[0]++;

                if (counter[0] == totalItems) {
                    walletAdapter.updateData(walletItems);
                    updateTotalBalance();
                }
            });
        }
    }

    private void getCurrentPrice(String symbol, PriceCallback callback) {
        String apiSymbol = symbol + "USDT";
        BinanceApiService apiService = RetrofitClient.getClient().create(BinanceApiService.class);

        apiService.getCryptoPrice(apiSymbol).enqueue(new Callback<CryptoModel>() {
            @Override
            public void onResponse(@NonNull Call<CryptoModel> call, @NonNull Response<CryptoModel> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        double price = Double.parseDouble(response.body().getPrice());
                        callback.onPriceReceived(price);
                    } catch (NumberFormatException e) {
                        callback.onPriceReceived(0);
                        Log.e("HomeActivity", "Error parsing price", e);
                    }
                } else {
                    callback.onPriceReceived(0);
                }
            }

            @Override
            public void onFailure(@NonNull Call<CryptoModel> call, @NonNull Throwable t) {
                callback.onPriceReceived(0);
                Log.e("HomeActivity", "API error for " + symbol, t);
            }
        });
    }

    private void updateTotalBalance() {
        cryptoWallet.updateTotalBalance(balance -> {
            runOnUiThread(() -> {
                String balanceText = String.format(Locale.US, "Баланс: $%.2f", balance);
                walletBalanceText.setText(balanceText);
            });
        });
    }

    private void clearInputFields() {
        cryptoWalletInput.setText("");
        amountInput.setText("");
    }

    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateWalletDisplay();
    }

    interface PriceCallback {
        void onPriceReceived(double price);
    }
}