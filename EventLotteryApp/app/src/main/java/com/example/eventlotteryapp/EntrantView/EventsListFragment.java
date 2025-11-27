package com.example.eventlotteryapp.EntrantView;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.eventlotteryapp.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A fragment representing a list of Items. Load the event from the database
 * and display it in a RecyclerView.
 */
public class EventsListFragment extends Fragment {

    private static final String ARG_COLUMN_COUNT = "column-count";
    private int mColumnCount = 1;
    private EventsListRecyclerViewAdapter adapter; // Adapter is now a fragment field

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public EventsListFragment() {
    }

    /**
     * Factory method to create a new instance of
     * this fragment using the provided parameters.
     */
    public static EventsListFragment newInstance(int columnCount) {
        EventsListFragment fragment = new EventsListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Called to do initial creation of a fragment.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_events_list, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.events_list);
        EditText searchBar = view.findViewById(R.id.search_bar);
        ProgressBar progressBar = view.findViewById(R.id.loading_indicator);

        // Initialize adapter with empty list and assign to fragment field
        List<EventItem> eventList = new ArrayList<>();
        adapter = setupRecyclerView(recyclerView, eventList);

        setupSearchBar(searchBar, adapter);

        loadEventsFromFirestore(adapter, progressBar);
        setupFilters(view, adapter);

        return view;
    }

    /**
     * Setup RecyclerView with GridLayoutManager and Adapter
     */
    private EventsListRecyclerViewAdapter setupRecyclerView(RecyclerView recyclerView, List<EventItem> eventList) {
        recyclerView.setHasFixedSize(true);
        int spanCount = getResources().getConfiguration().smallestScreenWidthDp >= 600 ? 3 : 2;
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), spanCount);
        recyclerView.setLayoutManager(gridLayoutManager);

        adapter = new EventsListRecyclerViewAdapter(eventList, position -> {
            EventItem clickedEvent = adapter.getFilteredList().get(position);
            Intent intent = new Intent(getContext(), EventDetailsActivity.class);
            intent.putExtra("eventId", clickedEvent.getId());
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);
        return adapter;
    }

    /**
     * Setup EditText to hide keyboard and clear focus on Done
     */
    @SuppressLint("ClickableViewAccessibility")
    private void setupSearchBar(EditText searchBar, EventsListRecyclerViewAdapter adapter) {
        searchBar.setSingleLine(true);
        searchBar.setImeOptions(EditorInfo.IME_ACTION_DONE);
        // Hide keyboard on Done
        searchBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                InputMethodManager imm = (InputMethodManager) getActivity()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(searchBar.getWindowToken(), 0);
                searchBar.clearFocus();
                return true;
            }
            return false;
        });

        // Real-time search
        searchBar.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) { }
        });

        searchBar.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                int drawableRight = 2; // index for RIGHT drawable
                int paddingRight = searchBar.getPaddingRight();
                if (event.getRawX() >= (searchBar.getRight() - searchBar.getCompoundDrawables()[drawableRight].getBounds().width() - paddingRight)) {
                    // CLICKED the calendar icon

                    openDatePicker(adapter);

                    return true;
                }
            }
            return false;
        });

    }

    /**
     * Load events from Firestore and update adapter
     */
    private void loadEventsFromFirestore(EventsListRecyclerViewAdapter adapter, ProgressBar progressBar) {
        progressBar.setVisibility(View.VISIBLE);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Events")
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    progressBar.setVisibility(View.GONE);

                    if (e != null) {
                        Log.e("Firestore", "Error loading events", e);
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        List<EventItem> newEvents = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            EventItem event = doc.toObject(EventItem.class);
                            event.setId(doc.getId());
                            newEvents.add(event);
                        }
                        adapter.updateList(newEvents);
                    }
                });
    }
    private void setupFilters(View root, EventsListRecyclerViewAdapter adapter) {
        MaterialButton allBtn = root.findViewById(R.id.all_filter);
        MaterialButton sportsBtn = root.findViewById(R.id.sports_filter);
        MaterialButton musicBtn = root.findViewById(R.id.music_filter);
        MaterialButton workshopsBtn = root.findViewById(R.id.workshops_filter);
        MaterialButton freeBtn = root.findViewById(R.id.free_filter);
        MaterialButton communityBtn = root.findViewById(R.id.community_filter);
        
        android.widget.HorizontalScrollView scrollView = root.findViewById(R.id.filter_scroller);

        List<MaterialButton> buttons = List.of(allBtn, sportsBtn, musicBtn, workshopsBtn, freeBtn, communityBtn);

        View.OnClickListener listener = v -> {
            MaterialButton selected = (MaterialButton) v;

            // update adapter
            String category = (String) selected.getTag(); // we'll set tags in XML
            adapter.applyCategoryFilter(category);

            // update button styles
            updateFilterButtonStyles(buttons, selected);
            
            // Center the selected button in the scroll view
            centerButtonInScrollView(scrollView, selected);
        };

        allBtn.setOnClickListener(listener);
        sportsBtn.setOnClickListener(listener);
        musicBtn.setOnClickListener(listener);
        workshopsBtn.setOnClickListener(listener);
        freeBtn.setOnClickListener(listener);
        communityBtn.setOnClickListener(listener);

        // default selection
        updateFilterButtonStyles(buttons, allBtn);
    }
    private void updateFilterButtonStyles(List<MaterialButton> buttons, MaterialButton selected) {
        int selectedBgColor = getResources().getColor(R.color.filter_selected_bg);
        int selectedTextColor = getResources().getColor(R.color.filter_selected_text);
        int unselectedBgColor = getResources().getColor(R.color.filter_unselected_bg);
        int unselectedTextColor = getResources().getColor(R.color.filter_unselected_text);
        
        int animationDuration = 250; // milliseconds
        
        for (MaterialButton btn : buttons) {
            // Get current colors - handle transparent/initial state
            ColorStateList currentBgTint = btn.getBackgroundTintList();
            int currentBgColor = unselectedBgColor; // Default starting point
            if (currentBgTint != null) {
                int color = currentBgTint.getDefaultColor();
                // If not transparent, use the actual color; otherwise start from unselected
                if ((color & 0xFF000000) != 0) { // Check if not fully transparent
                    currentBgColor = color;
                }
            }
            
            int currentTextColor = btn.getCurrentTextColor();
            // If text color appears to be default (likely black), use unselected color as baseline
            if (currentTextColor == 0xFF000000 || currentTextColor == 0) {
                currentTextColor = unselectedTextColor;
            }
            
            // Determine target colors
            int targetBgColor = (btn == selected) ? selectedBgColor : unselectedBgColor;
            int targetTextColor = (btn == selected) ? selectedTextColor : unselectedTextColor;
            
            // Skip animation if already at target colors
            if (currentBgColor == targetBgColor && currentTextColor == targetTextColor) {
                continue;
            }
            
            // Animate background color
            ValueAnimator bgAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), currentBgColor, targetBgColor);
            bgAnimator.setDuration(animationDuration);
            bgAnimator.addUpdateListener(animator -> {
                int color = (int) animator.getAnimatedValue();
                btn.setBackgroundTintList(ColorStateList.valueOf(color));
            });
            bgAnimator.start();
            
            // Animate text color
            ValueAnimator textAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), currentTextColor, targetTextColor);
            textAnimator.setDuration(animationDuration);
            textAnimator.addUpdateListener(animator -> {
                int color = (int) animator.getAnimatedValue();
                btn.setTextColor(color);
            });
            textAnimator.start();
        }
    }

    private void openDatePicker(EventsListRecyclerViewAdapter adapter) {
        // Get today's date
        Calendar today = Calendar.getInstance();
        int year = today.get(Calendar.YEAR);
        int month = today.get(Calendar.MONTH);
        int day = today.get(Calendar.DAY_OF_MONTH);


        DatePickerDialog dp = new DatePickerDialog(
                requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    adapter.applyDateFilter(selectedYear, selectedMonth, selectedDay);
                },
                year, month, day // <-- opens to today's date
        );
        // Title
        // Create a centered title
        TextView title = new TextView(requireContext());
        title.setText("Filter by Event Start Date");
        title.setPadding(20, 20, 20, 20);
        title.setTextSize(20);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, Typeface.BOLD);

        dp.setCustomTitle(title);


        // Disable past dates
        dp.getDatePicker().setMinDate(today.getTimeInMillis());

        dp.show();
    }

    private void centerButtonInScrollView(android.widget.HorizontalScrollView scrollView, MaterialButton button) {
        // Post to ensure layout is complete
        scrollView.post(() -> {
            int scrollViewWidth = scrollView.getWidth();
            int buttonLeft = button.getLeft();
            int buttonWidth = button.getWidth();
            int buttonCenter = buttonLeft + (buttonWidth / 2);
            int scrollViewCenter = scrollViewWidth / 2;
            int targetScrollX = buttonCenter - scrollViewCenter;
            
            // Smooth scroll to center the button
            scrollView.smoothScrollTo(targetScrollX, 0);
        });
    }


}
