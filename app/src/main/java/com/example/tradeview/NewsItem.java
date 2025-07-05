package com.example.tradeview;
public class NewsItem {
    private String title;
    private String url;
    private String source;
    private String imageUrl;
    private String publishedAt;

    public NewsItem(String title, String url, String source, String imageUrl, String publishedAt) {
        this.title = title;
        this.url = url;
        this.source = source;
        this.imageUrl = imageUrl;
        this.publishedAt = publishedAt;
    }

    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public String getSource() { return source; }
    public String getImageUrl() { return imageUrl; }
    public String getPublishedAt() { return publishedAt; }
}