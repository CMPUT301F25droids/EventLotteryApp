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

        adapter = new AdminProfileAdapter(
                new ArrayList<>(),
                this::showDeleteDialog,
                this::showBanDialog
        );

        recyclerView.setAdapter(adapter);

        loadProfiles();
    }

    private void loadProfiles() {
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);

        usersRef.get().addOnSuccessListener(query -> {

                    List<AdminProfileAdapter.UserProfile> profiles = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : query) {
                        String id = doc.getId();
                        String name = doc.getString("name");
                        String email = doc.getString("email");
                        String role = doc.getString("role");
                        String phone = doc.getString("phone");
                        Boolean banned = doc.getBoolean("organizerModeBanned");

                        if (banned == null) banned = false;

                        profiles.add(new AdminProfileAdapter.UserProfile(
                                id,
                                name != null ? name : "(no name)",
                                email != null ? email : "(no email)",
                                role != null ? role : "entrant",
                                phone != null ? phone : "",
                                banned
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

    private void showDeleteDialog(AdminProfileAdapter.UserProfile user) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Profile")
                .setMessage("Delete \"" + user.name + "\"?")
                .setPositiveButton("Delete", (d, w) -> deleteUser(user.id))
                .setNegativeButton("Cancel", null)
                .show();
    }

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

    private void showBanDialog(AdminProfileAdapter.UserProfile user) {
        new AlertDialog.Builder(this)
                .setTitle("Organizer Mode Control")
                .setMessage("Ban or unban organizer mode?")
                .setPositiveButton("Ban", (d, w) -> setOrganizerBan(user.id, true))
                .setNegativeButton("Unban", (d, w) -> setOrganizerBan(user.id, false))
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void setOrganizerBan(String userId, boolean banned) {
        firestore.collection("users")
                .document(userId)
                .update("organizerModeBanned", banned)
                .addOnSuccessListener(a -> {
                    Toast.makeText(this,
                            banned ? "Organizer mode banned" : "Organizer mode unbanned",
                            Toast.LENGTH_SHORT).show();
                    loadProfiles();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

}
