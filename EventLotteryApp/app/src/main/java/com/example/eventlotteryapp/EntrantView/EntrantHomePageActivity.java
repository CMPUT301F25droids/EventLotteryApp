package com.example.eventlotteryapp.EntrantView;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;
import com.example.eventlotteryapp.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.Objects;

/**
 * Main home page activity for entrants (participants).
 * Displays a tabbed interface using ViewPager2 with fragments for browsing events,
 * viewing my events, notifications, and profile management.
 * Supports edge-to-edge display and programmatic tab navigation via intent extras.
 * 
 * @author Droids Team
 */
public class EntrantHomePageActivity extends AppCompatActivity {
    /** TabLayout for displaying and selecting tabs. */
    TabLayout tabLayout;
    
    /** ViewPager2 for swiping between different fragments. */
    ViewPager2 viewPager2;
    
    /** Adapter for managing fragments in the ViewPager2. */
    EntrantViewPagerAdapter viewPagerAdapter;

    /**
     * Called when the activity is first created.
     * Initializes edge-to-edge display, sets up the ViewPager2 and TabLayout,
     * configures tab selection listeners, and optionally opens a specific tab
     * based on intent extras (via "open_tab" extra).
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

        tabLayout = findViewById(R.id.entrant_home_tabs);
        viewPager2 = findViewById(R.id.entrant_view_pager);
        viewPagerAdapter = new EntrantViewPagerAdapter(this);
        viewPager2.setAdapter(viewPagerAdapter);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                viewPager2.setCurrentItem(position);
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (position < tabLayout.getTabCount()) {
                    Objects.requireNonNull(tabLayout.getTabAt(position)).select();
                }
            }
        });

        int tabToOpen = getIntent().getIntExtra("open_tab", 0); // send to home tab
        viewPager2.setCurrentItem(tabToOpen, false);
        if (tabToOpen < tabLayout.getTabCount()) {
            Objects.requireNonNull(tabLayout.getTabAt(tabToOpen)).select();
        }

    }

}