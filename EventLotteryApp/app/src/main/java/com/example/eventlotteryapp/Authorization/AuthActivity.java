package com.example.eventlotteryapp.Authorization;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.example.eventlotteryapp.R;
import com.google.android.material.tabs.TabLayout;
import java.util.Objects;

/**
 * The main activity for user authentication.
 * This activity hosts the Login and Sign Up fragments, allowing users to switch between them.
 */
public class AuthActivity extends AppCompatActivity {
    private TabLayout tabLayout;
    private ViewPager2 viewPager2;
    private AuthViewPagerAdapter viewPagerAdapter;

    /**
     * Called when the activity is first created.
     * This method initializes the layout, sets up the ViewPager and TabLayout for login and sign-up forms.
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle).
     */
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
     * Programmatically switches the ViewPager to the Sign Up fragment.
     * This is useful for scenarios where you want to direct the user to the sign-up form, for example, from an external link or another part of the app.
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