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

/**
 * Second step in the event creation process, collecting event date information.
 * This fragment allows the organizer to set the event start and end dates using DatePickers.
 * Data is automatically saved to the shared ViewModel as the user selects dates.
 *
 * @author Droids Team
 */
public class CreateEventStep2Fragment extends Fragment {

    private FragmentCreateEventStep2Binding binding;
    private CreateEventViewModel viewModel;

    /**
     * Called to have the fragment instantiate its user interface view.
     *
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI, or null.
     */
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

    /**
     * Converts a DatePicker's selected date into a Date object.
     *
     * @param datePicker The DatePicker to extract the date from.
     * @return A Date object representing the selected date.
     */
    private Date getDateFromDatePicker(DatePicker datePicker) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
        return calendar.getTime();
    }

    /**
     * Updates a DatePicker to display the specified date.
     *
     * @param datePicker The DatePicker to update.
     * @param date The date to display in the DatePicker, or null to leave unchanged.
     */
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
