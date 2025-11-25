package com.example.eventlotteryapp.Notifications;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.eventlotteryapp.R;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;

public class NotificationsFragment extends Fragment {

    private ListView notificationsList;
    private ProgressBar progressBar;
    private ArrayList<Notification> notificationsArray;
    private NotificationArrayAdapter notificationAdapter;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference notificationsRef;

    public NotificationsFragment() {
        super(R.layout.fragment_notifications);
        notificationsArray = new ArrayList<>();
    }
    public static NotificationsFragment newInstance(FirebaseFirestore firestore) {
        NotificationsFragment fragment = new NotificationsFragment();
        fragment.db = firestore;
        return fragment;
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        notificationsList = view.findViewById(R.id.lvNotifications);
        progressBar = view.findViewById(R.id.progressBar);
        if (db == null) {
            db = FirebaseFirestore.getInstance();
        }

        loadNotifications();
    }

    private void loadNotifications() {
        progressBar.setVisibility(View.VISIBLE);
        notificationsList.setVisibility(View.GONE);

        notificationsRef = db.collection("notifications");

        notificationsRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("NotificationsFragment", error.toString());
                return;
            }

            notificationsArray.clear();
            if (value != null) {
                for (DocumentSnapshot doc : value.getDocuments()) {
                    notificationsArray.add(Notification.fromDocument(doc));
                }
            }

            Collections.sort(notificationsArray);
            updateUI();
        });
    }

    private void updateUI() {
        // Check if fragment is still attached before accessing context
        if (!isAdded() || getContext() == null) {
            return;
        }
        
        progressBar.setVisibility(View.GONE);
        notificationsList.setVisibility(View.VISIBLE);

        if (notificationAdapter == null) {
            notificationAdapter = new NotificationArrayAdapter(requireContext(), notificationsArray);
            notificationsList.setAdapter(notificationAdapter);
        } else {
            notificationAdapter.notifyDataSetChanged();
        }
    }
}
