package com.example.eventlotteryapp.EntrantView;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import com.example.eventlotteryapp.R;
import com.google.android.gms.common.api.internal.StatusCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment representing a list of Items.
 */
public class MyEventsFragment extends Fragment {

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    // TODO: Customize parameters
    private int mColumnCount = 1;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public MyEventsFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static MyEventsFragment newInstance(int columnCount) {
        MyEventsFragment fragment = new MyEventsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    private RecyclerView recyclerView;
    private MyEventsListRecyclerViewAdapter adapter;
    private List<MyEventItem> eventList;
    private View emptyStateContainer;
    private ProgressBar loading;
    private java.util.Set<String> loadedEventIds;
    private int[] completedQueries;
    private int[] pendingStatusLoads;
    private FirebaseFirestore db;
    private String userId;
    private DocumentReference user_ref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when fragment becomes visible (e.g., returning from EventDetailsActivity after leaving an event)
        if (userId != null && db != null && adapter != null && eventList != null && user_ref != null) {
            Log.d("MyEventsFragment", "onResume() - Refreshing event list...");
            // Clear current data
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
            adapter.updateList(eventList);
            
            // Re-run all waitlist queries to get fresh data
            // This is needed because the snapshot listener only watches the user document,
            // not the event documents, so it won't fire when event arrays change
            runWaitlistQueries();
        }
    }
    
    private void runWaitlistQueries() {
        if (userId == null || db == null || adapter == null || eventList == null) {
            return;
        }
        
        final int totalQueries = 3; // 3 waitlist queries (waitingListEntrantIds, selectedEntrantIds, cancelledEntrantIds)
        if (completedQueries == null) completedQueries = new int[]{0};
        if (pendingStatusLoads == null) pendingStatusLoads = new int[]{0};
        if (loadedEventIds == null) loadedEventIds = new java.util.HashSet<>();
        
        // Helper functions (need to recreate them here or make them instance methods)
        java.util.function.Consumer<Void> checkAllDone = (v) -> {
            Log.d("MyEventsFragment", "onResume check - Queries: " + completedQueries[0] + "/" + totalQueries + ", Pending: " + pendingStatusLoads[0]);
            if (completedQueries[0] >= totalQueries && pendingStatusLoads[0] == 0) {
                Log.d("MyEventsFragment", "onResume - All done! Total events: " + eventList.size());
                if (adapter != null) {
                    adapter.updateList(eventList);
                }
                if (loading != null) {
                    loading.setVisibility(View.GONE);
                }
                updateEmptyState(eventList, recyclerView, emptyStateContainer);
            }
        };
        
        // Simplified loadEvent for onResume
        java.util.function.Consumer<com.google.firebase.firestore.DocumentSnapshot> loadEvent = (eventDoc) -> {
            if (!eventDoc.exists()) return;
            String eventId = eventDoc.getId();
            if (loadedEventIds.contains(eventId)) return;
            
            MyEventItem event = eventDoc.toObject(MyEventItem.class);
            if (event == null) return;
            
            event.setId(eventId);
            loadedEventIds.add(eventId);
            pendingStatusLoads[0]++;
            
            getEventStatus(event, userId, db, new StatusCallback() {
                @Override
                public void onStatusRetrieved(MyEventItem.Status status) {
                    event.setStatus(status);
                    eventList.add(event);
                    pendingStatusLoads[0]--;
                    checkAllDone.accept(null);
                }
            });
        };
        
        // Run all queries
        db.collection("Events").whereArrayContains("waitingListEntrantIds", userId).get()
                .addOnSuccessListener(snap -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snap) {
                        loadEvent.accept(doc);
                    }
                    completedQueries[0]++;
                    checkAllDone.accept(null);
                })
                .addOnFailureListener(e -> {
                    Log.e("MyEventsFragment", "onResume query error", e);
                    completedQueries[0]++;
                    checkAllDone.accept(null);
                });
        
        db.collection("Events").whereArrayContains("selectedEntrantIds", userId).get()
                .addOnSuccessListener(snap -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snap) {
                        loadEvent.accept(doc);
                    }
                    completedQueries[0]++;
                    checkAllDone.accept(null);
                })
                .addOnFailureListener(e -> {
                    completedQueries[0]++;
                    checkAllDone.accept(null);
                });
        
        db.collection("Events").whereArrayContains("cancelledEntrantIds", userId).get()
                .addOnSuccessListener(snap -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snap) {
                        loadEvent.accept(doc);
                    }
                    completedQueries[0]++;
                    checkAllDone.accept(null);
                })
                .addOnFailureListener(e -> {
                    completedQueries[0]++;
                    checkAllDone.accept(null);
                });
        
        // Removed old Waitlist query - using only waitingListEntrantIds now
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_entrants_myevents_list, container, false);

        recyclerView = view.findViewById(R.id.events_list);
        recyclerView.setHasFixedSize(true);

        // ADD THIS LINE - Set the LayoutManager:
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Get empty state views
        emptyStateContainer = view.findViewById(R.id.empty_state_container);
        Button browseEventsButton = view.findViewById(R.id.browse_events_button);
        
        // Get scan QR button
        android.widget.ImageButton scanQrButton = view.findViewById(R.id.scan_qr_button);

        // Browse events button click listener
        browseEventsButton.setOnClickListener(v -> {
            // Switch to the Events tab (position 0)
            if (getActivity() instanceof EntrantHomePageActivity) {
                ((EntrantHomePageActivity) getActivity()).viewPager2.setCurrentItem(0);
            }
        });
        
        // Scan QR button click listener
        scanQrButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), ScanQrCodeActivity.class);
            startActivity(intent);
        });

        eventList = new ArrayList<>();
        adapter = new MyEventsListRecyclerViewAdapter(eventList,position -> {
            MyEventItem clickedEvent = eventList.get(position);
            // view event details
            Intent intent = new Intent(getContext(), EventDetailsActivity.class);
            intent.putExtra("eventId", clickedEvent.getId());
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);

        loading = view.findViewById(R.id.loading_indicator);
        loading.setVisibility(View.VISIBLE);
        
        // Initialize instance variables for refresh capability
        loadedEventIds = new java.util.HashSet<>();
        completedQueries = new int[]{0};
        pendingStatusLoads = new int[]{0};

        // Check if user is authenticated
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            loading.setVisibility(View.GONE);
            updateEmptyState(eventList, recyclerView, emptyStateContainer);
            return view;
        }

        db = FirebaseFirestore.getInstance();
        userId = auth.getCurrentUser().getUid();
        String userEmail = auth.getCurrentUser().getEmail();
        user_ref = db.collection("users").document(userId);
        
        Log.d("MyEventsFragment", "=== Loading My Events for user ===");
        Log.d("MyEventsFragment", "User ID: " + userId);
        Log.d("MyEventsFragment", "User Email: " + (userEmail != null ? userEmail : "null"));
        Log.d("MyEventsFragment", "Setting up data structures and listeners...");
        
        // Reset tracking variables
        loadedEventIds.clear();
        completedQueries[0] = 0;
        pendingStatusLoads[0] = 0;
        final int totalQueries = 4; // JoinedEvents + 3 waitlist queries (waitingListEntrantIds, selectedEntrantIds, cancelledEntrantIds)
        
        // Helper to check if all queries and status loads are done
        java.util.function.Consumer<Void> checkAllDone = (v) -> {
            Log.d("MyEventsFragment", "Checking completion - Queries: " + completedQueries[0] + "/" + totalQueries + ", Pending status: " + pendingStatusLoads[0]);
            if (completedQueries[0] >= totalQueries && pendingStatusLoads[0] == 0) {
                Log.d("MyEventsFragment", "=== All done! Total events loaded: " + eventList.size() + " ===");
                adapter.updateList(eventList);
                loading.setVisibility(View.GONE);
                updateEmptyState(eventList, recyclerView, emptyStateContainer);
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
                                Log.d("MyEventsFragment", "Syncing event " + eventId + " to JoinedEvents for user " + userEmail);
                                user_ref.update("JoinedEvents", com.google.firebase.firestore.FieldValue.arrayUnion(eventRef))
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d("MyEventsFragment", "Successfully synced event to JoinedEvents");
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("MyEventsFragment", "Failed to sync event to JoinedEvents", e);
                                        });
                            }
                        }
                    });
        };
        
        // Helper to load an event (avoid duplicates)
        java.util.function.Consumer<com.google.firebase.firestore.DocumentSnapshot> loadEvent = (eventDoc) -> {
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
            
            Log.d("MyEventsFragment", "Loading event: " + eventId + " (" + (event.getName() != null ? event.getName() : "unknown") + ")");
            
            // Sync this event to JoinedEvents if it's from a waitlist query
            syncEventToJoinedEvents.accept(eventId);
            
            getEventStatus(event, userId, db, new StatusCallback() {
                @Override
                public void onStatusRetrieved(MyEventItem.Status status) {
                    event.setStatus(status);
                    eventList.add(event);
                    Log.d("MyEventsFragment", "Event loaded with status: " + status + " - Total events: " + eventList.size());
                    pendingStatusLoads[0]--;
                    checkAllDone.accept(null);
                }
            });
        };
        
        // Load from user's JoinedEvents array
        try {
            Log.d("MyEventsFragment", "Setting up snapshot listener for user document...");
            db.collection("users").document(userId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    Log.d("MyEventsFragment", "Snapshot listener callback triggered");
                    if (e != null) {
                        Log.e("MyEventsFragment", "Listen failed.", e);
                        loading.setVisibility(View.GONE);
                        updateEmptyState(eventList, recyclerView, emptyStateContainer);
                        return;
                    }

                    Log.d("MyEventsFragment", "Document snapshot - exists: " + (documentSnapshot != null && documentSnapshot.exists()));
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        Log.d("MyEventsFragment", "User document exists, processing...");
                        eventList.clear();
                        loadedEventIds.clear();
                        completedQueries[0] = 0;

                        List<DocumentReference> joinedEvents = (List<DocumentReference>) documentSnapshot.get("JoinedEvents");
                        
                        Log.d("MyEventsFragment", "JoinedEvents count: " + (joinedEvents != null ? joinedEvents.size() : 0));
                        
                        if (joinedEvents != null && !joinedEvents.isEmpty()) {
                            final int[] loadedFromJoined = {0};
                            final int totalJoined = joinedEvents.size();
                            
                            for (DocumentReference eventRef : joinedEvents) {
                                if (eventRef == null) {
                                    loadedFromJoined[0]++;
                                    if (loadedFromJoined[0] >= totalJoined) {
                                        completedQueries[0]++;
                                        checkAllDone.accept(null);
                                    }
                                    continue;
                                }
                                
                                eventRef.get()
                                        .addOnSuccessListener(eventDoc -> {
                                            loadEvent.accept(eventDoc);
                                            loadedFromJoined[0]++;
                                            if (loadedFromJoined[0] >= totalJoined) {
                                                completedQueries[0]++;
                                                checkAllDone.accept(null);
                                            }
                                        })
                                        .addOnFailureListener(e2 -> {
                                            Log.e("Firestore", "Error loading event from JoinedEvents", e2);
                                            loadedFromJoined[0]++;
                                            if (loadedFromJoined[0] >= totalJoined) {
                                                completedQueries[0]++;
                                                checkAllDone.accept(null);
                                            }
                                        });
                            }
                        } else {
                            // No joined events, mark this query as done
                            completedQueries[0]++;
                            checkAllDone.accept(null);
                        }
                        
                        // Also query events where user is in waitlist arrays (to catch cases where JoinedEvents is out of sync)
                        // Query for events where user is in waitingListEntrantIds
                        db.collection("Events")
                                .whereArrayContains("waitingListEntrantIds", userId)
                                .get()
                                .addOnSuccessListener(querySnapshot -> {
                                    Log.d("MyEventsFragment", "waitingListEntrantIds query found: " + querySnapshot.size() + " events");
                                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                                        Log.d("MyEventsFragment", "Found event in waitingListEntrantIds: " + doc.getId());
                                        loadEvent.accept(doc);
                                    }
                                    completedQueries[0]++;
                                    checkAllDone.accept(null);
                                })
                                .addOnFailureListener(e2 -> {
                                    Log.e("MyEventsFragment", "Error querying waitingListEntrantIds", e2);
                                    completedQueries[0]++;
                                    checkAllDone.accept(null);
                                });
                        
                        // Query for events where user is in selectedEntrantIds
                        db.collection("Events")
                                .whereArrayContains("selectedEntrantIds", userId)
                                .get()
                                .addOnSuccessListener(querySnapshot -> {
                                    Log.d("MyEventsFragment", "selectedEntrantIds query found: " + querySnapshot.size() + " events");
                                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                                        Log.d("MyEventsFragment", "Found event in selectedEntrantIds: " + doc.getId());
                                        loadEvent.accept(doc);
                                    }
                                    completedQueries[0]++;
                                    checkAllDone.accept(null);
                                })
                                .addOnFailureListener(e2 -> {
                                    Log.e("MyEventsFragment", "Error querying selectedEntrantIds", e2);
                                    completedQueries[0]++;
                                    checkAllDone.accept(null);
                                });
                        
                        // Query for events where user is in cancelledEntrantIds
                        db.collection("Events")
                                .whereArrayContains("cancelledEntrantIds", userId)
                                .get()
                                .addOnSuccessListener(querySnapshot -> {
                                    Log.d("MyEventsFragment", "cancelledEntrantIds query found: " + querySnapshot.size() + " events");
                                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                                        Log.d("MyEventsFragment", "Found event in cancelledEntrantIds: " + doc.getId());
                                        loadEvent.accept(doc);
                                    }
                                    completedQueries[0]++;
                                    checkAllDone.accept(null);
                                })
                                .addOnFailureListener(e2 -> {
                                    Log.e("MyEventsFragment", "Error querying cancelledEntrantIds", e2);
                                    completedQueries[0]++;
                                    checkAllDone.accept(null);
                                });
                        
                        // Removed old Waitlist query - using only waitingListEntrantIds now

                    } else {
                        Log.d("MyEventsFragment", "User document does not exist - still checking waitlist queries");
                        // Even if user doc doesn't exist, still query waitlists in case user is enrolled
                        // Query for events where user is in waitingListEntrantIds
                        db.collection("Events")
                                .whereArrayContains("waitingListEntrantIds", userId)
                                .get()
                                .addOnSuccessListener(querySnapshot -> {
                                    Log.d("MyEventsFragment", "waitingListEntrantIds query found: " + querySnapshot.size() + " events");
                                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                                        Log.d("MyEventsFragment", "Found event in waitingListEntrantIds: " + doc.getId());
                                        loadEvent.accept(doc);
                                    }
                                    completedQueries[0]++;
                                    checkAllDone.accept(null);
                                })
                                .addOnFailureListener(e2 -> {
                                    Log.e("MyEventsFragment", "Error querying waitingListEntrantIds", e2);
                                    completedQueries[0]++;
                                    checkAllDone.accept(null);
                                });
                        
                        // Query for events where user is in selectedEntrantIds
                        db.collection("Events")
                                .whereArrayContains("selectedEntrantIds", userId)
                                .get()
                                .addOnSuccessListener(querySnapshot -> {
                                    Log.d("MyEventsFragment", "selectedEntrantIds query found: " + querySnapshot.size() + " events");
                                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                                        Log.d("MyEventsFragment", "Found event in selectedEntrantIds: " + doc.getId());
                                        loadEvent.accept(doc);
                                    }
                                    completedQueries[0]++;
                                    checkAllDone.accept(null);
                                })
                                .addOnFailureListener(e2 -> {
                                    Log.e("MyEventsFragment", "Error querying selectedEntrantIds", e2);
                                    completedQueries[0]++;
                                    checkAllDone.accept(null);
                                });
                        
                        // Query for events where user is in cancelledEntrantIds
                        db.collection("Events")
                                .whereArrayContains("cancelledEntrantIds", userId)
                                .get()
                                .addOnSuccessListener(querySnapshot -> {
                                    Log.d("MyEventsFragment", "cancelledEntrantIds query found: " + querySnapshot.size() + " events");
                                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                                        Log.d("MyEventsFragment", "Found event in cancelledEntrantIds: " + doc.getId());
                                        loadEvent.accept(doc);
                                    }
                                    completedQueries[0]++;
                                    checkAllDone.accept(null);
                                })
                                .addOnFailureListener(e2 -> {
                                    Log.e("MyEventsFragment", "Error querying cancelledEntrantIds", e2);
                                    completedQueries[0]++;
                                    checkAllDone.accept(null);
                                });
                        
                        // Removed old Waitlist query - using only waitingListEntrantIds now
                        
                        // Mark JoinedEvents query as done (since doc doesn't exist)
                        completedQueries[0]++;
                        checkAllDone.accept(null);
                    }
                });
        
        // Also try a one-time get() as a fallback to ensure we catch the data
        Log.d("MyEventsFragment", "Also doing one-time get() as fallback...");
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Log.d("MyEventsFragment", "One-time get() succeeded - document exists: " + documentSnapshot.exists());
                })
                .addOnFailureListener(e -> {
                    Log.e("MyEventsFragment", "One-time get() failed", e);
                });
        } catch (Exception ex) {
            Log.e("MyEventsFragment", "Exception setting up listeners", ex);
            loading.setVisibility(View.GONE);
            updateEmptyState(eventList, recyclerView, emptyStateContainer);
        }

        Log.d("MyEventsFragment", "Finished setting up MyEventsFragment");
        return view;
    }

    public interface StatusCallback {
        void onStatusRetrieved(MyEventItem.Status status);
    }
    private void getEventStatus(EventItem event, String userId, FirebaseFirestore db, StatusCallback callback) {
        DocumentReference eventRef = db.collection("Events").document(event.getId());

        eventRef.get().addOnSuccessListener(eventDoc -> {
            MyEventItem.Status status =  determineStatus(eventDoc, userId);
            callback.onStatusRetrieved(status);
        });
    }

    private static MyEventItem.Status determineStatus(DocumentSnapshot eventDoc, String userId) {
        // Get the three arrays
        List<String> selectedEntrants = (List<String>) eventDoc.get("selectedEntrantIds");
        List<String> cancelledEntrants = (List<String>) eventDoc.get("cancelledEntrantIds");
        List<String> waitingListEntrants = (List<String>) eventDoc.get("waitingListEntrantIds");
        
        // Check which array contains the userId
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
    private void checkFinished(int[] loadedCount, int total, List<MyEventItem> eventList,
                           MyEventsListRecyclerViewAdapter adapter, ProgressBar loading,
                           RecyclerView recyclerView, View emptyStateContainer) {

        loadedCount[0]++;
        if (loadedCount[0] == total) {
            adapter.updateList(eventList);
            loading.setVisibility(View.GONE);
            updateEmptyState(eventList, recyclerView, emptyStateContainer);
        }
    }

    // Helper method to show/hide empty state
    private void updateEmptyState(List<MyEventItem> eventList, RecyclerView recyclerView, View emptyStateContainer) {
        if (eventList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateContainer.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateContainer.setVisibility(View.GONE);
        }
    }
}