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

import java.util.List;

public class EventsListRecyclerViewAdapter extends RecyclerView.Adapter<EventsListRecyclerViewAdapter.ViewHolder> {
    public interface onEventClickListener {
        void onItemClick(int position);
    }

    private final List<EventItem> eventList;
    private final onEventClickListener listener;
    public EventsListRecyclerViewAdapter(List<EventItem> eventList,
                                         onEventClickListener listener) {
        this.eventList = eventList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate your event_list.xml layout
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.event_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EventItem event = eventList.get(position);
        holder.nameView.setText(event.getName());
        DocumentReference organizerRef = event.getOrganizer();
        if (organizerRef != null) { // ✅ check null
            organizerRef.get().addOnSuccessListener(userSnapshot -> {
                if (userSnapshot.exists()) {
                    String organizerName = userSnapshot.getString("Name");
                    holder.organizerView.setText(organizerName);
                }
            }).addOnFailureListener(e ->
                    Log.e("EventAdapter", "Error loading organizer", e));
        } else {
            holder.organizerView.setText("Unknown Organizer");
        }
        holder.costView.setText(event.getCost());

        String base64Image = event.getImage();

        if (base64Image != null && !base64Image.isEmpty()) {
            try {
                // Remove the "data:image/jpeg;base64," or similar prefix
                if (base64Image.startsWith("data:image")) {
                    base64Image = base64Image.substring(base64Image.indexOf(",") + 1);
                }

                byte[] decodedBytes = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT);
                Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                holder.imageView.setImageBitmap(bitmap);
            } catch (Exception e) {
                Log.e("EventAdapter", "Failed to decode image: " + e.getMessage());
            }
        }
        else {
        }

    }

    @Override
    public int getItemCount() {
        return eventList.size();
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
                    Log.d("RecyclerClick", "Item clicked: " + position);

                    if (position != RecyclerView.NO_POSITION) {
                        listener.onItemClick(position);
                    }
                }
            });
        }

    }

}
