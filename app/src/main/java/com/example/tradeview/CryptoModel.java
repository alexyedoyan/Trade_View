package com.example.tradeview;


public class CryptoModel {
    private String symbol;
    private String price;
    public CryptoModel(String symbol, String price) {
        this.symbol = symbol;
        this.price = price;
    }
    public String getSymbol() {
        return symbol;
    }
    public String getPrice() {
        return price;
    }
}