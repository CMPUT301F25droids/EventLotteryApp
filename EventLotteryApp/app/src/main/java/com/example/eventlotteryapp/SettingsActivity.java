package com.example.eventlotteryapp;

import android.os.Bundle;
import android.widget.Switch;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.eventlotteryapp.R;

public class SettingsActivity extends AppCompatActivity {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private Switch notificationSwitch;

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
