package is.hello.sense.ui.activities;


import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import is.hello.sense.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class RegisterAccountNoProfilePhotoTest {

    @Rule
    public ActivityTestRule<LaunchActivity> mActivityTestRule = new ActivityTestRule<>(LaunchActivity.class);

    @Test
    public void registerAccountNoProfilePhotoTest() {

        //Test navigate to register screen

        ViewInteraction button = onView(
                allOf(withId(R.id.fragment_onboarding_introduction_get_started), withText("Get Started"),
                      withParent(withId(R.id.fragment_onboarding_introduction_button_bar)),
                      isDisplayed()));
        button.perform(click());

        ViewInteraction button2 = onView(
                allOf(withId(R.id.view_onboarding_simple_step_primary), withText("Pair your Sense"), isDisplayed()));
        button2.perform(click());

        //end test

        //Test if profile image view loaded correctly

        ViewInteraction imageView = onView(
                allOf(withId(R.id.item_profile_picture_image), isDisplayed()));

        imageView.check(matches(isDisplayed()));

        //end test

        //Test clicking and entering first name

        ViewInteraction firstNameEditText = onView(
                allOf(withId(R.id.view_label_edit_text_input),
                      withParent(withParent(withId(R.id.fragment_onboarding_register_first_name_let))),
                      isDisplayed()));

        firstNameEditText.perform(click());
        firstNameEditText.perform(replaceText("Tester"));

        //end test


        // Tests next button moves focus to Email text view

        ViewInteraction nextButton = onView(
                allOf(withId(R.id.fragment_onboarding_register_next), withText("Next"), isDisplayed()));
        nextButton.perform(click());

        ViewInteraction nextButton2 = onView(
                allOf(withId(R.id.fragment_onboarding_register_next), withText("Next"), isDisplayed()));
        nextButton.perform(click());

        nextButton2.perform(click());

        ViewInteraction emailEditText = onView(
                allOf(withId(R.id.view_label_edit_text_input),
                      withParent(withParent(withId(R.id.fragment_onboarding_register_email_let))),
                      isDisplayed()));
        emailEditText.perform(replaceText("tester@sayhello.com"));

        //end test

        //Test entering password is visible by default

        nextButton.perform(click());

        ViewInteraction passwordEditText = onView(
                allOf(withId(R.id.view_label_edit_text_input_password),
                      withParent(withParent(withId(R.id.fragment_onboarding_register_password_let))),
                      isDisplayed()));
        passwordEditText.perform(replaceText("test123"));

        passwordEditText.check(matches(withText("test123")));

        //end test

        // Tests next button text becomes continue after passing simple validation

        ViewInteraction continueButton = onView(
                allOf(withId(R.id.fragment_onboarding_register_next), withText("Continue")));

        continueButton.check(matches(isDisplayed()));

        //end test
    }
}
