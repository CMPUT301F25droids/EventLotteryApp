package com.example.eventlotteryapp.EntrantView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class QrCaptureActivity extends com.journeyapps.barcodescanner.CaptureActivity {
    
    private static final String TAG = "QrCaptureActivity";
    private ImageView backButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Add back button after layout is complete
        View decorView = getWindow().getDecorView();
        decorView.post(() -> {
            decorView.postDelayed(() -> {
                addBackButtonOverlay();
            }, 500);
        });
    }
    
    private void addBackButtonOverlay() {
        try {
            // Remove existing button if any
            if (backButton != null && backButton.getParent() != null) {
                ((ViewGroup) backButton.getParent()).removeView(backButton);
            }
            
            // Get the decor view - this is the absolute root
            ViewGroup decorView = (ViewGroup) getWindow().getDecorView();
            
            // Create back button
            backButton = new ImageView(this);
            backButton.setImageResource(com.example.eventlotteryapp.R.drawable.ic_arrow_back);
            backButton.setContentDescription("Back");
            backButton.setClickable(true);
            backButton.setFocusable(true);
            backButton.setPadding(16, 16, 16, 16);
            
            // Set white color for visibility
            backButton.setColorFilter(android.graphics.Color.WHITE);
            
            // Create circular semi-transparent background
            android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
            background.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            background.setColor(0xFF000000); // Fully opaque black for maximum visibility
            backButton.setBackground(background);
            
            backButton.setOnClickListener(v -> {
                Log.d(TAG, "Back button clicked");
                finish();
            });
            
            // Add directly to decor view with absolute positioning
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                72,
                72
            );
            params.setMargins(32, 32, 0, 0);
            params.gravity = android.view.Gravity.TOP | android.view.Gravity.START;
            backButton.setLayoutParams(params);
            
            // Ensure it's on top with very high elevation
            backButton.setElevation(1000f);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                backButton.setZ(1000f);
            }
            
            // Add to decor view and bring to front
            decorView.addView(backButton);
            decorView.bringChildToFront(backButton);
            backButton.bringToFront();
            
            Log.d(TAG, "Back button added to decor view, elevation: " + backButton.getElevation());
        } catch (Exception e) {
            Log.e(TAG, "Error adding back button", e);
        }
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}

