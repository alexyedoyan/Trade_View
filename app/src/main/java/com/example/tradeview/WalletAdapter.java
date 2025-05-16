package com.example.tradeview;

import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.*;

public class WalletAdapter extends RecyclerView.Adapter<WalletAdapter.WalletViewHolder> {

    private List<WalletItem> walletItems;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position, String symbol);
    }

    public WalletAdapter(List<WalletItem> walletItems) {
        this.walletItems = walletItems != null ? walletItems : new ArrayList<>();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
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
        holder.bind(walletItems.get(position));
    }

    @Override
    public int getItemCount() {
        return walletItems.size();
    }

    class WalletViewHolder extends RecyclerView.ViewHolder {
        private final TextView symbolText, amountText, valueText, priceText;

        public WalletViewHolder(@NonNull View itemView) {
            super(itemView);
            symbolText = itemView.findViewById(R.id.symbolText);
            amountText = itemView.findViewById(R.id.amountText);
            valueText = itemView.findViewById(R.id.valueText);
            priceText = itemView.findViewById(R.id.priceText);
            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(getAdapterPosition(),
                            walletItems.get(getAdapterPosition()).getSymbol());
                    return true;
                }
                return false;
            });
        }

        public void bind(WalletItem item) {
            symbolText.setText(item.getSymbol());
            amountText.setText(String.format("%.6f", item.getAmount()));
            valueText.setText(String.format("$%.2f", item.getValue()));
            priceText.setText(String.format(Locale.US, "$%.2f", item.getCurrentPrice()));
        }
    }
}