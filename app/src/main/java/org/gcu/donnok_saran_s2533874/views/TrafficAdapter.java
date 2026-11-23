package org.gcu.donnok_saran_s2533874.views;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.gcu.donnok_saran_s2533874.R;
import org.gcu.donnok_saran_s2533874.model.TrafficItem;
import java.util.List;

//Adapter for displaying list of traffic incidents in the RecyclerView
public class TrafficAdapter extends RecyclerView.Adapter<TrafficAdapter.ViewHolder> {

    //Click interface for item tap events
    public interface OnItemClickListener {
        void onItemClick(TrafficItem item);
    }

    private List<TrafficItem> items;
    private final OnItemClickListener listener;

    public TrafficAdapter(List<TrafficItem> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    //Refresh list data when filters change
    public void updateItems(List<TrafficItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_traffic, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TrafficItem item = items.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() { return items != null ? items.size() : 0; }

    //ViewHolder holds views for individual list row
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtRoadName, txtDelayBadge, txtDescription, txtDates;

        ViewHolder(View itemView) {
            super(itemView);
            txtRoadName = itemView.findViewById(R.id.txtRoadName);
            txtDelayBadge = itemView.findViewById(R.id.txtDelayBadge);
            txtDescription = itemView.findViewById(R.id.txtDescription);
            txtDates = itemView.findViewById(R.id.txtDates);
        }

        void bind(TrafficItem item, OnItemClickListener listener) {
            String titleStr = (item.getRoadName().isEmpty() ? "" : "[" + item.getRoadName() + "] ") + item.getTitle();
            txtRoadName.setText(titleStr);

            txtDelayBadge.setText(item.getDelaySeverity());
            try {
                txtDelayBadge.setBackgroundColor(Color.parseColor(item.getDelayColorHex()));
            } catch (Exception e) {
                txtDelayBadge.setBackgroundColor(Color.DKGRAY);
            }

            txtDescription.setText(item.getDescription());

            String dateStr = "Category: " + item.getCategory();
            if (!item.getStartTime().isEmpty()) dateStr += " | Start: " + item.getStartTime().substring(0, Math.min(10, item.getStartTime().length()));
            txtDates.setText(dateStr);

            itemView.setOnClickListener(v -> listener.onItemClick(item));
        }
    }
}