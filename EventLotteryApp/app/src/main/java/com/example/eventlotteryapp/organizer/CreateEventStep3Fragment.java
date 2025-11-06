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
import com.example.eventlotteryapp.databinding.FragmentCreateEventStep3Binding;
import java.util.Calendar;
import java.util.Date;

public class CreateEventStep3Fragment extends Fragment {

    private FragmentCreateEventStep3Binding binding;
    private CreateEventViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCreateEventStep3Binding.inflate(inflater, container, false);
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
        binding.registrationOpensDatePicker.setOnDateChangedListener((view1, year, monthOfYear, dayOfMonth) -> {
            viewModel.registrationOpenDate.setValue(getDateFromDatePicker(binding.registrationOpensDatePicker));
        });

        binding.registrationClosesDatePicker.setOnDateChangedListener((view1, year, monthOfYear, dayOfMonth) -> {
            viewModel.registrationCloseDate.setValue(getDateFromDatePicker(binding.registrationClosesDatePicker));
        });

        // Observe LiveData
        viewModel.registrationOpenDate.observe(getViewLifecycleOwner(), date -> {
            updateDatePicker(binding.registrationOpensDatePicker, date);
        });

        viewModel.registrationCloseDate.observe(getViewLifecycleOwner(), date -> {
            updateDatePicker(binding.registrationClosesDatePicker, date);
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
