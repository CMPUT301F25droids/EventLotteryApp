package com.example.eventlotteryapp.ui.profile;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
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
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
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
        binding.roleToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            firestore.collection("users").document(auth.getUid())
                    .update("role", isOrganizer ? "organizer" : "entrant");

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

        // Lottery Results Spinner
        ArrayAdapter<CharSequence> lotteryAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.notification_options,
            android.R.layout.simple_spinner_item
        );
        lotteryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.lotteryResultsSpinner.setAdapter(lotteryAdapter);
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
                        Toast.makeText(getContext(), "User data not found", Toast.LENGTH_SHORT).show();
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

        String collection = isOrganizer ? "organizers" : "entrants";

        firestore.collection("users").document(uid).update(
            "name", name,
            "email", email,
            "phone", phone
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
        String collection = isOrganizer ? "organizers" : "entrants";
        
        firestore.collection(collection).document(uid).delete()
            .addOnSuccessListener(aVoid -> {
                auth.getCurrentUser().delete()
                    .addOnSuccessListener(aVoid1 -> {
                        Toast.makeText(getContext(), "Account deleted", Toast.LENGTH_SHORT).show();
                        // Navigate to login
                        requireActivity().finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error deleting account: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Error deleting account: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
