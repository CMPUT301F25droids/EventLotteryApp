package com.example.eventlotteryapp.Admin;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventlotteryapp.R;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Activity that displays a single event image in full screen
 * and allows the administrator to delete it.
 */
public class AdminViewImageActivity extends AppCompatActivity {

    private ImageView imageFull;
    private Button deleteBtn;

    private String eventId;   // Firestore ID of the event
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    /**
     * Initializes UI, loads the selected image, and attaches delete logic.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_view_image);

        imageFull = findViewById(R.id.adminFullImageView);
        deleteBtn = findViewById(R.id.adminDeleteImage);

        eventId = getIntent().getStringExtra("eventId");
        String base64 = getIntent().getStringExtra("imageBase64");

        loadImage(base64);

        deleteBtn.setOnClickListener(v -> deleteImage());
    }

    /**
     * Decodes Base64 and shows image in the ImageView.
     */
    private void loadImage(String base64) {
        if (base64 == null) {
            Toast.makeText(this, "Image not found", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            if (base64.contains(",")) {
                base64 = base64.substring(base64.indexOf(",") + 1);
            }

            byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            imageFull.setImageBitmap(bitmap);

        } catch (Exception e) {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Deletes the image from its parent Event document in Firestore.
     * Sends a result back so the list can refresh.
     */
    private void deleteImage() {
        if (eventId == null) {
            Toast.makeText(this, "Error: Event ID missing", Toast.LENGTH_SHORT).show();
            return;
        }

        firestore.collection("Events")
                .document(eventId)
                .update("Image", null)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Image deleted", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Delete failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }
}
