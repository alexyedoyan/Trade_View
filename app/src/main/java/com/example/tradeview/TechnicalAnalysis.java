package com.example.tradeview;

import java.util.*;

public class TechnicalAnalysis {

    public static class BollingerBands {
        public List<Double> upper;
        public List<Double> middle;
        public List<Double> lower;
    }
    public static class MACDData {
        public List<Double> macdLine;
        public List<Double> signalLine;
        public List<Double> histogram;

        public double[] toDoubleArray() {
            if (macdLine == null || signalLine == null ||
                    macdLine.isEmpty() || signalLine.isEmpty()) {
                return new double[]{0, 0};
            }
            return new double[]{
                    macdLine.get(macdLine.size()-1),
                    signalLine.get(signalLine.size()-1)
            };
        }
        public double[] getLastValues() {
            if (macdLine == null || macdLine.isEmpty() ||
                    signalLine == null || signalLine.isEmpty()) {
                return new double[]{0, 0};
            }
            return new double[]{
                    macdLine.get(macdLine.size()-1),
                    signalLine.get(signalLine.size()-1)
            };
        }
    }

    // 1. БАЗОВЫЕ МЕТОДЫ -----------------------------------------------------

    public static double getPriceRange(List<Double> prices) {
        if (prices == null || prices.isEmpty()) return 0;
        return Collections.max(prices) - Collections.min(prices);
    }

    public static double calculateSMA(List<Double> prices, int period) {
        if (prices == null || prices.size() < period) {
            throw new IllegalArgumentException("Not enough data for SMA");
        }
        double sum = 0;
        for (int i = prices.size() - period; i < prices.size(); i++) {
            sum += prices.get(i);
        }
        return sum / period;
    }

    public static List<Float> calculateSmaList(List<Double> prices, int period) {
        List<Float> smaValues = new ArrayList<>();
        for (int i = period; i <= prices.size(); i++) {
            smaValues.add((float) calculateSMA(prices.subList(i - period, i), period));
        }
        return smaValues;
    }

    public static double calculateEMA(List<Double> prices, int period) {
        if (prices == null || prices.size() < period) {
            throw new IllegalArgumentException("Not enough data for EMA");
        }
        double multiplier = 2.0 / (period + 1);
        double ema = calculateSMA(prices.subList(0, period), period);

        for (int i = period; i < prices.size(); i++) {
            ema = (prices.get(i) - ema) * multiplier + ema;
        }
        return ema;
    }

    public static List<Double> calculateEmaList(List<Double> prices, int period) {
        List<Double> emaValues = new ArrayList<>();
        if (prices.size() < period) return emaValues;

        double multiplier = 2.0 / (period + 1);
        double ema = calculateSMA(prices.subList(0, period), period);
        emaValues.add(ema);

        for (int i = period; i < prices.size(); i++) {
            ema = (prices.get(i) - ema) * multiplier + ema;
            emaValues.add(ema);
        }
        return emaValues;
    }

    // 2. МЕТОДЫ ДЛЯ MACD ----------------------------------------------------

    public static MACDData calculateMACD(List<Double> prices, int fastPeriod,
                                         int slowPeriod, int signalPeriod) {
        MACDData macdData = new MACDData();
        macdData.macdLine = new ArrayList<>();
        macdData.signalLine = new ArrayList<>();
        macdData.histogram = new ArrayList<>();

        List<Double> fastEMA = calculateEmaList(prices, fastPeriod);
        List<Double> slowEMA = calculateEmaList(prices, slowPeriod);

        for (int i = 0; i < Math.min(fastEMA.size(), slowEMA.size()); i++) {
            macdData.macdLine.add(fastEMA.get(i) - slowEMA.get(i));
        }

        macdData.signalLine = calculateEmaList(macdData.macdLine, signalPeriod);

        for (int i = 0; i < Math.min(macdData.macdLine.size(), macdData.signalLine.size()); i++) {
            macdData.histogram.add(
                    macdData.macdLine.get(i) - macdData.signalLine.get(i)
            );
        }

        return macdData;
    }

    public static String analyzeMACD(double macdValue, double signalValue) {
        if (macdValue > signalValue) {
            return "Bullish (MACD above Signal)";
        } else if (macdValue < signalValue) {
            return "Bearish (MACD below Signal)";
        }
        return "Neutral (MACD crossing)";
    }

    // 3. МЕТОДЫ ДЛЯ RSI -----------------------------------------------------

    public static double calculateRSI(List<Double> prices, int period) {
        if (prices == null || prices.size() <= period) {
            throw new IllegalArgumentException("Not enough data for RSI");
        }

        double avgGain = 0;
        double avgLoss = 0;

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

    // 4. ПРОГНОЗИРОВАНИЕ ТРЕНДА ---------------------------------------------

    public static String predictTrend(List<Double> prices) {
        if (prices.size() < 50) return "Not enough data";

        double sma20 = calculateSMA(prices, 20);
        double sma50 = calculateSMA(prices, 50);
        double lastPrice = prices.get(prices.size()-1);

        MACDData macd = calculateMACD(prices, 12, 26, 9);
        double[] macdValues = macd.getLastValues();
        String macdAnalysis = analyzeMACD(macdValues[0], macdValues[1]);

        double rsi = calculateRSI(prices, 14);

        StringBuilder trend = new StringBuilder();

        if (lastPrice > sma50 && sma20 > sma50) {
            trend.append("Strong Uptrend");
        } else if (lastPrice < sma50 && sma20 < sma50) {
            trend.append("Strong Downtrend");
        } else if (lastPrice > sma20) {
            trend.append("Mild Uptrend");
        } else {
            trend.append("Mild Downtrend");
        }

        trend.append("\nMACD: ").append(macdAnalysis);

        if (rsi > 70) trend.append("\nOverbought (RSI)");
        if (rsi < 30) trend.append("\nOversold (RSI)");

        return trend.toString();
    }

    // 5. ДОПОЛНИТЕЛЬНЫЕ ИНДИКАТОРЫ ------------------------------------------

    public static BollingerBands calculateBollingerBands(List<Double> prices, int period, double multiplier) {
        BollingerBands bands = new BollingerBands();
        bands.middle = new ArrayList<>();
        bands.upper = new ArrayList<>();
        bands.lower = new ArrayList<>();

        for (int i = period; i <= prices.size(); i++) {
            List<Double> window = prices.subList(i - period, i);
            double sma = calculateSMA(window, period);
            double stdDev = calculateStandardDeviation(window);

            bands.middle.add(sma);
            bands.upper.add(sma + stdDev * multiplier);
            bands.lower.add(sma - stdDev * multiplier);
        }

        return bands;
    }

    private static double calculateStandardDeviation(List<Double> values) {
        double mean = calculateSMA(values, values.size());
        double sum = 0;
        for (double val : values) {
            sum += Math.pow(val - mean, 2);
        }
        return Math.sqrt(sum / values.size());
    }
}