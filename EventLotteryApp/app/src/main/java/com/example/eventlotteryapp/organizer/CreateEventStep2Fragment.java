package com.example.eventlotteryapp.organizer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.eventlotteryapp.databinding.FragmentCreateEventStep2Binding;
import java.util.Calendar;
import java.util.Date;

public class CreateEventStep2Fragment extends Fragment {

    private FragmentCreateEventStep2Binding binding;
    private CreateEventViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCreateEventStep2Binding.inflate(inflater, container, false);
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
        binding.eventStartDatePicker.setOnDateChangedListener((view1, year, monthOfYear, dayOfMonth) -> {
            viewModel.eventStartDate.setValue(getDateFromDatePicker(binding.eventStartDatePicker));
        });

        binding.eventEndDatePicker.setOnDateChangedListener((view1, year, monthOfYear, dayOfMonth) -> {
            viewModel.eventEndDate.setValue(getDateFromDatePicker(binding.eventEndDatePicker));
        });

        // Observe LiveData
        viewModel.eventStartDate.observe(getViewLifecycleOwner(), date -> {
            updateDatePicker(binding.eventStartDatePicker, date);
        });

        viewModel.eventEndDate.observe(getViewLifecycleOwner(), date -> {
            updateDatePicker(binding.eventEndDatePicker, date);
        });
    }

    private Date getDateFromDatePicker(DatePicker datePicker) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
        return calendar.getTime();
    }

    private void updateDatePicker(DatePicker datePicker, Date date) {
        if (date != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            datePicker.updateDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
