package com.example.eventlotteryapp.EntrantView;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

import com.example.eventlotteryapp.UserSession;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

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
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            UserSession user_session = new UserSession();
            DocumentReference user_ref = UserSession.getCurrentUserRef();

            DocumentReference event_ref = db.collection("Events").document(eventId);
            event_ref.update("Waitlist", com.google.firebase.firestore.FieldValue.arrayRemove(user_ref))
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Firestore", "User removed from event waitlist");
                    })
                    .addOnFailureListener(e -> Log.e("Firestore", "Error adding user to waitlist", e));
            user_ref.update("JoinedEvents", com.google.firebase.firestore.FieldValue.arrayRemove(event_ref))
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Firestore", "Events removed from user's joined events");
                    })
                    .addOnFailureListener(e -> Log.e("Firestore", "Error removing user to waitlist", e));

            // Handle join action
            Intent intent = new Intent(getContext(), EntrantHomePageActivity.class);
            intent.putExtra("open_tab", 1); // e.g. 0 = Home, 1 = MyEvents, 2 = Notifications
            startActivity(intent);

            dismiss(); // close modal
        });

        cancelButton.setOnClickListener(v -> dismiss());
    }


}