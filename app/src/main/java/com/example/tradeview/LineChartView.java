package com.example.tradeview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class LineChartView extends View {

    private Paint paint;
    private Path path;
    private List<Float> dataPoints; // Данные для графика (цены закрытия)
    private float minValue, maxValue; // Минимальное и максимальное значение данных

    public LineChartView(Context context) {
        super(context);
        init();
    }

    public LineChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.BLUE);
        paint.setStrokeWidth(5);
        paint.setStyle(Paint.Style.STROKE);

        path = new Path();
        dataPoints = new ArrayList<>();
    }

    // Установка данных для графика
    public void setData(List<Float> data) {
        this.dataPoints = data;
        calculateMinMax();
        invalidate(); // Перерисовываем View
    }

    // Расчет минимального и максимального значения
    private void calculateMinMax() {
        if (dataPoints.isEmpty()) {
            minValue = 0;
            maxValue = 0;
            return;
        }

        minValue = dataPoints.get(0);
        maxValue = dataPoints.get(0);

        for (Float value : dataPoints) {
            if (value < minValue) minValue = value;
            if (value > maxValue) maxValue = value;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (dataPoints.isEmpty()) {
            return; // Нет данных для отрисовки
        }

        int width = getWidth();
        int height = getHeight();
        float xStep = (float) width / (dataPoints.size() - 1); // Шаг по оси X
        float yScale = height / (maxValue - minValue); // Масштаб по оси Y

        // Отрисовка графика
        path.reset();
        for (int i = 0; i < dataPoints.size(); i++) {
            float x = i * xStep;
            float y = height - (dataPoints.get(i) - minValue) * yScale;

            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }

        canvas.drawPath(path, paint);
    }
}