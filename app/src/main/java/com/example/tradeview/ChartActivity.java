package com.example.tradeview;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tradeview.api.BinanceApiService;

import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChartActivity extends AppCompatActivity {

    private CandleStickChartView candleStickChartView;
    private EditText cryptoInput;
    private Spinner timeframeSpinner;
    private Button getChartButton;
    private Button predictButton;
    private TextView predictionResult;
    private String selectedTimeframe = "1m";
    private List<CandleStick> currentCandles = new ArrayList<>();
    private List<Double> currentClosePrices = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        // Инициализация элементов
        candleStickChartView = findViewById(R.id.candleStickChartView);
        cryptoInput = findViewById(R.id.cryptoInput);
        timeframeSpinner = findViewById(R.id.timeframeSpinner);
        getChartButton = findViewById(R.id.getChartButton);
        predictButton = findViewById(R.id.predictButton);
        predictionResult = findViewById(R.id.predictionResult);

        // Настройка выбора временного интервала
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.timeframes,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timeframeSpinner.setAdapter(adapter);

        timeframeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedTimeframe = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedTimeframe = "1m";
            }
        });

        // Кнопка загрузки графика
        getChartButton.setOnClickListener(v -> loadChartData());

        // Кнопка предсказания
        predictButton.setOnClickListener(v -> {
            if (currentClosePrices.isEmpty()) {
                Toast.makeText(this, "Сначала загрузите данные", Toast.LENGTH_SHORT).show();
                return;
            }
            predictFuturePrices();
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
                Toast.makeText(ChartActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processChartData(List<List<String>> klines) {
        currentCandles.clear();
        currentClosePrices.clear();

        for (List<String> kline : klines) {
            float open = Float.parseFloat(kline.get(1));
            float high = Float.parseFloat(kline.get(2));
            float low = Float.parseFloat(kline.get(3));
            float close = Float.parseFloat(kline.get(4));

            currentCandles.add(new CandleStick(open, high, low, close));
            currentClosePrices.add((double) close);
        }

        candleStickChartView.setData(currentCandles);
        predictButton.setEnabled(true);
    }

    private void predictFuturePrices() {
        // Простое предсказание на основе SMA
        double sma10 = TechnicalAnalysis.calculateSMA(currentClosePrices, 10);
        double lastPrice = currentClosePrices.get(currentClosePrices.size() - 1);

        String prediction;
        if (lastPrice > sma10) {
            prediction = "📈 Вероятен рост цены (цена выше SMA10)";
        } else {
            prediction = "📉 Вероятно падение (цена ниже SMA10)";
        }

        // Добавляем другие индикаторы
        double rsi = TechnicalAnalysis.calculateRSI(currentClosePrices, 14);
        prediction += "\nRSI: " + String.format("%.2f", rsi) + " - " +
                (rsi > 70 ? "Перекупленность" : rsi < 30 ? "Перепроданность" : "Нейтрально");

        predictionResult.setText(prediction);
    }
}