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

public class AdminProfileAdapter extends RecyclerView.Adapter<AdminProfileAdapter.ProfileViewHolder> {

    public static class UserProfile {
        public final String id;
        public final String name;
        public final String email;
        public final String role;
        public final String phone;

        public UserProfile(String id, String name, String email, String role, String phone) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.role = role;
            this.phone = phone;
        }
    }

    /** Callback for row tap -> delete */
    public interface OnProfileClickListener {
        void onClick(UserProfile profile);
    }

    private final List<UserProfile> profiles = new ArrayList<>();
    private OnProfileClickListener clickListener;

    public AdminProfileAdapter(List<UserProfile> initialProfiles) {
        if (initialProfiles != null) profiles.addAll(initialProfiles);
    }

    public void updateProfiles(List<UserProfile> newProfiles) {
        profiles.clear();
        if (newProfiles != null) profiles.addAll(newProfiles);
        notifyDataSetChanged();
    }

    public void setOnProfileClickListener(OnProfileClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public ProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_profile, parent, false);
        return new ProfileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProfileViewHolder holder, int position) {
        UserProfile user = profiles.get(position);

        holder.name.setText(user.name != null ? user.name : "(no name)");
        holder.email.setText(user.email);
        holder.role.setText("Role: " + user.role);

        if (user.phone == null || user.phone.isEmpty()) {
            holder.phone.setVisibility(View.GONE);
        } else {
            holder.phone.setVisibility(View.VISIBLE);
            holder.phone.setText("Phone: " + user.phone);
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onClick(user);
        });
    }

    @Override
    public int getItemCount() {
        return profiles.size();
    }

    static class ProfileViewHolder extends RecyclerView.ViewHolder {

        TextView name, email, role, phone;

        public ProfileViewHolder(@NonNull View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.adminProfileName);
            email = itemView.findViewById(R.id.adminProfileEmail);
            role = itemView.findViewById(R.id.adminProfileRole);
            phone = itemView.findViewById(R.id.adminProfilePhone);
        }
    }
}
