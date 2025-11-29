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

import java.util.Objects;

public class EntrantHomePageActivity extends AppCompatActivity {
    TabLayout tabLayout;
    ViewPager2 viewPager2;
    EntrantViewPagerAdapter viewPagerAdapter;

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
                // Position 3 is the Scan button - open scanner instead of switching page
                if (position == 3) {
                    Intent intent = new Intent(EntrantHomePageActivity.this, ScanQrCodeActivity.class);
                    startActivity(intent);
                    // Reselect the previously selected tab after a short delay
                    tabLayout.postDelayed(() -> {
                        int currentPage = viewPager2.getCurrentItem();
                        // Map page position to tab position (scan tab is at 3, so pages >= 3 map to tab 4)
                        int tabPos = currentPage >= 3 ? currentPage + 1 : currentPage;
                        if (tabPos < tabLayout.getTabCount() && tabPos != 3) {
                            Objects.requireNonNull(tabLayout.getTabAt(tabPos)).select();
                        } else {
                            Objects.requireNonNull(tabLayout.getTabAt(0)).select();
                        }
                    }, 100);
                } else {
                    // Map tab position to page position (scan tab at 3, so tabs after 3 need -1)
                    int pagePosition = position > 3 ? position - 1 : position;
                    viewPager2.setCurrentItem(pagePosition);
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // If scan tab is reselected, open scanner again
                if (tab.getPosition() == 3) {
                    Intent intent = new Intent(EntrantHomePageActivity.this, ScanQrCodeActivity.class);
                    startActivity(intent);
                }
            }
        });

        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                // Map page position to tab position (scan tab is at position 3, so pages after position 2 need +1)
                int tabPosition = position >= 3 ? position + 1 : position;
                if (tabPosition < tabLayout.getTabCount()) {
                    Objects.requireNonNull(tabLayout.getTabAt(tabPosition)).select();
                }
            }
        });

        int tabToOpen = getIntent().getIntExtra("open_tab", 0); // send to home tab
        viewPager2.setCurrentItem(tabToOpen, false);
        // Map page position to tab position (scan tab is at 3, so pages >= 3 map to tab 4)
        int tabPos = tabToOpen >= 3 ? tabToOpen + 1 : tabToOpen;
        if (tabPos < tabLayout.getTabCount()) {
            Objects.requireNonNull(tabLayout.getTabAt(tabPos)).select();
        }

    }

}