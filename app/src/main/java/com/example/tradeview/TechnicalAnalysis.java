package com.example.tradeview;
import java.util.List;
import java.util.Arrays;
public class TechnicalAnalysis {
    public static double calculateSMA(List<Double> prices, int period) {
        if (prices.size() < period) {
            throw new IllegalArgumentException("Not enough data points to calculate SMA.");
        }

        double sum = 0;
        for (int i = prices.size() - period; i < prices.size(); i++) {
            sum += prices.get(i);
        }
        return sum / period;
    }
    public static double calculateEMA(List<Double> prices, int period) {
        if (prices.size() < period) {
            throw new IllegalArgumentException("Not enough data points to calculate EMA.");
        }

        double multiplier = 2.0 / (period + 1);
        double ema = calculateSMA(prices.subList(0, period), period);

        for (int i = period; i < prices.size(); i++) {
            ema = (prices.get(i) - ema) * multiplier + ema;
        }
        return ema;
    }
    public static double calculateRSI(List<Double> prices, int period) {
        if (prices.size() < period + 1) {
            throw new IllegalArgumentException("Not enough data points to calculate RSI.");
        }

        double avgGain = 0;
        double avgLoss = 0;
        for (int i = 1; i <= period; i++) {
            double change = prices.get(i) - prices.get(i - 1);
            if (change >= 0) {
                avgGain += change;
            } else {
                avgLoss += Math.abs(change);
            }
        }
        avgGain /= period;
        avgLoss /= period;
        for (int i = period + 1; i < prices.size(); i++) {
            double change = prices.get(i) - prices.get(i - 1);
            if (change >= 0) {
                avgGain = (avgGain * (period - 1) + change) / period;
                avgLoss = (avgLoss * (period - 1)) / period;
            } else {
                avgLoss = (avgLoss * (period - 1) + Math.abs(change)) / period;
                avgGain = (avgGain * (period - 1)) / period;
            }
        }

        if (avgLoss == 0) {
            return 100; // Avoid division by zero
        }
        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }

    public static double[] calculateMACD(List<Double> prices, int shortPeriod, int longPeriod, int signalPeriod) {
        if (prices.size() < longPeriod + signalPeriod) {
            throw new IllegalArgumentException("Not enough data points to calculate MACD.");
        }

        double[] macdValues = new double[prices.size() - longPeriod];
        double[] signalValues = new double[macdValues.length - signalPeriod + 1];
        double[] histogramValues = new double[signalValues.length];

        // Calculate MACD values
        for (int i = longPeriod; i < prices.size(); i++) {
            double shortEMA = calculateEMA(prices.subList(i - shortPeriod, i), shortPeriod);
            double longEMA = calculateEMA(prices.subList(i - longPeriod, i), longPeriod);
            macdValues[i - longPeriod] = shortEMA - longEMA;
        }


        // Calculate Histogram (MACD - Signal)
        for (int i = 0; i < histogramValues.length; i++) {
            histogramValues[i] = macdValues[i + signalPeriod - 1] - signalValues[i];
        }
        return new double[]{
                macdValues[macdValues.length - 1], // Latest MACD value
                signalValues[signalValues.length - 1], // Latest Signal value
                histogramValues[histogramValues.length - 1] // Latest Histogram value
        };
    }
}