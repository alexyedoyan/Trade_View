package com.example.tradeview;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import com.example.tradeview.R;

public class CryptoAdapter extends RecyclerView.Adapter<CryptoAdapter.CryptoViewHolder> {

    private List<CryptoModel> cryptoList;

    public CryptoAdapter(List<CryptoModel> cryptoList) {
        this.cryptoList = cryptoList;
    }

    @NonNull
    @Override
    public CryptoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_crypto, parent, false);
        return new CryptoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CryptoViewHolder holder, int position) {
        CryptoModel crypto = cryptoList.get(position);
        holder.symbolText.setText(crypto.getSymbol());
        holder.priceText.setText(crypto.getPrice());
    }

    @Override
    public int getItemCount() {
        return cryptoList.size();
    }

    public static class CryptoViewHolder extends RecyclerView.ViewHolder {
        TextView symbolText;
        TextView priceText;

        public CryptoViewHolder(@NonNull View itemView) {
            super(itemView);
            symbolText = itemView.findViewById(R.id.symbolText);
            priceText = itemView.findViewById(R.id.priceText);
        }
    }
}