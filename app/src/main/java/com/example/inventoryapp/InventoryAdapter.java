package com.example.inventoryapp;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;

import java.util.List;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {

    private Context context;
    private List<InventoryItem> items;
    private OnItemActionListener listener;

    public interface OnItemActionListener {
        void onEdit(InventoryItem item);
        void onDelete(InventoryItem item);
    }

    public InventoryAdapter(Context context, List<InventoryItem> items, OnItemActionListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_inventory_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InventoryItem item = items.get(position);

        holder.tvItemName.setText(item.getName());
        holder.tvCategory.setText(item.getCategory() != null ? item.getCategory() : "Uncategorized");
        holder.tvQuantity.setText("Qty: " + item.getQuantity());
        holder.tvPrice.setText(String.format("₹%.2f", item.getPrice()));
        holder.tvDescription.setText(item.getDescription() != null ? item.getDescription() : "No description");

        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            Glide.with(context)
                .load(item.getImageUrl())
                .transform(new CircleCrop())
                .placeholder(R.mipmap.ic_launcher)
                .into(holder.ivProductImage);
        } else {
            holder.ivProductImage.setImageResource(R.mipmap.ic_launcher);
        }

        // Stock status color coding
        if (item.isOutOfStock()) {
            holder.tvStatus.setText("OUT OF STOCK");
            holder.tvStatus.setTextColor(Color.parseColor("#F44336"));
            holder.tvQuantity.setTextColor(Color.parseColor("#F44336"));
            holder.cardView.setCardBackgroundColor(Color.parseColor("#FFEBEE"));
        } else if (item.isLowStock()) {
            holder.tvStatus.setText("LOW STOCK");
            holder.tvStatus.setTextColor(Color.parseColor("#FF9800"));
            holder.tvQuantity.setTextColor(Color.parseColor("#FF9800"));
            holder.cardView.setCardBackgroundColor(Color.parseColor("#FFF8E1"));
        } else {
            holder.tvStatus.setText("IN STOCK");
            holder.tvStatus.setTextColor(Color.parseColor("#4CAF50"));
            holder.tvQuantity.setTextColor(Color.parseColor("#4CAF50"));
            holder.cardView.setCardBackgroundColor(Color.WHITE);
        }

        holder.btnEdit.setOnClickListener(v -> listener.onEdit(item));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateList(List<InventoryItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvItemName, tvCategory, tvQuantity, tvPrice, tvDescription, tvStatus;
        ImageButton btnEdit, btnDelete;
        ImageView ivProductImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProductImage = itemView.findViewById(R.id.ivProductImage);
            cardView = itemView.findViewById(R.id.cardView);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }


}
