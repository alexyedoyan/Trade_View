package com.example.tradeview;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.tradeview.R;
import com.example.tradeview.api.BinanceApiService;
import com.example.tradeview.CryptoModel;
import com.example.tradeview.RetrofitClient;
import com.google.firebase.auth.FirebaseAuth;
import android.Manifest;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private RecyclerView cryptoRecyclerView;
    private ImageView chartImageView;
    private TextView priceTextView;
    private EditText cryptoInput;
    private Button searchButton;
    private static final int PICK_IMAGE = 1;
    private static final int REQUEST_PERMISSION = 2;
    private List<CryptoModel> cryptoList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Инициализация Firebase Auth
        auth = FirebaseAuth.getInstance();

        // Инициализация элементов интерфейса
        cryptoRecyclerView = findViewById(R.id.cryptoRecyclerView);
        cryptoRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        cryptoInput = findViewById(R.id.cryptoInput);
        searchButton = findViewById(R.id.searchButton);
        priceTextView = findViewById(R.id.priceTextView);
        chartImageView = findViewById(R.id.chartImageView);

        // Загрузка данных о криптовалютах
        loadCryptoData();

        // Обработка нажатия на кнопку поиска
        searchButton.setOnClickListener(v -> {
            String cryptoName = cryptoInput.getText().toString().trim().toUpperCase();
            if (!cryptoName.isEmpty()) {
                getCryptoPrice(cryptoName);
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

        // Загрузка фото графика
        Button uploadPhotoButton = findViewById(R.id.uploadPhotoButton);
        uploadPhotoButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_PERMISSION);
            } else {
                openGallery();
            }
        });
    }

    // Загрузка данных о криптовалютах
    private void loadCryptoData() {
        BinanceApiService apiService = RetrofitClient.getClient().create(BinanceApiService.class);

        // Пример запроса для получения всех текущих цен
        Call<List<CryptoModel>> call = apiService.getAllCryptoPrices();
        call.enqueue(new Callback<List<CryptoModel>>() {
            @Override
            public void onResponse(Call<List<CryptoModel>> call, Response<List<CryptoModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    cryptoList.clear();
                    cryptoList.addAll(response.body());
                    // Обновите RecyclerView
                    CryptoAdapter adapter = new CryptoAdapter(cryptoList);
                    cryptoRecyclerView.setAdapter(adapter);
                } else {
                    Toast.makeText(HomeActivity.this, "Failed to load data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<CryptoModel>> call, Throwable t) {
                Toast.makeText(HomeActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
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

    // Открытие галереи для выбора изображения
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE);
    }

    // Обработка результата выбора изображения
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            chartImageView.setImageURI(imageUri); // Отображаем фото
            Toast.makeText(this, "Фото графика загружено!", Toast.LENGTH_SHORT).show();
        }
    }

    // Обработка запроса разрешений
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    new AlertDialog.Builder(this)
                            .setTitle("Разрешение требуется")
                            .setMessage("Для загрузки фото графика необходимо разрешение на доступ к галерее.")
                            .setPositiveButton("OK", (dialog, which) -> {
                                ActivityCompat.requestPermissions(this,
                                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                        REQUEST_PERMISSION);
                            })
                            .setNegativeButton("Отмена", null)
                            .create()
                            .show();
                } else {
                    Toast.makeText(this, "Разрешение отклонено навсегда. Пожалуйста, предоставьте разрешение в настройках.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                }
            }
        }
    }
}