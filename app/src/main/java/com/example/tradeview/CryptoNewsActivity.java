package com.example.tradeview;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.util.ArrayList;
import java.util.List;

public class CryptoNewsActivity extends AppCompatActivity {
    private NewsAdapter newsAdapter;
    private SwipeRefreshLayout refreshLayout;
    private ProgressBar progressBar;
    private final Handler handler = new Handler();
    private static final long REFRESH_DELAY = 300000; // 5 минут

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crypto_news);

        RecyclerView newsRecyclerView = findViewById(R.id.newsRecyclerView);
        refreshLayout = findViewById(R.id.refreshLayout);
        progressBar = findViewById(R.id.progressBar);

        // Настройка RecyclerView
        newsAdapter = new NewsAdapter(new ArrayList<>());
        newsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        newsRecyclerView.setAdapter(newsAdapter);

        // Обновление при свайпе
        refreshLayout.setOnRefreshListener(this::loadNews);

        // Первая загрузка
        loadNews();
    }

    private void loadNews() {
        progressBar.setVisibility(View.VISIBLE);
        CryptoNewsApi.getLatestNews(new CryptoNewsApi.NewsCallback() {
            @Override
            public void onSuccess(List<NewsItem> news) {
                runOnUiThread(() -> {
                    newsAdapter.updateNews(news);
                    progressBar.setVisibility(View.GONE);
                    refreshLayout.setRefreshing(false);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(CryptoNewsActivity.this, error, Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    refreshLayout.setRefreshing(false);
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Автообновление каждые 5 минут
        handler.postDelayed(newsUpdater, REFRESH_DELAY);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(newsUpdater);
    }

    private final Runnable newsUpdater = new Runnable() {
        @Override
        public void run() {
            loadNews();
            handler.postDelayed(this, REFRESH_DELAY);
        }
    };
}