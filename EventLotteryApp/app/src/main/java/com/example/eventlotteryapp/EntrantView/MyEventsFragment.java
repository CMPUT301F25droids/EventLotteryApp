package com.example.eventlotteryapp.EntrantView;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.EntrantView.placeholder.PlaceholderContent;
import com.example.eventlotteryapp.UserSession;
import com.google.android.gms.common.api.internal.StatusCallback;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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
        View view = inflater.inflate(R.layout.fragment_events_list_list, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.events_list);
        recyclerView.setHasFixedSize(true);

        List<MyEventItem> eventList = new ArrayList<>();
        MyEventsListRecyclerViewAdapter adapter = new MyEventsListRecyclerViewAdapter(eventList,position -> {
            MyEventItem clickedEvent = eventList.get(position);
            // view event details
            Intent intent = new Intent(getContext(), EventDetailsActivity.class);
            intent.putExtra("eventId", clickedEvent.getId());
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = UserSession.getCurrentUserId();
        db.collection("users").document(userId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.e("Firestore", "Listen failed.", e);
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        eventList.clear();

                        List<DocumentReference> joinedEvents = (List<DocumentReference>) documentSnapshot.get("JoinedEvents");
                        if (joinedEvents != null && !joinedEvents.isEmpty()) {

                            final int[] loadedCount = {0};

                            for (DocumentReference eventRef : joinedEvents) {
                                if (eventRef != null) {
                                    eventRef.get()
                                            .addOnSuccessListener(eventDoc -> {
                                                if (eventDoc.exists()) {
                                                    MyEventItem event = (MyEventItem) eventDoc.toObject(EventItem.class);
                                                    event.setId(eventDoc.getId());

                                                    getEventStatus(event, userId, db, new StatusCallback() {
                                                        @Override
                                                        public void onStatusRetrieved(MyEventItem.Status status) {
                                                            event.setStatus(status);
                                                        }
                                                    });
                                                    eventList.add(event);
                                                }

                                                loadedCount[0]++;
                                                if (loadedCount[0] == joinedEvents.size()) {
                                                    adapter.notifyDataSetChanged();
                                                }
                                            })
                                            .addOnFailureListener(ev -> {
                                                loadedCount[0]++;
                                                Log.e("Firestore", "Error loading event", ev);
                                                if (loadedCount[0] == joinedEvents.size()) {
                                                    adapter.notifyDataSetChanged();
                                                }
                                            });
                                } else {
                                    loadedCount[0]++;
                                    if (loadedCount[0] == joinedEvents.size()) {
                                        adapter.notifyDataSetChanged();
                                    }
                                }
                            }

                        } else {
                            adapter.notifyDataSetChanged(); // no joined events
                        }

                    } else {
                        Log.d("Firestore", "Document does not exist");
                        adapter.notifyDataSetChanged();
                    }
                });

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
}