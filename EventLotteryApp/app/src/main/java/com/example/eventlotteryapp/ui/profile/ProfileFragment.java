package com.example.eventlotteryapp.ui.profile;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.eventlotteryapp.Authorization.AuthActivity;
import com.example.eventlotteryapp.EntrantView.EntrantHomePageActivity;
import com.example.eventlotteryapp.OrganizerHomePage;
import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.databinding.FragmentProfileBinding;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Fragment for managing user profile settings and information.
 * Allows users to view and edit their profile information, change notification preferences,
 * switch between entrant and organizer roles, and delete their account.
 * Handles comprehensive account deletion including cleanup of all related data.
 * 
 * @author Droids Team
 */
public class ProfileFragment extends Fragment {

    /** View binding for this fragment's layout. */
    private FragmentProfileBinding binding;
    
    /** Firestore database instance for saving profile changes. */
    private FirebaseFirestore firestore;
    
    /** Firebase Authentication instance for managing user session. */
    private FirebaseAuth auth;
    
    /** Flag indicating whether the current user is an organizer. */
    private boolean isOrganizer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Detect current role from activity
        if (getActivity() instanceof OrganizerHomePage) {
            isOrganizer = true;
            binding.roleToggleGroup.check(R.id.organizer_button);
        } else {
            isOrganizer = false;
            binding.roleToggleGroup.check(R.id.entrant_button);
        }

        setupRoleToggle();
        setupSpinners();
        setupButtons();
        loadUserData();
    }

    private void setupRoleToggle() {
        firestore.collection("users")
                .document(auth.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    Boolean banned = snapshot.getBoolean("organizerModeBanned");

                    if (banned != null && banned) {
                        binding.roleToggleGroup.check(R.id.entrant_button);
                        binding.roleToggleGroup.setEnabled(false);
                        isOrganizer = false;
                        binding.bannedOrganizerViewMsg.setVisibility(View.VISIBLE);
                    } else {
                        binding.roleToggleGroup.setEnabled(true);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("TAG", "Error getting document", e);
                });
        binding.roleToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.organizer_button) {
                    isOrganizer = true;
                    // Switch to organizer home page
                    Intent intent = new Intent(getActivity(), OrganizerHomePage.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                } else {
                    isOrganizer = false;
                    // Switch to entrant home page
                    Intent intent = new Intent(getActivity(), EntrantHomePageActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                }
            }
            firestore.collection("users").document(auth.getUid())
                    .update("role", isOrganizer ? "organizer" : "entrant");

        });
    }

    private void setupSpinners() {
        // Allow Notifications Spinner
        ArrayAdapter<CharSequence> notificationsAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.notification_options,
            android.R.layout.simple_spinner_item
        );
        notificationsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.allowNotificationsSpinner.setAdapter(notificationsAdapter);
    }

    private void setupButtons() {
        binding.logoutButton.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(requireActivity(), AuthActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });

        binding.saveChangesButton.setOnClickListener(v -> {
            saveProfileChanges();
        });

        binding.deleteAccountButton.setOnClickListener(v -> {
            showDeleteAccountDialog();
        });
    }

    private void loadUserData() {
        if (auth.getCurrentUser() == null) {
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        firestore.collection("users").document(uid).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        // Load user data
                        binding.fullNameEdit.setText(document.getString("name"));
                        binding.emailEdit.setText(document.getString("email"));
                        binding.phoneEdit.setText(document.getString("phone"));

                        binding.profileName.setText(document.getString("name"));
                        binding.profileEmail.setText(document.getString("email"));
                        if (document.getString("phone") != null && !document.getString("phone").isEmpty()) {
                            binding.profilePhone.setText(document.getString("phone") + " Phone");
                        }


                        if (document.getBoolean("notificationPreference") != null) {
                            boolean allow_notification = document.getBoolean("notificationPreference");
                            binding.allowNotificationsSpinner.setSelection(allow_notification ? 0 : 1);
                        } else {
                            binding.allowNotificationsSpinner.setSelection(0); // default is allow notifications
                        }

                        // Optionally, detect role if you store it in the user document
//                        String role = document.getString("role");
//                        if ("organizer".equals(role)) {
//                            isOrganizer = true;
//                            binding.roleToggleGroup.check(R.id.organizer_button);
//                        } else {
//                            isOrganizer = false;
//                            binding.roleToggleGroup.check(R.id.entrant_button);
//                        }
                    } else {
                        // User document doesn't exist - redirect to signup
                        Toast.makeText(getContext(), "Account not found. Please sign up first.", Toast.LENGTH_LONG).show();
                        auth.signOut();
                        Intent intent = new Intent(requireActivity(), AuthActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        if (getActivity() != null) {
                            getActivity().finish();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error loading user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveProfileChanges() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        String name = binding.fullNameEdit.getText().toString();
        String email = binding.emailEdit.getText().toString();
        String phone = binding.phoneEdit.getText().toString();
        boolean valid_name = validateName(name);
        boolean valid_email = validateEmail(email);
        boolean valid_phone = validatePhone(phone);

        if (!valid_name || !valid_email || !valid_phone) {
            return;
        }
        boolean allow_notification = binding.allowNotificationsSpinner.getSelectedItemPosition() == 0;

        // Update)
        String collection = isOrganizer ? "organizers" : "entrants";

        firestore.collection("users").document(uid).update(
            "name", name,
            "email", email,
            "phone", phone,
            "notificationPreference", allow_notification
        ).addOnSuccessListener(aVoid -> {
            // Update display fields
            binding.profileName.setText(name);
            binding.profileEmail.setText(email);
            if (phone != null && !phone.isEmpty()) {
                binding.profilePhone.setText(phone + " Phone");
            }
            Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Error updating profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(requireContext())
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> {
                deleteAccount();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deleteAccount() {
        if (auth.getCurrentUser() == null) {
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        // Delete all user-related data first
        deleteAllNotifications(uid);
        deleteJoinLocationsFromAllEvents(uid);
        cleanEntrantFromAllEvents(uid);
        deleteEventsOwnedByOrganizer(uid);

        // Delete user document
        firestore.collection("users").document(uid).delete()
            .addOnSuccessListener(aVoid -> {
                // Delete Firebase Auth user
                auth.getCurrentUser().delete()
                    .addOnSuccessListener(aVoid1 -> {
                        Toast.makeText(getContext(), "Account deleted", Toast.LENGTH_SHORT).show();
                        // Navigate to login
                        Intent intent = new Intent(requireActivity(), AuthActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error deleting account: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Error deleting account: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });

    }

    private boolean validateName(String name) {
        if (name == null || name.isEmpty()) {
            Toast.makeText(getContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
        // Add more validation as needed
    }

    private boolean validateEmail(String email) {
        if (!email.contains("@") || !email.contains(".")) {
            Toast.makeText(getContext(), "Invalid email format", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private boolean validatePhone(String phone) {
        if (phone != null && !phone.isEmpty() && !phone.matches("\\d+")) {
            Toast.makeText(getContext(), "Invalid phone number format", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;

    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    /**
     * Removes user from all event arrays and old Waitlist field
     */
    private void cleanEntrantFromAllEvents(String uid) {
        DocumentReference userRef = firestore.collection("users").document(uid);
        
        firestore.collection("Events").get()
                .addOnSuccessListener(query -> {
                    for (DocumentSnapshot event : query.getDocuments()) {
                        DocumentReference eventRef = event.getReference();
                        
                        // Remove from all entrant ID arrays
                        eventRef.update(
                                "waitingListEntrantIds", FieldValue.arrayRemove(uid),
                                "selectedEntrantIds", FieldValue.arrayRemove(uid),
                                "acceptedEntrantIds", FieldValue.arrayRemove(uid),
                                "declinedEntrantIds", FieldValue.arrayRemove(uid),
                                "cancelledEntrantIds", FieldValue.arrayRemove(uid),
                                "Waitlist", FieldValue.arrayRemove(userRef) // Old system with DocumentReference
                        ).addOnFailureListener(e -> {
                            Log.e("ProfileFragment", "Error removing user from event: " + event.getId(), e);
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ProfileFragment", "Error fetching events for cleanup", e);
                });
    }
    /**
     * Deletes all events owned by the organizer
     */
    private void deleteEventsOwnedByOrganizer(String organizerUid) {
        DocumentReference organizerRef = firestore.collection("users").document(organizerUid);

        firestore.collection("Events")
                .whereEqualTo("Organizer", organizerRef)
                .get()
                .addOnSuccessListener(query -> {
                    for (DocumentSnapshot event : query.getDocuments()) {
                        event.getReference().delete()
                                .addOnFailureListener(e -> {
                                    Log.e("ProfileFragment", "Error deleting event: " + event.getId(), e);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ProfileFragment", "Error fetching organizer events", e);
                });
    }

    /**
     * Deletes all notifications for the user
     */
    private void deleteAllNotifications(String uid) {
        firestore.collection("Notifications")
                .whereEqualTo("UserId", uid)
                .get()
                .addOnSuccessListener(query -> {
                    for (DocumentSnapshot notification : query.getDocuments()) {
                        notification.getReference().delete()
                                .addOnFailureListener(e -> {
                                    Log.e("ProfileFragment", "Error deleting notification: " + notification.getId(), e);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ProfileFragment", "Error fetching notifications for cleanup", e);
                });
    }

    /**
     * Deletes joinLocations subcollection from all events for the user
     */
    private void deleteJoinLocationsFromAllEvents(String uid) {
        firestore.collection("Events").get()
                .addOnSuccessListener(query -> {
                    for (DocumentSnapshot event : query.getDocuments()) {
                        DocumentReference eventRef = event.getReference();
                        // Delete the user's location document from joinLocations subcollection
                        eventRef.collection("joinLocations").document(uid).delete()
                                .addOnFailureListener(e -> {
                                    // It's okay if the document doesn't exist
                                    Log.d("ProfileFragment", "Could not delete joinLocation (may not exist): " + event.getId());
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ProfileFragment", "Error fetching events for joinLocations cleanup", e);
                });
    }



}
