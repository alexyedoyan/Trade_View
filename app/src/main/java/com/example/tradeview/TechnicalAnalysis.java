package com.example.tradeview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TechnicalAnalysis {

    // Вложенный класс для группировки близких уровней
    private static class LevelCluster {
        private List<Double> touches = new ArrayList<>();

        public LevelCluster(double firstPrice) {
            touches.add(firstPrice);
        }

        public void addTouch(double price) {
            touches.add(price);
        }

        public int getTouchCount() {
            return touches.size();
        }

        public double getAverage() {
            double sum = 0;
            for (Double price : touches) {
                sum += price;
            }
            return sum / touches.size();
        }

        public boolean isInRange(double price, double threshold) {
            return Math.abs(price - getAverage()) < threshold;
        }
    }

    // Метод для расчета SMA
    public static double calculateSMA(List<Double> prices, int period) {
        if (prices == null || prices.size() < period) {
            throw new IllegalArgumentException("Недостаточно данных для SMA");
        }
        double sum = 0;
        for (int i = prices.size() - period; i < prices.size(); i++) {
            sum += prices.get(i);
        }
        return sum / period;
    }

    // Метод для расчета списка значений SMA
    public static List<Float> calculateSmaList(List<Double> prices, int period) {
        List<Float> smaValues = new ArrayList<>();
        for (int i = period; i <= prices.size(); i++) {
            smaValues.add((float) calculateSMA(prices.subList(i - period, i), period));
        }
        return smaValues;
    }

    // Метод для расчета EMA
    public static double calculateEMA(List<Double> prices, int period) {
        if (prices == null || prices.size() < period) {
            throw new IllegalArgumentException("Недостаточно данных для EMA");
        }
        double multiplier = 2.0 / (period + 1);
        double ema = calculateSMA(prices.subList(0, period), period);
        for (int i = period; i < prices.size(); i++) {
            ema = (prices.get(i) - ema) * multiplier + ema;
        }
        return ema;
    }

    // Метод для расчета RSI
    public static double calculateRSI(List<Double> prices, int period) {
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

    // Улучшенный метод для поиска значимых уровней
    public static List<Double> findSignificantLevels(List<Double> prices,
                                                     boolean findSupport,
                                                     int minTouchCount,
                                                     double mergeThreshold) {

        if (prices == null || prices.size() < 20) {
            return new ArrayList<>();
        }

        // 1. Находим все потенциальные экстремумы
        List<Double> extremes = new ArrayList<>();
        for (int i = 5; i < prices.size() - 5; i++) {
            if (isLocalExtreme(prices, i, 5, findSupport)) {
                extremes.add(prices.get(i));
            }
        }

        // 2. Группируем близкие уровни
        List<LevelCluster> clusters = new ArrayList<>();
        for (Double price : extremes) {
            boolean merged = false;
            for (LevelCluster cluster : clusters) {
                if (cluster.isInRange(price, mergeThreshold)) {
                    cluster.addTouch(price);
                    merged = true;
                    break;
                }
            }
            if (!merged) {
                clusters.add(new LevelCluster(price));
            }
        }

        // 3. Фильтруем по значимости
        List<Double> significantLevels = new ArrayList<>();
        for (LevelCluster cluster : clusters) {
            if (cluster.getTouchCount() >= minTouchCount) {
                significantLevels.add(cluster.getAverage());
            }
        }

        return significantLevels;
    }

    // Проверка на локальный экстремум
    private static boolean isLocalExtreme(List<Double> prices, int index,
                                          int lookback, boolean findSupport) {
        double current = prices.get(index);

        for (int i = Math.max(0, index-lookback); i <= Math.min(prices.size()-1, index+lookback); i++) {
            if (i == index) continue;

            if (findSupport && prices.get(i) < current) {
                return false; // Для поддержки - все соседние точки должны быть выше
            }
            if (!findSupport && prices.get(i) > current) {
                return false; // Для сопротивления - все соседние точки должны быть ниже
            }
        }
        return true;
    }

    // Метод для поиска обоих типов уровней
    public static List<List<Double>> findSupportResistanceLevels(List<Double> prices,
                                                                 int minTouchCount,
                                                                 double mergeThreshold,
                                                                 double priceScale) {
        List<Double> supports = findSignificantLevels(prices, true, minTouchCount, mergeThreshold);
        List<Double> resistances = findSignificantLevels(prices, false, minTouchCount, mergeThreshold);

        // Фильтруем уровни, которые слишком близко друг к другу
        List<Double> filteredSupports = filterProximateLevels(supports, priceScale);
        List<Double> filteredResistances = filterProximateLevels(resistances, priceScale);

        List<List<Double>> result = new ArrayList<>();
        result.add(filteredSupports);
        result.add(filteredResistances);
        return result;
    }

    // Фильтрация слишком близких уровней
    private static List<Double> filterProximateLevels(List<Double> levels, double minDistance) {
        List<Double> filtered = new ArrayList<>();
        for (Double level : levels) {
            boolean keep = true;
            for (Double existing : filtered) {
                if (Math.abs(level - existing) < minDistance) {
                    keep = false;
                    break;
                }
            }
            if (keep) {
                filtered.add(level);
            }
        }
        return filtered;
    }

    // Анализ тренда
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

    // Дополнительный метод для получения диапазона цен
    public static double getPriceRange(List<Double> prices) {
        if (prices == null || prices.isEmpty()) return 0;
        double min = Collections.min(prices);
        double max = Collections.max(prices);
        return max - min;
    }
}