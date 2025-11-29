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

public class CustomDatePickerDialog extends Dialog {
    private DatePicker datePicker;
    private OnDateSetListener listener;
    private Calendar minDate;

    public interface OnDateSetListener {
        void onDateSet(int year, int month, int dayOfMonth);
    }

    public CustomDatePickerDialog(@NonNull Context context, OnDateSetListener listener, Calendar initialDate, Calendar minDate) {
        super(context, R.style.CustomDatePickerDialog);
        this.listener = listener;
        this.minDate = minDate;
        
        // Set initial date
        if (initialDate == null) {
            initialDate = Calendar.getInstance();
        }
    }

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

