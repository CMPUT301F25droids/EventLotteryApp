package com.example.eventlotteryapp.Admin;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventlotteryapp.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminBrowseProfilesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;

    private AdminProfileAdapter adapter;
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_browse_profiles);

        recyclerView = findViewById(R.id.recyclerAdminProfiles);
        progressBar = findViewById(R.id.adminProfilesProgress);
        emptyView = findViewById(R.id.adminProfilesEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminProfileAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        loadProfiles();
    }

    private void loadProfiles() {
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);

        firestore.collection("users")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<AdminProfileAdapter.UserProfile> profiles = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String id = doc.getId();
                        String name = doc.getString("name");
                        String email = doc.getString("email");
                        String role = doc.getString("role");
                        String phone = doc.getString("phone");

                        AdminProfileAdapter.UserProfile profile =
                                new AdminProfileAdapter.UserProfile(
                                        id,
                                        name != null ? name : "(no name)",
                                        email != null ? email : "(no email)",
                                        role != null ? role : "entrant",
                                        phone != null ? phone : ""
                                );

                        profiles.add(profile);
                    }

                    progressBar.setVisibility(View.GONE);

                    if (profiles.isEmpty()) {
                        emptyView.setVisibility(View.VISIBLE);
                    } else {
                        emptyView.setVisibility(View.GONE);
                        adapter.updateProfiles(profiles);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    emptyView.setText("Error loading profiles");
                    emptyView.setVisibility(View.VISIBLE);
                });
    }
}
