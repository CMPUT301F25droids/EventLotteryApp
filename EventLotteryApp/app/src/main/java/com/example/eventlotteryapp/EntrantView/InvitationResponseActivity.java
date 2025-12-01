package com.example.eventlotteryapp.EntrantView;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.TextView;
import com.example.eventlotteryapp.R;

/**
 * Activity for responding to event invitations.
 * Currently displays a placeholder message. This activity is intended
 * for handling invitation acceptance or decline actions.
 * 
 * @author Droids Team
 */
public class InvitationResponseActivity extends AppCompatActivity {

    /**
     * Called when the activity is first created.
     * Initializes the layout and displays a placeholder message.
     * 
     * @param savedInstanceState If the activity is being re-initialized after previously
     *                           being shut down, this Bundle contains the data it most
     *                           recently supplied in onSaveInstanceState(Bundle).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invitation_response);

        TextView textView = findViewById(R.id.text_response);
        textView.setText("You have opened the invitation response screen!");
    }
}
