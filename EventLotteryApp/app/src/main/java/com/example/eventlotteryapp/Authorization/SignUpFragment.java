package com.example.eventlotteryapp.Authorization;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.eventlotteryapp.EntrantView.EntrantHomePageActivity;
import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import android.content.Intent;

/**
 * Handles new user registration.
 * Creates Firebase Auth account and stores user information in Firestore.
 */
public class SignUpFragment extends Fragment {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private EditText nameEt, emailEt, passEt, phoneEt;
    private Button signUpBtn;
    private ProgressBar progress;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_sign_up, container, false);

        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        nameEt  = v.findViewById(R.id.sign_name);
        emailEt = v.findViewById(R.id.sign_email);
        passEt  = v.findViewById(R.id.sign_password);
        phoneEt = v.findViewById(R.id.sign_phone);
        signUpBtn = v.findViewById(R.id.signup_button);
        progress  = v.findViewById(R.id.signup_progress);

        signUpBtn.setOnClickListener(view -> attemptSignUp());

        return v;
    }

    private void attemptSignUp() {
        String name  = nameEt.getText().toString().trim();
        String email = emailEt.getText().toString().trim();
        String pass  = passEt.getText().toString();
        String phone = phoneEt.getText().toString().trim();

        if (TextUtils.isEmpty(name)) { nameEt.setError("Required"); return; }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { emailEt.setError("Invalid"); return; }
        if (pass.length() < 6) { passEt.setError("Min 6 chars"); return; }

        progress.setVisibility(View.VISIBLE);
        signUpBtn.setEnabled(false);

        auth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser fUser = auth.getCurrentUser();
                        saveUserToFirestore(fUser, name, phone);
                    } else {
                        progress.setVisibility(View.GONE);
                        signUpBtn.setEnabled(true);
                        Toast.makeText(getContext(),
                                "Sign up failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestore(FirebaseUser fUser, String name, String phone) {
        if (fUser == null) return;
        User user = new User(fUser.getUid(), name, fUser.getEmail(), phone.isEmpty() ? null : phone);

        db.collection("users").document(fUser.getUid())
                .set(user)
                .addOnSuccessListener(unused -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Profile created", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(getActivity(), EntrantHomePageActivity.class));
                    requireActivity().finish();
                })
                .addOnFailureListener(e -> {
                    progress.setVisibility(View.GONE);
                    signUpBtn.setEnabled(true);
                    Toast.makeText(getContext(), "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
