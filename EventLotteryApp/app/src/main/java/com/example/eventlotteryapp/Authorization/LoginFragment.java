package com.example.eventlotteryapp.Authorization;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.eventlotteryapp.EntrantView.EntrantHomePageActivity;
import com.example.eventlotteryapp.OrganizerHomePage;
import com.example.eventlotteryapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Handles user login functionality.
 * Navigates to EntrantHomePage after authenticating successfully.
 */
public class LoginFragment extends Fragment {

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);
        
        auth = FirebaseAuth.getInstance();

        // auto login
        if (auth.getCurrentUser() != null) {
            navigateToHome();
        }

        emailEditText = view.findViewById(R.id.editTextTextEmailAddress2);
        passwordEditText = view.findViewById(R.id.editTextTextPassword);
        loginButton = view.findViewById(R.id.login_button);
        
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString();

                if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                    // For testing: use anonymous authentication if fields are empty
                    signInAnonymously();
                } else {
                    // Try email/password authentication
                    signInWithEmailPassword(email, password);
                }
            }
        });
        TextView forgotPassword = view.findViewById(R.id.forgot_password);

        forgotPassword.setOnClickListener(v -> showResetPasswordDialog());

        return view;
    }
    private void showResetPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Reset Password");

        final EditText input = new EditText(requireContext());
        input.setHint("Enter your email");
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        builder.setView(input);

        builder.setPositiveButton("Send Reset Link", (dialog, which) -> {
            String email = input.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(getContext(), "Email is required", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Password reset link sent!", Toast.LENGTH_LONG).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }


    private void signInAnonymously() {
        auth.signInAnonymously()
            .addOnCompleteListener(requireActivity(), task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = auth.getCurrentUser();
                    if (user != null) {
                        Toast.makeText(getContext(), "Signed in anonymously for testing", Toast.LENGTH_SHORT).show();
                        navigateToHome();
                    }
                } else {
                    Toast.makeText(getContext(), "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    private void signInWithEmailPassword(String email, String password) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity(), task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = auth.getCurrentUser();
                    if (user != null) {
                        Toast.makeText(getContext(), "Signed in successfully", Toast.LENGTH_SHORT).show();
                        navigateToHome();
                    }
                } else {
                    Toast.makeText(getContext(), "Login failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    private void navigateToHome() {
        // Navigate to EntrantHomePageActivity (the correct entrant home page)
        Intent intent = new Intent(getActivity(), EntrantHomePageActivity.class);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}