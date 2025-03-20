package com.example.tradeview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import com.example.tradeview.CandleStick;
import java.util.ArrayList;
import java.util.List;

public class CandleStickChartView extends View {

    private Paint paint;
    private List<CandleStick> candles;
    private float minValue, maxValue;

    public CandleStickChartView(Context context) {
        super(context);
        init();
    }

    public CandleStickChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setStrokeWidth(5);
        candles = new ArrayList<>();
    }

    // Установка данных для графика
    public void setData(List<CandleStick> candles) {
        this.candles = candles;
        calculateMinMax();
        invalidate(); // Перерисовываем View
    }

    // Расчет минимального и максимального значения
    private void calculateMinMax() {
        if (candles.isEmpty()) {
            minValue = 0;
            maxValue = 0;
            return;
        }

        minValue = candles.get(0).getLow();
        maxValue = candles.get(0).getHigh();

        for (CandleStick candle : candles) {
            if (candle.getLow() < minValue) minValue = candle.getLow();
            if (candle.getHigh() > maxValue) maxValue = candle.getHigh();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (candles.isEmpty()) {
            return; // Нет данных для отрисовки
        }

        int width = getWidth();
        int height = getHeight();
        float xStep = (float) width / candles.size(); // Шаг по оси X
        float yScale = height / (maxValue - minValue); // Масштаб по оси Y

        // Отрисовка свечей
        for (int i = 0; i < candles.size(); i++) {
            CandleStick candle = candles.get(i);
            float x = i * xStep + xStep / 2; // Центр свечи

            // Координаты для свечи
            float openY = height - (candle.getOpen() - minValue) * yScale;
            float closeY = height - (candle.getClose() - minValue) * yScale;
            float highY = height - (candle.getHigh() - minValue) * yScale;
            float lowY = height - (candle.getLow() - minValue) * yScale;

            // Цвет свечи (зеленый для роста, красный для падения)
            if (candle.getClose() >= candle.getOpen()) {
                paint.setColor(Color.GREEN);
            } else {
                paint.setColor(Color.RED);
            }

            // Отрисовка тела свечи
            canvas.drawRect(
                    x - xStep / 4, Math.min(openY, closeY),
                    x + xStep / 4, Math.max(openY, closeY),
                    paint
            );

            // Отрисовка тени свечи
            paint.setColor(Color.BLACK);
            canvas.drawLine(x, highY, x, lowY, paint);
        }
    }
}