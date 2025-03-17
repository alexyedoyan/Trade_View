package com.example.tradeview;

import java.util.List;

public class ExchangeInfo {

    private String timezone;
    private long serverTime;
    private List<Symbol> symbols;

    // Геттеры
    public String getTimezone() {
        return timezone;
    }

    public long getServerTime() {
        return serverTime;
    }

    public List<Symbol> getSymbols() {
        return symbols;
    }
    public static class Symbol {
        private String symbol;
        private String status;
        private String baseAsset;
        private String quoteAsset;

        // Геттеры
        public String getSymbol() {
            return symbol;
        }

        public String getStatus() {
            return status;
        }

        public String getBaseAsset() {
            return baseAsset;
        }

        public String getQuoteAsset() {
            return quoteAsset;
        }
    }
}