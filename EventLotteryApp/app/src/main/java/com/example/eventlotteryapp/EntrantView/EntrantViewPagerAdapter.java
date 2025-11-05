package com.example.eventlotteryapp.EntrantView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.eventlotteryapp.Authorization.LoginFragment;
import com.example.eventlotteryapp.Authorization.SignUpFragment;

public class EntrantViewPagerAdapter extends FragmentStateAdapter {
    public EntrantViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new EventsList();
            default:
                return new EventsList();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}
