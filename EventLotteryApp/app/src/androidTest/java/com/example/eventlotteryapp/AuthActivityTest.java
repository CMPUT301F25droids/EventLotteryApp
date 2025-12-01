package com.example.eventlotteryapp;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventlotteryapp.Authorization.AuthActivity;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isSelected;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.AllOf.allOf;

@RunWith(AndroidJUnit4.class)
public class AuthActivityTest {

    @Rule
    public ActivityScenarioRule<AuthActivity> activityRule =
            new ActivityScenarioRule<>(AuthActivity.class);

    /**
     * Before each test, ensure we are signed out and that the test user doesn't exist.
     * This makes the tests repeatable.
     */
    @Before
    public void setup() {
        FirebaseAuth.getInstance().signOut();
        deleteTestUserIfExists();
    }

    private void deleteTestUserIfExists() {
        try {
            // Try to sign in with the test user's credentials.
            Tasks.await(FirebaseAuth.getInstance().signInWithEmailAndPassword("test@test.com", "test1234"));

            // If sign-in succeeds, the user exists. Get the user and delete them.
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                Tasks.await(user.delete());
            }
        } catch (ExecutionException | InterruptedException e) {
            // ExecutionException is expected if the user doesn't exist, so we can ignore it.
            // For InterruptedException, restore the interrupted status.
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        } finally {
            // Always sign out to ensure a clean state for the test.
            FirebaseAuth.getInstance().signOut();
        }
    }

    @Test
    public void test_activityLaunches() {
        onView(withId(R.id.main)).check(matches(isDisplayed()));
    }

    @Test
    public void test_tabsAreVisible() {
        onView(allOf(withText("Login"), isDescendantOfA(withId(R.id.authorization_tabs)))).check(matches(isDisplayed()));
        onView(allOf(withText("Sign Up"), isDescendantOfA(withId(R.id.authorization_tabs)))).check(matches(isDisplayed()));
    }

    @Test
    public void test_swipeBetweenTabs() {
        onView(withId(R.id.auth_view_pager)).perform(ViewActions.swipeLeft());
        onView(allOf(withText("Sign Up"), isDescendantOfA(withId(R.id.authorization_tabs)))).check(matches(withParent(isSelected())));

        onView(withId(R.id.auth_view_pager)).perform(ViewActions.swipeRight());
        onView(allOf(withText("Login"), isDescendantOfA(withId(R.id.authorization_tabs)))).check(matches(withParent(isSelected())));
    }

    @Test
    public void test_clickTabs() {
        onView(allOf(withText("Sign Up"), isDescendantOfA(withId(R.id.authorization_tabs)))).perform(click());
        onView(allOf(withText("Sign Up"), isDescendantOfA(withId(R.id.authorization_tabs)))).check(matches(withParent(isSelected())));

        onView(allOf(withText("Login"), isDescendantOfA(withId(R.id.authorization_tabs)))).perform(click());
        onView(allOf(withText("Login"), isDescendantOfA(withId(R.id.authorization_tabs)))).check(matches(withParent(isSelected())));
    }

    @Test
    public void test_signUpAndLogin() throws InterruptedException {
        // Go to sign up
        onView(allOf(withText("Sign Up"), isDescendantOfA(withId(R.id.authorization_tabs)))).perform(click());

        // Fill out sign up form
        onView(withId(R.id.sign_name)).perform(typeText("Test User"), closeSoftKeyboard());
        onView(withId(R.id.sign_email)).perform(typeText("test@test.com"), closeSoftKeyboard());
        onView(withId(R.id.sign_password)).perform(typeText("test1234"), closeSoftKeyboard());
        onView(withId(R.id.signup_button)).perform(click());

        // Wait for Firebase to create user and navigate to the new activity.
        // A more advanced solution would use Espresso Idling Resources.
        Thread.sleep(5000);

        // Verify we landed on the home page after signup
        onView(withId(R.id.scan_qr_button)).check(matches(isDisplayed()));

        // Sign out to test the login flow.
        FirebaseAuth.getInstance().signOut();

        // Relaunch the AuthActivity to get to the login screen.
        activityRule.getScenario().close();
        ActivityScenario.launch(AuthActivity.class);
        Thread.sleep(1000); // Wait for activity to launch

        // Now, log in with the new user
        onView(withId(R.id.editTextTextEmailAddress2)).perform(typeText("test@test.com"), closeSoftKeyboard());
        onView(withId(R.id.editTextTextPassword)).perform(typeText("test1234"), closeSoftKeyboard());
        onView(withId(R.id.login_button)).perform(click());

        // Wait for Firebase to log in user and navigate.
        Thread.sleep(5000);

        // Verify we landed on the home page again after login.
        onView(withId(R.id.scan_qr_button)).check(matches(isDisplayed()));
    }
}
