package com.example.tradeview;

import java.util.List;

public class KlineData {

    private long openTime;
    private String openPrice;
    private String highPrice;
    private String lowPrice;
    private String closePrice;
    private String volume;
    private long closeTime;
    private String quoteVolume;
    private int numberOfTrades;
    private String takerBuyBaseAssetVolume;
    private String takerBuyQuoteAssetVolume;
    public KlineData(List<String> kline) {
        this.openTime = Long.parseLong(kline.get(0));
        this.openPrice = kline.get(1);
        this.highPrice = kline.get(2);
        this.lowPrice = kline.get(3);
        this.closePrice = kline.get(4);
        this.volume = kline.get(5);
        this.closeTime = Long.parseLong(kline.get(6));
        this.quoteVolume = kline.get(7);
        this.numberOfTrades = Integer.parseInt(kline.get(8));
        this.takerBuyBaseAssetVolume = kline.get(9);
        this.takerBuyQuoteAssetVolume = kline.get(10);
    }
    public long getOpenTime() {
        return openTime;
    }

    public String getOpenPrice() {
        return openPrice;
    }

    public String getHighPrice() {
        return highPrice;
    }

    public String getLowPrice() {
        return lowPrice;
    }

    public String getClosePrice() {
        return closePrice;
    }

    public String getVolume() {
        return volume;
    }

    public long getCloseTime() {
        return closeTime;
    }

    public String getQuoteVolume() {
        return quoteVolume;
    }

    public int getNumberOfTrades() {
        return numberOfTrades;
    }

    public String getTakerBuyBaseAssetVolume() {
        return takerBuyBaseAssetVolume;
    }

    public String getTakerBuyQuoteAssetVolume() {
        return takerBuyQuoteAssetVolume;
    }
}