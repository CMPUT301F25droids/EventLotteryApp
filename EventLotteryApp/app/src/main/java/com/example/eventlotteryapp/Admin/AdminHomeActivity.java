package com.example.eventlotteryapp.Admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.example.eventlotteryapp.Authorization.AuthActivity;
import com.example.eventlotteryapp.R;
import com.google.firebase.auth.FirebaseAuth;

public class AdminHomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);

        Button browseEventsButton = findViewById(R.id.adminBrowseEventsButton);
        Button browseProfilesButton = findViewById(R.id.adminBrowseProfilesButton);
        Button logoutButton = findViewById(R.id.adminLogoutButton);

        // Browse Events
        browseEventsButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminHomeActivity.this, AdminBrowseEventsActivity.class);
            startActivity(intent);
        });

        // Browse Profiles
        browseProfilesButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminHomeActivity.this, AdminBrowseProfilesActivity.class);
            startActivity(intent);
        });

        // Logout
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(AdminHomeActivity.this, AuthActivity.class));
            finish();
        });
    }
}
