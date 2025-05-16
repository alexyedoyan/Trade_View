package com.example.tradeview;

public class WalletItem {
    private final String symbol;
    private final double amount;
    private final double value;
    private final double currentPrice;

    public WalletItem(String symbol, double amount, double value, double currentPrice) {
        this.symbol = symbol;
        this.amount = amount;
        this.value = value;
        this.currentPrice = currentPrice;
    }

    public String getSymbol() {
        return symbol;
    }
    public double getCurrentPrice() {
        return currentPrice;
    }

    public double getAmount() {
        return amount;
    }

    public double getValue() {
        return value;
    }
}