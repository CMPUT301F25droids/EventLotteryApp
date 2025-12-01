package com.example.eventlotteryapp.EntrantView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * Custom capture activity for QR code scanning.
 * Extends the ZXing CaptureActivity and adds a custom back button overlay
 * for better user experience. The back button is added programmatically
 * to the decor view with high elevation to ensure visibility.
 * 
 * @author Droids Team
 */
public class QrCaptureActivity extends com.journeyapps.barcodescanner.CaptureActivity {
    
    /** Tag for logging purposes. */
    private static final String TAG = "QrCaptureActivity";
    
    /** The back button overlay added to the scanner view. */
    private ImageView backButton;
    
    /**
     * Called when the activity is first created.
     * 
     * @param savedInstanceState If the activity is being re-initialized after previously
     *                           being shut down, this Bundle contains the data it most
     *                           recently supplied in onSaveInstanceState(Bundle).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    /**
     * Called when the activity is resumed.
     * Adds a custom back button overlay to the scanner view after a short delay
     * to ensure the layout is complete.
     */
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
    
    /**
     * Adds a custom back button overlay to the scanner view.
     * Creates a circular black button with a white arrow icon positioned
     * at the top-left corner of the screen with high elevation for visibility.
     */
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
    
    /**
     * Called when the back button is pressed.
     * Finishes the activity to return to the previous screen.
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}

