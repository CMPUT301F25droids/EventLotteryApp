package com.example.eventlotteryapp.Notifications;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.eventlotteryapp.Helpers.RelativeTime;
import com.example.eventlotteryapp.R;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;

public class NotificationArrayAdapter extends ArrayAdapter<Notification> {

    public NotificationArrayAdapter(Context context, ArrayList<Notification> notifications) {
        super(context, 0, notifications);
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

        timeStamp.setText(notification.getRelevantTime());
        message.setText(notification.getMessage());
        title.setText(notification.getType());

        return view;
    }
}
