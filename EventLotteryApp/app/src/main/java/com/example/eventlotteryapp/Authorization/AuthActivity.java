package com.example.eventlotteryapp.Authorization;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.example.eventlotteryapp.R;
import com.google.android.material.tabs.TabLayout;
import java.util.Objects;

public class AuthActivity extends AppCompatActivity {
    TabLayout tabLayout;
    ViewPager2 viewPager2;
    AuthViewPagerAdapter viewPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        tabLayout = findViewById(R.id.authorization_tabs);
        viewPager2 = findViewById(R.id.auth_view_pager);
        viewPagerAdapter = new AuthViewPagerAdapter(this);
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
    }
    
    /**
     * Switch to the Sign Up tab (typically position 1)
     */
    public void switchToSignUp() {
        if (viewPager2 != null && viewPager2.getAdapter() != null) {
            // Assuming Sign Up is at position 1 (index 1)
            int signUpPosition = 1;
            if (signUpPosition < viewPager2.getAdapter().getItemCount()) {
                viewPager2.setCurrentItem(signUpPosition, true);
            }
        }
    }
}