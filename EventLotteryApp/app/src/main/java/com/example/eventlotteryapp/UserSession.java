package com.example.eventlotteryapp;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Manages the current user session for the application.
 * Provides access to the current user's Firestore document reference and ID.
 * This is a temporary implementation using a hardcoded user ID for development.
 * 
 * @author Droids Team
 */
public class UserSession {
    /** 
     * Default user ID used for development and testing.
     * Change this to match your existing user document ID in Firestore.
     * Sample User: Will Jones
     */
    private static final String DEFAULT_USER_ID = "S2GNk2GqOYEziT2PL3mx";

    /**
     * Gets the Firestore DocumentReference for the current user.
     * 
     * @return DocumentReference pointing to the current user's document in the "users" collection
     */
    public static DocumentReference getCurrentUserRef() {
        return FirebaseFirestore.getInstance()
                .collection("users")
                .document(DEFAULT_USER_ID);
    }

    /**
     * Gets the current user's unique identifier.
     * 
     * @return the user ID string
     */
    public static String getCurrentUserId() {
        return DEFAULT_USER_ID;
    }
}
