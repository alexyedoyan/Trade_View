package com.example.tradeview;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import java.util.ArrayList;
import java.util.List;
import androidx.appcompat.app.AlertDialog;
import android.provider.Settings;
import android.Manifest;

public class HomeActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private RecyclerView cryptoRecyclerView;
    private ImageView chartImageView;
    private static final int PICK_IMAGE = 1;
    private static final int REQUEST_PERMISSION = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        auth = FirebaseAuth.getInstance();
        cryptoRecyclerView = findViewById(R.id.cryptoRecyclerView);
        cryptoRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        List<CryptoModel> cryptoList = new ArrayList<>();
        cryptoList.add(new CryptoModel("BTCUSDT", "$30,000.50"));
        cryptoList.add(new CryptoModel("ETHUSDT", "$2,000.75"));
        cryptoList.add(new CryptoModel("BNBUSDT", "$300.25"));
        CryptoAdapter adapter = new CryptoAdapter(cryptoList);
        cryptoRecyclerView.setAdapter(adapter);
        findViewById(R.id.logoutButton).setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(HomeActivity.this, LoginActivity.class));
            finish();
        });
        chartImageView = findViewById(R.id.chartImageView);
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
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            chartImageView.setImageURI(imageUri); // Отображаем фото
            Toast.makeText(this, "Фото графика загружено!", Toast.LENGTH_SHORT).show();
        }
    }

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