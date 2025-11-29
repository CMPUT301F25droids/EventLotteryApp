package com.example.eventlotteryapp.Admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventlotteryapp.R;
import com.google.firebase.firestore.CollectionReference;
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
    private final CollectionReference usersRef = firestore.collection("users");

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

        // NEW: tap entire profile row to delete
        adapter.setOnProfileClickListener(this::showDeleteDialog);

        loadProfiles();
    }

    private void loadProfiles() {
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);

        usersRef.get()
                .addOnSuccessListener(querySnapshot -> {

                    List<AdminProfileAdapter.UserProfile> profiles = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String id = doc.getId();
                        String name = doc.getString("name");
                        String email = doc.getString("email");
                        String role = doc.getString("role");
                        String phone = doc.getString("phone");

                        profiles.add(new AdminProfileAdapter.UserProfile(
                                id,
                                name != null ? name : "(no name)",
                                email != null ? email : "(no email)",
                                role != null ? role : "entrant",
                                phone != null ? phone : ""
                        ));
                    }

                    progressBar.setVisibility(View.GONE);

                    if (profiles.isEmpty()) {
                        emptyView.setText("No profiles found");
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

    // Ask admin before deleting
    private void showDeleteDialog(AdminProfileAdapter.UserProfile profile) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Profile")
                .setMessage("Are you sure you want to delete \"" + profile.name + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> deleteUser(profile.id))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Delete from Firestore and refresh list
    private void deleteUser(String userId) {
        usersRef.document(userId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Profile deleted", Toast.LENGTH_SHORT).show();
                    loadProfiles();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show()
                );
    }
}
