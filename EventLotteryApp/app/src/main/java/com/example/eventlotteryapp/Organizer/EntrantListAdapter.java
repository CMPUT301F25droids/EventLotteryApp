package com.example.eventlotteryapp.Organizer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.Models.User;
import java.util.List;

/**
 * Adapter for the entrant RecyclerView in EntrantListActivity.
 * Binds user data to list items.
 */
public class EntrantListAdapter extends RecyclerView.Adapter<EntrantListAdapter.Holder> {

    private final List<User> items;

    public EntrantListAdapter(List<User> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_entrant, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        User u = items.get(position);
        h.name.setText(u.getName());
        h.email.setText(u.getEmail());
        h.phone.setText(u.getPhone() == null ? "" : u.getPhone());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView name, email, phone;
        Holder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.textName);
            email = itemView.findViewById(R.id.textEmail);
            phone = itemView.findViewById(R.id.textPhone);
        }
    }
}