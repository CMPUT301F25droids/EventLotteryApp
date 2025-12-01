package com.example.eventlotteryapp.Notifications;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.eventlotteryapp.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * ArrayAdapter for displaying notifications in a ListView.
 * Supports multi-selection of notifications for bulk deletion operations.
 * Displays notification icons, timestamps, messages, and event names.
 * 
 * @author Droids Team
 */
public class NotificationArrayAdapter extends ArrayAdapter<Notification> {

    /** Listener for selection change events. */
    private OnSelectionChangeListener selectionChangeListener;
    private Set<String> selectedDocumentIds = new HashSet<>(); // Use documentId instead of Notification object

    /**
     * Interface for listening to selection changes in the adapter.
     */
    public interface OnSelectionChangeListener {
        /**
         * Called when the number of selected notifications changes.
         * 
         * @param selectedCount the new number of selected notifications
         */
        void onSelectionChanged(int selectedCount);
    }

    /**
     * Constructs a new NotificationArrayAdapter.
     * 
     * @param context the Android context
     * @param notifications the list of notifications to display
     */
    public NotificationArrayAdapter(Context context, ArrayList<Notification> notifications) {
        super(context, 0, notifications);
    }

    /**
     * Sets the listener for selection change events.
     * 
     * @param listener the OnSelectionChangeListener to set
     */
    public void setOnSelectionChangeListener(OnSelectionChangeListener listener) {
        this.selectionChangeListener = listener;
    }

    /**
     * Gets a copy of the set of currently selected notifications.
     * 
     * @return a new HashSet containing the selected notifications
     */
    public Set<Notification> getSelectedNotifications() {
        Set<Notification> selected = new HashSet<>();
        for (int i = 0; i < getCount(); i++) {
            Notification notification = getItem(i);
            if (notification != null && selectedDocumentIds.contains(notification.getDocumentId())) {
                selected.add(notification);
            }
        }
        return selected;
    }

    public void clearSelection() {
        selectedDocumentIds.clear();
        notifyDataSetChanged();
        if (selectionChangeListener != null) {
            selectionChangeListener.onSelectionChanged(0);
        }
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view;
        if (convertView == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.notification_list_item, parent, false);
        } else {
            view = convertView;
        }

        Notification notification = getItem(position);
        TextView timeStamp = view.findViewById(R.id.tvTimestamp);
        TextView message = view.findViewById(R.id.tvNotificationMessage);
        TextView title = view.findViewById(R.id.tvNotificationTitle);
        ImageView icon = view.findViewById(R.id.ivNotificationIcon);
        CheckBox checkBox = view.findViewById(R.id.cbSelectNotification);

        if (notification.getType().contains("lottery")) {
            icon.setImageResource(R.drawable.ticket);
        } else {
            icon.setImageResource(R.drawable.mail_box);
        }

        timeStamp.setText(notification.getRelevantTime());
        message.setText(notification.getMessage());
        title.setText(notification.getEventName());

        // Set checkbox state - remove listener first to avoid triggering during state setting
        checkBox.setOnCheckedChangeListener(null);
        String docId = notification.getDocumentId();
        boolean isSelected = docId != null && selectedDocumentIds.contains(docId);
        checkBox.setChecked(isSelected);
        checkBox.setFocusable(false);
        checkBox.setClickable(true);

        // Set up checkbox click listener - use documentId for reliable tracking
        // Store documentId in tag for reliable retrieval during view recycling
        checkBox.setTag(docId);
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String documentId = (String) buttonView.getTag();
            if (documentId == null) {
                documentId = notification.getDocumentId(); // Fallback to current notification's ID
            }
            
            android.util.Log.d("NotificationAdapter", "Checkbox changed: isChecked=" + isChecked + ", docId=" + documentId);
            
            if (documentId != null && !documentId.isEmpty()) {
                if (isChecked) {
                    selectedDocumentIds.add(documentId);
                } else {
                    selectedDocumentIds.remove(documentId);
                }
                
                int count = selectedDocumentIds.size();
                android.util.Log.d("NotificationAdapter", "Selected count: " + count + ", listener null: " + (selectionChangeListener == null));
                
                if (selectionChangeListener != null) {
                    selectionChangeListener.onSelectionChanged(count);
                } else {
                    android.util.Log.e("NotificationAdapter", "Selection change listener is null!");
                }
            } else {
                android.util.Log.w("NotificationAdapter", "DocumentId is null or empty, cannot track selection");
            }
        });

        // Prevent item click when clicking checkbox
        checkBox.setOnClickListener(v -> {
            // This prevents the parent item click from firing
        });

        return view;
    }
}
