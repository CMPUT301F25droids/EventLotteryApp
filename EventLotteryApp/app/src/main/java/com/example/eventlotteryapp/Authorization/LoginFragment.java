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

/**
 * Fragment for user login functionality.
 * Handles user authentication via Firebase Auth and redirects users to the appropriate
 * home page based on their role (entrant, organizer, or admin).
 * 
 * @author Droids Team
 */
public class LoginFragment extends Fragment {

    /** EditText field for entering email address. */
    private EditText emailEditText;
    
    /** EditText field for entering password. */
    private EditText passwordEditText;
    
    /** Button to trigger the login process. */
    private Button loginButton;
    
    /** Firebase Authentication instance. */
    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        auth = FirebaseAuth.getInstance();

        // Check if user is already logged in - auto-redirect immediately
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            // Fetch role and redirect in one call
            fetchRoleAndRedirect(currentUser);
        }

        emailEditText = view.findViewById(R.id.editTextTextEmailAddress2);
        passwordEditText = view.findViewById(R.id.editTextTextPassword);
        loginButton = view.findViewById(R.id.login_button);

        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(getContext(), "Please enter your email and password", Toast.LENGTH_SHORT).show();
                return;
            }
            
            signInWithEmailPassword(email, password);
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

        // Try cache first for faster response, then fallback to server
        db.collection("users")
                .document(user.getUid())
                .get(com.google.firebase.firestore.Source.CACHE)
                .addOnSuccessListener(doc -> {
                    handleUserDocument(doc, true);
                })
                .addOnFailureListener(e -> {
                    // Cache miss or error - try server
                    db.collection("users")
                            .document(user.getUid())
                            .get(com.google.firebase.firestore.Source.SERVER)
                            .addOnSuccessListener(doc -> {
                                handleUserDocument(doc, false);
                            })
                            .addOnFailureListener(e2 -> {
                                // Server also failed - sign out
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Failed to load user data: " + e2.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                                auth.signOut();
                            });
                });
    }
    
    private void handleUserDocument(com.google.firebase.firestore.DocumentSnapshot doc, boolean fromCache) {
        if (!doc.exists()) {
            // User document doesn't exist - they need to create an account first
            if (getContext() != null) {
                Toast.makeText(getContext(), "Account not found. Please sign up first.", Toast.LENGTH_LONG).show();
            }
            auth.signOut();
            
            // Switch to signup tab/fragment
            if (getActivity() instanceof AuthActivity) {
                ((AuthActivity) getActivity()).switchToSignUp();
            }
            return;
        }
        
        String role = doc.getString("role");
        if (role == null || role.isEmpty()) {
            // Role not set - default to entrant
            role = "entrant";
        }
        redirectUser(role);
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