package com.example.tradeview;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WalletAdapter extends RecyclerView.Adapter<WalletAdapter.WalletViewHolder> {
    private List<WalletItem> walletItems;
    public List<WalletItem> getWalletItems() {
        return walletItems;
    }
    public WalletAdapter(List<WalletItem> walletItems) {
        this.walletItems = walletItems != null ? walletItems : new ArrayList<>();
    }

    public void updateData(List<WalletItem> newItems) {
        this.walletItems = newItems != null ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public WalletViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_wallet, parent, false);
        return new WalletViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WalletViewHolder holder, int position) {
        WalletItem item = walletItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return walletItems.size();
    }

    static class WalletViewHolder extends RecyclerView.ViewHolder {
        private final TextView symbolText;
        private final TextView amountText;
        private final TextView valueText;

        public WalletViewHolder(@NonNull View itemView) {
            super(itemView);
            symbolText = itemView.findViewById(R.id.symbolText);
            amountText = itemView.findViewById(R.id.amountText);
            valueText = itemView.findViewById(R.id.valueText);
        }

        public void bind(WalletItem item) {
            symbolText.setText(item.getSymbol());
            amountText.setText(String.format(Locale.US, "%.6f", item.getAmount()));
            valueText.setText(String.format(Locale.US, "$%.2f", item.getValue()));
        }
    }
}