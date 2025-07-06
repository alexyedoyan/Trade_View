package com.example.tradeview;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ChartActivity extends AppCompatActivity {

    private CandleStickChartView candleStickChartView;
    private EditText cryptoInput;
    private Spinner timeframeSpinner;
    private Button getChartButton, predictButton, btnUpdateLevels;
    private TextView predictionResult;
    private ProgressBar progressBar;
    private String selectedTimeframe = "1d";
    private List<CandleStick> currentCandles = new ArrayList<>();
    private List<Double> currentClosePrices = new ArrayList<>();
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        initViews();
        setupTimeframeSpinner();
        setupButtons();
        setupBottomNavigation();

        String cryptoName = getIntent().getStringExtra("CRYPTO_NAME");
        if (cryptoName != null && !cryptoName.isEmpty()) {
            cryptoInput.setText(cryptoName);
            loadChartData();
        }
    }

    private void initViews() {
        candleStickChartView = findViewById(R.id.candleStickChartView);
        cryptoInput = findViewById(R.id.cryptoInput);
        timeframeSpinner = findViewById(R.id.timeframeSpinner);
        getChartButton = findViewById(R.id.getChartButton);
        predictButton = findViewById(R.id.predictButton);
        btnUpdateLevels = findViewById(R.id.btnUpdateLevels);
        predictionResult = findViewById(R.id.predictionResult);
        progressBar = findViewById(R.id.progressBar);
        bottomNav = findViewById(R.id.bottom_navigation);
    }

    private void setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_chart) {
                // Уже на Chart, ничего не делаем
                return true;
            } else if (id == R.id.nav_news) {
                startActivity(new Intent(this, CryptoNewsActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }

            return false;
        });
        bottomNav.setSelectedItemId(R.id.nav_chart);
    }

    private void setupTimeframeSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.timeframes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timeframeSpinner.setAdapter(adapter);

        timeframeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedTimeframe = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedTimeframe = "1d";
            }
        });
    }

    private void setupButtons() {
        getChartButton.setOnClickListener(v -> loadChartData());
        predictButton.setOnClickListener(v -> {
            if (currentClosePrices.isEmpty()) {
                Toast.makeText(this, "Сначала загрузите данные", Toast.LENGTH_SHORT).show();
                return;
            }
            predictFuturePrices();
        });

        btnUpdateLevels.setOnClickListener(v -> {
            if (currentClosePrices.isEmpty()) {
                Toast.makeText(this, "Сначала загрузите данные", Toast.LENGTH_SHORT).show();
            } else {
                findAndDrawKeyLevels();
                Toast.makeText(this, "Уровни обновлены", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadChartData() {
        String cryptoName = cryptoInput.getText().toString().trim().toUpperCase();
        if (cryptoName.isEmpty()) {
            Toast.makeText(this, "Введите название пары", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        BinanceApiService apiService = RetrofitClient.getClient().create(BinanceApiService.class);
        Call<List<List<String>>> call = apiService.getKlineData(cryptoName, selectedTimeframe, 100);

        call.enqueue(new Callback<List<List<String>>>() {
            @Override
            public void onResponse(Call<List<List<String>>> call, Response<List<List<String>>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    processChartData(response.body());
                } else {
                    Toast.makeText(ChartActivity.this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<List<String>>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ChartActivity.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processChartData(List<List<String>> klines) {
        currentCandles.clear();
        currentClosePrices.clear();
        List<Float> volumes = new ArrayList<>();

        for (List<String> kline : klines) {
            try {
                float open = Float.parseFloat(kline.get(1));
                float high = Float.parseFloat(kline.get(2));
                float low = Float.parseFloat(kline.get(3));
                float close = Float.parseFloat(kline.get(4));
                float volume = Float.parseFloat(kline.get(5));

                currentCandles.add(new CandleStick(open, high, low, close));
                currentClosePrices.add((double) close);
                volumes.add(volume);
            } catch (Exception e) {
                Log.e("ChartActivity", "Error parsing kline data", e);
            }
        }

        if (!currentCandles.isEmpty()) {
            candleStickChartView.setData(currentCandles);
            candleStickChartView.setVolumes(volumes);

            List<Float> sma10 = TechnicalAnalysis.calculateSmaList(currentClosePrices, 10);
            List<Float> sma50 = TechnicalAnalysis.calculateSmaList(currentClosePrices, 50);
            TechnicalAnalysis.BollingerBands bands = TechnicalAnalysis.calculateBollingerBands(currentClosePrices, 20, 2);

            candleStickChartView.setSmaValues(sma10);
            candleStickChartView.setBollingerBands(bands);

            findAndDrawKeyLevels();
            predictButton.setEnabled(true);
        }
    }

    private void findAndDrawKeyLevels() {
        if (currentClosePrices.isEmpty()) return;
        // Ваша логика для уровней поддержки/сопротивления
    }

    private void predictFuturePrices() {
        double sma10 = TechnicalAnalysis.calculateSMA(currentClosePrices, 10);
        double sma50 = TechnicalAnalysis.calculateSMA(currentClosePrices, 50);
        double ema20 = TechnicalAnalysis.calculateEMA(currentClosePrices, 20);
        double rsi = TechnicalAnalysis.calculateRSI(currentClosePrices, 14);
        double lastPrice = currentClosePrices.get(currentClosePrices.size() - 1);

        TechnicalAnalysis.MACDData macdData = TechnicalAnalysis.calculateMACD(currentClosePrices, 12, 26, 9);
        double[] macd = macdData.getLastValues();
        String macdAnalysis = TechnicalAnalysis.analyzeMACD(macd[0], macd[1]);

        StringBuilder prediction = new StringBuilder();
        prediction.append(String.format(Locale.US, "Цена: %.4f\n", lastPrice));
        prediction.append(String.format(Locale.US, "SMA 10/50: %.4f / %.4f\n", sma10, sma50));
        prediction.append(String.format(Locale.US, "EMA 20: %.4f\n", ema20));
        prediction.append(String.format(Locale.US, "RSI: %.1f (%s)\n", rsi, getRsiCondition(rsi)));
        prediction.append(String.format("MACD: %.4f | Signal: %.4f\n", macd[0], macd[1]));
        prediction.append("MACD анализ: ").append(macdAnalysis).append("\n\n");
        prediction.append("Тренд: ").append(TechnicalAnalysis.predictTrend(currentClosePrices));

        predictionResult.setText(prediction.toString());
    }

    private String getRsiCondition(double rsi) {
        if (rsi > 70) return "Перекупленность";
        if (rsi < 30) return "Перепроданность";
        return "Нейтрально";
    }
}