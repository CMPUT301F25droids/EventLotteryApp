package com.example.eventlotteryapp.organizer;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * Adapter for the ViewPager2 in CreateEventActivity.
 * Manages the six-step event creation process by providing fragments for each step.
 * 
 * @author Droids Team
 */
public class CreateEventPagerAdapter extends FragmentStateAdapter {

    /** Total number of steps in the event creation process. */
    private static final int NUM_PAGES = 6;

    /**
     * Constructs a new CreateEventPagerAdapter.
     * 
     * @param fragmentActivity the FragmentActivity that will host the fragments
     */
    public CreateEventPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    /**
     * Creates and returns a fragment for the specified step position.
     * 
     * @param position the step position (0-5)
     * @return the fragment for the specified step
     */
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new CreateEventStep1Fragment();
            case 1:
                return new CreateEventStep2Fragment();
            case 2:
                return new CreateEventStep3Fragment();
            case 3:
                return new CreateEventStep4Fragment();
            case 4:
                return new CreateEventStep5Fragment();
            case 5:
                return new CreateEventStep6Fragment();
            default:
                return new CreateEventStep1Fragment(); // Should not happen
        }
    }

    /**
     * Returns the total number of steps in the event creation process.
     * 
     * @return the number of pages (always 6)
     */
    @Override
    public int getItemCount() {
        return NUM_PAGES;
    }
}
