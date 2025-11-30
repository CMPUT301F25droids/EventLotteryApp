package com.example.eventlotteryapp.EntrantView;

import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventlotteryapp.R;
import com.google.firebase.firestore.DocumentReference;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class EventsListRecyclerViewAdapter extends RecyclerView.Adapter<EventsListRecyclerViewAdapter.ViewHolder> {
    public interface onEventClickListener {
        void onItemClick(int position);
    }

    protected final List<EventItem> originalList;  // all events
    private final List<EventItem> filteredList;  // currently displayed events
    private final onEventClickListener listener;

    public EventsListRecyclerViewAdapter(List<EventItem> eventList,
                                         onEventClickListener listener) {
        this.originalList = new ArrayList<>(eventList);
        this.filteredList = new ArrayList<>(eventList);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.event_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EventItem event = filteredList.get(position);

        // --- CRITICAL: Clear recycled content ---
        holder.imageView.setImageDrawable(null);
        holder.organizerView.setText("Organized by: Loading...");
        holder.nameView.setText("");
        holder.costView.setText("");

        // --- Set simple fields ---
        holder.nameView.setText(event.getName());
        holder.costView.setText(event.getCost());

        // --- Load organizer safely ---
        DocumentReference organizerRef = event.getOrganizer();
        if (organizerRef != null) {
            organizerRef.get().addOnSuccessListener(userSnapshot -> {
                // Ensure this row is still the same position after an async load
                int currentPos = holder.getBindingAdapterPosition();
                if (currentPos == RecyclerView.NO_POSITION) return;

                if (userSnapshot.exists()) {
                    String organizerName = userSnapshot.getString("name");
                    holder.organizerView.setText("Organized by: " + organizerName);
                } else {
                    holder.organizerView.setText("Organized by: Unknown");
                }

            }            ).addOnFailureListener(e ->
                    Log.e("EventAdapter", "Error loading organizer", e)
            );
        } else {
            holder.organizerView.setText("Organized by: Unknown");
        }

        // --- Load image safely ---
        String base64Image = event.getImage();
        if (base64Image != null && !base64Image.isEmpty()) {
            try {
                if (base64Image.startsWith("data:image")) {
                    base64Image = base64Image.substring(base64Image.indexOf(",") + 1);
                }

                byte[] decodedBytes = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT);
                Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                // Again: verify row hasn't been recycled before applying bitmap
                int currentPos = holder.getAdapterPosition();
                if (currentPos != RecyclerView.NO_POSITION) {
                    holder.imageView.setImageBitmap(bitmap);
                }

            } catch (Exception e) {
                Log.e("EventAdapter", "Failed to decode image: " + e.getMessage());
                holder.imageView.setImageResource(R.drawable.placeholder_image);
            }
        } else {
            holder.imageView.setImageResource(R.drawable.placeholder_image);
        }
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    /** Filter events by query with title priority */
    public void filter(String query) {
        query = query.toLowerCase();
        filteredList.clear();

        if (query.isEmpty()) {
            filteredList.addAll(originalList);
        } else {
            List<EventItem> titleMatches = new ArrayList<>();
            List<EventItem> descriptionMatches = new ArrayList<>();

            for (EventItem item : originalList) {
                String title = item.getName() != null ? item.getName().toLowerCase() : "";
                String desc = item.getDescription() != null ? item.getDescription().toLowerCase() : "";

                if (title.contains(query)) {
                    titleMatches.add(item);
                } else if (desc.contains(query)) {
                    descriptionMatches.add(item);
                }
            }

            filteredList.addAll(titleMatches);
            filteredList.addAll(descriptionMatches);
        }

        notifyDataSetChanged();
    }
    public void updateList(List<? extends EventItem> newList) {
        originalList.clear();
        originalList.addAll(newList);
        filteredList.clear();
        filteredList.addAll(newList);
        notifyDataSetChanged();
    }

    public List<EventItem> getFilteredList() {
        return filteredList;
    }
    public void applyCategoryFilter(String filterType) {
        filteredList.clear();
        for (EventItem item : originalList) {
            String title = item.getName().toLowerCase();
            String description = "";
            if (item.getDescription() != null) {
                description = item.getDescription().toLowerCase();
            }
            switch (filterType) {
                case "sports":
                    if (title.contains("sport") ||
                            title.contains("basketball") ||
                            title.contains("football") ||
                            title.contains("soccer") ||
                            title.contains("swimming") ||
                            title.contains("hockey") ||
                            description.contains("sport"))
                        filteredList.add(item);
                    break;

                case "music":
                    if (title.contains("music") ||
                            title.contains("concert") ||
                            title.contains("band") ||
                            description.contains("music"))
                        filteredList.add(item);
                    break;

                case "workshops":
                    if (title.contains("workshop") ||
                            title.contains("training") ||
                            description.contains("workshop"))
                        filteredList.add(item);
                    break;

                case "free":
                    if (item.getCost().equalsIgnoreCase("free") ||
                            item.getCost().equals("0"))
                        filteredList.add(item);
                    break;

                case "community":
                    if (title.contains("community") ||
                            description.contains("community"))
                        filteredList.add(item);
                    break;

                case "all":
                default:
                    filteredList.addAll(originalList);
                    break;
            }
        }

        notifyDataSetChanged();
    }
    public void applyDateFilter(int year, int month, int day) {

        Calendar selected = Calendar.getInstance();
        selected.set(year, month, day, 0, 0, 0);
        selected.set(Calendar.MILLISECOND, 0);

        long selectedMillis = selected.getTimeInMillis();

        filteredList.clear(); // filter into this

        for (EventItem e : originalList) {
            if (e.getEventStartDate() == null) continue;

            Calendar eventCal = Calendar.getInstance();
            eventCal.setTime(e.getEventStartDate());
            eventCal.set(Calendar.HOUR_OF_DAY, 0);
            eventCal.set(Calendar.MINUTE, 0);
            eventCal.set(Calendar.SECOND, 0);
            eventCal.set(Calendar.MILLISECOND, 0);

            if (eventCal.getTimeInMillis() >= selectedMillis) {
                filteredList.add(e);
            }
        }

        notifyDataSetChanged();
    }


    public class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView nameView;
        public final TextView organizerView;
        public final TextView costView;
        public final ImageView imageView;

        public ViewHolder(View view) {
            super(view);
            nameView = view.findViewById(R.id.event_name);
            organizerView = view.findViewById(R.id.event_organizer);
            costView = view.findViewById(R.id.event_cost);
            imageView = view.findViewById(R.id.event_poster);

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
