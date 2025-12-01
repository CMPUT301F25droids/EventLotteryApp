package com.example.eventlotteryapp.Notifications;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.eventlotteryapp.EntrantView.EventDetailsActivity;
import com.example.eventlotteryapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class NotificationsFragment extends Fragment {

    private ListView notificationsList;
    private ProgressBar progressBar;
    private Button btnDeleteSelected;
    private ArrayList<Notification> notificationsArray;
    private NotificationArrayAdapter notificationAdapter;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference notificationsRef;
    FirebaseAuth auth;

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
        btnDeleteSelected = view.findViewById(R.id.btnDeleteSelected);
        
        if (db == null) {
            db = FirebaseFirestore.getInstance();
        }

        // Set up delete selected button
        btnDeleteSelected.setOnClickListener(v -> deleteSelectedNotifications());

        loadNotifications();
    }

    private void loadNotifications() {
        progressBar.setVisibility(View.VISIBLE);
        notificationsList.setVisibility(View.GONE);
        auth = FirebaseAuth.getInstance();
        notificationsRef = db.collection("Notifications");
        notificationsArray.clear();

        String currentUserId = auth.getUid();
        if (currentUserId == null) {
            updateUI();
            return;
        }

        // Get current user's role to filter notifications by UserType
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(userDoc -> {
                    String userRole = userDoc.getString("role");
                    String userType = (userRole != null && userRole.equals("organizer")) ? "organizer" : "entrant";
                    
                    // Filter notifications by both UserId and UserType to separate logs
                    notificationsRef.whereEqualTo("UserId", currentUserId)
                            .whereEqualTo("UserType", userType)
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
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("notif", "error getting user document", e);
                    // Fallback: load without UserType filter for backward compatibility
                    notificationsRef.whereEqualTo("UserId", currentUserId)
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
                            });
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
        }
        
        // Set up selection change listener
        notificationAdapter.setOnSelectionChangeListener(selectedCount -> {
            if (selectedCount > 0) {
                btnDeleteSelected.setVisibility(View.VISIBLE);
                btnDeleteSelected.setText("Delete Selected (" + selectedCount + ")");
            } else {
                btnDeleteSelected.setVisibility(View.GONE);
            }
        });
        
        notificationAdapter.notifyDataSetChanged();

        // Add click listener to navigate to event details
        notificationsList.setOnItemClickListener((parent, view, position, id) -> {
            Notification notification = notificationsArray.get(position);
            String eventId = notification.getEventId();
            
            if (eventId != null && !eventId.isEmpty()) {
                Intent intent = new Intent(getContext(), EventDetailsActivity.class);
                intent.putExtra("eventId", eventId);
                startActivity(intent);
            }
        });
    }

    private void deleteSelectedNotifications() {
        Set<Notification> selected = notificationAdapter.getSelectedNotifications();
        
        if (selected.isEmpty()) {
            Toast.makeText(getContext(), "No notifications selected", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnDeleteSelected.setEnabled(false);
        
        AtomicInteger deletedCount = new AtomicInteger(0);
        int totalToDelete = selected.size();
        
        for (Notification notification : selected) {
            String documentId = notification.getDocumentId();
            if (documentId == null || documentId.isEmpty()) {
                if (deletedCount.incrementAndGet() == totalToDelete) {
                    finishDeletion();
                }
                continue;
            }

            // Delete from Firestore
            db.collection("Notifications")
                    .document(documentId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        // Remove from local array
                        notificationsArray.remove(notification);
                        
                        if (deletedCount.incrementAndGet() == totalToDelete) {
                            finishDeletion();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("NotificationsFragment", "Error deleting notification: " + documentId, e);
                        if (deletedCount.incrementAndGet() == totalToDelete) {
                            finishDeletion();
                        }
                    });
        }
    }

    private void finishDeletion() {
        progressBar.setVisibility(View.GONE);
        btnDeleteSelected.setEnabled(true);
        
        // Clear selection and update UI
        notificationAdapter.clearSelection();
        notificationAdapter.notifyDataSetChanged();
        
        // Reload notifications to ensure sync
        loadNotifications();
        
        Toast.makeText(getContext(), "Notifications deleted", Toast.LENGTH_SHORT).show();
    }
}
