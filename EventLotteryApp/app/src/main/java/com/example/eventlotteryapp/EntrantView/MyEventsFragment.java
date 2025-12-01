package com.example.eventlotteryapp.EntrantView;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventlotteryapp.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment displaying the user's events with filtering capabilities.
 * Shows events where the user is in waiting list, selected, or cancelled lists.
 * Provides filter buttons to view events by status (Selected, Pending, Not Selected).
 */
public class MyEventsFragment extends Fragment {

    private static final String ARG_COLUMN_COUNT = "column-count";
    private static final String TAG = "MyEventsFragment";
    private int mColumnCount = 1;

    /** Current filter status (null means show all events) */
    private MyEventItem.Status currentFilter = null;

    /** Complete list of all loaded events */
    private List<MyEventItem> fullEventList = new ArrayList<>();

    /** Filtered list of events displayed in RecyclerView */
    private List<MyEventItem> eventList = new ArrayList<>();

    // UI components
    private RecyclerView recyclerView;
    private MyEventsListRecyclerViewAdapter adapter;
    private View emptyStateContainer;
    private ProgressBar loading;

    // Firebase
    private FirebaseFirestore db;
    private String userId;
    private DocumentReference user_ref;

    // Tracking variables for async loading
    private java.util.Set<String> loadedEventIds = new java.util.HashSet<>();
    private int[] completedQueries = new int[]{0};
    private int[] pendingStatusLoads = new int[]{0};

    /**
     * Required empty public constructor for the fragment manager.
     */
    public MyEventsFragment() {
    }

    /**
     * Factory method to create a new instance of this fragment.
     *
     * @param columnCount Number of columns for grid layout
     * @return A new instance of MyEventsFragment
     */
    public static MyEventsFragment newInstance(int columnCount) {
        MyEventsFragment fragment = new MyEventsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Called to do initial creation of fragment.
     *
     * @param savedInstanceState If the fragment is being re-created from a previous saved state
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    /**
     * Called when the fragment is visible to the user and actively running.
     * Refreshes the event list to ensure data is current.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (userId != null && db != null && adapter != null && user_ref != null) {
            Log.d(TAG, "onResume() - Refreshing event list...");
            refreshEventList();
        }
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     *
     * @param inflater The LayoutInflater object to inflate views
     * @param container The parent view that the fragment UI should be attached to
     * @param savedInstanceState Previous saved state
     * @return The View for the fragment's UI
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_entrants_myevents_list, container, false);

        // Initialize UI components
        recyclerView = view.findViewById(R.id.events_list);
        emptyStateContainer = view.findViewById(R.id.empty_state_container);
        loading = view.findViewById(R.id.loading_indicator);
        Button browseEventsButton = view.findViewById(R.id.browse_events_button);
        android.widget.ImageButton scanQrButton = view.findViewById(R.id.scan_qr_button);

        // Setup RecyclerView
        setupRecyclerView();

        // Setup filter buttons
        setupFilterButtons(view);

        // Browse events button click listener
        browseEventsButton.setOnClickListener(v -> {
            if (getActivity() instanceof EntrantHomePageActivity) {
                ((EntrantHomePageActivity) getActivity()).viewPager2.setCurrentItem(0);
            }
        });

        // Scan QR button click listener
        scanQrButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), ScanQrCodeActivity.class);
            startActivity(intent);
        });

        // Check if user is authenticated
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            loading.setVisibility(View.GONE);
            updateEmptyState();
            return view;
        }

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        userId = auth.getCurrentUser().getUid();
        user_ref = db.collection("users").document(userId);

        Log.d(TAG, "User ID: " + userId);

        // Load events
        loadEvents();

        return view;
    }

    /**
     * Sets up the RecyclerView with adapter and layout manager.
     */
    private void setupRecyclerView() {
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new MyEventsListRecyclerViewAdapter(eventList, position -> {
            MyEventItem clickedEvent = eventList.get(position);
            Intent intent = new Intent(getContext(), EventDetailsActivity.class);
            intent.putExtra("eventId", clickedEvent.getId());
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);

        Log.d(TAG, "RecyclerView setup complete. Initial list size: " + eventList.size());
    }

    /**
     * Sets up the filter buttons (Selected, Pending, Not Selected) with click listeners.
     *
     * @param view The root view containing the filter buttons
     */
    private void setupFilterButtons(View view) {
        MaterialButton selectFilter = view.findViewById(R.id.selected_filter);
        MaterialButton pendingFilter = view.findViewById(R.id.pending_filter);
        MaterialButton notSelectedFilter = view.findViewById(R.id.not_selected_filter);

        selectFilter.setOnClickListener(v -> {
            if (currentFilter == MyEventItem.Status.SELECTED) {
                currentFilter = null;
                updateFilterButtonStyle(selectFilter, false);
            } else {
                currentFilter = MyEventItem.Status.SELECTED;
                updateFilterButtonStyle(selectFilter, true);
                updateFilterButtonStyle(pendingFilter, false);
                updateFilterButtonStyle(notSelectedFilter, false);
            }
            applyFilter();
        });

        pendingFilter.setOnClickListener(v -> {
            if (currentFilter == MyEventItem.Status.PENDING) {
                currentFilter = null;
                updateFilterButtonStyle(pendingFilter, false);
            } else {
                currentFilter = MyEventItem.Status.PENDING;
                updateFilterButtonStyle(selectFilter, false);
                updateFilterButtonStyle(pendingFilter, true);
                updateFilterButtonStyle(notSelectedFilter, false);
            }
            applyFilter();
        });

        notSelectedFilter.setOnClickListener(v -> {
            if (currentFilter == MyEventItem.Status.NOT_SELECTED) {
                currentFilter = null;
                updateFilterButtonStyle(notSelectedFilter, false);
            } else {
                currentFilter = MyEventItem.Status.NOT_SELECTED;
                updateFilterButtonStyle(selectFilter, false);
                updateFilterButtonStyle(pendingFilter, false);
                updateFilterButtonStyle(notSelectedFilter, true);
            }
            applyFilter();
        });
    }

    /**
     * Updates the visual style of a filter button based on selection state.
     *
     * @param button The MaterialButton to style
     * @param isSelected True if the button should be styled as selected
     */
    private void updateFilterButtonStyle(MaterialButton button, boolean isSelected) {
        if (isSelected) {
            button.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.selected_tab_color)));
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            button.setStrokeWidth(0);
        } else {
            button.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
            button.setStrokeWidth((int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()));
        }
    }

    /**
     * Applies the current filter to the event list.
     * If currentFilter is null, shows all events. Otherwise, filters by status.
     */
    private void applyFilter() {
        Log.d(TAG, "=== APPLY FILTER ===");
        Log.d(TAG, "Current filter: " + currentFilter);
        Log.d(TAG, "Full list size: " + fullEventList.size());

        eventList.clear();

        if (currentFilter == null) {
            // Show all events
            eventList.addAll(fullEventList);
            Log.d(TAG, "Showing all events: " + eventList.size());
        } else {
            // Filter by status
            for (MyEventItem event : fullEventList) {
                if (event.getStatus() == currentFilter) {
                    eventList.add(event);
                }
            }
            Log.d(TAG, "Filtered to " + eventList.size() + " events with status: " + currentFilter);
        }

        if (adapter != null) {
            Log.d(TAG, "Calling adapter.updateList() with " + eventList.size() + " events");
            adapter.updateList(eventList);
        }
        updateEmptyState();

        Log.d(TAG, "RecyclerView visibility: " + (recyclerView.getVisibility() == View.VISIBLE ? "VISIBLE" : "GONE"));
        Log.d(TAG, "Empty state visibility: " + (emptyStateContainer.getVisibility() == View.VISIBLE ? "VISIBLE" : "GONE"));
    }

    /**
     * Loads all events for the current user from Firestore.
     * Queries multiple collections: JoinedEvents, waitingListEntrantIds, selectedEntrantIds, cancelledEntrantIds.
     */
    private void loadEvents() {
        loading.setVisibility(View.VISIBLE);

        Log.d(TAG, "=== LOAD EVENTS CALLED ===");
        Log.d(TAG, "User ID: " + userId);

        // Reset tracking variables
        loadedEventIds.clear();
        completedQueries[0] = 0;
        pendingStatusLoads[0] = 0;
        fullEventList.clear();
        eventList.clear();

        Log.d(TAG, "Lists cleared. Starting queries...");

        final int totalQueries = 4; // JoinedEvents + 3 waitlist queries

        // Helper to check if all queries and status loads are done
        java.util.function.Consumer<Void> checkAllDone = (v) -> {
            Log.d(TAG, "Checking completion - Queries: " + completedQueries[0] + "/" + totalQueries +
                    ", Pending status: " + pendingStatusLoads[0]);
            if (completedQueries[0] >= totalQueries && pendingStatusLoads[0] == 0) {
                Log.d(TAG, "All done! Total events loaded: " + fullEventList.size());
                applyFilter(); // Apply current filter to display events
                loading.setVisibility(View.GONE);
            }
        };

        // Helper to sync event to JoinedEvents if not already there
        java.util.function.Consumer<String> syncEventToJoinedEvents = (eventId) -> {
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(userDoc -> {
                        if (userDoc.exists()) {
                            List<DocumentReference> joinedEvents = (List<DocumentReference>) userDoc.get("JoinedEvents");
                            DocumentReference eventRef = db.collection("Events").document(eventId);

                            boolean needsSync = true;
                            if (joinedEvents != null) {
                                for (DocumentReference ref : joinedEvents) {
                                    if (ref != null && ref.getId().equals(eventId)) {
                                        needsSync = false;
                                        break;
                                    }
                                }
                            }

                            if (needsSync) {
                                Log.d(TAG, "Syncing event " + eventId + " to JoinedEvents");
                                user_ref.update("JoinedEvents", com.google.firebase.firestore.FieldValue.arrayUnion(eventRef));
                            }
                        }
                    });
        };

        // Helper to load an event (avoid duplicates)
        java.util.function.Consumer<DocumentSnapshot> loadEvent = (eventDoc) -> {
            if (!eventDoc.exists()) {
                return;
            }

            String eventId = eventDoc.getId();
            if (loadedEventIds.contains(eventId)) {
                return; // Already loading or loaded
            }

            MyEventItem event = eventDoc.toObject(MyEventItem.class);
            if (event == null) {
                return;
            }

            event.setId(eventId);
            loadedEventIds.add(eventId);
            pendingStatusLoads[0]++;

            Log.d(TAG, "Loading event: " + eventId + " (" + (event.getName() != null ? event.getName() : "unknown") + ")");

            syncEventToJoinedEvents.accept(eventId);

            getEventStatus(event, userId, db, status -> {
                event.setStatus(status);
                fullEventList.add(event); // Add to full list instead of eventList
                Log.d(TAG, "Event loaded with status: " + status + " - Total events: " + fullEventList.size());
                pendingStatusLoads[0]--;
                checkAllDone.accept(null);
            });
        };

        // Setup snapshot listener for user document
        db.collection("users").document(userId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    Log.d(TAG, "=== SNAPSHOT LISTENER FIRED ===");
                    if (e != null) {
                        Log.e(TAG, "Listen failed.", e);
                        loading.setVisibility(View.GONE);
                        updateEmptyState();
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        Log.d(TAG, "User document exists, processing JoinedEvents...");

                        List<DocumentReference> joinedEvents = (List<DocumentReference>) documentSnapshot.get("JoinedEvents");

                        Log.d(TAG, "JoinedEvents count: " + (joinedEvents != null ? joinedEvents.size() : 0));

                        if (joinedEvents != null && !joinedEvents.isEmpty()) {
                            final int[] loadedFromJoined = {0};
                            final int totalJoined = joinedEvents.size();

                            for (DocumentReference eventRef : joinedEvents) {
                                if (eventRef == null) {
                                    Log.d(TAG, "Null eventRef in JoinedEvents");
                                    loadedFromJoined[0]++;
                                    if (loadedFromJoined[0] >= totalJoined) {
                                        completedQueries[0]++;
                                        Log.d(TAG, "JoinedEvents query complete (with nulls)");
                                        checkAllDone.accept(null);
                                    }
                                    continue;
                                }

                                Log.d(TAG, "Fetching event: " + eventRef.getId());
                                eventRef.get()
                                        .addOnSuccessListener(eventDoc -> {
                                            loadEvent.accept(eventDoc);
                                            loadedFromJoined[0]++;
                                            if (loadedFromJoined[0] >= totalJoined) {
                                                completedQueries[0]++;
                                                Log.d(TAG, "JoinedEvents query complete");
                                                checkAllDone.accept(null);
                                            }
                                        })
                                        .addOnFailureListener(e2 -> {
                                            Log.e(TAG, "Error loading event from JoinedEvents", e2);
                                            loadedFromJoined[0]++;
                                            if (loadedFromJoined[0] >= totalJoined) {
                                                completedQueries[0]++;
                                                checkAllDone.accept(null);
                                            }
                                        });
                            }
                        } else {
                            Log.d(TAG, "No JoinedEvents, marking query complete");
                            completedQueries[0]++;
                            checkAllDone.accept(null);
                        }
                    } else {
                        Log.d(TAG, "User document does not exist");
                        completedQueries[0]++;
                        checkAllDone.accept(null);
                    }
                });

        // Query for events where user is in waitingListEntrantIds
        queryEventsWhere("waitingListEntrantIds", loadEvent, checkAllDone);

        // Query for events where user is in selectedEntrantIds
        queryEventsWhere("selectedEntrantIds", loadEvent, checkAllDone);

        // Query for events where user is in cancelledEntrantIds
        queryEventsWhere("cancelledEntrantIds", loadEvent, checkAllDone);
    }

    /**
     * Queries Firestore for events where the user is in a specific array field.
     *
     * @param arrayField The name of the array field to query
     * @param loadEvent Consumer to load each event document
     * @param checkAllDone Consumer to check if all queries are complete
     */
    private void queryEventsWhere(String arrayField,
                                  java.util.function.Consumer<DocumentSnapshot> loadEvent,
                                  java.util.function.Consumer<Void> checkAllDone) {
        db.collection("Events")
                .whereArrayContains(arrayField, userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d(TAG, arrayField + " query found: " + querySnapshot.size() + " events");
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        Log.d(TAG, "Found event in " + arrayField + ": " + doc.getId());
                        loadEvent.accept(doc);
                    }
                    completedQueries[0]++;
                    checkAllDone.accept(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error querying " + arrayField, e);
                    completedQueries[0]++;
                    checkAllDone.accept(null);
                });
    }

    /**
     * Refreshes the entire event list by reloading all data from Firestore.
     * Called when the fragment resumes to ensure data is current.
     */
    private void refreshEventList() {
        fullEventList.clear();
        eventList.clear();
        if (loadedEventIds != null) {
            loadedEventIds.clear();
        }
        if (completedQueries != null) {
            completedQueries[0] = 0;
        }
        if (pendingStatusLoads != null) {
            pendingStatusLoads[0] = 0;
        }
        if (loading != null) {
            loading.setVisibility(View.VISIBLE);
        }
        if (adapter != null) {
            adapter.updateList(eventList);
        }

        loadEvents();
    }

    /**
     * Callback interface for status retrieval.
     */
    public interface StatusCallback {
        /**
         * Called when the event status has been retrieved.
         *
         * @param status The status of the event for the current user
         */
        void onStatusRetrieved(MyEventItem.Status status);
    }

    /**
     * Gets the status of an event for the current user.
     *
     * @param event The event item
     * @param userId The current user's ID
     * @param db Firestore instance
     * @param callback Callback to receive the status
     */
    private void getEventStatus(EventItem event, String userId, FirebaseFirestore db, StatusCallback callback) {
        DocumentReference eventRef = db.collection("Events").document(event.getId());

        eventRef.get().addOnSuccessListener(eventDoc -> {
            MyEventItem.Status status = determineStatus(eventDoc, userId);
            callback.onStatusRetrieved(status);
        });
    }

    /**
     * Determines the user's status for an event based on which array they're in.
     *
     * @param eventDoc The event document snapshot
     * @param userId The user's ID
     * @return The status (SELECTED, NOT_SELECTED, PENDING, or UNKNOWN)
     */
    private static MyEventItem.Status determineStatus(DocumentSnapshot eventDoc, String userId) {
        List<String> selectedEntrants = (List<String>) eventDoc.get("selectedEntrantIds");
        List<String> cancelledEntrants = (List<String>) eventDoc.get("cancelledEntrantIds");
        List<String> waitingListEntrants = (List<String>) eventDoc.get("waitingListEntrantIds");

        if (selectedEntrants != null && selectedEntrants.contains(userId)) {
            return MyEventItem.Status.SELECTED;
        }

        if (cancelledEntrants != null && cancelledEntrants.contains(userId)) {
            return MyEventItem.Status.NOT_SELECTED;
        }

        if (waitingListEntrants != null && waitingListEntrants.contains(userId)) {
            return MyEventItem.Status.PENDING;
        }

        return MyEventItem.Status.UNKNOWN;
    }

    /**
     * Updates the empty state visibility based on whether there are events to display.
     * Shows empty state with "Browse Events" button if no events, otherwise shows RecyclerView.
     */
    private void updateEmptyState() {
        if (eventList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateContainer.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateContainer.setVisibility(View.GONE);
        }
    }
}