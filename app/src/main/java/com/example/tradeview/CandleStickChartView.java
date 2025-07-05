package com.example.tradeview;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import java.util.*;

public class CandleStickChartView extends View {
    private Paint candlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint smaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint gridPaint = new Paint();
    private Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint volumePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint bollingerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint supportPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint resistancePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint predictionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Данные
    private List<CandleStick> candles = new ArrayList<>();
    private List<Float> volumes = new ArrayList<>();
    private List<Float> smaValues = new ArrayList<>();
    private TechnicalAnalysis.BollingerBands bollingerBands;
    private List<Double> supportLevels = new ArrayList<>();
    private List<Double> resistanceLevels = new ArrayList<>();
    private float minPrice, maxPrice;
    private boolean isDarkMode = true;
    private int bullColor = Color.GREEN;
    private int bearColor = Color.RED;

    // Прогнозирование
    private float predictedPrice = 0;
    private boolean showPrediction = false;
    private RectF predictionRect = new RectF();

    // Параметры зума и скролла
    private ScaleGestureDetector scaleDetector;
    private float scaleFactor = 1.0f;
    private float translateX = 0f;
    private float lastTouchX;
    private int visibleCandles = 50;

    // Интерактивность
    private int selectedIndex = -1;
    private RectF tooltipRect = new RectF();
    private Paint tooltipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public CandleStickChartView(Context context) {
        super(context);
        init();
    }

    public CandleStickChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        scaleDetector = new ScaleGestureDetector(getContext(), new ScaleListener());

        // Стили для свечей
        candlePaint.setStyle(Paint.Style.FILL);

        // Стили для теней
        shadowPaint.setStrokeWidth(1.5f);

        // Стили для SMA
        smaPaint.setColor(Color.BLUE);
        smaPaint.setStrokeWidth(2f);

        // Стили для Bollinger Bands
        bollingerPaint.setColor(Color.CYAN);
        bollingerPaint.setStyle(Paint.Style.STROKE);
        bollingerPaint.setStrokeWidth(1.5f);

        // Стили для сетки
        gridPaint.setStrokeWidth(0.5f);
        gridPaint.setStyle(Paint.Style.STROKE);

        // Стили для текста
        textPaint.setTextSize(24f);

        // Стили для объемов
        volumePaint.setStyle(Paint.Style.FILL);

        // Стили для уровней
        supportPaint.setColor(Color.GREEN);
        supportPaint.setStyle(Paint.Style.STROKE);
        supportPaint.setStrokeWidth(2f);

        resistancePaint.setColor(Color.RED);
        resistancePaint.setStyle(Paint.Style.STROKE);
        resistancePaint.setStrokeWidth(2f);

        // Стили для тултипа
        tooltipPaint.setColor(Color.argb(200, 50, 50, 50));
        tooltipPaint.setTextSize(28f);

        // Стили для прогноза
        predictionPaint.setColor(Color.YELLOW);
        predictionPaint.setStrokeWidth(3f);
        predictionPaint.setStyle(Paint.Style.STROKE);
        predictionPaint.setPathEffect(new DashPathEffect(new float[]{10, 5}, 0));

        updateThemeColors();
    }

    private void updateThemeColors() {
        if (isDarkMode) {
            setBackgroundColor(Color.BLACK);
            shadowPaint.setColor(Color.argb(180, 255, 255, 255));
            gridPaint.setColor(Color.argb(50, 255, 255, 255));
            textPaint.setColor(Color.WHITE);
        } else {
            setBackgroundColor(Color.WHITE);
            shadowPaint.setColor(Color.argb(180, 0, 0, 0));
            gridPaint.setColor(Color.argb(30, 0, 0, 0));
            textPaint.setColor(Color.BLACK);
        }
    }

    public void setData(List<CandleStick> candles) {
        this.candles = candles;
        calculatePriceRange();
        invalidate();
    }

    public void setVolumes(List<Float> volumes) {
        this.volumes = volumes;
        invalidate();
    }

    public void setSmaValues(List<Float> smaValues) {
        this.smaValues = smaValues;
        invalidate();
    }

    public void setBollingerBands(TechnicalAnalysis.BollingerBands bands) {
        this.bollingerBands = bands;
        invalidate();
    }

    public void setSupportResistanceLevels(List<Double> supports, List<Double> resistances) {
        this.supportLevels = supports;
        this.resistanceLevels = resistances;
        invalidate();
    }

    public void setDarkMode(boolean darkMode) {
        isDarkMode = darkMode;
        updateThemeColors();
        invalidate();
    }

    public void showPrediction(float price) {
        this.predictedPrice = price;
        this.showPrediction = true;
        calculatePriceRange();
        invalidate();
    }

    public void hidePrediction() {
        this.showPrediction = false;
        invalidate();
    }

    public boolean isShowingPrediction() {
        return showPrediction;
    }

    public float getPredictedPrice() {
        return predictedPrice;
    }

    private void calculatePriceRange() {
        if (candles.isEmpty()) {
            minPrice = 0;
            maxPrice = 1;
            return;
        }

        minPrice = Float.MAX_VALUE;
        maxPrice = Float.MIN_VALUE;

        for (CandleStick candle : candles) {
            minPrice = Math.min(minPrice, candle.getLow());
            maxPrice = Math.max(maxPrice, candle.getHigh());
        }

        if (showPrediction && predictedPrice != 0) {
            minPrice = Math.min(minPrice, predictedPrice);
            maxPrice = Math.max(maxPrice, predictedPrice);
        }

        float padding = (maxPrice - minPrice) * 0.05f;
        minPrice -= padding;
        maxPrice += padding;
    }

    private float priceToY(float price) {
        if (maxPrice == minPrice) {
            return getHeight() / 2;
        }
        return getHeight() * (1 - (price - minPrice) / (maxPrice - minPrice));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();
        canvas.translate(translateX, 0);
        canvas.scale(scaleFactor, 1f);

        drawGrid(canvas);
        drawSupportResistanceLevels(canvas);
        drawBollingerBands(canvas);
        drawCandles(canvas);
        drawSmaLine(canvas);
        drawVolumes(canvas);

        canvas.restore();

        if (showPrediction && predictedPrice != 0 && !candles.isEmpty()) {
            drawPrediction(canvas);
        }

        if (selectedIndex != -1) {
            drawTooltip(canvas);
        }
    }

    private void drawGrid(Canvas canvas) {
        // Вертикальные линии
        for (int i = 0; i <= 10; i++) {
            float x = getWidth() * i / 10f;
            canvas.drawLine(x, 0, x, getHeight(), gridPaint);
        }

        // Горизонтальные линии
        for (int i = 0; i <= 10; i++) {
            float y = getHeight() * i / 10f;
            canvas.drawLine(0, y, getWidth(), y, gridPaint);
        }

        // Подписи цен
        for (int i = 0; i <= 5; i++) {
            float y = getHeight() * i / 5f;
            float price = minPrice + (maxPrice - minPrice) * (1 - i / 5f);
            canvas.drawText(String.format(Locale.US, "%.2f", price), 10, y - 5, textPaint);
        }
    }

    private void drawCandles(Canvas canvas) {
        if (candles.isEmpty()) return;

        float candleWidth = getWidth() / visibleCandles * 0.8f;
        float gap = getWidth() / visibleCandles * 0.2f;

        for (int i = 0; i < candles.size(); i++) {
            if (i * (candleWidth + gap) + translateX + candleWidth < 0) continue;
            if (i * (candleWidth + gap) + translateX > getWidth()) break;

            CandleStick candle = candles.get(i);
            float x = i * (candleWidth + gap) + gap / 2;
            float openY = priceToY(candle.getOpen());
            float closeY = priceToY(candle.getClose());
            float highY = priceToY(candle.getHigh());
            float lowY = priceToY(candle.getLow());

            // Тень
            canvas.drawLine(x + candleWidth / 2, highY,
                    x + candleWidth / 2, lowY, shadowPaint);

            // Тело свечи
            candlePaint.setColor(candle.getClose() > candle.getOpen() ? bullColor : bearColor);
            canvas.drawRect(x, Math.min(openY, closeY),
                    x + candleWidth, Math.max(openY, closeY), candlePaint);
        }
    }

    private void drawSmaLine(Canvas canvas) {
        if (smaValues == null || smaValues.size() != candles.size() || smaValues.isEmpty()) return;

        float candleWidth = getWidth() / visibleCandles;
        Path smaPath = new Path();

        for (int i = 1; i < smaValues.size(); i++) {
            float x1 = (i - 1) * candleWidth + candleWidth / 2;
            float y1 = priceToY(smaValues.get(i - 1));
            float x2 = i * candleWidth + candleWidth / 2;
            float y2 = priceToY(smaValues.get(i));

            if (i == 1) {
                smaPath.moveTo(x1, y1);
            }
            smaPath.lineTo(x2, y2);
        }

        canvas.drawPath(smaPath, smaPaint);
    }

    private void drawBollingerBands(Canvas canvas) {
        if (bollingerBands == null || bollingerBands.upper.size() != candles.size()) return;

        float candleWidth = getWidth() / visibleCandles;
        Path upperPath = new Path();
        Path lowerPath = new Path();

        for (int i = 0; i < bollingerBands.upper.size(); i++) {
            float x = i * candleWidth + candleWidth / 2;
            float upperY = priceToY(bollingerBands.upper.get(i).floatValue());
            float lowerY = priceToY(bollingerBands.lower.get(i).floatValue());

            if (i == 0) {
                upperPath.moveTo(x, upperY);
                lowerPath.moveTo(x, lowerY);
            } else {
                upperPath.lineTo(x, upperY);
                lowerPath.lineTo(x, lowerY);
            }
        }

        canvas.drawPath(upperPath, bollingerPaint);
        canvas.drawPath(lowerPath, bollingerPaint);
    }

    private void drawVolumes(Canvas canvas) {
        if (volumes.isEmpty() || volumes.size() != candles.size()) return;

        float maxVolume = Collections.max(volumes);
        float volumeHeight = getHeight() * 0.2f;
        float candleWidth = getWidth() / visibleCandles * 0.8f;
        float gap = getWidth() / visibleCandles * 0.2f;

        for (int i = 0; i < volumes.size(); i++) {
            float x = i * (candleWidth + gap) + gap / 2;
            float height = (volumes.get(i) / maxVolume) * volumeHeight;

            volumePaint.setColor(candles.get(i).getClose() > candles.get(i).getOpen() ?
                    Color.GREEN : Color.RED);

            canvas.drawRect(x, getHeight() - height,
                    x + candleWidth, getHeight(),
                    volumePaint);
        }
    }

    private void drawSupportResistanceLevels(Canvas canvas) {
        for (Double level : supportLevels) {
            float y = priceToY(level.floatValue());
            canvas.drawLine(0, y, getWidth(), y, supportPaint);
            canvas.drawText("S: " + String.format(Locale.US, "%.2f", level),
                    getWidth() - 150, y - 10, textPaint);
        }

        for (Double level : resistanceLevels) {
            float y = priceToY(level.floatValue());
            canvas.drawLine(0, y, getWidth(), y, resistancePaint);
            canvas.drawText("R: " + String.format(Locale.US, "%.2f", level),
                    getWidth() - 150, y - 10, textPaint);
        }
    }

    private void drawPrediction(Canvas canvas) {
        float candleWidth = getWidth() / visibleCandles;
        float lastX = (candles.size() - 1) * candleWidth + candleWidth / 2;
        float lastY = priceToY(candles.get(candles.size() - 1).getClose());
        float nextX = lastX + candleWidth;
        float nextY = priceToY(predictedPrice);

        // Линия прогноза
        canvas.drawLine(lastX, lastY, nextX, nextY, predictionPaint);

        // Точка прогноза
        float radius = 8f;
        predictionRect.set(nextX - radius, nextY - radius, nextX + radius, nextY + radius);
        canvas.drawArc(predictionRect, 0, 360, false, predictionPaint);

        // Текст прогноза
        String text = String.format(Locale.US, "%.2f", predictedPrice);
        float textX = nextX + 15f;
        float textY = nextY;

        if (textX + textPaint.measureText(text) > getWidth()) {
            textX = nextX - 15f - textPaint.measureText(text);
        }

        canvas.drawText(text, textX, textY, textPaint);
    }

    private void drawTooltip(Canvas canvas) {
        CandleStick candle = candles.get(selectedIndex);
        String text = String.format(Locale.US, "O: %.2f\nH: %.2f\nL: %.2f\nC: %.2f",
                candle.getOpen(), candle.getHigh(), candle.getLow(), candle.getClose());

        Rect bounds = new Rect();
        tooltipPaint.getTextBounds(text, 0, text.length(), bounds);
        int padding = 20;
        float width = bounds.width() + 2 * padding;
        float height = bounds.height() + 2 * padding;

        float x = Math.min(getWidth() - width, lastTouchX);
        float y = Math.min(getHeight() - height, 100f);

        tooltipRect.set(x, y, x + width, y + height);
        canvas.drawRoundRect(tooltipRect, 10, 10, tooltipPaint);

        tooltipPaint.setColor(Color.WHITE);
        canvas.drawText(text, x + padding, y + padding + bounds.height(), tooltipPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                handleSelection(event.getX());
                return true;

            case MotionEvent.ACTION_MOVE:
                if (!scaleDetector.isInProgress()) {
                    float dx = event.getX() - lastTouchX;
                    translateX -= dx;
                    translateX = Math.min(0, Math.max(translateX, -getWidth() * (scaleFactor - 1)));
                    lastTouchX = event.getX();
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
                selectedIndex = -1;
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void handleSelection(float x) {
        float candleWidth = (getWidth() / visibleCandles) * 0.8f;
        float gap = (getWidth() / visibleCandles) * 0.2f;

        int index = (int) ((x - translateX) / (candleWidth + gap));
        if (index >= 0 && index < candles.size()) {
            selectedIndex = index;
            invalidate();
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float oldScale = scaleFactor;
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(1f, Math.min(scaleFactor, 3f));

            float focusX = detector.getFocusX();
            translateX = focusX - (focusX - translateX) * (scaleFactor / oldScale);

            visibleCandles = (int) (50 / scaleFactor);
            invalidate();
            return true;
        }
    }
}