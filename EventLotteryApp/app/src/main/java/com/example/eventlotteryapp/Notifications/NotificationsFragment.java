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
        
        if (btnDeleteSelected == null) {
            android.util.Log.e("NotificationsFragment", "Delete button not found in layout!");
        } else {
            android.util.Log.d("NotificationsFragment", "Delete button found and initialized");
        }
        
        if (db == null) {
            db = FirebaseFirestore.getInstance();
        }

        // Set up delete selected button
        if (btnDeleteSelected != null) {
            btnDeleteSelected.setOnClickListener(v -> deleteSelectedNotifications());
        }

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
                    // Check if user document exists
                    if (!userDoc.exists()) {
                        Log.e("notif", "User document does not exist");
                        updateUI();
                        return;
                    }
                    
                    String userRole = userDoc.getString("role");
                    String userType = (userRole != null && userRole.equals("organizer")) ? "organizer" : "entrant";
                    
                    // Query notifications by UserId and UserType to properly separate organizer and entrant notifications
                    final ArrayList<DocumentSnapshot> filteredDocs = new ArrayList<>();
                    final AtomicInteger queryCount = new AtomicInteger(0);
                    final int totalQueries = 2;
                    
                    // Query 1: Get notifications with matching UserType
                    notificationsRef.whereEqualTo("UserId", currentUserId)
                            .whereEqualTo("UserType", userType)
                            .get()
                            .addOnCompleteListener((task) -> {
                                if (task.isSuccessful()) {
                                    for (DocumentSnapshot doc : task.getResult()) {
                                        filteredDocs.add(doc);
                                    }
                                } else {
                                    Log.e("notif", "error getting documents with UserType", task.getException());
                                }
                                
                                if (queryCount.incrementAndGet() == totalQueries) {
                                    processFilteredDocs(filteredDocs);
                                }
                            });
                    
                    // Query 2: Get all notifications for backward compatibility (those without UserType)
                    notificationsRef.whereEqualTo("UserId", currentUserId)
                            .get()
                            .addOnCompleteListener((backwardTask) -> {
                                if (backwardTask.isSuccessful()) {
                                    for (DocumentSnapshot doc : backwardTask.getResult()) {
                                        String docUserType = doc.getString("UserType");
                                        // Only add if UserType is null (old notifications) and not already added
                                        if (docUserType == null) {
                                            boolean exists = false;
                                            for (DocumentSnapshot existing : filteredDocs) {
                                                if (existing.getId().equals(doc.getId())) {
                                                    exists = true;
                                                    break;
                                                }
                                            }
                                            if (!exists) {
                                                filteredDocs.add(doc);
                                            }
                                        }
                                    }
                                } else {
                                    Log.e("notif", "error getting all documents", backwardTask.getException());
                                }
                                
                                if (queryCount.incrementAndGet() == totalQueries) {
                                    processFilteredDocs(filteredDocs);
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
                                    ArrayList<DocumentSnapshot> allDocs = new ArrayList<>();
                                    for (DocumentSnapshot doc : task.getResult()) {
                                        allDocs.add(doc);
                                    }
                                    processFilteredDocs(allDocs);
                                } else {
                                    Log.e("notif", "error getting documents", task.getException());
                                    updateUI();
                                }
                            });
                });
    }

    private void processFilteredDocs(ArrayList<DocumentSnapshot> filteredDocs) {
        int totalDocs = filteredDocs.size();

        if (totalDocs == 0) {
            updateUI();
            return;
        }

        AtomicInteger loadedCount = new AtomicInteger(0);

        for (DocumentSnapshot doc : filteredDocs) {
            Notification.fromDocument(doc, notification -> {
                notificationsArray.add(notification);

                if (loadedCount.incrementAndGet() == totalDocs) {
                    Collections.sort(notificationsArray);
                    updateUI();
                }
            });
        }
    }

    private void updateUI() {
        // Check if fragment is still attached before accessing context
        if (!isAdded() || getContext() == null) {
            return;
        }
        
        progressBar.setVisibility(View.GONE);
        notificationsList.setVisibility(View.VISIBLE);

        // Create or update adapter
        if (notificationAdapter == null) {
            notificationAdapter = new NotificationArrayAdapter(requireContext(), notificationsArray);
            notificationsList.setAdapter(notificationAdapter);
        } else {
            // Recreate adapter to ensure data is in sync
            // The array will be repopulated by loadNotifications, so we just need to notify
            // But wait, we're in updateUI which is called after data is loaded, so we need to update
            // Actually, since we're passing the same array reference, we should just notify
            // But ArrayAdapter doesn't automatically sync, so we need to recreate or manually update
            // For now, let's recreate to ensure data is in sync
            notificationAdapter = new NotificationArrayAdapter(requireContext(), notificationsArray);
            notificationsList.setAdapter(notificationAdapter);
        }
        
        // Always set up selection change listener (in case adapter was recreated)
        if (notificationAdapter != null) {
            notificationAdapter.setOnSelectionChangeListener(selectedCount -> {
                android.util.Log.d("NotificationsFragment", "Selection changed: " + selectedCount);
                if (btnDeleteSelected == null) {
                    android.util.Log.e("NotificationsFragment", "Delete button is null in listener!");
                    return;
                }
                // Ensure we're on the UI thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (selectedCount > 0) {
                            android.util.Log.d("NotificationsFragment", "Showing delete button, count: " + selectedCount);
                            btnDeleteSelected.setVisibility(View.VISIBLE);
                            btnDeleteSelected.setText("Delete Selected (" + selectedCount + ")");
                            // Force a layout update
                            btnDeleteSelected.requestLayout();
                        } else {
                            android.util.Log.d("NotificationsFragment", "Hiding delete button");
                            btnDeleteSelected.setVisibility(View.GONE);
                        }
                    });
                }
            });
            android.util.Log.d("NotificationsFragment", "Selection listener set up on adapter");
        } else {
            android.util.Log.e("NotificationsFragment", "Adapter is null when setting up listener!");
        }
        
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
        if (notificationAdapter == null) {
            Toast.makeText(getContext(), "Notifications not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Set<Notification> selected = notificationAdapter.getSelectedNotifications();
        
        if (selected.isEmpty()) {
            Toast.makeText(getContext(), "No notifications selected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if fragment is still attached
        if (!isAdded() || getContext() == null) {
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnDeleteSelected.setEnabled(false);
        
        AtomicInteger completedCount = new AtomicInteger(0);
        AtomicInteger skippedCount = new AtomicInteger(0);
        int totalToDelete = selected.size();
        
        for (Notification notification : selected) {
            String documentId = notification.getDocumentId();
            if (documentId == null || documentId.isEmpty()) {
                Log.w("NotificationsFragment", "Notification has no documentId, skipping deletion. Event: " + notification.getEventName());
                skippedCount.incrementAndGet();
                if (completedCount.incrementAndGet() == totalToDelete) {
                    finishDeletionWithMessage(skippedCount.get());
                }
                continue;
            }

            // Delete from Firestore
            db.collection("Notifications")
                    .document(documentId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d("NotificationsFragment", "Successfully deleted notification: " + documentId);
                        // Remove from local array
                        notificationsArray.remove(notification);
                        
                        if (completedCount.incrementAndGet() == totalToDelete) {
                            finishDeletionWithMessage(skippedCount.get());
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("NotificationsFragment", "Error deleting notification: " + documentId, e);
                        if (completedCount.incrementAndGet() == totalToDelete) {
                            finishDeletionWithMessage(skippedCount.get());
                        }
                    });
        }
    }

    private void finishDeletion() {
        finishDeletionWithMessage(0);
    }

    private void finishDeletionWithMessage(int skippedCount) {
        // Check if fragment is still attached before accessing context
        if (!isAdded() || getContext() == null) {
            return;
        }
        
        progressBar.setVisibility(View.GONE);
        btnDeleteSelected.setEnabled(true);
        
        // Clear selection and update UI
        if (notificationAdapter != null) {
            notificationAdapter.clearSelection();
            notificationAdapter.notifyDataSetChanged();
        }
        
        // Reload notifications to ensure sync
        loadNotifications();
        
        String message = "Notifications deleted";
        if (skippedCount > 0) {
            message += " (" + skippedCount + " skipped - missing document ID)";
        }
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
}
