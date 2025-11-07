package com.example.eventlotteryapp.EntrantView;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.eventlotteryapp.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment representing a list of Items.
 */
public class EventsListFragment extends Fragment {

    private static final String ARG_COLUMN_COUNT = "column-count";
    private int mColumnCount = 1;
    
    private FirebaseFirestore db;
    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public EventsListFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static EventsListFragment newInstance(int columnCount) {
        EventsListFragment fragment = new EventsListFragment();
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
        View view = inflater.inflate(R.layout.fragment_events_list_list, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.events_list);
        recyclerView.setHasFixedSize(true);
        int spanCount = getResources().getConfiguration().smallestScreenWidthDp >= 600 ? 3 : 2;

        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), spanCount);
        recyclerView.setLayoutManager(gridLayoutManager);

        List<EventItem> eventList = new ArrayList<>();
        EventsListRecyclerViewAdapter adapter = new EventsListRecyclerViewAdapter(eventList,position -> {
            EventItem clickedEvent = eventList.get(position);
            // view event details
            Intent intent = new Intent(getContext(), EventDetailsActivity.class);
            intent.putExtra("eventId", clickedEvent.getId());
            startActivity(intent);

        });

        recyclerView.setAdapter(adapter);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Events")
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.e("Firestore", "Error loading events", e);
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        eventList.clear();
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            EventItem event = doc.toObject(EventItem.class);
                            event.setId(doc.getId()); // store Firestore document ID if needed
                            eventList.add(event);
                        }
                        adapter.notifyDataSetChanged();
                    }
                });

        return view;
    }
}