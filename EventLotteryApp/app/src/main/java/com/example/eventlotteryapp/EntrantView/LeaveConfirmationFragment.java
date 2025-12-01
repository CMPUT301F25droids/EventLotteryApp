package com.example.eventlotteryapp.EntrantView;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.databinding.FragmentJoinConfirmationListDialogBinding;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import java.util.Date;
import java.util.List;

/**
 * <p>A fragment that shows a list of items as a modal bottom sheet.</p>
 * <p>You can show this modal bottom sheet from your activity like this:</p>
 * <pre>
 *     LeaveConfirmationFragment.newInstance(30).show(getSupportFragmentManager(), "dialog");
 * </pre>
 */
public class LeaveConfirmationFragment extends BottomSheetDialogFragment {

    // TODO: Customize parameter argument names
    private static final String ARG_EVENT_ID = "event_id";
    private String eventId;
    private FragmentJoinConfirmationListDialogBinding binding;

    // TODO: Customize parameters
    public static LeaveConfirmationFragment newInstance(String eventId) {
        final LeaveConfirmationFragment fragment = new LeaveConfirmationFragment();
        final Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        this.eventId = getArguments().getString(ARG_EVENT_ID);
        return inflater.inflate(R.layout.fragment_leave_confirmation, container, false);

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Button leaveButton = view.findViewById(R.id.confirm_join_button);
        Button cancelButton = view.findViewById(R.id.cancel_join_button);
        TextView messageView = view.findViewById(R.id.join_confirm_message);

        // Fetch event name and update message dynamically
        if (eventId != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("Events").document(eventId)
                .get()
                .addOnSuccessListener(eventDoc -> {
                    if (eventDoc.exists()) {
                        String eventName = eventDoc.getString("Name");
                        if (eventName != null && !eventName.isEmpty()) {
                            String message = "You're about to leave the waiting list for " + eventName + ".\n\n" +
                                    "Once you leave, your waitlist spot may be taken by others.";
                            messageView.setText(message);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error fetching event name", e);
                });
        }

        leaveButton.setOnClickListener(v -> {
            // Check if user is authenticated
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() == null) {
                Toast.makeText(requireContext(), "Please log in to leave events", Toast.LENGTH_SHORT).show();
                dismiss();
                return;
            }

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            String userId = auth.getCurrentUser().getUid();
            DocumentReference user_ref = db.collection("users").document(userId);
            DocumentReference event_ref = db.collection("Events").document(eventId);
            
            // Check if lottery has run and user was selected before allowing leave
            event_ref.get()
                    .addOnSuccessListener(eventDoc -> {
                        if (eventDoc.exists()) {
                            Date registrationCloseDate = eventDoc.getDate("registrationCloseDate");
                            Date now = new Date();
                            boolean isEventClosed = (registrationCloseDate != null && now.after(registrationCloseDate));
                            
                            List<String> selectedEntrantIds = (List<String>) eventDoc.get("selectedEntrantIds");
                            boolean isSelected = (selectedEntrantIds != null && selectedEntrantIds.contains(userId));
                            
                            // Block leave if lottery has run and user was selected
                            if (isEventClosed && isSelected) {
                                Toast.makeText(requireContext(), "Cannot leave waitlist. The lottery has run and you were selected.", Toast.LENGTH_SHORT).show();
                                dismiss();
                                return;
                            }
                            
                            // Remove user from waiting list and also from declined list if they're in it
                            // Use a batch update to ensure both operations succeed together
                            WriteBatch batch = db.batch();
                            batch.update(event_ref, "waitingListEntrantIds", com.google.firebase.firestore.FieldValue.arrayRemove(userId));
                            batch.update(event_ref, "declinedEntrantIds", com.google.firebase.firestore.FieldValue.arrayRemove(userId));
                            
                            batch.commit()
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("Firestore", "User removed from waiting list and declined list");
                                        
                                        Toast.makeText(requireContext(), "Left waiting list", Toast.LENGTH_SHORT).show();
                                        dismiss(); // close modal
                                        
                                        // Refresh the parent activity UI after the update completes
                                        // Add a small delay to ensure Firestore cache is updated
                                        if (getActivity() != null && getActivity() instanceof EventDetailsActivity) {
                                            EventDetailsActivity activity = (EventDetailsActivity) getActivity();
                                            // Post a delayed refresh to allow Firestore to propagate the change
                                            activity.getWindow().getDecorView().postDelayed(() -> {
                                                activity.refreshWaitingListStatus();
                                            }, 1000); // 1 second delay to ensure cache update
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("Firestore", "Error removing user from waiting list", e);
                                        Toast.makeText(requireContext(), "Error leaving waiting list. Please try again.", Toast.LENGTH_SHORT).show();
                                    });
                            
                            // Remove from user's JoinedEvents (if user document exists)
                            user_ref.update("JoinedEvents", com.google.firebase.firestore.FieldValue.arrayRemove(event_ref))
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("Firestore", "Event removed from user's JoinedEvents");
                                    })
                                    .addOnFailureListener(e -> {
                                        // It's okay if user document doesn't exist - that's expected for some users
                                        Log.d("Firestore", "Could not remove from JoinedEvents (user doc may not exist): " + e.getMessage());
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firestore", "Error checking event status", e);
                        Toast.makeText(requireContext(), "Error checking event status. Please try again.", Toast.LENGTH_SHORT).show();
                        dismiss();
                    });
        });

        cancelButton.setOnClickListener(v -> dismiss());
    }


}