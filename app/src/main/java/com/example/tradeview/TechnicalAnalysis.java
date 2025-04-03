package com.example.tradeview;

import java.util.List;

public class TechnicalAnalysis {

    // Существующие методы...
    public static double calculateSMA(List<Double> prices, int period) { if (prices == null || prices.size() < period) {
        throw new IllegalArgumentException("Недостаточно данных для SMA");
    }

        double sum = 0;
        for (int i = prices.size() - period; i < prices.size(); i++) {
            sum += prices.get(i);
        }
        return sum / period;
    }
    public static double calculateEMA(List<Double> prices, int period) { if (prices == null || prices.size() < period) {
        throw new IllegalArgumentException("Недостаточно данных для EMA");
    }

        double multiplier = 2.0 / (period + 1);
        double ema = calculateSMA(prices.subList(0, period), period);

        for (int i = period; i < prices.size(); i++) {
            ema = (prices.get(i) - ema) * multiplier + ema;
        }
        return ema;}
    public static double calculateRSI(List<Double> prices, int period)  {
        if (prices == null || prices.size() <= period) {
            throw new IllegalArgumentException("Недостаточно данных для расчета RSI");
        }

        double avgGain = 0;
        double avgLoss = 0;

        // Первоначальный расчет средних gain/loss
        for (int i = 1; i <= period; i++) {
            double change = prices.get(i) - prices.get(i-1);
            if (change >= 0) {
                avgGain += change;
            } else {
                avgLoss += Math.abs(change);
            }
        }

        avgGain /= period;
        avgLoss /= period;

        // Последующие расчеты
        for (int i = period + 1; i < prices.size(); i++) {
            double change = prices.get(i) - prices.get(i-1);

            if (change >= 0) {
                avgGain = (avgGain * (period - 1) + change) / period;
                avgLoss = (avgLoss * (period - 1)) / period;
            } else {
                avgLoss = (avgLoss * (period - 1) + Math.abs(change)) / period;
                avgGain = (avgGain * (period - 1)) / period;
            }
        }

        if (avgLoss == 0) return 100;
        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }
        public static String predictTrend(List<Double> prices) {
        if (prices.size() < 20) return "Недостаточно данных";
        double sma10 = calculateSMA(prices, 10);
        double sma20 = calculateSMA(prices, 20);
        double lastPrice = prices.get(prices.size() - 1);

        if (lastPrice > sma20 && sma10 > sma20) {
            return "Сильный восходящий тренд";
        } else if (lastPrice < sma20 && sma10 < sma20) {
            return "Сильный нисходящий тренд";
        } else {
            return "Неопределённый тренд";
        }
    }
}