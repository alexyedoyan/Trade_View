package com.example.tradeview;

public class CandleStick {
    private float open;
    private float high;
    private float low;
    private float close;

    public CandleStick(float open, float high, float low, float close) {
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
    }

    public float getOpen() {
        return open;
    }

    public float getHigh() {
        return high;
    }

    public float getLow() {
        return low;
    }

    public float getClose() {
        return close;
    }
}