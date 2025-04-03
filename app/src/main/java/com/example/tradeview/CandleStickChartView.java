package com.example.tradeview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
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

        // Отрисовка сетки
        paint.setColor(Color.LTGRAY);
        paint.setStrokeWidth(1);

        // Горизонтальные линии (уровни цен)
        for (int i = 0; i <= 10; i++) {
            float y = height - (height / 10) * i;
            canvas.drawLine(0, y, width, y, paint);

            // Подписи цен
            float price = minValue + (maxValue - minValue) * (i / 10f);
            paint.setColor(Color.BLACK);
            paint.setTextSize(24);
            canvas.drawText(String.format("%.2f", price), 10, y - 10, paint);
        }

        // Вертикальные линии (временные интервалы)
        for (int i = 0; i < candles.size(); i++) {
            float x = i * xStep;
            canvas.drawLine(x, 0, x, height, paint);

            // Подписи времени (если данные доступны)
            if (i % 10 == 0) {
                paint.setColor(Color.BLACK);
                paint.setTextSize(24);
                canvas.drawText(String.valueOf(i), x, height - 10, paint);
            }
        }

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
        paint.setColor(Color.BLUE);
        paint.setStrokeWidth(2f);
        List<Double> smaValues = new ArrayList<>();
        for (int i = 10; i < candles.size(); i++) {
            double sum = 0;
            for (int j = i - 10; j < i; j++) {
                sum += candles.get(j).getClose();
            }
            smaValues.add(sum / 10);
        }

        for (int i = 1; i < smaValues.size(); i++) {
            float x1 = (i - 1) * xStep + xStep / 2;
            float x2 = i * xStep + xStep / 2;
            float y1 = height - (float)((smaValues.get(i - 1) - minValue) * yScale);
            float y2 = height - (float)((smaValues.get(i) - minValue) * yScale);
            canvas.drawLine(x1, y1, x2, y2, paint);
        }
    }
}