package com.example.eventlotteryapp.EntrantView;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.TextView;
import com.example.eventlotteryapp.R;

public class InvitationResponseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invitation_response);

        TextView textView = findViewById(R.id.text_response);
        textView.setText("You have opened the invitation response screen!");
    }
}
