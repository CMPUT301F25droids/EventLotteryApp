package com.example.eventlotteryapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Settings button removed - SettingsActivity is not used
        // Users can manage notifications via ProfileFragment instead
    }
}
