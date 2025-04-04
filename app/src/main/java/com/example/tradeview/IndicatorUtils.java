package com.example.tradeview;

import java.util.List;

public class IndicatorUtils {

    // Определение тренда по SMA
    public static String getTrendBySMA(List<Double> prices, int shortPeriod, int longPeriod) {
        double shortSMA = TechnicalAnalysis.calculateSMA(prices, shortPeriod);
        double longSMA = TechnicalAnalysis.calculateSMA(prices, longPeriod);

        if (shortSMA > longSMA) {
            return "Восходящий (SMA " + shortPeriod + " > SMA " + longPeriod + ")";
        } else {
            return "Нисходящий (SMA " + shortPeriod + " < SMA " + longPeriod + ")";
        }
    }

    // Анализ RSI
    public static String analyzeRSI(double rsi) {
        if (rsi > 70) {
            return "Перекупленность.";
        } else if (rsi < 30) {
            return "Перепроданность";
        } else {
            return "Нейтральная зона";
        }
    }
}