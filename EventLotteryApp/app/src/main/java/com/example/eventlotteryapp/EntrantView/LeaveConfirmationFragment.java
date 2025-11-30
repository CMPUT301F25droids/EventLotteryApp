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
import android.widget.Toast;

import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.databinding.FragmentJoinConfirmationListDialogBinding;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

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
            
            // Remove user from waiting list
            event_ref.update("waitingListEntrantIds", com.google.firebase.firestore.FieldValue.arrayRemove(userId))
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Firestore", "User removed from waiting list");
                    })
                    .addOnFailureListener(e -> Log.e("Firestore", "Error removing user from waiting list", e));
            
            // Remove from user's JoinedEvents (if user document exists)
            user_ref.update("JoinedEvents", com.google.firebase.firestore.FieldValue.arrayRemove(event_ref))
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Firestore", "Event removed from user's JoinedEvents");
                    })
                    .addOnFailureListener(e -> {
                        // It's okay if user document doesn't exist - that's expected for some users
                        Log.d("Firestore", "Could not remove from JoinedEvents (user doc may not exist): " + e.getMessage());
                    });

            Toast.makeText(requireContext(), "Left waiting list", Toast.LENGTH_SHORT).show();
            dismiss(); // close modal
            
            // Navigate back and refresh - the snapshot listener will pick up the changes
            if (getActivity() != null) {
                getActivity().finish();
            }
        });

        cancelButton.setOnClickListener(v -> dismiss());
    }


}