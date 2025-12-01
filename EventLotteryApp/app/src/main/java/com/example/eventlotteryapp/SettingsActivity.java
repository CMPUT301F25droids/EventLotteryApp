package com.example.eventlotteryapp;

import android.os.Bundle;
import android.widget.Switch;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.eventlotteryapp.R;

/**
 * Activity for managing user settings and preferences.
 * Allows users to toggle notification preferences which are stored in Firestore.
 * The notification preference is saved to the user's document in the "entrants" collection.
 * 
 * @author Droids Team
 */
public class SettingsActivity extends AppCompatActivity {

    /** Firestore database instance for saving user preferences. */
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    
    /** Switch widget for toggling notification preferences. */
    private Switch notificationSwitch;

    /**
     * Called when the activity is first created.
     * Initializes the layout, loads the current notification preference from Firestore,
     * and sets up the switch change listener to save preferences.
     * 
     * @param savedInstanceState If the activity is being re-initialized after previously
     *                           being shut down, this Bundle contains the data it most
     *                           recently supplied in onSaveInstanceState(Bundle).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        notificationSwitch = findViewById(R.id.notificationsSwitch);
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Load current preference
        db.collection("entrants").document(uid).get().addOnSuccessListener(doc -> {
            Boolean enabled = doc.getBoolean("notificationsEnabled");
            notificationSwitch.setChecked(enabled != null && enabled);
        });

        // Save new preference
        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            db.collection("entrants").document(uid).update("notificationsEnabled", isChecked);
        });
    }
}
