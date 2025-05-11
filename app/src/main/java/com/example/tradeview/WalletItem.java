package com.example.tradeview;

public class WalletItem {
    private final String symbol;
    private final double amount;
    private final double value;

    public WalletItem(String symbol, double amount, double value) {
        this.symbol = symbol;
        this.amount = amount;
        this.value = value;
    }

    public String getSymbol() {
        return symbol;
    }

    public double getAmount() {
        return amount;
    }

    public double getValue() {
        return value;
    }
}