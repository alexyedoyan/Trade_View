package com.example.tradeview;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.tradeview.api.BinanceApiService;
import java.util.*;
import retrofit2.*;

public class ChartActivity extends AppCompatActivity {
    private CandleStickChartView candleStickChartView;
    private EditText cryptoInput;
    private Spinner timeframeSpinner;
    private Button getChartButton, predictButton, btnUpdateLevels;
    private TextView predictionResult;
    private String selectedTimeframe = "1d";
    private List<CandleStick> currentCandles = new ArrayList<>();
    private List<Double> currentClosePrices = new ArrayList<>();

    // Параметры для уровней поддержки/сопротивления
    private static final int MIN_TOUCH_COUNT = 3;
    private static final double MERGE_THRESHOLD_PERCENT = 0.005; // 0.5%
    private static final double LEVEL_DISTANCE_PERCENT = 0.01;   // 1%

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        initViews();
        setupTimeframeSpinner();
        setupButtons();
    }

    private void initViews() {
        candleStickChartView = findViewById(R.id.candleStickChartView);
        cryptoInput = findViewById(R.id.cryptoInput);
        timeframeSpinner = findViewById(R.id.timeframeSpinner);
        getChartButton = findViewById(R.id.getChartButton);
        predictButton = findViewById(R.id.predictButton);
        btnUpdateLevels = findViewById(R.id.btnUpdateLevels);
        predictionResult = findViewById(R.id.predictionResult);

        // Настройка темного режима графика
        candleStickChartView.setDarkMode(true);
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

        BinanceApiService apiService = RetrofitClient.getClient().create(BinanceApiService.class);
        Call<List<List<String>>> call = apiService.getKlineData(cryptoName, selectedTimeframe, 100);

        call.enqueue(new Callback<List<List<String>>>() {
            @Override
            public void onResponse(Call<List<List<String>>> call, Response<List<List<String>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    processChartData(response.body());
                } else {
                    Toast.makeText(ChartActivity.this, "Ошибка загрузки", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<List<String>>> call, Throwable t) {
                Toast.makeText(ChartActivity.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processChartData(List<List<String>> klines) {
        currentCandles.clear();
        currentClosePrices.clear();

        for (List<String> kline : klines) {
            try {
                float open = Float.parseFloat(kline.get(1));
                float high = Float.parseFloat(kline.get(2));
                float low = Float.parseFloat(kline.get(3));
                float close = Float.parseFloat(kline.get(4));

                currentCandles.add(new CandleStick(open, high, low, close));
                currentClosePrices.add((double) close);
            } catch (Exception e) {
                Log.e("ChartActivity", "Error parsing kline data", e);
            }
        }

        if (!currentCandles.isEmpty()) {
            candleStickChartView.setData(currentCandles);

            // Рассчитываем SMA для отображения на графике
            List<Float> smaValues = TechnicalAnalysis.calculateSmaList(currentClosePrices, 10);
            candleStickChartView.setSmaValues(smaValues);

            findAndDrawKeyLevels();
            predictButton.setEnabled(true);
        }
    }

    private void findAndDrawKeyLevels() {
        // Рассчитываем масштаб для определения расстояния между уровнями
        double priceRange = getPriceRange(currentClosePrices);
        double mergeThreshold = priceRange * MERGE_THRESHOLD_PERCENT;
        double minDistance = priceRange * LEVEL_DISTANCE_PERCENT;

        // Находим значимые уровни
        List<List<Double>> levels = TechnicalAnalysis.findSupportResistanceLevels(
                currentClosePrices,
                MIN_TOUCH_COUNT,
                mergeThreshold,
                minDistance
        );

        List<Double> supports = levels.get(0);
        List<Double> resistances = levels.get(1);

        // Устанавливаем уровни на график
        candleStickChartView.setSupportLevels(supports);
        candleStickChartView.setResistanceLevels(resistances);

        // Логируем для отладки
        Log.d("Levels", "Supports: " + supports);
        Log.d("Levels", "Resistances: " + resistances);
    }

    private double getPriceRange(List<Double> prices) {
        if (prices.isEmpty()) return 0;
        double min = Collections.min(prices);
        double max = Collections.max(prices);
        return max - min;
    }

    private void predictFuturePrices() {
        // Рассчитываем индикаторы
        double sma10 = TechnicalAnalysis.calculateSMA(currentClosePrices, 10);
        double sma50 = TechnicalAnalysis.calculateSMA(currentClosePrices, 50);
        double ema20 = TechnicalAnalysis.calculateEMA(currentClosePrices, 20);
        double rsi = TechnicalAnalysis.calculateRSI(currentClosePrices, 14);
        double lastPrice = currentClosePrices.get(currentClosePrices.size() - 1);

        // Получаем текущие уровни с графика
        List<Double> supports = candleStickChartView.getSupportLevels();
        List<Double> resistances = candleStickChartView.getResistanceLevels();

        // Формируем прогноз
        String prediction = buildPredictionString(lastPrice, sma10, sma50, ema20, rsi, supports, resistances);
        predictionResult.setText(prediction);
    }

    private String buildPredictionString(double lastPrice, double sma10, double sma50,
                                         double ema20, double rsi,
                                         List<Double> supports, List<Double> resistances) {
        StringBuilder sb = new StringBuilder();

        // Основные индикаторы
        sb.append(String.format("📊 Цена: %.4f\n", lastPrice));
        sb.append(String.format("📈 SMA 10/50: %.4f / %.4f\n", sma10, sma50));
        sb.append(String.format("🔮 EMA 20: %.4f\n", ema20));
        sb.append(String.format("📉 RSI: %.1f (%s)\n\n", rsi, getRsiCondition(rsi)));

        // Уровни поддержки/сопротивления
        if (!supports.isEmpty()) {
            sb.append("🛡️ Поддержки:\n");
            for (Double level : supports) {
                sb.append(String.format("• %.4f (%.2f%%)\n",
                        level, ((level - lastPrice)/lastPrice)*100));
            }
            sb.append("\n");
        }

        if (!resistances.isEmpty()) {
            sb.append("⛰️ Сопротивления:\n");
            for (Double level : resistances) {
                sb.append(String.format("• %.4f (%.2f%%)\n",
                        level, ((level - lastPrice)/lastPrice)*100));
            }
            sb.append("\n");
        }

        // Тренд
        sb.append("🔍 Тренд: ")
                .append(TechnicalAnalysis.predictTrend(currentClosePrices));

        return sb.toString();
    }

    private String getRsiCondition(double rsi) {
        if (rsi > 70) return "Перекупленность";
        if (rsi < 30) return "Перепроданность";
        return "Нейтрально";
    }
}