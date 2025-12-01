package com.example.eventlotteryapp.Notifications;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.eventlotteryapp.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class NotificationArrayAdapter extends ArrayAdapter<Notification> {

    private OnSelectionChangeListener selectionChangeListener;
    private Set<Notification> selectedNotifications = new HashSet<>();

    public interface OnSelectionChangeListener {
        void onSelectionChanged(int selectedCount);
    }

    public NotificationArrayAdapter(Context context, ArrayList<Notification> notifications) {
        super(context, 0, notifications);
    }

    public void setOnSelectionChangeListener(OnSelectionChangeListener listener) {
        this.selectionChangeListener = listener;
    }

    public Set<Notification> getSelectedNotifications() {
        return new HashSet<>(selectedNotifications);
    }

    public void clearSelection() {
        selectedNotifications.clear();
        notifyDataSetChanged();
        if (selectionChangeListener != null) {
            selectionChangeListener.onSelectionChanged(0);
        }
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view;
        if (convertView == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.notification_list_item, parent, false);
        } else {
            view = convertView;
        }

        Notification notification = getItem(position);
        TextView timeStamp = view.findViewById(R.id.tvTimestamp);
        TextView message = view.findViewById(R.id.tvNotificationMessage);
        TextView title = view.findViewById(R.id.tvNotificationTitle);
        ImageView icon = view.findViewById(R.id.ivNotificationIcon);
        CheckBox checkBox = view.findViewById(R.id.cbSelectNotification);

        if (notification.getType().contains("lottery")) {
            icon.setImageResource(R.drawable.ticket);
        } else {
            icon.setImageResource(R.drawable.mail_box);
        }

        timeStamp.setText(notification.getRelevantTime());
        message.setText(notification.getMessage());
        title.setText(notification.getEventName());

        // Set checkbox state
        checkBox.setChecked(selectedNotifications.contains(notification));
        checkBox.setFocusable(false);
        checkBox.setClickable(true);

        // Set up checkbox click listener
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedNotifications.add(notification);
            } else {
                selectedNotifications.remove(notification);
            }
            
            if (selectionChangeListener != null) {
                selectionChangeListener.onSelectionChanged(selectedNotifications.size());
            }
        });

        // Prevent item click when clicking checkbox
        checkBox.setOnClickListener(v -> {
            // This prevents the parent item click from firing
        });

        return view;
    }
}
