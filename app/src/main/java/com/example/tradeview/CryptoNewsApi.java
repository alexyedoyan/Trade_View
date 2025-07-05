package com.example.tradeview;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class CryptoNewsApi {
    private static final String TAG = "CryptoNewsApi";
    private static final String API_URL = "https://min-api.cryptocompare.com/data/v2/news/?lang=EN";

    public interface NewsCallback {
        void onSuccess(List<NewsItem> news);
        void onError(String error);
    }

    public static void getLatestNews(NewsCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                InputStream inputStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                List<NewsItem> newsList = parseNews(response.toString());
                callback.onSuccess(newsList);

            } catch (Exception e) {
                Log.e(TAG, "Error fetching news", e);
                callback.onError("Failed to load news: " + e.getMessage());
            }
        }).start();
    }

    private static List<NewsItem> parseNews(String jsonResponse) {
        List<NewsItem> newsList = new ArrayList<>();
        try {
            JSONObject response = new JSONObject(jsonResponse);
            JSONArray data = response.getJSONArray("Data");

            for (int i = 0; i < data.length(); i++) {
                JSONObject item = data.getJSONObject(i);
                newsList.add(new NewsItem(
                        item.getString("title"),
                        item.getString("url"),
                        item.getString("source"),
                        item.getString("imageurl"),
                        item.getString("published_on")
                ));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing news", e);
        }
        return newsList;
    }
}