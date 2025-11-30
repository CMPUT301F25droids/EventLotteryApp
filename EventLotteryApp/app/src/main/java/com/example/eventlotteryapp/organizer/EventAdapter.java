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

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private List<MyEventsFragment.EventWithId> events;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
    private OnItemClickListener clickListener;

    public interface OnItemClickListener {
        void onItemClick(String eventId);
    }

    public EventAdapter(List<MyEventsFragment.EventWithId> events) {
        this.events = new ArrayList<>(events);
    }

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

    public void updateEvents(List<MyEventsFragment.EventWithId> newEvents) {
        this.events = new ArrayList<>(newEvents);
        notifyDataSetChanged();
    }

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

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView titleText, dateText, locationText, statusText;
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
