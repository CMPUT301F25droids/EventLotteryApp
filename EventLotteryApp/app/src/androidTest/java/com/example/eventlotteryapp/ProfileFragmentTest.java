package com.example.eventlotteryapp;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventlotteryapp.Authorization.AuthActivity;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.AllOf.allOf;

@RunWith(AndroidJUnit4.class)
public class ProfileFragmentTest {

    @Before
    public void cleanupBackend() {
        FirebaseAuth.getInstance().signOut();
        try {
            Tasks.await(FirebaseAuth.getInstance().signInWithEmailAndPassword("test@test.com", "test1234"));
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                Tasks.await(user.delete());
            }
        } catch (ExecutionException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        } finally {
            FirebaseAuth.getInstance().signOut();
        }
    }

    private void signUpAndLogin() throws InterruptedException {
        onView(allOf(withText("Sign Up"), isDescendantOfA(withId(R.id.authorization_tabs)))).perform(click());
        onView(withId(R.id.sign_name)).perform(typeText("Test User"), closeSoftKeyboard());
        onView(withId(R.id.sign_email)).perform(typeText("test@test.com"), closeSoftKeyboard());
        onView(withId(R.id.sign_password)).perform(typeText("test1234"), closeSoftKeyboard());
        onView(withId(R.id.signup_button)).perform(click());
        Thread.sleep(5000);
    }

    @Test
    public void test_profileLogout() throws InterruptedException {
        try (ActivityScenario<AuthActivity> scenario = ActivityScenario.launch(AuthActivity.class)) {
            signUpAndLogin();

            onView(allOf(withText("Profile"), isDescendantOfA(withId(R.id.entrant_home_tabs)))).perform(click());
            Thread.sleep(1000);

            onView(withId(R.id.profile_email)).perform(scrollTo()).check(matches(withText("test@test.com")));

            onView(withId(R.id.logout_button)).perform(scrollTo(), click());

            Thread.sleep(1000);

            onView(withId(R.id.main)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void test_updateProfile() throws InterruptedException {
        try (ActivityScenario<AuthActivity> scenario = ActivityScenario.launch(AuthActivity.class)) {
            signUpAndLogin();

            onView(allOf(withText("Profile"), isDescendantOfA(withId(R.id.entrant_home_tabs)))).perform(click());
            Thread.sleep(2000); // Wait for user data to load

            onView(withId(R.id.full_name_edit)).perform(scrollTo(), typeText("New Name"), closeSoftKeyboard());
            onView(withId(R.id.phone_edit)).perform(scrollTo(), typeText("1234567890"), closeSoftKeyboard());
            onView(withId(R.id.save_changes_button)).perform(scrollTo(), click());

            Thread.sleep(2000); // Wait for Firestore to update

            onView(withId(R.id.profile_name)).perform(scrollTo()).check(matches(withText("New Name")));
            onView(withId(R.id.profile_phone)).perform(scrollTo()).check(matches(withText("1234567890 Phone")));
        }
    }

    @Test
    public void test_switchRoles() throws InterruptedException {
        try (ActivityScenario<AuthActivity> scenario = ActivityScenario.launch(AuthActivity.class)) {
            signUpAndLogin();

            onView(allOf(withText("Profile"), isDescendantOfA(withId(R.id.entrant_home_tabs)))).perform(click());
            Thread.sleep(1000);

            onView(withId(R.id.organizer_button)).perform(scrollTo(), click());

            Thread.sleep(2000);

            onView(withId(R.id.organizer_home_tabs)).check(matches(isDisplayed()));
        }
    }
}
