package com.example.eventlotteryapp.EntrantView;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

import com.example.eventlotteryapp.UserSession;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.databinding.FragmentJoinConfirmationListDialogBinding;
import com.google.firebase.Firebase;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

/**
 * <p>A fragment that shows a list of items as a modal bottom sheet.</p>
 * <p>You can show this modal bottom sheet from your activity like this:</p>
 * <pre>
 *     JoinConfirmationFragment.newInstance(30).show(getSupportFragmentManager(), "dialog");
 * </pre>
 */
public class JoinConfirmationFragment extends BottomSheetDialogFragment {

    // TODO: Customize parameter argument names
    private static final String ARG_EVENT_ID = "event_id";
    private String eventId;
    private FragmentJoinConfirmationListDialogBinding binding;

    // TODO: Customize parameters
    public static JoinConfirmationFragment newInstance(String eventId) {
        final JoinConfirmationFragment fragment = new JoinConfirmationFragment();
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
        return inflater.inflate(R.layout.fragment_join_confirmation_list_dialog, container, false);

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Button joinButton = view.findViewById(R.id.confirm_join_button);
        Button cancelButton = view.findViewById(R.id.cancel_join_button);


        joinButton.setOnClickListener(v -> {
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    UserSession user_session = new UserSession();
                    DocumentReference user_ref = UserSession.getCurrentUserRef();

                    DocumentReference event_ref = db.collection("Events").document(eventId);
                    event_ref.update("Waitlist", com.google.firebase.firestore.FieldValue.arrayUnion(user_ref))
                            .addOnSuccessListener(aVoid -> {
                                Log.d("Firestore", "User added to waitlist");
                            })
                            .addOnFailureListener(e -> Log.e("Firestore", "Error adding user to waitlist", e));

            // Handle join action
            dismiss(); // close modal
        });

        cancelButton.setOnClickListener(v -> dismiss());
    }


}