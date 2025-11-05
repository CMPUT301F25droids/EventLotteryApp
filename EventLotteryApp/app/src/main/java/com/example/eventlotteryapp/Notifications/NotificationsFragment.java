package com.example.eventlotteryapp.Notifications;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.eventlotteryapp.Helpers.DateTimeFormat;
import com.example.eventlotteryapp.R;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;

// Create a new Fragment every time it is to be displayed
public class NotificationsFragment extends Fragment {
    private ListView notifications;
    private FirebaseFirestore db;
    private CollectionReference notificationsRef;
    private NotificationArrayAdapter notificationAdapter;
    private ArrayList<Notification> notificationsArray;
    private ProgressBar progressBar;

    public NotificationsFragment(FirebaseFirestore db) {
        super(R.layout.fragment_notifications);
        this.db = db;
        notificationsArray = new ArrayList<>();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //Initialize views
        notifications = view.findViewById(R.id.lvNotifications);
        progressBar = view.findViewById(R.id.progressBar);

        loadNotifications();
    }

    private void loadNotifications() {
        progressBar.setVisibility(View.VISIBLE);
        notifications.setVisibility(View.GONE);

        notificationsRef = db.collection("notifications");
        notificationsRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("Firestore", error.toString());
            }
        });
        notificationsRef.get()
                        .addOnCompleteListener(task -> {
                            notificationsArray.clear(); // Sanity check

                            if (task.isSuccessful()) {
                                for (DocumentSnapshot doc : task.getResult()) {
                                    Notification notification = Notification.fromDocument(doc);
                                    notificationsArray.add(notification);
                            }
                            } else {
                                progressBar.setVisibility(View.VISIBLE);
                            }
                        });

        // Sort by timestamp descending (newest first)
        Collections.sort(notificationsArray);
        updateUI();
    }

    private void updateUI() {
        progressBar.setVisibility(View.GONE);
        notifications.setVisibility(View.VISIBLE);

        if (notificationAdapter == null) {
            notificationAdapter = new NotificationArrayAdapter(requireContext(), notificationsArray);
            notifications.setAdapter(notificationAdapter);
        } else {
            notificationAdapter.notifyDataSetChanged();
        }
    }

    public void addDummyNotifications() {
        notificationsArray.clear();
        Notification notification1 = new Notification(DateTimeFormat.toDate("2025-11-04 20:20:20"), Notification.NotificationType.LOTTERY, "YOU WON THe lottery");
        Notification notification2 = new Notification(DateTimeFormat.toDate("2025-11-04 21:20:20"), Notification.NotificationType.MESSAGE, "testing Dummy message");
        notificationsArray.add(notification1);
        notificationsArray.add(notification2);

        Collections.sort(notificationsArray);
        updateUI();
    }
}
