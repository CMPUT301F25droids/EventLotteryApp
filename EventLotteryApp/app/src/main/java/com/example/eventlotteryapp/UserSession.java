package com.example.eventlotteryapp;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserSession {
    // Change this to match your existing user document ID in Firestore
    private static final String DEFAULT_USER_ID = "S2GNk2GqOYEziT2PL3mx"; // Sample User: Will Jones

    public static DocumentReference getCurrentUserRef() {
        return FirebaseFirestore.getInstance()
                .collection("users")
                .document(DEFAULT_USER_ID);
    }

    public static String getCurrentUserId() {
        return DEFAULT_USER_ID;
    }
}
