package com.example.eventlotteryapp.EntrantView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.databinding.FragmentJoinConfirmationListDialogBinding;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;

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
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private String eventId;
    private FragmentJoinConfirmationListDialogBinding binding;
    private FusedLocationProviderClient fusedLocationClient;

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
        TextView messageView = view.findViewById(R.id.join_confirm_message);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        // Fetch event name and update message dynamically
        if (eventId != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("Events").document(eventId)
                .get()
                .addOnSuccessListener(eventDoc -> {
                    if (eventDoc.exists()) {
                        String eventName = eventDoc.getString("Name");
                        if (eventName != null && !eventName.isEmpty()) {
                            String message = "You're about to join the waiting list for " + eventName + ". " +
                                    "You'll be notified if you're selected in the lottery.\n\n" +
                                    "You can leave the waiting list anytime before registration closes.";
                            messageView.setText(message);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error fetching event name", e);
                });
        }

        joinButton.setOnClickListener(v -> {
            // Check if event requires geolocation
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("Events").document(eventId)
                .get()
                .addOnSuccessListener(eventDoc -> {
                    Boolean requireGeolocation = eventDoc.getBoolean("requireGeolocation");
                    boolean needsLocation = (requireGeolocation != null && requireGeolocation);
                    
                    if (needsLocation) {
                        // Geolocation is required - must capture location
                        requestLocationAndJoin();
                    } else {
                        // Geolocation is optional - try to get location but don't require it
                        tryToGetLocationAndJoin();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error checking geolocation requirement", e);
                    // Join without location if we can't check
                    joinEvent(null, null);
                });
        });

        cancelButton.setOnClickListener(v -> dismiss());
    }
    
    private void requestLocationAndJoin() {
        // Check permissions
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        
        // Get location (required)
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        joinEvent(location.getLatitude(), location.getLongitude());
                    } else {
                        // Location not available - required, so show error
                        Toast.makeText(requireContext(), "Location is required but not available. Please enable location services.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Location", "Error getting location", e);
                    Toast.makeText(requireContext(), "Could not get location. Location is required for this event.", Toast.LENGTH_LONG).show();
                });
        } else {
            Toast.makeText(requireContext(), "Location permission is required for this event.", Toast.LENGTH_LONG).show();
        }
    }
    
    private void tryToGetLocationAndJoin() {
        // Check permissions
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted - join without location (it's optional)
            joinEvent(null, null);
            return;
        }
        
        // Try to get location (optional)
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        // Got location - save it
                        joinEvent(location.getLatitude(), location.getLongitude());
                    } else {
                        // Location not available - that's okay, it's optional
                        joinEvent(null, null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Location", "Error getting location", e);
                    // Failed to get location - that's okay, it's optional
                    joinEvent(null, null);
                });
        } else {
            // No permission - that's okay, it's optional
            joinEvent(null, null);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocationAndJoin();
            } else {
                Toast.makeText(requireContext(), "Location permission denied. Joining without location.", Toast.LENGTH_SHORT).show();
                joinEvent(null, null);
            }
        }
    }
    
    private void joinEvent(Double latitude, Double longitude) {
        // Check if user is authenticated
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Please log in to join events", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = auth.getCurrentUser().getUid();
        DocumentReference user_ref = db.collection("users").document(userId);
        DocumentReference event_ref = db.collection("Events").document(eventId);
        
        // Check if event is closed before allowing join
        event_ref.get()
                .addOnSuccessListener(eventDoc -> {
                    if (eventDoc.exists()) {
                        Date registrationCloseDate = eventDoc.getDate("registrationCloseDate");
                        Date now = new Date();
                        boolean isEventClosed = (registrationCloseDate != null && now.after(registrationCloseDate));
                        
                        if (isEventClosed) {
                            Toast.makeText(requireContext(), "Registration for this event is closed.", Toast.LENGTH_SHORT).show();
                            dismiss();
                            return;
                        }
                        
                        // Check waiting list limit
                        Boolean limitWaitingList = eventDoc.getBoolean("limitWaitingList");
                        boolean isLimitEnabled = (limitWaitingList != null && limitWaitingList);
                        
                        if (isLimitEnabled) {
                            Long waitingListSizeLong = eventDoc.getLong("waitingListSize");
                            int waitingListLimit = (waitingListSizeLong != null) ? waitingListSizeLong.intValue() : 0;
                            
                            // If limit is 0 or not set, treat as infinite (no limit)
                            if (waitingListLimit > 0) {
                                List<String> waitingListEntrantIds = (List<String>) eventDoc.get("waitingListEntrantIds");
                                int currentWaitingListSize = (waitingListEntrantIds != null) ? waitingListEntrantIds.size() : 0;
                                
                                // Check if user is already in the waiting list
                                boolean userAlreadyInList = (waitingListEntrantIds != null && waitingListEntrantIds.contains(userId));
                                
                                if (!userAlreadyInList && currentWaitingListSize >= waitingListLimit) {
                                    Toast.makeText(requireContext(), "The waiting list is full. Maximum " + waitingListLimit + " entrants allowed.", Toast.LENGTH_LONG).show();
                                    dismiss();
                                    return;
                                }
                            }
                        }
                        
                        // Add user to waiting list
                        event_ref.update("waitingListEntrantIds", com.google.firebase.firestore.FieldValue.arrayUnion(userId))
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("Firestore", "User added to waiting list");
                                })
                                .addOnFailureListener(e -> Log.e("Firestore", "Error adding user to waiting list", e));
                        
                        // Update user's joined events
                        user_ref.update("JoinedEvents", com.google.firebase.firestore.FieldValue.arrayUnion(event_ref))
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("Firestore", "Events added to users joined events");
                                })
                                .addOnFailureListener(e -> Log.e("Firestore", "Error adding user to waitlist", e));

                        // Store location if available
                        if (latitude != null && longitude != null) {
                            Map<String, Object> locationData = new HashMap<>();
                            locationData.put("latitude", latitude);
                            locationData.put("longitude", longitude);
                            locationData.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());
                            
                            db.collection("Events").document(eventId)
                                .collection("joinLocations").document(userId)
                                .set(locationData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("Firestore", "Location saved for user");
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("Firestore", "Error saving location", e);
                                });
                        }

                        // Handle join action
                        Intent intent = new Intent(getContext(), EntrantHomePageActivity.class);
                        intent.putExtra("open_tab", 1); // e.g. 0 = Home, 1 = MyEvents, 2 = Notifications
                        startActivity(intent);

                        dismiss(); // close modal
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error checking event status", e);
                    Toast.makeText(requireContext(), "Error checking event status. Please try again.", Toast.LENGTH_SHORT).show();
                    dismiss();
                });
    }


}