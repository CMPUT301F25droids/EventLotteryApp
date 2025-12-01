package com.example.eventlotteryapp;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.eventlotteryapp.EntrantView.EntrantHomePageActivity;
import com.example.eventlotteryapp.EntrantView.EventsListFragment;
import com.example.eventlotteryapp.EntrantView.MyEventsFragment;
import com.example.eventlotteryapp.Notifications.NotificationsFragment;
import com.example.eventlotteryapp.ui.profile.ProfileFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Main home page activity for entrants (participants).
 * Displays a tabbed interface with fragments for browsing events, viewing
 * my events, notifications, and profile management.
 * 
 * @author Droids Team
 */
public class EntrantHomePage extends AppCompatActivity{
    /** Firestore database instance for data operations. */
    private FirebaseFirestore db;

    /**
     * Called when the activity is first created.
     * Sets up edge-to-edge display, initializes the layout, and configures
     * the tab selection listener for fragment navigation.
     * 
     * @param savedInstanceState If the activity is being re-initialized after previously
     *                           being shut down, this Bundle contains the data it most
     *                           recently supplied in onSaveInstanceState(Bundle).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_entrant_home_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        setOnTabSelectedListener(findViewById(R.id.entrant_home_tabs));
    }

    /**
     * Sets up the tab selection listener to switch between different fragments.
     * Handles navigation to Events List, My Events, Notifications, and Profile fragments.
     * 
     * @param tab the TabLayout to configure with selection listeners
     */
    private void setOnTabSelectedListener(TabLayout tab) {
        tab.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                if (position == 2) {
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

    /**
     * Replaces the current fragment in the fragment container with the specified fragment.
     * 
     * @param fragment the fragment to display in the container
     */
    private void selectFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

}
