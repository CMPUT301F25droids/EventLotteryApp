package com.example.eventlotteryapp.EntrantView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.eventlotteryapp.Notifications.NotificationsFragment;
import com.example.eventlotteryapp.ui.profile.ProfileFragment;
import com.google.firebase.firestore.FirebaseFirestore;

public class EntrantViewPagerAdapter extends FragmentStateAdapter {
    public EntrantViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

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

    @Override
    public int getItemCount() {
        return 4;
    }
}
