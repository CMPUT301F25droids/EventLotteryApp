package com.example.eventlotteryapp.organizer;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.models.User;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays list of entrants for a given event.
 * Retrieves entrant data from Firestore.
 */
public class EntrantListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EntrantListAdapter adapter;
    private final List<User> data = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_list);

        recyclerView = findViewById(R.id.recyclerEntrants);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EntrantListAdapter(data);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        String eventId = getIntent().getStringExtra("eventId");
        if (eventId == null || eventId.isEmpty()) return;

        db.collection("Events").document(eventId)
                .collection("entrants")
                .get()
                .addOnSuccessListener(snap -> {
                    data.clear();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        User u = d.toObject(User.class);
                        if (u != null) data.add(u);
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}

