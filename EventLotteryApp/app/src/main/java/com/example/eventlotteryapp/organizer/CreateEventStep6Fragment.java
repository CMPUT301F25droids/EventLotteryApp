package com.example.eventlotteryapp.organizer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.eventlotteryapp.databinding.FragmentCreateEventStep6Binding;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CreateEventStep6Fragment extends Fragment {

    private FragmentCreateEventStep6Binding binding;
    private CreateEventViewModel viewModel;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d", Locale.getDefault());
    private SimpleDateFormat dateFormatWithYear = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCreateEventStep6Binding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(CreateEventViewModel.class);

        // Set up toolbar back icon
        binding.toolbar.setNavigationOnClickListener(v -> {
            if (getActivity() instanceof CreateEventActivity) {
                ((CreateEventActivity) getActivity()).handleBackPress();
            }
        });

        // Register for activity result
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    binding.posterImageView.setImageURI(uri);
                    // Store the image URI in ViewModel
                    viewModel.posterImageUri.setValue(uri);
                    // Convert to base64 and store
                    convertImageToBase64(uri);
                }
            }
        );

        binding.editPosterButton.setOnClickListener(v -> {
            imagePickerLauncher.launch("image/*");
        });
        
        // Load existing image if available
        loadPosterImage();

        // Publish Event button - calls the activity's saveEvent method
        binding.publishEventButton.setOnClickListener(v -> {
            if (getActivity() instanceof CreateEventActivity) {
                // Disable buttons immediately to prevent multiple clicks
                setPublishButtonsEnabled(false);
                ((CreateEventActivity) getActivity()).saveEvent();
            }
        });

        // Generate QR Code button - publishes event and navigates to QR code screen
        binding.generateQrButton.setOnClickListener(v -> {
            if (getActivity() instanceof CreateEventActivity) {
                // Disable buttons immediately to prevent multiple clicks
                setPublishButtonsEnabled(false);
                CreateEventActivity activity = (CreateEventActivity) getActivity();
                activity.saveEvent(eventId -> {
                    // Navigate to Share QR Code activity with the event ID
                    Intent intent = new Intent(requireContext(), ShareQrCodeActivity.class);
                    intent.putExtra(ShareQrCodeActivity.EXTRA_EVENT_ID, eventId);
                    startActivity(intent);
                    requireActivity().finish();
                });
            }
        });

        // Observe ViewModel and update UI
        updateUI();
    }

    private void updateUI() {
        // Title
        String title = viewModel.title.getValue();
        if (title != null && !title.isEmpty()) {
            binding.eventTitleText.setText(title);
        }

        // Dates and Location
        Date eventStartDate = viewModel.eventStartDate.getValue();
        Date eventEndDate = viewModel.eventEndDate.getValue();
        String location = viewModel.location.getValue();
        
        StringBuilder datesLocation = new StringBuilder();
        if (eventStartDate != null && eventEndDate != null) {
            datesLocation.append(dateFormat.format(eventStartDate))
                        .append(" – ")
                        .append(dateFormatWithYear.format(eventEndDate));
        }
        if (location != null && !location.isEmpty()) {
            if (datesLocation.length() > 0) {
                datesLocation.append(" • ");
            }
            datesLocation.append(location);
        }
        binding.eventDatesLocationText.setText(datesLocation.toString());

        // Price
        Double price = viewModel.price.getValue();
        if (price != null) {
            binding.eventPriceText.setText(String.format("$%.0f", price));
            binding.priceText.setText(String.format("$%.0f", price));
        }

        // Description
        String description = viewModel.description.getValue();
        if (description != null && !description.isEmpty()) {
            binding.descriptionText.setText(description);
        } else {
            binding.descriptionText.setText("No description provided");
        }

        // Location
        if (location != null && !location.isEmpty()) {
            binding.locationText.setText(location);
        } else {
            binding.locationText.setText("No location provided");
        }

        // Event Dates
        if (eventStartDate != null && eventEndDate != null) {
            binding.eventDatesText.setText(dateFormat.format(eventStartDate) + " – " + dateFormatWithYear.format(eventEndDate));
        } else if (eventStartDate != null) {
            binding.eventDatesText.setText(dateFormatWithYear.format(eventStartDate));
        } else if (eventEndDate != null) {
            binding.eventDatesText.setText(dateFormatWithYear.format(eventEndDate));
        } else {
            binding.eventDatesText.setText("No dates set");
        }

        // Registration Period
        Date regOpenDate = viewModel.registrationOpenDate.getValue();
        Date regCloseDate = viewModel.registrationCloseDate.getValue();
        if (regOpenDate != null && regCloseDate != null) {
            binding.registrationPeriodText.setText(dateFormat.format(regOpenDate) + " – " + dateFormatWithYear.format(regCloseDate));
        } else if (regOpenDate != null) {
            binding.registrationPeriodText.setText("Opens: " + dateFormatWithYear.format(regOpenDate));
        } else if (regCloseDate != null) {
            binding.registrationPeriodText.setText("Closes: " + dateFormatWithYear.format(regCloseDate));
        } else {
            binding.registrationPeriodText.setText("No registration period set");
        }

        // Max Participants
        Integer maxParticipants = viewModel.maxParticipants.getValue();
        if (maxParticipants != null) {
            binding.maxParticipantsText.setText("Max Participants: " + maxParticipants);
        }

        // Waiting List
        Boolean limitWaitingList = viewModel.limitWaitingList.getValue();
        Integer waitingListSize = viewModel.waitingListSize.getValue();
        if (limitWaitingList != null && limitWaitingList && waitingListSize != null) {
            binding.waitingListText.setText("Waiting List Limit: Enabled (" + waitingListSize + " max)");
        } else {
            binding.waitingListText.setText("Waiting List Limit: Disabled");
        }

        // Geolocation
        Boolean requireGeolocation = viewModel.requireGeolocation.getValue();
        if (requireGeolocation != null && requireGeolocation) {
            binding.geolocationText.setText("Geolocation Verification: Enabled");
        } else {
            binding.geolocationText.setText("Geolocation Verification: Disabled");
        }
    }

    /**
     * Enable or disable the publish buttons
     * Called by CreateEventActivity to prevent multiple clicks while saving
     */
    public void setPublishButtonsEnabled(boolean enabled) {
        if (binding != null) {
            binding.publishEventButton.setEnabled(enabled);
            binding.generateQrButton.setEnabled(enabled);
            // Optionally change alpha to show disabled state
            binding.publishEventButton.setAlpha(enabled ? 1.0f : 0.5f);
            binding.generateQrButton.setAlpha(enabled ? 1.0f : 0.5f);
        }
    }

    private void loadPosterImage() {
        // Load existing image if available
        Uri existingUri = viewModel.posterImageUri.getValue();
        if (existingUri != null) {
            binding.posterImageView.setImageURI(existingUri);
        } else {
            // Try to load from base64 (for edit mode)
            String base64Image = viewModel.posterImageBase64.getValue();
            if (base64Image != null && !base64Image.isEmpty()) {
                loadImageFromBase64(base64Image);
            }
        }
        
        // Observe base64 image changes (for edit mode)
        viewModel.posterImageBase64.observe(getViewLifecycleOwner(), base64Image -> {
            if (base64Image != null && !base64Image.isEmpty() && viewModel.posterImageUri.getValue() == null) {
                loadImageFromBase64(base64Image);
            }
        });
    }
    
    private void loadImageFromBase64(String base64Image) {
        try {
            // Remove the "data:image/jpeg;base64," or similar prefix
            String base64Data = base64Image;
            if (base64Image.startsWith("data:image")) {
                base64Data = base64Image.substring(base64Image.indexOf(",") + 1);
            }
            
            byte[] decodedBytes = Base64.decode(base64Data, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            
            if (bitmap != null) {
                binding.posterImageView.setImageBitmap(bitmap);
            }
        } catch (Exception e) {
            android.util.Log.e("CreateEventStep6Fragment", "Error loading image from base64", e);
        }
    }
    
    private void convertImageToBase64(Uri imageUri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
            if (inputStream != null) {
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();
                
                // Compress and convert to base64
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.WEBP, 80, outputStream);
                byte[] imageBytes = outputStream.toByteArray();
                String base64String = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
                
                // Store in ViewModel with data URI prefix
                String dataUri = "data:image/webp;base64," + base64String;
                viewModel.posterImageBase64.setValue(dataUri);
            }
        } catch (Exception e) {
            android.util.Log.e("CreateEventStep6Fragment", "Error converting image to base64", e);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

