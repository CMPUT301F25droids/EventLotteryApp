package com.example.eventlotteryapp.organizer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
                    // TODO: Store the image URI in ViewModel
                }
            }
        );

        binding.editPosterButton.setOnClickListener(v -> {
            imagePickerLauncher.launch("image/*");
        });

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
        } else {
            binding.eventDatesText.setText("No dates set");
        }

        // Registration Period
        Date regOpenDate = viewModel.registrationOpenDate.getValue();
        Date regCloseDate = viewModel.registrationCloseDate.getValue();
        if (regOpenDate != null && regCloseDate != null) {
            binding.registrationPeriodText.setText(dateFormat.format(regOpenDate) + " – " + dateFormatWithYear.format(regCloseDate));
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

