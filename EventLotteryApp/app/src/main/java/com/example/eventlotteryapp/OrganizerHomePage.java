package com.example.eventlotteryapp;

import android.content.Intent;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import com.example.eventlotteryapp.Notifications.NotificationsFragment;
import com.example.eventlotteryapp.organizer.CreateEventActivity;
import com.example.eventlotteryapp.organizer.MyEventsFragment;
import com.example.eventlotteryapp.ui.profile.ProfileFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;

public class OrganizerHomePage extends AppCompatActivity {
    private FirebaseFirestore db;
    private MyEventsFragment myEventsFragment;
    private TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_organizer_home_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        tabLayout = findViewById(R.id.organizer_home_tabs);
        setOnTabSelectedListener(tabLayout);
        
        // Show My Events fragment by default when Dashboard tab is selected
        myEventsFragment = new MyEventsFragment();
        selectFragment(myEventsFragment);
        
        // Ensure Dashboard tab is selected by default
        TabLayout.Tab dashboardTab = tabLayout.getTabAt(0);
        if (dashboardTab != null) {
            dashboardTab.select();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Check if we're currently showing Dashboard fragment
        Fragment currentFragment = getSupportFragmentManager()
            .findFragmentById(R.id.fragment_container);
        
        // If Dashboard fragment is shown but Create tab is selected, reset to Dashboard tab
        // This fixes the issue where Create tab appears selected after pressing back from CreateEventActivity
        if (tabLayout != null && currentFragment instanceof MyEventsFragment) {
            int selectedTabPosition = tabLayout.getSelectedTabPosition();
            // If Create tab (position 1) is selected but we're showing Dashboard, reset to Dashboard
            if (selectedTabPosition == 1) {
                TabLayout.Tab dashboardTab = tabLayout.getTabAt(0);
                if (dashboardTab != null) {
                    dashboardTab.select();
                }
            }
        }
        
        // Reload events when returning to this activity (e.g., from CreateEventActivity)
        if (myEventsFragment != null && myEventsFragment.isAdded()) {
            myEventsFragment.loadEvents();
        }
    }

    private void setOnTabSelectedListener(TabLayout tab) {
        tab.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                if (position == 0) {
                    // Dashboard tab - show My Events
                    if (myEventsFragment == null) {
                        myEventsFragment = new MyEventsFragment();
                    }
                    selectFragment(myEventsFragment);
                } else if (position == 1) {
                    // Create tab - navigate to CreateEventActivity
                    // When user returns, they'll see Dashboard, so we'll reset tab in onResume
                    Intent intent = new Intent(OrganizerHomePage.this, CreateEventActivity.class);
                    startActivity(intent);
                } else if (position == 2) {
                    selectFragment(NotificationsFragment.newInstance(db));
                } else if (position == 3) {
                    selectFragment(new ProfileFragment());
                }
            }
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
        });
    }

    private void selectFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}

