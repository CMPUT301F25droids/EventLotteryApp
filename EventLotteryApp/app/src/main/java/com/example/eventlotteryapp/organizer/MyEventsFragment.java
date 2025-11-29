package com.example.eventlotteryapp.organizer;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.data.Event;
import com.example.eventlotteryapp.databinding.FragmentMyEventsBinding;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MyEventsFragment extends Fragment {

    private FragmentMyEventsBinding binding;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private EventAdapter eventAdapter;
    private final List<EventWithId> allEvents = new ArrayList<>();
    private String currentFilter = "active"; // active, upcoming, closed

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMyEventsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
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
        eventAdapter.setOnItemClickListener(eventId -> {
            Intent intent = new Intent(getContext(), OrganizerEventDetailsActivity.class);
            intent.putExtra("eventId", eventId);
            startActivity(intent);
        });
        binding.eventsRecyclerView.setLayoutManager(
                new LinearLayoutManager(getContext(),
                        LinearLayoutManager.HORIZONTAL,
                        false)
        );
        binding.eventsRecyclerView.setAdapter(eventAdapter);
    }

    private void setupFilterButtons() {
        MaterialButton activeBtn = binding.activeFilterButton;
        MaterialButton upcomingBtn = binding.upcomingFilterButton;
        MaterialButton closedBtn = binding.closedFilterButton;

        List<MaterialButton> buttons = List.of(activeBtn, upcomingBtn, closedBtn);

        // Tag each button so we know which filter it represents
        activeBtn.setTag("active");
        upcomingBtn.setTag("upcoming");
        closedBtn.setTag("closed");

        View.OnClickListener listener = v -> {
            MaterialButton selected = (MaterialButton) v;
            String filter = (String) selected.getTag();

            setFilter(filter);                        // Update current filter
            updateFilterButtonStyles(buttons, selected);  // Update colors
        };

        activeBtn.setOnClickListener(listener);
        upcomingBtn.setOnClickListener(listener);
        closedBtn.setOnClickListener(listener);

        // Default selection
        updateFilterButtonStyles(buttons, activeBtn);
    }

    private void setFilter(String filter) {
        currentFilter = filter;
        filterEvents();
    }

    private void updateFilterButtonStyles(List<MaterialButton> buttons,
                                          MaterialButton selected) {
        int selectedBgColor = getResources().getColor(R.color.filter_selected_bg);
        int selectedTextColor = getResources().getColor(R.color.filter_selected_text);
        int unselectedBgColor = getResources().getColor(R.color.filter_unselected_bg);
        int unselectedTextColor = getResources().getColor(R.color.filter_unselected_text);

        int animationDuration = 250; // milliseconds

        for (MaterialButton btn : buttons) {
            // Get current colors - handle transparent/initial state
            ColorStateList currentBgTint = btn.getBackgroundTintList();
            int currentBgColor = unselectedBgColor; // Default starting point
            if (currentBgTint != null) {
                int color = currentBgTint.getDefaultColor();
                // If not transparent, use the actual color; otherwise start from unselected
                if ((color & 0xFF000000) != 0) { // Check if not fully transparent
                    currentBgColor = color;
                }
            }

            int currentTextColor = btn.getCurrentTextColor();
            // If text color appears to be default (likely black), use unselected color as baseline
            if (currentTextColor == 0xFF000000 || currentTextColor == 0) {
                currentTextColor = unselectedTextColor;
            }

            // Determine target colors
            int targetBgColor = (btn == selected) ? selectedBgColor : unselectedBgColor;
            int targetTextColor = (btn == selected) ? selectedTextColor : unselectedTextColor;

            // Skip animation if already at target colors
            if (currentBgColor == targetBgColor && currentTextColor == targetTextColor) {
                continue;
            }

            // Animate background color
            ValueAnimator bgAnimator = ValueAnimator.ofObject(
                    new ArgbEvaluator(), currentBgColor, targetBgColor);
            bgAnimator.setDuration(animationDuration);
            bgAnimator.addUpdateListener(animator -> {
                int color = (int) animator.getAnimatedValue();
                btn.setBackgroundTintList(ColorStateList.valueOf(color));
            });
            bgAnimator.start();

            // Animate text color
            ValueAnimator textAnimator = ValueAnimator.ofObject(
                    new ArgbEvaluator(), currentTextColor, targetTextColor);
            textAnimator.setDuration(animationDuration);
            textAnimator.addUpdateListener(animator -> {
                int color = (int) animator.getAnimatedValue();
                btn.setTextColor(color);
            });
            textAnimator.start();
        }
    }

    public void loadEvents() {
        if (auth.getCurrentUser() == null) {
            Log.w("MyEventsFragment", "No current user, cannot load events");
            return;
        }

        String organizerId = auth.getCurrentUser().getUid();

        // Create DocumentReference for the current organizer
        com.google.firebase.firestore.DocumentReference organizerRef =
                firestore.collection("users").document(organizerId);

        Log.d("MyEventsFragment", "Loading events for organizerId: " + organizerId);

        // For debugging: log a few events
        firestore.collection("Events").limit(20).get()
                .addOnSuccessListener(allDocs -> {
                    Log.d("MyEventsFragment", "=== DEBUG: All events in collection ===");
                    Log.d("MyEventsFragment", "Total events found: " + allDocs.size());
                    for (QueryDocumentSnapshot doc : allDocs) {
                        String docTitle = doc.getString("title");
                        Log.d("MyEventsFragment",
                                "Event: " + doc.getId() + " | Title: " + docTitle);
                    }
                    Log.d("MyEventsFragment", "=== END DEBUG ===");
                });

        // Real query: events where Organizer field points to this user
        firestore.collection("Events")
                .whereEqualTo("Organizer", organizerRef)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allEvents.clear();
                    Log.d("MyEventsFragment", "Query result: Found "
                            + queryDocumentSnapshots.size()
                            + " events for organizer: " + organizerId);

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            Event event = doc.toObject(Event.class);
                            if (event == null) {
                                continue;
                            }
                            allEvents.add(new EventWithId(doc.getId(), event));
                            Log.d("MyEventsFragment",
                                    "Successfully loaded event: " + event.getTitle());
                        } catch (Exception e) {
                            Log.e("MyEventsFragment",
                                    "Error parsing event: " + doc.getId(), e);
                        }
                    }
                    updateFilterCounts();
                    filterEvents();
                })
                .addOnFailureListener(e ->
                        Log.e("MyEventsFragment", "Error loading events", e));
    }

    private void updateFilterCounts() {
        Date now = new Date();
        int activeCount = 0;
        int upcomingCount = 0;
        int closedCount = 0;

        for (EventWithId eventWithId : allEvents) {
            Event event = eventWithId.event;
            if (event.getEventStartDate() != null && event.getEventEndDate() != null) {
                if (now.before(event.getEventStartDate())) {
                    upcomingCount++;
                } else if (now.after(event.getEventEndDate())) {
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

            boolean matches;

            if (event.getEventStartDate() == null || event.getEventEndDate() == null) {
                // Events without dates show in active filter
                matches = currentFilter.equals("active");
            } else {
                switch (currentFilter) {
                    case "active":
                        matches = !now.before(event.getEventStartDate())
                                && !now.after(event.getEventEndDate());
                        break;
                    case "upcoming":
                        matches = now.before(event.getEventStartDate());
                        break;
                    case "closed":
                        matches = now.after(event.getEventEndDate());
                        break;
                    default:
                        matches = true;
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
    public static class EventWithId {
        public String id;
        public Event event;

        public EventWithId(String id, Event event) {
            this.id = id;
            this.event = event;
        }
    }
}