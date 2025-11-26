package com.example.eventlotteryapp.EntrantView;

import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.eventlotteryapp.R;
import com.google.firebase.firestore.DocumentReference;

import java.util.List;

public class MyEventsListRecyclerViewAdapter extends EventsListRecyclerViewAdapter{

    public MyEventsListRecyclerViewAdapter(List<? extends EventItem> list, onEventClickListener listener) {
        super((List<EventItem>) list, listener);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate your event_list.xml layout
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.myevent_item, parent, false);
        return new ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull EventsListRecyclerViewAdapter.ViewHolder holder, int position) {

        MyEventItem event = (MyEventItem) originalList.get(position);
        holder.nameView.setText(event.getName());
        String lotteryPlaceHolder = "Lottery Status: ";
        if (event.getStatus() == MyEventItem.Status.PENDING) {
            ((ViewHolder) holder).statusView.setText(lotteryPlaceHolder + "Pending");
        } else if (event.getStatus() == MyEventItem.Status.SELECTED) {
            ((ViewHolder) holder).statusView.setText(lotteryPlaceHolder + "Selected");
        } else if (event.getStatus() == MyEventItem.Status.NOT_SELECTED){
            ((ViewHolder) holder).statusView.setText(lotteryPlaceHolder + "Not Selected");
        } else {
            ((ViewHolder) holder).statusView.setText(lotteryPlaceHolder + "Unknown");
        }
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

    public class ViewHolder extends EventsListRecyclerViewAdapter.ViewHolder {
        public final TextView statusView;

        public ViewHolder(View view) {
            super(view);
            statusView = view.findViewById(R.id.event_status);
        }
    }

}
