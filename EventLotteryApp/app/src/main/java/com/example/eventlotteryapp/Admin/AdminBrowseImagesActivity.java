package com.example.eventlotteryapp.Admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventlotteryapp.R;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity that lets the admin browse all event images
 * and open a selected image for full view or deletion.
 */
public class AdminBrowseImagesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private AdminImageAdapter adapter;

    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final CollectionReference eventsRef = firestore.collection("Events");

    /**
     * Initializes UI components and loads all images from Firestore.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_browse_images);

        // Back button
        android.widget.ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recyclerAdminImages);
        progressBar = findViewById(R.id.adminImagesProgress);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new AdminImageAdapter(new ArrayList<>(), this::openImage);
        recyclerView.setAdapter(adapter);

        loadImages();
    }

    /**
     * Retrieves all images from event documents and displays them.
     */
    private void loadImages() {
        progressBar.setVisibility(View.VISIBLE);

        eventsRef.get().addOnSuccessListener(query -> {
            List<AdminImageAdapter.ImageItem> imageItems = new ArrayList<>();

            for (QueryDocumentSnapshot doc : query) {
                String eventId = doc.getId();
                String base64 = doc.getString("Image");

                if (base64 != null && !base64.trim().isEmpty()) {
                    imageItems.add(new AdminImageAdapter.ImageItem(eventId, base64));
                }
            }

            adapter.updateImages(imageItems);
            progressBar.setVisibility(View.GONE);

        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Failed to load images: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Opens a single image in a dedicated screen for deletion.
     */
    private void openImage(String eventId, String base64) {
        Intent intent = new Intent(this, AdminViewImageActivity.class);
        intent.putExtra("eventId", eventId);
        intent.putExtra("imageBase64", base64);
        startActivityForResult(intent, 10);
    }

    /**
     * Refreshes the list after an image is deleted.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 10 && resultCode == RESULT_OK) {
            loadImages();
        }
    }
}
