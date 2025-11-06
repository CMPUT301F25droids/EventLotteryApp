package com.example.eventlotteryapp.EntrantView;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
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
        setContentView(R.layout.activity_entrant_home_page);

        tabLayout = findViewById(R.id.entrant_home_tabs);
        viewPager2 = findViewById(R.id.entrant_view_pager);

        viewPagerAdapter = new EntrantViewPagerAdapter(this);
        viewPager2.setAdapter(viewPagerAdapter);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager2.setCurrentItem(tab.getPosition());
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
                Objects.requireNonNull(tabLayout.getTabAt(position)).select();
            }
        });

        int tabToOpen = getIntent().getIntExtra("open_tab", 0); // send to home tab
        viewPager2.setCurrentItem(tabToOpen, false);
        Objects.requireNonNull(tabLayout.getTabAt(tabToOpen)).select();

    }

}