package com.example.inventoryapp;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AuditLogAdapter extends RecyclerView.Adapter<AuditLogAdapter.ViewHolder> {

    private List<AuditLog> logs;

    public AuditLogAdapter(List<AuditLog> logs) {
        this.logs = logs;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_audit_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AuditLog log = logs.get(position);
        String action = log.getAction();
        int qty = log.getQuantity();
        String details = log.getDetails();

        String displayText;
        int color;

        if ("SOLD".equalsIgnoreCase(action)) {
            color = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_red);
            displayText = "SOLD (" + qty + " units): " + details;
        } else if ("ADDED".equalsIgnoreCase(action)) {
            color = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_green);
            displayText = "ADDED (" + qty + " units): " + details;
        } else if ("UPDATED".equalsIgnoreCase(action)) {
            color = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_orange);
            displayText = "UPDATED: " + details;
        } else {
            color = Color.BLACK;
            displayText = action + ": " + details;
        }

        holder.tvActionDetails.setText(displayText);
        holder.tvActionDetails.setTextColor(color);
        holder.tvTimestampUser.setText(log.getTimestamp() + " by " + log.getUserName());
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvActionDetails, tvTimestampUser;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvActionDetails = itemView.findViewById(R.id.tvActionDetails);
            tvTimestampUser = itemView.findViewById(R.id.tvTimestampUser);
        }
    }
}
