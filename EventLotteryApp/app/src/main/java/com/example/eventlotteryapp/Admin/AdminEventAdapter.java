package com.example.eventlotteryapp.Admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventlotteryapp.R;

import java.util.List;

/**
 * RecyclerView adapter used by the admin panel to display
 * a list of events in the Browse Events screen.
 */
public class AdminEventAdapter extends RecyclerView.Adapter<AdminEventAdapter.ViewHolder> {

    private final List<AdminEvent> events;

    /**
     * Creates a new adapter instance.
     *
     * @param events A list of events to display.
     */
    public AdminEventAdapter(List<AdminEvent> events) {
        this.events = events;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_event, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AdminEvent event = events.get(position);
        holder.title.setText(event.getTitle());
        holder.location.setText(event.getLocation());
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * ViewHolder class holding the UI components for each event item.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, location;

        /**
         * Initializes UI references for a single event card.
         *
         * @param itemView The inflated item view layout.
         */
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.event_title);
            location = itemView.findViewById(R.id.event_location);
        }
    }
}

