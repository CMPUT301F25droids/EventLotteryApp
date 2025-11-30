package com.example.eventlotteryapp.Admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventlotteryapp.R;

import java.util.ArrayList;
import java.util.List;

public class AdminProfileAdapter extends RecyclerView.Adapter<AdminProfileAdapter.ProfileViewHolder> {

    public interface OnDeleteClick {
        void onDelete(UserProfile user);
    }

    public interface OnBanClick {
        void onBan(UserProfile user);
    }

    public static class UserProfile {
        public final String id;
        public final String name;
        public final String email;
        public final String role;
        public final String phone;
        public final boolean organizerBanned;

        public UserProfile(String id, String name, String email, String role, String phone, boolean organizerBanned) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.role = role;
            this.phone = phone;
            this.organizerBanned = organizerBanned;
        }
    }

    private final List<UserProfile> profiles = new ArrayList<>();
    private final OnDeleteClick deleteListener;
    private final OnBanClick banListener;

    public AdminProfileAdapter(List<UserProfile> initialProfiles,
                               OnDeleteClick deleteListener,
                               OnBanClick banListener) {

        if (initialProfiles != null) profiles.addAll(initialProfiles);
        this.deleteListener = deleteListener;
        this.banListener = banListener;
    }

    public void updateProfiles(List<UserProfile> newProfiles) {
        profiles.clear();
        if (newProfiles != null) profiles.addAll(newProfiles);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_profile, parent, false);
        return new ProfileViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ProfileViewHolder h, int position) {
        UserProfile user = profiles.get(position);

        h.name.setText(user.name);
        h.email.setText(user.email);
        h.role.setText("Role: " + user.role);

        if (user.phone == null || user.phone.isEmpty()) {
            h.phone.setVisibility(View.GONE);
        } else {
            h.phone.setVisibility(View.VISIBLE);
            h.phone.setText("Phone: " + user.phone);
        }

        h.status.setText(user.organizerBanned ? "Organizer BANNED" : "Organizer Active");
        h.status.setTextColor(user.organizerBanned ? 0xFFFF4444 : 0xFF44DD44);

        h.itemView.setOnClickListener(v -> deleteListener.onDelete(user));
        h.banButton.setOnClickListener(v -> banListener.onBan(user));
    }

    @Override
    public int getItemCount() {
        return profiles.size();
    }

    static class ProfileViewHolder extends RecyclerView.ViewHolder {

        TextView name, email, role, phone, status;
        Button banButton;

        public ProfileViewHolder(@NonNull View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.adminProfileName);
            email = itemView.findViewById(R.id.adminProfileEmail);
            role = itemView.findViewById(R.id.adminProfileRole);
            phone = itemView.findViewById(R.id.adminProfilePhone);
            status = itemView.findViewById(R.id.adminProfileBanStatus);
            banButton = itemView.findViewById(R.id.adminBanButton);
        }
    }
}
