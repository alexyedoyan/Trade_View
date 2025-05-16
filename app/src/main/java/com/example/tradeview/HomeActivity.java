package com.example.tradeview;
import android.util.Log;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import retrofit2.*;
import com.google.common.util.concurrent.AtomicDouble;

public class HomeActivity extends AppCompatActivity {

    // UI Components
    private ProgressBar progressBar;
    private TextView walletBalanceText;
    private RecyclerView walletRecyclerView;
    private EditText cryptoSearchInput, cryptoWalletInput, amountInput;
    private TextView priceTextView;
    private Button searchButton, chartButton, addCryptoButton, logoutButton;

    // Adapter
    private WalletAdapter walletAdapter;

    // Data
    private CryptoWallet cryptoWallet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize wallet with context
        cryptoWallet = new CryptoWallet(this);

        // Initialize views
        initViews();

        // Setup RecyclerView
        setupRecyclerView();

        // Setup button listeners
        setupButtonListeners();

        // Load initial data
        loadInitialData();
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
        logoutButton = findViewById(R.id.logoutButton);
    }

    private void setupRecyclerView() {
        walletAdapter = new WalletAdapter(new ArrayList<>());
        walletAdapter.setOnItemClickListener(this::handleWalletItemClick);
        walletRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        walletRecyclerView.setAdapter(walletAdapter);
    }

    private void handleWalletItemClick(int position, String symbol) {
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

    private void setupButtonListeners() {
        searchButton.setOnClickListener(v -> handleSearchPrice());
        chartButton.setOnClickListener(v -> handleOpenChart());
        addCryptoButton.setOnClickListener(v -> handleAddCrypto());
        logoutButton.setOnClickListener(v -> logoutUser());
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

        RetrofitClient.getClient().create(BinanceApiService.class)
                .getCryptoPrice(symbol)
                .enqueue(new Callback<CryptoModel>() {
                    @Override
                    public void onResponse(Call<CryptoModel> call, Response<CryptoModel> response) {
                        showProgress(false);
                        if (response.isSuccessful() && response.body() != null) {
                            displayCryptoPrice(response.body());
                        } else {
                            showToast("Ошибка получения данных");
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
        for (Map.Entry<String, Double> entry : cryptoWallet.getHoldings().entrySet()) {
            String symbol = entry.getKey();
            double amount = entry.getValue();

            getCurrentPrice(symbol, currentPrice -> {
                double value = amount * currentPrice;
                walletItems.add(new WalletItem(symbol, amount, value, currentPrice));

                if (walletItems.size() == cryptoWallet.getHoldings().size()) {
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
            public void onResponse(Call<CryptoModel> call, Response<CryptoModel> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        double price = Double.parseDouble(response.body().getPrice());
                        callback.onPriceReceived(price);
                    } catch (NumberFormatException e) {
                        callback.onPriceReceived(0);
                        Log.e("Wallet", "Error parsing price", e);
                    }
                } else {
                    callback.onPriceReceived(0);
                }
            }

            @Override
            public void onFailure(Call<CryptoModel> call, Throwable t) {
                callback.onPriceReceived(0);
                Log.e("Wallet", "API error for " + symbol, t);
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