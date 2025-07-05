package com.example.tradeview;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {
    private List<NewsItem> newsList;

    public NewsAdapter(List<NewsItem> newsList) {
        this.newsList = newsList;
    }

    public void updateNews(List<NewsItem> newNews) {
        newsList.clear();
        newsList.addAll(newNews);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_news, parent, false);
        return new NewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        NewsItem news = newsList.get(position);
        holder.bind(news);
    }

    @Override
    public int getItemCount() {
        return newsList.size();
    }

    static class NewsViewHolder extends RecyclerView.ViewHolder {
        private final ImageView newsImage;
        private final TextView titleText;
        private final TextView sourceText;

        public NewsViewHolder(@NonNull View itemView) {
            super(itemView);
            newsImage = itemView.findViewById(R.id.newsImage);
            titleText = itemView.findViewById(R.id.titleText);
            sourceText = itemView.findViewById(R.id.sourceText);
        }

        public void bind(NewsItem news) {
            titleText.setText(news.getTitle());
            sourceText.setText(news.getSource());

            Glide.with(itemView.getContext())
                    .load(news.getImageUrl())
                    .placeholder(R.drawable.ic_news_placeholder)
                    .into(newsImage);

            itemView.setOnClickListener(v -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(news.getUrl()));
                itemView.getContext().startActivity(browserIntent);
            });
        }
    }
}