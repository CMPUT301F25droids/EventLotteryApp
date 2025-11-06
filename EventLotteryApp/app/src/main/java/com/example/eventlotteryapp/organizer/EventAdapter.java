package com.example.eventlotteryapp.organizer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.data.Event;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private List<MyEventsFragment.EventWithId> events;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

    public EventAdapter(List<MyEventsFragment.EventWithId> events) {
        this.events = new ArrayList<>(events);
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_event_card, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        MyEventsFragment.EventWithId eventWithId = events.get(position);
        Event event = eventWithId.event;

        holder.titleText.setText(event.title());
        
        if (event.eventStartDate() != null) {
            holder.dateText.setText(dateFormat.format(event.eventStartDate()));
        } else {
            holder.dateText.setText("Date TBD");
        }

        holder.locationText.setText(event.location());

        // Determine status
        Date now = new Date();
        String status;
        int statusIndicatorRes;
        
        if (event.eventStartDate() == null || event.eventEndDate() == null) {
            status = "Pending";
            statusIndicatorRes = R.drawable.status_indicator_yellow;
        } else if (now.before(event.eventStartDate())) {
            status = "Upcoming";
            statusIndicatorRes = R.drawable.status_indicator_yellow;
        } else if (now.after(event.eventEndDate())) {
            status = "Closed";
            statusIndicatorRes = R.drawable.status_indicator_red;
        } else {
            status = "Open";
            statusIndicatorRes = R.drawable.status_indicator_green;
        }

        holder.statusText.setText(status);
        holder.statusIndicator.setBackgroundResource(statusIndicatorRes);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    public void updateEvents(List<MyEventsFragment.EventWithId> newEvents) {
        this.events = new ArrayList<>(newEvents);
        notifyDataSetChanged();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView titleText;
        TextView dateText;
        TextView locationText;
        TextView statusText;
        View statusIndicator;
        ImageView eventImage;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.event_title);
            dateText = itemView.findViewById(R.id.event_date);
            locationText = itemView.findViewById(R.id.event_location);
            statusText = itemView.findViewById(R.id.event_status);
            statusIndicator = itemView.findViewById(R.id.status_indicator);
            eventImage = itemView.findViewById(R.id.event_image);
        }
    }
}


