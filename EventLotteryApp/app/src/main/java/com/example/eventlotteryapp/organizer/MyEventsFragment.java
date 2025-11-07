package com.example.eventlotteryapp.organizer;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.data.Event;
import com.example.eventlotteryapp.databinding.FragmentMyEventsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyEventsFragment extends Fragment {

    private FragmentMyEventsBinding binding;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private EventAdapter eventAdapter;
    private List<EventWithId> allEvents = new ArrayList<>();
    private String currentFilter = "active"; // active, upcoming, closed

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMyEventsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        setupRecyclerView();
        setupFilterButtons();
        loadEvents();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload events when fragment becomes visible (e.g., returning from CreateEventActivity)
        loadEvents();
    }

    private void setupRecyclerView() {
        eventAdapter = new EventAdapter(new ArrayList<>());
        binding.eventsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.eventsRecyclerView.setAdapter(eventAdapter);
    }

    private void setupFilterButtons() {
        binding.activeFilterButton.setOnClickListener(v -> setFilter("active"));
        binding.upcomingFilterButton.setOnClickListener(v -> setFilter("upcoming"));
        binding.closedFilterButton.setOnClickListener(v -> setFilter("closed"));
    }

    private void setFilter(String filter) {
        currentFilter = filter;
        updateFilterButtons();
        filterEvents();
    }

    private void updateFilterButtons() {
        // All buttons have the same style (black 4% opacity background, black text)
        // They all look the same, but we keep the method for consistency
        binding.activeFilterButton.setBackgroundResource(R.drawable.filter_button_inactive);
        binding.activeFilterButton.setTextColor(getResources().getColor(R.color.black));
        binding.upcomingFilterButton.setBackgroundResource(R.drawable.filter_button_inactive);
        binding.upcomingFilterButton.setTextColor(getResources().getColor(R.color.black));
        binding.closedFilterButton.setBackgroundResource(R.drawable.filter_button_inactive);
        binding.closedFilterButton.setTextColor(getResources().getColor(R.color.black));
    }

    public void loadEvents() {
        if (auth.getCurrentUser() == null) {
            Log.w("MyEventsFragment", "No current user, cannot load events");
            return;
        }

        String organizerId = auth.getCurrentUser().getUid();
        Date now = new Date();
        
        // Create DocumentReference for the current organizer
        com.google.firebase.firestore.DocumentReference organizerRef = firestore.collection("Users").document(organizerId);

        Log.d("MyEventsFragment", "Loading events for organizerId: " + organizerId);
        Log.d("MyEventsFragment", "Current user UID: " + organizerId);
        
        // First, let's check all events to see what's in the database
        firestore.collection("Events").limit(20).get()
            .addOnSuccessListener(allDocs -> {
                Log.d("MyEventsFragment", "=== DEBUG: All events in collection ===");
                Log.d("MyEventsFragment", "Total events found: " + allDocs.size());
                for (QueryDocumentSnapshot doc : allDocs) {
                    // Try to get organizerId from Organizer DocumentReference or fallback to organizerId field
                    String docOrganizerId = null;
                    com.google.firebase.firestore.DocumentReference organizerDocRef = doc.getDocumentReference("Organizer");
                    if (organizerDocRef != null) {
                        docOrganizerId = organizerDocRef.getId();
                    } else {
                        docOrganizerId = doc.getString("organizerId");  // Fallback for old events
                    }
                    String docTitle = doc.getString("title");
                    Log.d("MyEventsFragment", "Event: " + doc.getId() + 
                        " | Title: " + docTitle + 
                        " | organizerId: " + docOrganizerId + 
                        " | Matches current user: " + (docOrganizerId != null && organizerId.equals(docOrganizerId)));
                }
                Log.d("MyEventsFragment", "=== END DEBUG ===");
            });
        
        // Query by Organizer DocumentReference
        firestore.collection("Events")
            .whereEqualTo("Organizer", organizerRef)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                allEvents.clear();
                Log.d("MyEventsFragment", "Query result: Found " + queryDocumentSnapshots.size() + " events for organizer: " + organizerId);
                
                if (queryDocumentSnapshots.isEmpty()) {
                    Log.w("MyEventsFragment", "No events found for organizerId: " + organizerId);
                }
                
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    try {
                        // Extract organizerId from Organizer DocumentReference or fallback to organizerId field
                        String extractedOrganizerId = null;
                        com.google.firebase.firestore.DocumentReference organizerDocRef = doc.getDocumentReference("Organizer");
                        if (organizerDocRef != null) {
                            extractedOrganizerId = organizerDocRef.getId();
                        } else {
                            extractedOrganizerId = doc.getString("organizerId");  // Fallback for old events
                        }
                        
                        // Firestore doesn't support Java records, so we manually construct Event from document fields
                        Event event = new Event(
                            doc.getString("title") != null ? doc.getString("title") : doc.getString("Name"),  // Support both old and new field names
                            doc.getString("description"),
                            doc.getString("location"),
                            doc.getDouble("price") != null ? doc.getDouble("price") : 0.0,
                            doc.getDate("eventStartDate"),
                            doc.getDate("eventEndDate"),
                            doc.getDate("registrationOpenDate"),
                            doc.getDate("registrationCloseDate"),
                            doc.getLong("maxParticipants") != null ? doc.getLong("maxParticipants").intValue() : 0,
                            extractedOrganizerId != null ? extractedOrganizerId : "",
                            doc.getDate("createdAt")
                        );
                        allEvents.add(new EventWithId(doc.getId(), event));
                        Log.d("MyEventsFragment", "Successfully loaded event: " + event.title());
                    } catch (Exception e) {
                        Log.e("MyEventsFragment", "Error parsing event: " + doc.getId(), e);
                        // Skip this event if we can't parse it
                    }
                }
                updateFilterCounts();
                filterEvents();
            })
            .addOnFailureListener(e -> {
                Log.e("MyEventsFragment", "Error loading events", e);
            });
    }

    private void updateFilterCounts() {
        Date now = new Date();
        int activeCount = 0;
        int upcomingCount = 0;
        int closedCount = 0;

        for (EventWithId eventWithId : allEvents) {
            Event event = eventWithId.event;
            if (event.eventStartDate() != null && event.eventEndDate() != null) {
                if (now.before(event.eventStartDate())) {
                    upcomingCount++;
                } else if (now.after(event.eventEndDate())) {
                    closedCount++;
                } else {
                    activeCount++;
                }
            } else {
                // Events without dates are considered active
                activeCount++;
            }
        }

        binding.activeFilterButton.setText("Active: " + activeCount);
        binding.upcomingFilterButton.setText("Upcoming: " + upcomingCount);
        binding.closedFilterButton.setText("Closed: " + closedCount);
    }

    private void filterEvents() {
        Date now = new Date();
        List<EventWithId> filtered = new ArrayList<>();

        for (EventWithId eventWithId : allEvents) {
            Event event = eventWithId.event;
            
            boolean matches = false;
            
            if (event.eventStartDate() == null || event.eventEndDate() == null) {
                // Events without dates show in active filter
                matches = currentFilter.equals("active");
            } else {
                switch (currentFilter) {
                    case "active":
                        matches = !now.before(event.eventStartDate()) && !now.after(event.eventEndDate());
                        break;
                    case "upcoming":
                        matches = now.before(event.eventStartDate());
                        break;
                    case "closed":
                        matches = now.after(event.eventEndDate());
                        break;
                }
            }

            if (matches) {
                filtered.add(eventWithId);
            }
        }

        eventAdapter.updateEvents(filtered);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // Helper class to store event with its ID
    static class EventWithId {
        String id;
        Event event;

        EventWithId(String id, Event event) {
            this.id = id;
            this.event = event;
        }
    }
}

