package com.example.eventlotteryapp.Admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventlotteryapp.Authorization.AuthActivity;
import com.example.eventlotteryapp.R;
import com.google.firebase.auth.FirebaseAuth;

/**
 * The main landing screen for administrators.
 * Allows navigation to event browsing, profile browsing,
 * image browsing, and logout functionality.
 */
public class AdminHomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);

        // Back button
        android.widget.ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        Button browseEventsButton = findViewById(R.id.adminBrowseEventsButton);
        Button browseProfilesButton = findViewById(R.id.adminBrowseProfilesButton);
        Button browseImagesButton = findViewById(R.id.adminBrowseImagesButton);
        Button notificationLogsButton = findViewById(R.id.adminNotificationLogsButton);
        Button logoutButton = findViewById(R.id.adminLogoutButton);

        /**
         * Opens the page where the admin can browse all events.
         */
        browseEventsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminBrowseEventsActivity.class);
            startActivity(intent);
        });

        /**
         * Opens the page where the admin can browse user profiles.
         */
        browseProfilesButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminBrowseProfilesActivity.class);
            startActivity(intent);
        });

        /**
         * Opens the page where the admin can browse all uploaded event images.
         */
        browseImagesButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminBrowseImagesActivity.class);
            startActivity(intent);
        });

        /**
         * Opens the page where the admin can review notification logs.
         */
        notificationLogsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminNotificationLogsActivity.class);
            startActivity(intent);
        });

        /**
         * Logs the admin out and returns to the authentication page.
         */
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, AuthActivity.class));
            finish();
        });
    }
}
