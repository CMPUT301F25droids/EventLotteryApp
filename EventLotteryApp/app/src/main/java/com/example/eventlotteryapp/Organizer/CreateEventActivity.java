package com.example.eventlotteryapp.Organizer;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.eventlotteryapp.R;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

/**
 * Allows organizer to create new event.
 * Supports geolocation requirement toggle and optional entrant limit.
 * Stores event in Firestore under 'events' collection.
 */

public class CreateEventActivity extends AppCompatActivity {

    private EditText eventName;
    private Switch geoSwitch;
    private Button createButton;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        eventName = findViewById(R.id.editEventName);
        geoSwitch = findViewById(R.id.switchGeo);
        createButton = findViewById(R.id.buttonCreateEvent);

        db = FirebaseFirestore.getInstance();

        createButton.setOnClickListener(v -> createEvent());
    }

    private void createEvent() {
        String name = eventName.getText().toString().trim();
        boolean requireGeo = geoSwitch.isChecked();

        if (name.isEmpty()) {
            Toast.makeText(this, "Enter event name", Toast.LENGTH_SHORT).show();
            return;
        }

        String eventId = name.replace(" ", "_").toLowerCase();

        Map<String, Object> event = new HashMap<>();
        event.put("name", name);
        event.put("requireGeolocation", requireGeo);

        db.collection("events").document(eventId)
                .set(event)
                .addOnSuccessListener(a -> Toast.makeText(this, "Event created", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}