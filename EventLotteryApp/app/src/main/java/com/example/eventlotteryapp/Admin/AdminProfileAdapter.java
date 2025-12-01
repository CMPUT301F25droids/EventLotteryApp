package com.example.eventlotteryapp.Admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventlotteryapp.R;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for displaying all user profiles in the admin panel.
 * Supports deleting a profile and banning/unbanning organizer mode.
 */
public class AdminProfileAdapter extends RecyclerView.Adapter<AdminProfileAdapter.ProfileViewHolder> {

    /**
     * Local model representing one user's profile info.
     */
    public static class UserProfile {
        public final String id;
        public final String name;
        public final String email;
        public final String role;
        public final String phone;
        public final boolean organizerBanned;

        public UserProfile(String id, String name, String email, String role,
                           String phone, boolean organizerBanned) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.role = role;
            this.phone = phone;
            this.organizerBanned = organizerBanned;
        }
    }

    /** Listener for deleting a profile. */
    public interface OnDeleteClickListener {
        void onDelete(UserProfile profile);
    }

    /** Listener for banning/unbanning organizer mode. */
    public interface OnBanClickListener {
        void onBan(UserProfile profile);
    }

    private final List<UserProfile> profiles = new ArrayList<>();
    private final OnDeleteClickListener deleteListener;
    private final OnBanClickListener banListener;

    /**
     * Creates an adapter with initial list of profiles.
     */
    public AdminProfileAdapter(List<UserProfile> initialProfiles,
                               OnDeleteClickListener deleteListener,
                               OnBanClickListener banListener) {
        if (initialProfiles != null) profiles.addAll(initialProfiles);
        this.deleteListener = deleteListener;
        this.banListener = banListener;
    }

    /**
     * Wrapper method so tests can override notifyDataSetChanged().
     */
    protected void safeNotifyDataSetChanged() {
        notifyDataSetChanged();
    }

    /**
     * Updates displayed profiles.
     */
    public void updateProfiles(List<UserProfile> newProfiles) {
        profiles.clear();
        if (newProfiles != null) profiles.addAll(newProfiles);
        safeNotifyDataSetChanged();  // prevents test crashes
    }

    @NonNull
    @Override
    public ProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_profile, parent, false);
        return new ProfileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProfileViewHolder holder,
                                 int position) {
        UserProfile user = profiles.get(position);

        holder.name.setText(user.name);
        holder.email.setText(user.email);
        holder.role.setText("Role: " + user.role);

        if (user.phone == null || user.phone.isEmpty()) {
            holder.phone.setVisibility(View.GONE);
        } else {
            holder.phone.setVisibility(View.VISIBLE);
            holder.phone.setText("Phone: " + user.phone);
        }

        // Tap row → delete
        holder.itemView.setOnClickListener(v -> {
            if (deleteListener != null) deleteListener.onDelete(user);
        });

        // Ban/unban button
        holder.banButton.setText(user.organizerBanned ? "Unban Organizer" : "Ban Organizer");
        holder.banButton.setOnClickListener(v -> {
            if (banListener != null) banListener.onBan(user);
        });
    }

    @Override
    public int getItemCount() {
        return profiles.size();
    }

    /**
     * Holds view references for one profile row.
     */
    static class ProfileViewHolder extends RecyclerView.ViewHolder {

        TextView name, email, role, phone, banButton;

        public ProfileViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.adminProfileName);
            email = itemView.findViewById(R.id.adminProfileEmail);
            role = itemView.findViewById(R.id.adminProfileRole);
            phone = itemView.findViewById(R.id.adminProfilePhone);
            banButton = itemView.findViewById(R.id.adminBanButton);
        }
    }
}
