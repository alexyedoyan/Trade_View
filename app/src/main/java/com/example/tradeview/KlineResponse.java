package com.example.tradeview;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class KlineResponse {
    private String open;
    private String high;
    private String low;
    private String close;
    private long openTime;
    @SerializedName("data")
    private List<KlineData> data;

    public List<KlineData> getData() {
        return data;
    }
}
