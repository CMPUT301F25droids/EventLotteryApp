package com.example.eventlotteryapp.Admin;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventlotteryapp.Authorization.AuthActivity;
import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.data.Event;
import com.example.eventlotteryapp.organizer.EventAdapter;
import com.example.eventlotteryapp.organizer.MyEventsFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity that allows the administrator to view all events
 * and delete them if necessary.
 */
public class AdminBrowseEventsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;

    private EventAdapter eventAdapter;
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final CollectionReference eventsRef = firestore.collection("Events");

    /**
     * Sets up UI, recycler view, and loads events from Firestore.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_browse_events);

        // Back button
        android.widget.ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recyclerAdminEvents);
        progressBar = findViewById(R.id.adminEventsProgress);
        emptyView = findViewById(R.id.adminEventsEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        eventAdapter = new EventAdapter(new ArrayList<>());
        recyclerView.setAdapter(eventAdapter);

        eventAdapter.setOnItemClickListener(this::showDeleteDialog);

        Button logoutBtn = findViewById(R.id.adminLogoutButton);
        logoutBtn.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, AuthActivity.class));
            finish();
        });

        loadAllEvents();
    }

    /**
     * Fetches all events ordered by creation date and displays them.
     */
    private void loadAllEvents() {
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);

        eventsRef.orderBy("createdAt")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    List<MyEventsFragment.EventWithId> events = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Event event = doc.toObject(Event.class);
                        if (event != null) {
                            events.add(new MyEventsFragment.EventWithId(doc.getId(), event));
                        }
                    }

                    progressBar.setVisibility(View.GONE);

                    if (events.isEmpty()) {
                        emptyView.setText("No events found");
                        emptyView.setVisibility(View.VISIBLE);
                    } else {
                        emptyView.setVisibility(View.GONE);
                        eventAdapter.updateEvents(events);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    emptyView.setText("Error loading events");
                    emptyView.setVisibility(View.VISIBLE);
                });
    }

    /**
     * Shows a confirmation dialog before deleting an event.
     */
    private void showDeleteDialog(String eventId) {
        new AlertDialog.Builder(this)
                .setTitle("Remove event")
                .setMessage("Are you sure you want to permanently delete this event?")
                .setPositiveButton("Delete", (dialog, which) -> deleteEvent(eventId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Deletes an event document from Firestore and refreshes the list.
     */
    private void deleteEvent(String eventId) {
        progressBar.setVisibility(View.VISIBLE);

        eventsRef.document(eventId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Event removed", Toast.LENGTH_SHORT).show();
                    loadAllEvents();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to remove event: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}
