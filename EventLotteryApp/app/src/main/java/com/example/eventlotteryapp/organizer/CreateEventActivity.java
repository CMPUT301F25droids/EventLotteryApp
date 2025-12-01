package com.example.eventlotteryapp.organizer;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;
import com.example.eventlotteryapp.OrganizerHomePage;
import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.data.Event;
import com.example.eventlotteryapp.databinding.ActivityCreateEventBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CreateEventActivity extends AppCompatActivity {

    private ActivityCreateEventBinding binding;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private CreateEventViewModel viewModel;
    private CreateEventPagerAdapter pagerAdapter;
    private String eventId; // null for create mode, non-null for edit mode
    private boolean isSaving = false; // Flag to prevent multiple simultaneous save operations

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set navigation bar color to white to prevent pink tint
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setNavigationBarColor(getResources().getColor(R.color.white, getTheme()));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                window.getDecorView().setSystemUiVisibility(
                    window.getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                );
            }
        }
        
        binding = ActivityCreateEventBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        viewModel = new ViewModelProvider(this).get(CreateEventViewModel.class);
        
        // Check if we're in edit mode
        eventId = getIntent().getStringExtra("eventId");
        if (eventId != null && !eventId.isEmpty()) {
            setTitle("Edit Event");
            loadEventForEditing(eventId);
        } else {
            setTitle("Create Event");
        }

        pagerAdapter = new CreateEventPagerAdapter(this);
        binding.viewPager.setAdapter(pagerAdapter);
        binding.viewPager.setUserInputEnabled(false); // Disable swipe

        binding.backButton.setOnClickListener(v -> {
            int currentItem = binding.viewPager.getCurrentItem();
            if (currentItem > 0) {
                binding.viewPager.setCurrentItem(currentItem - 1);
            }
        });

        binding.nextButton.setOnClickListener(v -> {
            int currentItem = binding.viewPager.getCurrentItem();
            if (currentItem < pagerAdapter.getItemCount() - 1) {
                binding.viewPager.setCurrentItem(currentItem + 1);
            }
        });

        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updatePaginationIndicators(position);
                updateButtonLayout(position);
            }
        });
        
        // Initialize pagination indicators and button layout for first page
        updatePaginationIndicators(0);
        updateButtonLayout(0);
    }

    private void updatePaginationIndicators(int currentPosition) {
        View[] indicators = {
            binding.pageIndicator1,
            binding.pageIndicator2,
            binding.pageIndicator3,
            binding.pageIndicator4,
            binding.pageIndicator5,
            binding.pageIndicator6
        };
        
        for (int i = 0; i < indicators.length; i++) {
            if (i == currentPosition) {
                indicators[i].setBackgroundResource(R.drawable.page_indicator_active);
            } else {
                indicators[i].setBackgroundResource(R.drawable.page_indicator_inactive);
            }
        }
    }

    private void updateButtonLayout(int position) {
        // On step 6 (last step), hide navigation buttons as fragment has its own
        if (position == pagerAdapter.getItemCount() - 1) {
            binding.navigationButtons.setVisibility(View.GONE);
            binding.paginationContainer.setVisibility(View.GONE);
            return;
        } else {
            binding.navigationButtons.setVisibility(View.VISIBLE);
            binding.paginationContainer.setVisibility(View.VISIBLE);
        }

        // Handle Back button visibility - when hidden, Next fills full width
        if (position == 0) {
            binding.backButton.setVisibility(View.GONE);
            // Make Next button fill full width when Back is hidden
            android.widget.LinearLayout.LayoutParams nextParams = 
                (android.widget.LinearLayout.LayoutParams) binding.nextButton.getLayoutParams();
            nextParams.width = android.widget.LinearLayout.LayoutParams.MATCH_PARENT;
            nextParams.weight = 0;
            binding.nextButton.setLayoutParams(nextParams);
        } else {
            binding.backButton.setVisibility(View.VISIBLE);
            // Make Next button share space with Back button
            android.widget.LinearLayout.LayoutParams nextParams = 
                (android.widget.LinearLayout.LayoutParams) binding.nextButton.getLayoutParams();
            nextParams.width = 0;
            nextParams.weight = 1;
            binding.nextButton.setLayoutParams(nextParams);
        }

        // Handle Next button text and action
        binding.nextButton.setText("Next");
        binding.nextButton.setOnClickListener(v -> {
            int currentItem = binding.viewPager.getCurrentItem();
            if (currentItem < pagerAdapter.getItemCount() - 1) {
                binding.viewPager.setCurrentItem(currentItem + 1);
            }
        });
    }

    @Override
    public void onBackPressed() {
        handleBackPress();
    }

    public void handleBackPress() {
        if (hasEnteredData()) {
            showExitConfirmationDialog();
        } else {
            finish();
        }
    }

    private boolean hasEnteredData() {
        String title = viewModel.title.getValue();
        String description = viewModel.description.getValue();
        String location = viewModel.location.getValue();
        Double price = viewModel.price.getValue();
        Date eventStartDate = viewModel.eventStartDate.getValue();
        Date eventEndDate = viewModel.eventEndDate.getValue();
        Date registrationOpenDate = viewModel.registrationOpenDate.getValue();
        Date registrationCloseDate = viewModel.registrationCloseDate.getValue();
        Integer maxParticipants = viewModel.maxParticipants.getValue();
        Integer waitingListSize = viewModel.waitingListSize.getValue();
        Boolean requireGeolocation = viewModel.requireGeolocation.getValue();
        Boolean limitWaitingList = viewModel.limitWaitingList.getValue();

        // Check if any field has been filled
        return (title != null && !title.trim().isEmpty()) ||
               (description != null && !description.trim().isEmpty()) ||
               (location != null && !location.trim().isEmpty()) ||
               (price != null && price > 0.0) ||
               eventStartDate != null ||
               eventEndDate != null ||
               registrationOpenDate != null ||
               registrationCloseDate != null ||
               (maxParticipants != null && maxParticipants > 0) ||
               (waitingListSize != null && waitingListSize > 0) ||
               (requireGeolocation != null && requireGeolocation) ||
               (limitWaitingList != null && limitWaitingList);
    }

    private void showExitConfirmationDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Exit Event Creation?")
            .setMessage("You have entered some information. Are you sure you want to go back? Your progress will be lost.")
            .setPositiveButton("Go Back", (dialog, which) -> {
                finish();
            })
            .setNegativeButton("Cancel", (dialog, which) -> {
                // Do nothing, stay on current page
            })
            .show();
    }

    public void saveEvent() {
        saveEvent(null);
    }

    public void saveEvent(OnEventSavedListener listener) {
        // Prevent multiple simultaneous save operations
        if (isSaving) {
            android.util.Log.d("CreateEventActivity", "Save already in progress, ignoring duplicate click");
            return;
        }
        
        isSaving = true;
        disablePublishButtons();
        
        String title = viewModel.title.getValue();
        String description = viewModel.description.getValue();
        String location = viewModel.location.getValue();
        Double price = viewModel.price.getValue();
        Date eventStartDate = viewModel.eventStartDate.getValue();
        Date eventEndDate = viewModel.eventEndDate.getValue();
        Date registrationOpenDate = viewModel.registrationOpenDate.getValue();
        Date registrationCloseDate = viewModel.registrationCloseDate.getValue();
        Integer maxParticipants = viewModel.maxParticipants.getValue();


        if (title == null || title.isEmpty() ||
            description == null || description.isEmpty() ||
            location == null || location.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields on the first page", Toast.LENGTH_SHORT).show();
            binding.viewPager.setCurrentItem(0);
            isSaving = false;
            enablePublishButtons();
            return;
        }

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "You must be logged in to create events. Please log in first.", Toast.LENGTH_LONG).show();
            android.util.Log.e("CreateEventActivity", "Cannot save event: User is not authenticated");
            isSaving = false;
            enablePublishButtons();
            return;
        }
        
        String organizerId = auth.getCurrentUser().getUid();
        Integer waitingListSize = viewModel.waitingListSize.getValue();
        Boolean requireGeolocation = viewModel.requireGeolocation.getValue();
        Boolean limitWaitingList = viewModel.limitWaitingList.getValue();
        String posterImageBase64 = viewModel.posterImageBase64.getValue();
        
        android.util.Log.d("CreateEventActivity", "Saving event with organizerId: " + organizerId);
        android.util.Log.d("CreateEventActivity", "Event title: " + title);
        
        // Use Map to ensure Firestore serialization works correctly
        Map<String, Object> eventData = new HashMap<>();
        
        // OLD SYSTEM FIELDS (to match existing Events collection structure)
        eventData.put("Name", title);  // Old field name
        eventData.put("Cost", price != null ? String.format("$%.2f", price) : "$0.00");  // Old field name, formatted as string
        eventData.put("Organizer", firestore.collection("users").document(organizerId));  // Old field name, as DocumentReference
        if (posterImageBase64 != null && !posterImageBase64.isEmpty()) {
            eventData.put("Image", posterImageBase64);  // Old field name, base64 string
        }
        
        // Waitlist structure (old system format - array)
        eventData.put("Waitlist", new ArrayList<String>());  // Initialize with empty array
        
        // NEW SYSTEM FIELDS (additional fields for new functionality)
        eventData.put("title", title);  // Keep for backward compatibility
        eventData.put("description", description);
        eventData.put("location", location);
        eventData.put("price", price != null ? price : 0.0);  // Keep numeric price
        // organizerId removed - using Organizer DocumentReference instead
        
        // Event dates
        eventData.put("eventStartDate", eventStartDate);
        eventData.put("eventEndDate", eventEndDate);
        eventData.put("registrationOpenDate", registrationOpenDate);
        eventData.put("registrationCloseDate", registrationCloseDate);
        
        // Participant settings
        eventData.put("maxParticipants", maxParticipants != null ? maxParticipants : 0);
        eventData.put("waitingListSize", waitingListSize != null ? waitingListSize : 0);
        eventData.put("limitWaitingList", limitWaitingList != null ? limitWaitingList : false);
        
        // Event settings
        eventData.put("requireGeolocation", requireGeolocation != null ? requireGeolocation : false);
        
        // Only initialize empty lists for new events, preserve existing lists for edits
        if (eventId == null) {
        eventData.put("waitingListEntrantIds", new ArrayList<String>());
        eventData.put("selectedEntrantIds", new ArrayList<String>());
        eventData.put("cancelledEntrantIds", new ArrayList<String>());
            // createdAt will be set by Firestore server timestamp for new events
        eventData.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
        }
        // For edit mode, we don't overwrite existing lists or createdAt

        if (eventId != null) {
            // Update existing event
            firestore.collection("Events").document(eventId)
                    .update(eventData)
                    .addOnSuccessListener(aVoid -> {
                        android.util.Log.d("CreateEventActivity", "Event updated successfully with ID: " + eventId);
                        isSaving = false;
                        enablePublishButtons();
                        Toast.makeText(this, "Event updated successfully", Toast.LENGTH_SHORT).show();
                        
                        if (listener != null) {
                            listener.onEventSaved(eventId);
                        } else {
                            finish();
                        }
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("CreateEventActivity", "Error updating event", e);
                        isSaving = false;
                        enablePublishButtons();
                        Toast.makeText(this, "Error updating event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Create new event
        firestore.collection("Events")
                .add(eventData)
                .addOnSuccessListener(documentReference -> {
                        String newEventId = documentReference.getId();
                        android.util.Log.d("CreateEventActivity", "Event saved successfully with ID: " + newEventId);
                    android.util.Log.d("CreateEventActivity", "Event organizerId: " + organizerId);
                    
                    // Verify the saved event
                        firestore.collection("Events").document(newEventId).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String savedOrganizerId = documentSnapshot.getString("organizerId");
                                android.util.Log.d("CreateEventActivity", "Verified saved event - organizerId in Firestore: " + savedOrganizerId);
                            }
                        });
                    
                    isSaving = false;
                    enablePublishButtons();
                    Toast.makeText(this, "Event created successfully", Toast.LENGTH_SHORT).show();
                    
                    if (listener != null) {
                            listener.onEventSaved(newEventId);
                    } else {
                        // Use finish() to return to previous activity instead of recreating it
                        // This ensures the fragment's onResume() is called
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("CreateEventActivity", "Error creating event", e);
                    isSaving = false;
                    enablePublishButtons();
                    Toast.makeText(this, "Error creating event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void loadEventForEditing(String eventId) {
        firestore.collection("Events").document(eventId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (!documentSnapshot.exists()) {
                    android.util.Log.e("CreateEventActivity", "Event not found for editing: " + eventId);
                    Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                
                // Populate ViewModel with event data
                viewModel.title.setValue(documentSnapshot.getString("title") != null ? 
                    documentSnapshot.getString("title") : documentSnapshot.getString("Name"));
                viewModel.description.setValue(documentSnapshot.getString("description"));
                viewModel.location.setValue(documentSnapshot.getString("location"));
                
                // Price
                Double price = documentSnapshot.getDouble("price");
                if (price == null) {
                    String costStr = documentSnapshot.getString("Cost");
                    if (costStr != null && costStr.startsWith("$")) {
                        try {
                            price = Double.parseDouble(costStr.substring(1));
                        } catch (NumberFormatException e) {
                            price = 0.0;
                        }
                    } else {
                        price = 0.0;
                    }
                }
                viewModel.price.setValue(price);
                
                // Dates
                viewModel.eventStartDate.setValue(documentSnapshot.getDate("eventStartDate"));
                viewModel.eventEndDate.setValue(documentSnapshot.getDate("eventEndDate"));
                viewModel.registrationOpenDate.setValue(documentSnapshot.getDate("registrationOpenDate"));
                viewModel.registrationCloseDate.setValue(documentSnapshot.getDate("registrationCloseDate"));
                
                // Participant settings
                Long maxParticipantsLong = documentSnapshot.getLong("maxParticipants");
                viewModel.maxParticipants.setValue(maxParticipantsLong != null ? maxParticipantsLong.intValue() : 0);
                
                Long waitingListSizeLong = documentSnapshot.getLong("waitingListSize");
                viewModel.waitingListSize.setValue(waitingListSizeLong != null ? waitingListSizeLong.intValue() : 0);
                
                // Settings
                Boolean requireGeolocation = documentSnapshot.getBoolean("requireGeolocation");
                viewModel.requireGeolocation.setValue(requireGeolocation != null ? requireGeolocation : false);
                
                Boolean limitWaitingList = documentSnapshot.getBoolean("limitWaitingList");
                viewModel.limitWaitingList.setValue(limitWaitingList != null ? limitWaitingList : false);
                
                // Image
                String posterImageBase64 = documentSnapshot.getString("Image");
                if (posterImageBase64 != null && !posterImageBase64.isEmpty()) {
                    viewModel.posterImageBase64.setValue(posterImageBase64);
                    // Note: posterImageUri will be set by Step5Fragment when it observes posterImageBase64
                }
                
                android.util.Log.d("CreateEventActivity", "Event data loaded for editing");
            })
            .addOnFailureListener(e -> {
                android.util.Log.e("CreateEventActivity", "Error loading event for editing", e);
                Toast.makeText(this, "Error loading event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
                });
    }

    /**
     * Disable publish buttons to prevent multiple clicks while saving
     */
    private void disablePublishButtons() {
        // Find the Step6Fragment and disable buttons
        Fragment step6Fragment = findStep6Fragment();
        if (step6Fragment instanceof CreateEventStep6Fragment) {
            ((CreateEventStep6Fragment) step6Fragment).setPublishButtonsEnabled(false);
        }
    }

    /**
     * Enable publish buttons after save operation completes
     */
    private void enablePublishButtons() {
        // Find the Step6Fragment and enable buttons
        Fragment step6Fragment = findStep6Fragment();
        if (step6Fragment instanceof CreateEventStep6Fragment) {
            ((CreateEventStep6Fragment) step6Fragment).setPublishButtonsEnabled(true);
        }
    }

    /**
     * Find the Step6Fragment from the ViewPager
     */
    private Fragment findStep6Fragment() {
        // ViewPager2 creates fragments with tags: "android:switcher:" + viewPagerId + ":" + position
        int viewPagerId = binding.viewPager.getId();
        int step6Position = pagerAdapter.getItemCount() - 1;
        String fragmentTag = "android:switcher:" + viewPagerId + ":" + step6Position;
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(fragmentTag);
        
        // Fallback: try to find by iterating through fragments
        if (fragment == null) {
            for (Fragment f : getSupportFragmentManager().getFragments()) {
                if (f instanceof CreateEventStep6Fragment) {
                    return f;
                }
            }
        }
        return fragment;
    }

    public interface OnEventSavedListener {
        void onEventSaved(String eventId);
    }
}
