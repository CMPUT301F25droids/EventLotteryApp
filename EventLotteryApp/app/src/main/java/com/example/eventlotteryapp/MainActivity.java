package com.example.eventlotteryapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Main entry point activity for the Event Lottery application.
 * Provides a simple interface with a button to navigate to the settings screen.
 * 
 * @author Droids Team
 */
public class MainActivity extends AppCompatActivity {

    /** Button that opens the settings activity when clicked. */
    private Button openSettingsButton;

    /**
     * Called when the activity is first created.
     * Initializes the layout and sets up the settings button click listener.
     * 
     * @param savedInstanceState If the activity is being re-initialized after previously
     *                           being shut down, this Bundle contains the data it most
     *                           recently supplied in onSaveInstanceState(Bundle).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        openSettingsButton = findViewById(R.id.openSettingsButton);
        openSettingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }
}
