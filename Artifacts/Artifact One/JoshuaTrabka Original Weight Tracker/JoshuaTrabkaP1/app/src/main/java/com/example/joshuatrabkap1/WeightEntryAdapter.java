package com.example.joshuatrabkap1;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView Adapter for displaying a list of WeightEntry objects.
 */
public class WeightEntryAdapter extends RecyclerView.Adapter<WeightEntryAdapter.ViewHolder> {

    private final Context context;
    private List<WeightEntry> entries;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
    private final OnItemInteractionListener listener;

    // Interface to communicate click events back to the Activity/Fragment
    public interface OnItemInteractionListener {
        void onEditClick(WeightEntry entry);
        void onDeleteClick(WeightEntry entry);
    }

    public WeightEntryAdapter(Context context, List<WeightEntry> entries, OnItemInteractionListener listener) {
        this.context = context;
        this.entries = entries;
        this.listener = listener;
    }

    // Replace the existing data set with a new list and refresh the view
    public void setEntries(List<WeightEntry> newEntries) {
        this.entries = newEntries;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Here we inflate a layout for a single row: res_layout_weight_row.xml
        View view = LayoutInflater.from(context).inflate(R.layout.res_layout_weight_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final WeightEntry entry = entries.get(position);

        // Bind data
        holder.dateView.setText(dateFormat.format(entry.getDate()));
        holder.weightView.setText(String.format(Locale.US, "%.1f lbs", entry.getWeight()));

        // Setup click listeners
        // 1. Edit/Row Click: entire row opens the edit dialog
        holder.itemView.setOnClickListener(v -> listener.onEditClick(entry));

        // 2. Delete Button Click: triggers delete confirmation
        holder.actionButton.setOnClickListener(v -> listener.onDeleteClick(entry));
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    /**
     * ViewHolder holds the views for a single item (row) in the RecyclerView.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView dateView;
        final TextView weightView;
        final Button actionButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            dateView = itemView.findViewById(R.id.text_row_date);
            weightView = itemView.findViewById(R.id.text_row_weight);
            actionButton = itemView.findViewById(R.id.button_row_delete);
        }
    }
}

