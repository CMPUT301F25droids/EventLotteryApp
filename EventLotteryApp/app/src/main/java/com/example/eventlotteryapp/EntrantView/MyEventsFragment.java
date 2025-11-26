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
import com.example.eventlotteryapp.UserSession;
import com.google.firebase.firestore.DocumentReference;
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_entrants_myevents_list, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.events_list);
        recyclerView.setHasFixedSize(true);

        // ADD THIS LINE - Set the LayoutManager:
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Get empty state views
        View emptyStateContainer = view.findViewById(R.id.empty_state_container);
        Button browseEventsButton = view.findViewById(R.id.browse_events_button);

        // Browse events button click listener
        browseEventsButton.setOnClickListener(v -> {
            // Switch to the Events tab (position 0)
            if (getActivity() instanceof EntrantHomePageActivity) {
                ((EntrantHomePageActivity) getActivity()).viewPager2.setCurrentItem(0);
            }
        });

        List<EventItem> eventList = new ArrayList<>();
        EventsListRecyclerViewAdapter adapter = new EventsListRecyclerViewAdapter(eventList, position -> {
            EventItem clickedEvent = eventList.get(position);
            // view event details
            Intent intent = new Intent(getContext(), EventDetailsActivity.class);
            intent.putExtra("eventId", clickedEvent.getId());
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);

        ProgressBar loading = view.findViewById(R.id.loading_indicator);
        loading.setVisibility(View.VISIBLE);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = UserSession.getCurrentUserId();
        db.collection("users").document(userId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.e("Firestore", "Listen failed.", e);
                        loading.setVisibility(View.GONE);
                        updateEmptyState(eventList, recyclerView, emptyStateContainer);
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        eventList.clear();

                        List<DocumentReference> joinedEvents = (List<DocumentReference>) documentSnapshot.get("JoinedEvents");

                        if (joinedEvents != null && !joinedEvents.isEmpty()) {

                            final int total = joinedEvents.size();
                            final int[] loadedCount = {0};

                            for (DocumentReference eventRef : joinedEvents) {

                                if (eventRef == null) {
                                    checkFinished(loadedCount, total, eventList, adapter, loading, recyclerView, emptyStateContainer);
                                    continue;
                                }

                                eventRef.get()
                                        .addOnSuccessListener(eventDoc -> {
                                            if (eventDoc.exists()) {
                                                EventItem event = eventDoc.toObject(EventItem.class);
                                                if (event != null) {
                                                    event.setId(eventDoc.getId());
                                                    eventList.add(event);
                                                }
                                            }
                                            checkFinished(loadedCount, total, eventList, adapter, loading, recyclerView, emptyStateContainer);
                                        })
                                        .addOnFailureListener(e2 -> {
                                            Log.e("Firestore", "Error loading event", e2);
                                            checkFinished(loadedCount, total, eventList, adapter, loading, recyclerView, emptyStateContainer);
                                        });
                            }

                        } else {
                            adapter.updateList(eventList);
                            loading.setVisibility(View.GONE);
                            updateEmptyState(eventList, recyclerView, emptyStateContainer);
                        }

                    } else {
                        Log.d("Firestore", "Document does not exist");
                        adapter.updateList(eventList);
                        loading.setVisibility(View.GONE);
                        updateEmptyState(eventList, recyclerView, emptyStateContainer);
                    }
                });

        return view;
    }

    private void checkFinished(int[] loadedCount, int total, List<EventItem> eventList,
                               EventsListRecyclerViewAdapter adapter, ProgressBar loading,
                               RecyclerView recyclerView, View emptyStateContainer) {

        loadedCount[0]++;
        if (loadedCount[0] == total) {
            adapter.updateList(eventList);
            loading.setVisibility(View.GONE);
            updateEmptyState(eventList, recyclerView, emptyStateContainer);
        }
    }

    // Helper method to show/hide empty state
    private void updateEmptyState(List<EventItem> eventList, RecyclerView recyclerView, View emptyStateContainer) {
        if (eventList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateContainer.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateContainer.setVisibility(View.GONE);
        }
    }
}