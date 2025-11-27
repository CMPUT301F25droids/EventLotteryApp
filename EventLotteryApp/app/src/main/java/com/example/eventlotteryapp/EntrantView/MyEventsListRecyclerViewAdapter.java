package com.example.eventlotteryapp.EntrantView;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.eventlotteryapp.R;
import com.google.firebase.firestore.DocumentReference;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
        ViewHolder myViewHolder = (ViewHolder) holder;
        
        // Set event name
        holder.nameView.setText(event.getName());
        
        // Set status with emoji matching Figma design
        if (event.getStatus() == MyEventItem.Status.PENDING) {
            myViewHolder.statusView.setText("🟡 Pending");
        } else if (event.getStatus() == MyEventItem.Status.SELECTED) {
            myViewHolder.statusView.setText("🟢 Accepted");
        } else if (event.getStatus() == MyEventItem.Status.NOT_SELECTED){
            myViewHolder.statusView.setText("🔴 Not Selected");
        } else {
            myViewHolder.statusView.setText("Unknown");
        }
        
        // Format and set date
        if (event.getEventStartDate() != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
            myViewHolder.dateView.setText(dateFormat.format(event.getEventStartDate()));
        } else {
            myViewHolder.dateView.setText("Date TBD");
        }
        
        // Set location (placeholder for now - EventItem doesn't have location field)
        myViewHolder.locationView.setText("Location TBD");
        
        // Set cost
        holder.costView.setText(event.getCost());

        // Load and set image with rounded corners
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
    }

    public class ViewHolder extends EventsListRecyclerViewAdapter.ViewHolder {
        public final TextView statusView;
        public final TextView dateView;
        public final TextView locationView;

        public ViewHolder(View view) {
            super(view);
            statusView = view.findViewById(R.id.event_status);
            dateView = view.findViewById(R.id.event_date);
            locationView = view.findViewById(R.id.event_location);
        }
    }

}
