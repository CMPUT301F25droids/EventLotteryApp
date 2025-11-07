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
                try {
                    viewModel.price.setValue(Double.parseDouble(s.toString()));
                } catch (NumberFormatException e) {
                    viewModel.price.setValue(0.0);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
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
            // Only set text if price is not 0.0, otherwise keep it empty to show hint
            if (aDouble != null && aDouble != 0.0) {
                String priceString = String.valueOf(aDouble);
                if (!binding.eventPriceEditText.getText().toString().equals(priceString)) {
                    binding.eventPriceEditText.setText(priceString);
                }
            } else if (binding.eventPriceEditText.getText().toString().equals("0.0")) {
                binding.eventPriceEditText.setText("");
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
