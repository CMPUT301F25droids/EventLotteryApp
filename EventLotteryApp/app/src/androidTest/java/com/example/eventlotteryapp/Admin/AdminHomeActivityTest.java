package com.example.eventlotteryapp.Admin;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventlotteryapp.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AdminHomeActivityTest {

    @Before
    public void setup() {
        Intents.init(); // Start intent capturing
    }

    @After
    public void cleanup() {
        Intents.release();
    }

    @Test
    public void testBrowseEventsButtonLaunchesActivity() {
        ActivityScenario.launch(AdminHomeActivity.class);

        // Click the Browse Events button
        onView(withId(R.id.adminBrowseEventsButton)).perform(click());

        // Assert that the intent goes to AdminBrowseEventsActivity
        intended(hasComponent(AdminBrowseEventsActivity.class.getName()));
    }
}