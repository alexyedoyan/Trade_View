package com.example.tradeview;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.tradeview.R;
import com.example.tradeview.api.BinanceApiService;
import com.example.tradeview.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.List;

public class ChartActivity extends AppCompatActivity {

    private CandleStickChartView candleStickChartView;
    private EditText cryptoInput;
    private Spinner timeframeSpinner;
    private Button getChartButton;
    private String selectedTimeframe = "1m"; // По умолчанию 1 минута

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        candleStickChartView = findViewById(R.id.candleStickChartView);
        cryptoInput = findViewById(R.id.cryptoInput);
        timeframeSpinner = findViewById(R.id.timeframeSpinner);
        getChartButton = findViewById(R.id.getChartButton);

        // Настройка Spinner
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
                selectedTimeframe = "1m"; // По умолчанию 1 минута
            }
        });

        // Обработка нажатия на кнопку получения графика
        getChartButton.setOnClickListener(v -> {
            String cryptoName = cryptoInput.getText().toString().trim().toUpperCase();
            if (!cryptoName.isEmpty()) {
                getCryptoChart(cryptoName, selectedTimeframe);
            } else {
                Toast.makeText(this, "Введите название криптовалюты", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Получение данных графика
    private void getCryptoChart(String cryptoName, String interval) {
        BinanceApiService apiService = RetrofitClient.getClient().create(BinanceApiService.class);

        // Запрос для получения исторических данных (свечей)
        Call<List<List<String>>> call = apiService.getKlineData(cryptoName, interval, 100);
        call.enqueue(new Callback<List<List<String>>>() {
            @Override
            public void onResponse(Call<List<List<String>>> call, Response<List<List<String>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<CandleStick> candles = new ArrayList<>();
                    for (List<String> kline : response.body()) {
                        candles.add(new CandleStick(
                                Float.parseFloat(kline.get(1)), // Цена открытия
                                Float.parseFloat(kline.get(2)), // Максимальная цена
                                Float.parseFloat(kline.get(3)), // Минимальная цена
                                Float.parseFloat(kline.get(4))  // Цена закрытия
                        ));
                    }
                    candleStickChartView.setData(candles); // Устанавливаем данные для графика
                } else {
                    Toast.makeText(ChartActivity.this, "Ошибка при загрузке данных", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<List<String>>> call, Throwable t) {
                Toast.makeText(ChartActivity.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}