package com.example.eventlotteryapp.organizer;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.databinding.FragmentCreateEventStep4Binding;
import com.example.eventlotteryapp.databinding.NumberStepperBinding;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class CreateEventStep4Fragment extends Fragment {

    private FragmentCreateEventStep4Binding binding;
    private CreateEventViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCreateEventStep4Binding.inflate(inflater, container, false);
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

        setupMaxParticipantsStepper();
        setupWaitingListStepper();
        setupGeolocationToggle();
        setupWaitingListToggle();
    }

    private void setupMaxParticipantsStepper() {
        NumberStepperBinding stepperBinding = binding.maxParticipantsStepper;
        stepperBinding.labelText.setText("Maximum Participants");
        EditText valueText = stepperBinding.valueText;

        stepperBinding.decreaseButton.setOnClickListener(v -> {
            Integer current = viewModel.maxParticipants.getValue();
            if (current != null && current > 0) {
                viewModel.maxParticipants.setValue(current - 1);
            }
        });

        stepperBinding.increaseButton.setOnClickListener(v -> {
            Integer current = viewModel.maxParticipants.getValue();
            viewModel.maxParticipants.setValue((current != null ? current : 0) + 1);
        });

        // Handle direct text input
        valueText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = s.toString();
                if (!text.isEmpty()) {
                    try {
                        int value = Integer.parseInt(text);
                        if (value >= 0) {
                            viewModel.maxParticipants.setValue(value);
                        }
                    } catch (NumberFormatException e) {
                        // Invalid input, ignore
                    }
                } else {
                    viewModel.maxParticipants.setValue(0);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        viewModel.maxParticipants.observe(getViewLifecycleOwner(), value -> {
            if (value != null) {
                String currentText = valueText.getText().toString();
                String newText = String.valueOf(value);
                if (!currentText.equals(newText)) {
                    valueText.setText(newText);
                    valueText.setSelection(newText.length());
                }
                // Enable/disable decrease button based on value
                stepperBinding.decreaseButton.setEnabled(value > 0);
            }
        });
    }

    private void setupWaitingListStepper() {
        NumberStepperBinding stepperBinding = binding.waitingListStepper;
        stepperBinding.labelText.setText("Waiting List Size");
        EditText valueText = stepperBinding.valueText;

        stepperBinding.decreaseButton.setOnClickListener(v -> {
            Integer current = viewModel.waitingListSize.getValue();
            if (current != null && current > 0) {
                viewModel.waitingListSize.setValue(current - 1);
            }
        });

        stepperBinding.increaseButton.setOnClickListener(v -> {
            Integer current = viewModel.waitingListSize.getValue();
            viewModel.waitingListSize.setValue((current != null ? current : 0) + 1);
        });

        // Handle direct text input
        valueText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = s.toString();
                if (!text.isEmpty()) {
                    try {
                        int value = Integer.parseInt(text);
                        if (value >= 0) {
                            viewModel.waitingListSize.setValue(value);
                        }
                    } catch (NumberFormatException e) {
                        // Invalid input, ignore
                    }
                } else {
                    viewModel.waitingListSize.setValue(0);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        viewModel.waitingListSize.observe(getViewLifecycleOwner(), value -> {
            if (value != null) {
                String currentText = valueText.getText().toString();
                String newText = String.valueOf(value);
                if (!currentText.equals(newText)) {
                    valueText.setText(newText);
                    valueText.setSelection(newText.length());
                }
                // Enable/disable decrease button based on value
                stepperBinding.decreaseButton.setEnabled(value > 0);
            }
        });
    }

    private void setupGeolocationToggle() {
        SwitchMaterial geolocationSwitch = binding.geolocationSwitch;
        
        geolocationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.requireGeolocation.setValue(isChecked);
        });

        viewModel.requireGeolocation.observe(getViewLifecycleOwner(), isChecked -> {
            if (isChecked != null && geolocationSwitch.isChecked() != isChecked) {
                geolocationSwitch.setChecked(isChecked);
            }
        });

        // Make the container clickable to toggle the switch
        binding.geolocationToggleContainer.setOnClickListener(v -> {
            geolocationSwitch.setChecked(!geolocationSwitch.isChecked());
        });
    }

    private void setupWaitingListToggle() {
        SwitchMaterial waitingListSwitch = binding.waitingListSwitch;
        
        waitingListSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.limitWaitingList.setValue(isChecked);
            // Show/hide waiting list stepper based on toggle
            binding.waitingListStepper.getRoot().setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        viewModel.limitWaitingList.observe(getViewLifecycleOwner(), isChecked -> {
            if (isChecked != null) {
                if (waitingListSwitch.isChecked() != isChecked) {
                    waitingListSwitch.setChecked(isChecked);
                }
                binding.waitingListStepper.getRoot().setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });

        // Make the container clickable to toggle the switch
        binding.waitingListToggleContainer.setOnClickListener(v -> {
            waitingListSwitch.setChecked(!waitingListSwitch.isChecked());
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
