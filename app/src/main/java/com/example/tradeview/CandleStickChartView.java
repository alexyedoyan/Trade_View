package com.example.tradeview;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import java.util.*;

public class CandleStickChartView extends View {
    private Paint paint, smaPaint, levelPaint, textPaint, mainLevelPaint;
    private List<CandleStick> candles;
    private List<Float> smaValues;
    private List<Double> supportLevels;
    private List<Double> resistanceLevels;
    private Double mainSupportLevel;
    private Double mainResistanceLevel;
    private float minValue, maxValue;
    private int bullColor = Color.GREEN;
    private int bearColor = Color.RED;
    private int shadowColor = Color.DKGRAY;
    private float shadowWidth = 2f;
    private int highlightColor = Color.YELLOW;
    private int highlightedCandleIndex = -1;


    public CandleStickChartView(Context context) {
        super(context);
        init();
    }

    public CandleStickChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        smaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        smaPaint.setColor(Color.BLUE);
        smaPaint.setStrokeWidth(3f);

        levelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        levelPaint.setStyle(Paint.Style.STROKE);
        levelPaint.setStrokeWidth(2f);

        mainLevelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mainLevelPaint.setStyle(Paint.Style.STROKE);
        mainLevelPaint.setStrokeWidth(4f);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(36f);
        textPaint.setColor(Color.WHITE);

        candles = new ArrayList<>();
        supportLevels = new ArrayList<>();
        resistanceLevels = new ArrayList<>();
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    public void setData(List<CandleStick> candles) {
        this.candles = candles;
        calculateMinMax();
        invalidate();
    }

    public void setSmaValues(List<Float> smaValues) {
        this.smaValues = smaValues;
        invalidate();
    }

    public void setSupportLevels(List<Double> levels) {
        this.supportLevels = levels != null ? new ArrayList<>(levels) : new ArrayList<Double>();
        invalidate();
    }

    public void setResistanceLevels(List<Double> levels) {
        this.resistanceLevels = levels != null ? new ArrayList<>(levels) : new ArrayList<Double>();
        invalidate();
    }

    public void setMainSupportLevel(Double level) {
        this.mainSupportLevel = level;
        invalidate();
    }

    public void setMainResistanceLevel(Double level) {
        this.mainResistanceLevel = level;
        invalidate();
    }

    public List<Double> getSupportLevels() {
        return new ArrayList<>(supportLevels);
    }

    public List<Double> getResistanceLevels() {
        return new ArrayList<>(resistanceLevels);
    }

    private void calculateMinMax() {
        if (candles.isEmpty()) {
            minValue = 0;
            maxValue = 1;
            return;
        }

        minValue = candles.get(0).getLow();
        maxValue = candles.get(0).getHigh();

        for (CandleStick candle : candles) {
            minValue = Math.min(minValue, candle.getLow());
            maxValue = Math.max(maxValue, candle.getHigh());
        }

        // Добавляем padding 5%
        float range = maxValue - minValue;
        minValue -= range * 0.05f;
        maxValue += range * 0.05f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (candles.isEmpty()) return;

        drawGrid(canvas);
        drawMainLevels(canvas);
        drawLevels(canvas);
        drawSmaLine(canvas);
        drawCandles(canvas);
    }

    private void drawGrid(Canvas canvas) {
        Paint gridPaint = new Paint();
        gridPaint.setColor(Color.GRAY);
        gridPaint.setStrokeWidth(1f);

        for (int i = 0; i <= 10; i++) {
            float y = getHeight() * i / 10f;
            canvas.drawLine(0, y, getWidth(), y, gridPaint);
        }
    }

    private void drawMainLevels(Canvas canvas) {
        // Главная поддержка
        if (mainSupportLevel != null) {
            mainLevelPaint.setColor(Color.GREEN);
            float y = mapY(mainSupportLevel.floatValue());
            canvas.drawLine(0, y, getWidth(), y, mainLevelPaint);
            canvas.drawText("MAIN SUPPORT: " + mainSupportLevel, 50, y - 20, textPaint);
        }

        // Главное сопротивление
        if (mainResistanceLevel != null) {
            mainLevelPaint.setColor(Color.RED);
            float y = mapY(mainResistanceLevel.floatValue());
            canvas.drawLine(0, y, getWidth(), y, mainLevelPaint);
            canvas.drawText("MAIN RESISTANCE: " + mainResistanceLevel, 50, y - 20, textPaint);
        }
    }

    private void drawLevels(Canvas canvas) {
        // Уровни поддержки
        levelPaint.setColor(Color.GREEN);
        for (Double level : supportLevels) {
            if (level.equals(mainSupportLevel)) continue;
            float y = mapY(level.floatValue());
            canvas.drawLine(0, y, getWidth(), y, levelPaint);
            canvas.drawText("S: " + level, 10, y - 10, textPaint);
        }

        // Уровни сопротивления
        levelPaint.setColor(Color.RED);
        for (Double level : resistanceLevels) {
            if (level.equals(mainResistanceLevel)) continue;
            float y = mapY(level.floatValue());
            canvas.drawLine(0, y, getWidth(), y, levelPaint);
            canvas.drawText("R: " + level, 10, y - 10, textPaint);
        }
    }

    private void drawCandles(Canvas canvas) {
        float width = getWidth();
        float height = getHeight();
        float candleWidth = width / candles.size() * 0.8f;
        float gap = width / candles.size() * 0.2f;

        for (int i = 0; i < candles.size(); i++) {
            CandleStick candle = candles.get(i);
            float x = i * (candleWidth + gap) + gap/2;
            float openY = mapY(candle.getOpen());
            float closeY = mapY(candle.getClose());
            float highY = mapY(candle.getHigh());
            float lowY = mapY(candle.getLow());

            // Тень
            paint.setColor(shadowColor);
            paint.setStrokeWidth(shadowWidth);
            canvas.drawLine(x + candleWidth/2, highY, x + candleWidth/2, lowY, paint);

            // Тело
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(candle.getClose() > candle.getOpen() ? bullColor : bearColor);
            canvas.drawRoundRect(
                    x, Math.min(openY, closeY),
                    x + candleWidth, Math.max(openY, closeY),
                    10f, 10f, paint
            );
        }
    }

    private void drawSmaLine(Canvas canvas) {
        if (smaValues == null || smaValues.size() != candles.size()) return;

        float width = getWidth();
        float candleWidth = width / candles.size() * 0.8f;
        float gap = width / candles.size() * 0.2f;

        for (int i = 1; i < smaValues.size(); i++) {
            canvas.drawLine(
                    (i-1) * (candleWidth + gap) + candleWidth/2 + gap/2, mapY(smaValues.get(i-1)),
                    i * (candleWidth + gap) + candleWidth/2 + gap/2, mapY(smaValues.get(i)),
                    smaPaint
            );
        }
    }

    private float mapY(float value) {
        if (maxValue <= minValue) return getHeight() / 2f;
        return getHeight() * (1 - (value - minValue) / (maxValue - minValue));
    }

    public void setDarkMode(boolean enabled) {
        if (enabled) {
            setBackgroundColor(Color.BLACK);
            textPaint.setColor(Color.WHITE);
        } else {
            setBackgroundColor(Color.WHITE);
            textPaint.setColor(Color.BLACK);
        }
        invalidate();
    }
}