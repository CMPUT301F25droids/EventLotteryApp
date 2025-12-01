package com.example.eventlotteryapp.organizer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
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

/**
 * RecyclerView adapter for displaying event cards in the organizer's event list.
 * Handles rendering event information including title, date, location, status, and images.
 * Supports click listeners for navigating to event details.
 * 
 * @author Droids Team
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    /** List of events with their IDs to display. */
    private List<MyEventsFragment.EventWithId> events;
    
    /** Date formatter for displaying event dates. */
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
    
    /** Click listener for handling event item clicks. */
    private OnItemClickListener clickListener;

    /**
     * Interface for handling clicks on event items.
     */
    public interface OnItemClickListener {
        /**
         * Called when an event item is clicked.
         * 
         * @param eventId the unique identifier of the clicked event
         */
        void onItemClick(String eventId);
    }

    /**
     * Constructs a new EventAdapter with the specified list of events.
     * 
     * @param events the list of events to display
     */
    public EventAdapter(List<MyEventsFragment.EventWithId> events) {
        this.events = new ArrayList<>(events);
    }

    /**
     * Sets the click listener for event items.
     * 
     * @param listener the OnItemClickListener to set
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
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
        MyEventsFragment.EventWithId wrapper = events.get(position);
        Event event = wrapper.event;

        // Title, date, location, status
        holder.titleText.setText(event.getTitle());
        holder.locationText.setText(event.getLocation());

        if (event.getEventStartDate() != null) {
            holder.dateText.setText(dateFormat.format(event.getEventStartDate()));
        } else {
            holder.dateText.setText("Date TBD");
        }

        Date now = new Date();
        String status;
        int indicator;

        if (event.getEventStartDate() == null || event.getEventEndDate() == null) {
            status = "Pending";
            indicator = R.drawable.status_indicator_yellow;
        } else if (now.before(event.getEventStartDate())) {
            status = "Upcoming";
            indicator = R.drawable.status_indicator_yellow;
        } else if (now.after(event.getEventEndDate())) {
            status = "Closed";
            indicator = R.drawable.status_indicator_red;
        } else {
            status = "Open";
            indicator = R.drawable.status_indicator_green;
        }

        holder.statusText.setText(status);
        holder.statusIndicator.setBackgroundResource(indicator);

        // Loads event image
        String base64 = event.getImage();
        if (base64 != null && !base64.trim().isEmpty()) {
            Bitmap bitmap = decodeBase64(base64);

            if (bitmap != null) {
                holder.eventImage.setImageBitmap(bitmap);
            } else {
                holder.eventImage.setImageResource(R.drawable.placeholder_image);
            }
        } else {
            holder.eventImage.setImageResource(R.drawable.placeholder_image);
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onItemClick(wrapper.id);
        });
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * Updates the list of events and notifies the adapter of the change.
     * 
     * @param newEvents the new list of events to display
     */
    public void updateEvents(List<MyEventsFragment.EventWithId> newEvents) {
        this.events = new ArrayList<>(newEvents);
        notifyDataSetChanged();
    }

    /**
     * Decodes a base64-encoded string into a Bitmap image.
     * 
     * @param base64Str the base64-encoded image string
     * @return the decoded Bitmap, or null if decoding fails
     */
    private Bitmap decodeBase64(String base64Str) {
        try {
            // Removes prefix if present
            if (base64Str.contains(",")) {
                base64Str = base64Str.substring(base64Str.indexOf(",") + 1);
            }
            byte[] decoded = Base64.decode(base64Str, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * ViewHolder for event card items in the RecyclerView.
     * Holds references to all views that need to be updated for each event.
     */
    static class EventViewHolder extends RecyclerView.ViewHolder {
        /** Text view displaying the event title. */
        TextView titleText, dateText, locationText, statusText;
        
        /** View displaying the status indicator (colored dot). */
        View statusIndicator;
        
        /** Image view displaying the event poster image. */
        ImageView eventImage;

        /**
         * Constructs a new EventViewHolder.
         * 
         * @param itemView the root view of the event card layout
         */
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
