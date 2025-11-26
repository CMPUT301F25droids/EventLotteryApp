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
import com.example.eventlotteryapp.UserSession;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

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

        notificationsRef = db.collection("Notifications");
        String userId = UserSession.getCurrentUserId();
        DocumentReference userRef = db.document("Users/" + userId);
        notificationsArray.clear();
        notificationsRef.whereEqualTo("UserId", userRef)
                        .get()
                        .addOnCompleteListener((task) -> {
                            if (task.isSuccessful()) {
                                int totalDocs = task.getResult().size();

                                if (totalDocs == 0) {
                                    updateUI();
                                    return;
                                }

                                AtomicInteger loadedCount = new AtomicInteger(0);

                                for (DocumentSnapshot doc : task.getResult()) {
                                    Notification.fromDocument(doc, notification -> {
                                        notificationsArray.add(notification);

                                        if (loadedCount.incrementAndGet() == totalDocs) {
                                            Collections.sort(notificationsArray);
                                            updateUI();
                                        }
                                    });
                                }
                            } else {
                                Log.e("notif", "error getting documents", task.getException());
                                updateUI();
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
