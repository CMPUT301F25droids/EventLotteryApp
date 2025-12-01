package com.example.eventlotteryapp.EntrantView;

import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventlotteryapp.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying MyEventItems in a RecyclerView.
 * Shows events with status indicators (Pending, Accepted, Not Selected).
 */
public class MyEventsListRecyclerViewAdapter extends RecyclerView.Adapter<MyEventsListRecyclerViewAdapter.ViewHolder> {

    public interface onEventClickListener {
        void onItemClick(int position);
    }

    private List<MyEventItem> eventList;
    private final onEventClickListener listener;

    public MyEventsListRecyclerViewAdapter(List<? extends EventItem> list, onEventClickListener listener) {
        this.eventList = new ArrayList<>();
        if (list != null) {
            for (EventItem item : list) {
                if (item instanceof MyEventItem) {
                    this.eventList.add((MyEventItem) item);
                }
            }
        }
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.myevent_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Log.d("MyEventsAdapter", "onBindViewHolder called for position: " + position);
        MyEventItem event = eventList.get(position);
        Log.d("MyEventsAdapter", "Binding event: " + event.getName() + " with status: " + event.getStatus());

        // Set event name
        holder.nameView.setText(event.getName());

        // Set status with emoji
        if (event.getStatus() == MyEventItem.Status.PENDING) {
            holder.statusView.setText("🟡 Pending");
        } else if (event.getStatus() == MyEventItem.Status.SELECTED) {
            holder.statusView.setText("🟢 Accepted");
        } else if (event.getStatus() == MyEventItem.Status.NOT_SELECTED) {
            holder.statusView.setText("🔴 Not Selected");
        } else {
            holder.statusView.setText("Unknown");
        }

        // Format and set date
        if (event.getEventStartDate() != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
            holder.dateView.setText(dateFormat.format(event.getEventStartDate()));
        } else {
            holder.dateView.setText("Date TBD");
        }

        // Set location
        holder.locationView.setText("Location TBD");

        // Set cost
        holder.costView.setText(event.getCost());

        // Load and set image
        String base64Image = event.getImage();
        if (base64Image != null && !base64Image.isEmpty()) {
            try {
                if (base64Image.startsWith("data:image")) {
                    base64Image = base64Image.substring(base64Image.indexOf(",") + 1);
                }

                byte[] decodedBytes = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT);
                Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                holder.imageView.setImageBitmap(bitmap);
            } catch (Exception e) {
                Log.e("MyEventsAdapter", "Failed to decode image: " + e.getMessage());
            }
        }
    }

    @Override
    public int getItemCount() {
        Log.d("MyEventsAdapter", "getItemCount() called. Returning: " + eventList.size());
        return eventList.size();
    }

    /**
     * Updates the adapter with a new list of events.
     *
     * @param newList The new list of events to display
     */
    public void updateList(List<? extends EventItem> newList) {
        this.eventList.clear();
        if (newList != null) {
            for (EventItem item : newList) {
                if (item instanceof MyEventItem) {
                    this.eventList.add((MyEventItem) item);
                }
            }
        }
        Log.d("MyEventsAdapter", "updateList called. New size: " + eventList.size());
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView nameView;
        public final TextView organizerView;
        public final TextView costView;
        public final ImageView imageView;
        public final TextView statusView;
        public final TextView dateView;
        public final TextView locationView;

        public ViewHolder(View view) {
            super(view);
            nameView = view.findViewById(R.id.event_name);
            organizerView = view.findViewById(R.id.event_organizer);
            costView = view.findViewById(R.id.event_cost);
            imageView = view.findViewById(R.id.event_poster);
            statusView = view.findViewById(R.id.event_status);
            dateView = view.findViewById(R.id.event_date);
            locationView = view.findViewById(R.id.event_location);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onItemClick(position);
                    }
                }
            });
        }
    }
}