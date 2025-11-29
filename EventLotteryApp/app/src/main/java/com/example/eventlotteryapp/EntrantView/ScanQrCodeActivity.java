package com.example.eventlotteryapp.EntrantView;

import android.content.Intent;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

public class ScanQrCodeActivity extends AppCompatActivity {
    
    private ActivityResultLauncher<ScanOptions> scanLauncher;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Register the activity result launcher
        scanLauncher = registerForActivityResult(new ScanContract(), result -> {
            if (result.getContents() != null) {
                String eventId = result.getContents();
                
                // Navigate to event details
                Intent intent = new Intent(this, EventDetailsActivity.class);
                intent.putExtra("eventId", eventId);
                startActivity(intent);
                finish();
            } else {
                // User cancelled or scan failed
                finish();
            }
        });
        
        // Launch QR scanner
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt("Scan Event QR Code");
        options.setCameraId(0);
        options.setBeepEnabled(true);
        options.setBarcodeImageEnabled(false);
        options.setOrientationLocked(false);
        options.setCaptureActivity(QrCaptureActivity.class);
        
        scanLauncher.launch(options);
    }
}

