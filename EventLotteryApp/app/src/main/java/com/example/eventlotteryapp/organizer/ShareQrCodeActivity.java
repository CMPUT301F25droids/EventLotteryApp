package com.example.eventlotteryapp.organizer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import com.example.eventlotteryapp.OrganizerHomePage;
import com.example.eventlotteryapp.R;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Activity for generating and sharing QR codes for events.
 * Creates a QR code containing the event ID that can be scanned by entrants to quickly access event details.
 * Provides functionality to share the QR code image via Android's share intent.
 *
 * @author Droids Team
 */
public class ShareQrCodeActivity extends AppCompatActivity {

    /** Intent extra key for passing the event ID. */
    public static final String EXTRA_EVENT_ID = "extra_event_id";
    private Bitmap qrCodeBitmap;

    /**
     * Called when the activity is first created.
     * Generates a QR code from the event ID and displays it.
     * Sets up the share button and navigation back to dashboard.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down,
     *                          this contains the data it most recently supplied in onSaveInstanceState.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_qr_code);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        String eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null) {
            finish();
            return;
        }

        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            qrCodeBitmap = barcodeEncoder.encodeBitmap(eventId, BarcodeFormat.QR_CODE, 400, 400);
            ImageView imageViewQrCode = findViewById(R.id.qr_code_image_view);
            imageViewQrCode.setImageBitmap(qrCodeBitmap);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error generating QR code", Toast.LENGTH_SHORT).show();
        }

        Button shareButton = findViewById(R.id.share_qr_button);
        shareButton.setOnClickListener(v -> shareQrCode());

        Button backToDashboardButton = findViewById(R.id.back_to_dashboard_button);
        backToDashboardButton.setOnClickListener(v -> navigateToDashboard());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Go back to previous activity (should be OrganizerEventDetailsActivity)
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Shares the QR code image via Android's share intent.
     * Saves the QR code bitmap to a temporary file and creates a share intent with the image URI.
     */
    private void shareQrCode() {
        if (qrCodeBitmap == null) {
            Toast.makeText(this, "QR code not available", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Save bitmap to cache directory
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs();
            File file = new File(cachePath, "qr_code.png");
            FileOutputStream stream = new FileOutputStream(file);
            qrCodeBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            // Get URI using FileProvider
            Uri imageUri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".fileprovider",
                file
            );

            // Create share intent
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out this event QR code!");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Start share chooser
            startActivity(Intent.createChooser(shareIntent, "Share QR Code"));

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error sharing QR code", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Navigates back to the OrganizerHomePage dashboard.
     * Clears the activity stack to ensure a clean navigation state.
     */
    private void navigateToDashboard() {
        Intent intent = new Intent(this, OrganizerHomePage.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
