package com.example.tradeview;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.tradeview.R;
import com.example.tradeview.api.BinanceApiService;
import com.example.tradeview.CryptoModel;
import com.example.tradeview.RetrofitClient;
import com.google.firebase.auth.FirebaseAuth;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private TextView priceTextView;
    private EditText cryptoInput;
    private Button searchButton;
    private Button chartButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Инициализация Firebase Auth
        auth = FirebaseAuth.getInstance();

        // Инициализация элементов интерфейса
        cryptoInput = findViewById(R.id.cryptoInput);
        searchButton = findViewById(R.id.searchButton);
        priceTextView = findViewById(R.id.priceTextView);
        chartButton = findViewById(R.id.chartButton);

        // Обработка нажатия на кнопку поиска
        searchButton.setOnClickListener(v -> {
            String cryptoName = cryptoInput.getText().toString().trim().toUpperCase();
            if (!cryptoName.isEmpty()) {
                getCryptoPrice(cryptoName);
            } else {
                Toast.makeText(this, "Введите название криптовалюты", Toast.LENGTH_SHORT).show();
            }
        });

        // Обработка нажатия на кнопку перехода к графику
        chartButton.setOnClickListener(v -> {
            String cryptoName = cryptoInput.getText().toString().trim().toUpperCase();
            if (!cryptoName.isEmpty()) {
                Intent intent = new Intent(HomeActivity.this, ChartActivity.class);
                intent.putExtra("CRYPTO_NAME", cryptoName); // Передаем название криптовалюты
                startActivity(intent);
            } else {
                Toast.makeText(this, "Введите название криптовалюты", Toast.LENGTH_SHORT).show();
            }
        });

        // Выход из системы
        findViewById(R.id.logoutButton).setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(HomeActivity.this, LoginActivity.class));
            finish();
        });
    }

    // Получение цены конкретной криптовалюты
    private void getCryptoPrice(String cryptoName) {
        BinanceApiService apiService = RetrofitClient.getClient().create(BinanceApiService.class);

        // Запрос для получения цены конкретной криптовалюты
        Call<CryptoModel> call = apiService.getCryptoPrice(cryptoName);
        call.enqueue(new Callback<CryptoModel>() {
            @Override
            public void onResponse(Call<CryptoModel> call, Response<CryptoModel> response) {
                if (response.isSuccessful() && response.body() != null) {
                    CryptoModel crypto = response.body();
                    priceTextView.setText("Цена " + crypto.getSymbol() + ": " + crypto.getPrice());
                } else {
                    Toast.makeText(HomeActivity.this, "Криптовалюта не найдена", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<CryptoModel> call, Throwable t) {
                Toast.makeText(HomeActivity.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}