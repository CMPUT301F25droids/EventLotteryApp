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

import com.example.eventlotteryapp.Admin.AdminHomeActivity;
import com.example.eventlotteryapp.EntrantView.EntrantHomePageActivity;
import com.example.eventlotteryapp.OrganizerHomePage;
import com.example.eventlotteryapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

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

        if (auth.getCurrentUser() != null) {
            fetchRoleAndRedirect(auth.getCurrentUser());
        }

        emailEditText = view.findViewById(R.id.editTextTextEmailAddress2);
        passwordEditText = view.findViewById(R.id.editTextTextPassword);
        loginButton = view.findViewById(R.id.login_button);

        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                signInAnonymously();
            } else {
                signInWithEmailPassword(email, password);
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
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(getContext(), "Password reset link sent!", Toast.LENGTH_LONG).show()
                    )
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show()
                    );
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
                            redirectUser("entrant");
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
                            fetchRoleAndRedirect(user);
                        }
                    } else {
                        Toast.makeText(getContext(), "Login failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchRoleAndRedirect(FirebaseUser user) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String role = doc.getString("role");
                    redirectUser(role);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to load role", Toast.LENGTH_SHORT).show()
                );
    }

    private void redirectUser(String role) {
        if ("admin".equals(role)) {
            startActivity(new Intent(getActivity(), AdminHomeActivity.class));
        } else if ("organizer".equals(role)) {
            startActivity(new Intent(getActivity(), OrganizerHomePage.class));
        } else {
            startActivity(new Intent(getActivity(), EntrantHomePageActivity.class));
        }

        requireActivity().finish();
    }
}