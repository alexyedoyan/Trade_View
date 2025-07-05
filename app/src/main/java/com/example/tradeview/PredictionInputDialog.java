package com.example.tradeview;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;

public class PredictionInputDialog extends Dialog {

    public interface PredictionCallback {
        void onPredictionComplete(float predictedPrice);
    }

    private final PricePredictor predictor;
    private final String cryptoSymbol;
    private final List<CandleStick> candleSticks;
    private final PredictionCallback callback;

    public PredictionInputDialog(@NonNull Context context,
                                 PricePredictor predictor,
                                 String cryptoSymbol,
                                 List<CandleStick> candleSticks,
                                 PredictionCallback callback) {
        super(context);
        this.predictor = predictor;
        this.cryptoSymbol = cryptoSymbol;
        this.candleSticks = candleSticks;
        this.callback = callback;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_prediction_input);

        // Инициализация элементов UI
        Spinner periodSpinner = findViewById(R.id.periodSpinner);
        Button predictBtn = findViewById(R.id.predictBtn);
        Button cancelBtn = findViewById(R.id.cancelBtn);
        TextView cryptoSymbolText = findViewById(R.id.cryptoSymbolText);

        // Установка символа криптовалюты
        cryptoSymbolText.setText(cryptoSymbol);

        // Настройка выбора периода анализа
        List<Integer> periods = new ArrayList<>();
        periods.add(10);
        periods.add(20);
        periods.add(30);
        periods.add(50);
        periods.add(100);

        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                periods);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        periodSpinner.setAdapter(adapter);

        // Обработка нажатия кнопки прогноза
        predictBtn.setOnClickListener(v -> {
            if (candleSticks == null || candleSticks.isEmpty()) {
                Toast.makeText(getContext(), "Нет данных для анализа", Toast.LENGTH_SHORT).show();
                return;
            }

            int period = (Integer) periodSpinner.getSelectedItem();

            // Проверка, что данных достаточно для выбранного периода
            if (candleSticks.size() < period) {
                Toast.makeText(getContext(),
                        "Недостаточно данных. Доступно: " + candleSticks.size() + " из " + period,
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Берем только последние N свечей для анализа
            List<CandleStick> lastCandles = candleSticks.subList(
                    candleSticks.size() - period,
                    candleSticks.size());

            // Получаем прогноз
            float prediction = predictor.predictNextPrice(lastCandles);

            // Передаем результат через callback
            if (callback != null) {
                callback.onPredictionComplete(prediction);
            }

            dismiss();
        });

        // Обработка отмены
        cancelBtn.setOnClickListener(v -> dismiss());
    }
}