package com.example.tradeview;
import java.util.List;

public class PricePredictor {
    private int lookbackPeriod = 20; // Кол-во свечей для анализа

    public void setLookbackPeriod(int period) {
        this.lookbackPeriod = Math.max(5, Math.min(period, 100));
    }

    public float predictNextPrice(List<CandleStick> candles) {
        if (candles == null || candles.size() < 2) {
            return candles.get(0).getClose();
        }

        int dataPoints = Math.min(lookbackPeriod, candles.size());
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        for (int i = 0; i < dataPoints; i++) {
            CandleStick candle = candles.get(candles.size() - dataPoints + i);
            sumX += i;
            sumY += candle.getClose();
            sumXY += i * candle.getClose();
            sumX2 += i * i;
        }

        double slope = (dataPoints * sumXY - sumX * sumY) / (dataPoints * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / dataPoints;

        return (float)(slope * dataPoints + intercept);
    }
}