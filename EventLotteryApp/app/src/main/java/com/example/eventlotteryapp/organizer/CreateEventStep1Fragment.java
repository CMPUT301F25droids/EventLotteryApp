package com.example.eventlotteryapp.organizer;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.eventlotteryapp.databinding.FragmentCreateEventStep1Binding;

public class CreateEventStep1Fragment extends Fragment {

    private FragmentCreateEventStep1Binding binding;
    private CreateEventViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCreateEventStep1Binding.inflate(inflater, container, false);
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

        // Set up listeners
        binding.eventTitleEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.title.setValue(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.eventDescriptionEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.description.setValue(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.eventLocationEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.location.setValue(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.eventPriceEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = s.toString().trim();
                if (text.isEmpty()) {
                    // Don't update ViewModel while user is typing - prevents cursor reset
                    return;
                }
                try {
                    double value = Double.parseDouble(text);
                    // Only update if the value actually changed to prevent feedback loops
                    Double currentValue = viewModel.price.getValue();
                    if (currentValue == null || Math.abs(currentValue - value) > 0.001) {
                        viewModel.price.setValue(value);
                    }
                } catch (NumberFormatException e) {
                    // Invalid input, but don't set to 0.0 as user might still be typing
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Handle empty field when user finishes editing
        binding.eventPriceEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                // Field lost focus - validate and update ViewModel if empty
                String text = binding.eventPriceEditText.getText().toString().trim();
                if (text.isEmpty()) {
                    viewModel.price.setValue(0.0);
                } else {
                    try {
                        double value = Double.parseDouble(text);
                        viewModel.price.setValue(value);
                    } catch (NumberFormatException e) {
                        // Invalid number - set to 0.0
                        viewModel.price.setValue(0.0);
                    }
                }
            }
        });

        // Observe LiveData
        viewModel.title.observe(getViewLifecycleOwner(), s -> {
            if (!binding.eventTitleEditText.getText().toString().equals(s)) {
                binding.eventTitleEditText.setText(s);
            }
        });

        viewModel.description.observe(getViewLifecycleOwner(), s -> {
            if (!binding.eventDescriptionEditText.getText().toString().equals(s)) {
                binding.eventDescriptionEditText.setText(s);
            }
        });

        viewModel.location.observe(getViewLifecycleOwner(), s -> {
            if (!binding.eventLocationEditText.getText().toString().equals(s)) {
                binding.eventLocationEditText.setText(s);
            }
        });

        viewModel.price.observe(getViewLifecycleOwner(), aDouble -> {
            // Don't update text if user is currently typing in the field to prevent cursor reset
            if (binding.eventPriceEditText.hasFocus()) {
                return;
            }
            
            // Only set text if price is not 0.0, otherwise keep it empty to show hint
            if (aDouble != null && aDouble != 0.0) {
                String priceString = formatPriceString(aDouble);
                String currentText = binding.eventPriceEditText.getText().toString();
                
                // Only update if the text is actually different (ignore formatting differences)
                if (!currentText.equals(priceString)) {
                    try {
                        double currentValue = currentText.isEmpty() ? 0.0 : Double.parseDouble(currentText);
                        // Only update if the numeric value is actually different
                        if (Math.abs(currentValue - aDouble) > 0.001) {
                            binding.eventPriceEditText.setText(priceString);
                        }
                    } catch (NumberFormatException e) {
                        binding.eventPriceEditText.setText(priceString);
                    }
                }
            } else if (!binding.eventPriceEditText.getText().toString().isEmpty() && 
                       binding.eventPriceEditText.getText().toString().equals("0.0")) {
                binding.eventPriceEditText.setText("");
            }
        });
    }

    /**
     * Format price as string, removing unnecessary decimal points and zeros
     * e.g., 1.0 -> "1", 1.5 -> "1.5", 10.00 -> "10"
     */
    private String formatPriceString(double price) {
        if (price == (long) price) {
            return String.valueOf((long) price);
        } else {
            return String.valueOf(price);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
