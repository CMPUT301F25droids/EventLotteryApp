package com.example.eventlotteryapp.EntrantView;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.eventlotteryapp.R;

import java.util.Calendar;

/**
 * Custom date picker dialog for selecting dates in the Event Lottery application.
 * Provides a styled dialog with a DatePicker widget, minimum date restrictions,
 * and custom styling. The dialog is centered on screen and sized to 90% of screen width.
 * 
 * @author Droids Team
 */
public class CustomDatePickerDialog extends Dialog {
    /** The DatePicker widget for selecting dates. */
    private DatePicker datePicker;
    
    /** Listener to be notified when a date is selected. */
    private OnDateSetListener listener;
    
    /** The minimum selectable date (past dates will be disabled). */
    private Calendar minDate;

    /**
     * Interface for receiving callbacks when a date is selected in the dialog.
     */
    public interface OnDateSetListener {
        /**
         * Called when the user confirms a date selection.
         * 
         * @param year the selected year
         * @param month the selected month (0-11, where 0 is January)
         * @param dayOfMonth the selected day of the month (1-31)
         */
        void onDateSet(int year, int month, int dayOfMonth);
    }

    /**
     * Constructs a new CustomDatePickerDialog.
     * 
     * @param context the Android context for creating the dialog
     * @param listener the listener to be notified when a date is selected
     * @param initialDate the initial date to display in the picker (null to use today's date)
     * @param minDate the minimum selectable date (null to allow all past dates)
     */
    public CustomDatePickerDialog(@NonNull Context context, OnDateSetListener listener, Calendar initialDate, Calendar minDate) {
        super(context, R.style.CustomDatePickerDialog);
        this.listener = listener;
        this.minDate = minDate;
        
        // Set initial date
        if (initialDate == null) {
            initialDate = Calendar.getInstance();
        }
    }

    /**
     * Called when the dialog is created.
     * Initializes the dialog layout, configures the DatePicker widget,
     * sets up minimum date restrictions, and configures button click listeners.
     * 
     * @param savedInstanceState If the dialog is being re-initialized after previously
     *                           being shut down, this Bundle contains the data it most
     *                           recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_custom_date_picker);
        
        // Configure dialog window
        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            // Set dialog width to 90% of screen width, centered
            android.view.WindowManager.LayoutParams params = window.getAttributes();
            int screenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
            params.width = (int) (screenWidth * 0.9);
            params.height = android.view.WindowManager.LayoutParams.WRAP_CONTENT;
            params.gravity = android.view.Gravity.CENTER;
            window.setAttributes(params);
        }

        datePicker = findViewById(R.id.date_picker);
        TextView title = findViewById(R.id.dialog_title);
        Button btnCancel = findViewById(R.id.btn_cancel);
        Button btnOk = findViewById(R.id.btn_ok);

        // Set initial date
        Calendar today = Calendar.getInstance();
        datePicker.init(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH), null);

        // Set minimum date (disable past dates)
        if (minDate != null) {
            datePicker.setMinDate(minDate.getTimeInMillis());
        }

        // Apply custom styling to DatePicker
        styleDatePicker();

        btnCancel.setOnClickListener(v -> dismiss());

        btnOk.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDateSet(
                    datePicker.getYear(),
                    datePicker.getMonth(),
                    datePicker.getDayOfMonth()
                );
            }
            dismiss();
        });
    }

    /**
     * Applies custom styling to the DatePicker widget.
     * Attempts to style the internal CalendarView component, though styling options
     * are limited due to variations in DatePicker's internal structure across Android versions.
     * If styling fails, the dialog continues with the default appearance.
     */
    private void styleDatePicker() {
        // Find the CalendarView inside DatePicker and style it
        // This is a workaround since DatePicker's internal structure varies by Android version
        try {
            // Get the CalendarView
            View calendarView = datePicker.getChildAt(0);
            if (calendarView instanceof android.widget.CalendarView) {
                android.widget.CalendarView cv = (android.widget.CalendarView) calendarView;
                // Note: CalendarView styling is limited, but we can try to apply some styles
            }
        } catch (Exception e) {
            // If styling fails, continue with default appearance
        }
    }
}

