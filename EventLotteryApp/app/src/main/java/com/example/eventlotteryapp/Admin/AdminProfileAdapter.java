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

    private List<UserProfile> profiles = new ArrayList<>();

    public AdminProfileAdapter(List<UserProfile> profiles) {
        this.profiles = new ArrayList<>(profiles);
    }

    public void updateProfiles(List<UserProfile> newProfiles) {
        this.profiles = new ArrayList<>(newProfiles);
        notifyDataSetChanged();
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
        UserProfile profile = profiles.get(position);
        holder.nameText.setText(profile.name);
        holder.emailText.setText(profile.email);
        holder.roleText.setText("Role: " + profile.role);
        if (profile.phone == null || profile.phone.isEmpty()) {
            holder.phoneText.setText("");
            holder.phoneText.setVisibility(View.GONE);
        } else {
            holder.phoneText.setVisibility(View.VISIBLE);
            holder.phoneText.setText("Phone: " + profile.phone);
        }
    }

    @Override
    public int getItemCount() {
        return profiles.size();
    }

    static class ProfileViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView emailText;
        TextView roleText;
        TextView phoneText;

        ProfileViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.profile_name);
            emailText = itemView.findViewById(R.id.profile_email);
            roleText = itemView.findViewById(R.id.profile_role);
            phoneText = itemView.findViewById(R.id.profile_phone);
        }
    }
}

