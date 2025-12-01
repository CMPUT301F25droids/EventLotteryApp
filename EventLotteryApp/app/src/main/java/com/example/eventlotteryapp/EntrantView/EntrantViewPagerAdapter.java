package com.example.eventlotteryapp.EntrantView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.eventlotteryapp.Notifications.NotificationsFragment;
import com.example.eventlotteryapp.ui.profile.ProfileFragment;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Adapter for the ViewPager2 in EntrantHomePageActivity.
 * Manages the four main fragments for entrants: Events List, My Events,
 * Notifications, and Profile.
 * 
 * @author Droids Team
 */
public class EntrantViewPagerAdapter extends FragmentStateAdapter {
    /**
     * Constructs a new EntrantViewPagerAdapter.
     * 
     * @param fragmentActivity the FragmentActivity that will host the fragments
     */
    public EntrantViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    /**
     * Creates and returns a fragment for the specified position.
     * Position 0: EventsListFragment, Position 1: MyEventsFragment,
     * Position 2: NotificationsFragment, Position 3: ProfileFragment.
     * 
     * @param position the fragment position (0-3)
     * @return the fragment for the specified position
     */
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new EventsListFragment();
            case 1:
                return new MyEventsFragment();
            case 2:
                return NotificationsFragment.newInstance(FirebaseFirestore.getInstance());
            case 3:
                return new ProfileFragment();
            default:
                return new EventsListFragment();
        }
    }

    /**
     * Returns the total number of fragments in the entrant home page.
     * 
     * @return the number of pages (always 4)
     */
    @Override
    public int getItemCount() {
        return 4;
    }
}
