package com.example.eventlotteryapp.organizer;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class CreateEventPagerAdapter extends FragmentStateAdapter {

    private static final int NUM_PAGES = 6;

    public CreateEventPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

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

    @Override
    public int getItemCount() {
        return NUM_PAGES;
    }
}
