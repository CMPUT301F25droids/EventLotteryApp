package com.example.eventlotteryapp.Authorization;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * Adapter for the ViewPager2 in AuthActivity.
 * Manages the two authentication fragments: Login and Sign Up.
 * 
 * @author Droids Team
 */
public class AuthViewPagerAdapter extends FragmentStateAdapter {
    /**
     * Constructs a new AuthViewPagerAdapter.
     * 
     * @param fragmentActivity the FragmentActivity that will host the fragments
     */
    public AuthViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    /**
     * Creates and returns a fragment for the specified position.
     * Position 0 returns LoginFragment, position 1 returns SignUpFragment.
     * 
     * @param position the fragment position (0 for login, 1 for sign up)
     * @return the fragment for the specified position
     */
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 1) {
            return new SignUpFragment();
        }
        return new LoginFragment();
    }

    /**
     * Returns the total number of authentication fragments.
     * 
     * @return the number of pages (always 2: Login and Sign Up)
     */
    @Override
    public int getItemCount() {
        return 2;
    }
}
