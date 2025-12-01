package com.example.eventlotteryapp.Admin;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventlotteryapp.R;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for displaying event images in the admin panel.
 * Each card shows a thumbnail of an event image and a delete button.
 */
public class AdminImageAdapter extends RecyclerView.Adapter<AdminImageAdapter.ViewHolder> {

    /**
     * Callback interface used to notify when an image entry is clicked.
     */
    public interface OnImageClick {
        void onClick(String eventId, String base64);
    }

    /**
     * Model class representing an event image stored in Firestore.
     */
    public static class ImageItem {
        public String eventId;
        public String base64Image;

        public ImageItem(String eventId, String base64Image) {
            this.eventId = eventId;
            this.base64Image = base64Image;
        }
    }

    private List<ImageItem> images = new ArrayList<>();
    private final OnImageClick clickListener;

    /**
     * Creates an adapter with the given image list and click callback.
     */
    public AdminImageAdapter(List<ImageItem> images, OnImageClick listener) {
        this.images = new ArrayList<>(images);
        this.clickListener = listener;
    }

    /**
     * Updates the list of images displayed.
     */
    public void updateImages(List<ImageItem> newImages) {
        this.images = new ArrayList<>(newImages);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AdminImageAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminImageAdapter.ViewHolder holder, int position) {
        ImageItem item = images.get(position);

        Bitmap bitmap = decodeBase64(item.base64Image);

        if (bitmap != null) {
            holder.image.setImageBitmap(bitmap);
        } else {
            holder.image.setImageResource(R.drawable.placeholder_image);
        }

        holder.deleteButton.setOnClickListener(v -> {
            v.setPressed(false); // fixes ripple glitch
            clickListener.onClick(item.eventId, item.base64Image);
        });
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    /**
     * Converts a Base64 string into a Bitmap for display.
     */
    private Bitmap decodeBase64(String base64) {
        try {
            if (base64 == null || base64.trim().isEmpty()) return null;

            // Remove prefix data:image/... if present
            if (base64.startsWith("data")) {
                int commaIndex = base64.indexOf(",");
                if (commaIndex != -1) {
                    base64 = base64.substring(commaIndex + 1);
                }
            }

            byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ViewHolder representing one image entry in the list.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        Button deleteButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.adminImageThumb);
            deleteButton = itemView.findViewById(R.id.adminDeleteImage);
        }
    }
}

