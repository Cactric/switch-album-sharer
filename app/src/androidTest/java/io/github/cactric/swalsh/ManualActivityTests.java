package io.github.cactric.swalsh;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.espresso.Espresso;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.cactric.swalsh.ui.MainActivity;
import io.github.cactric.swalsh.ui.ManualActivity;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ManualActivityTests {
    @Rule
    public ActivityScenarioRule<ManualActivity> activityRule =
            new ActivityScenarioRule<>(ManualActivity.class);

    @Test
    public void everythingIsOnScreen() {
        // TODO: make it check that things are not obscured by the keyboard
        // Click the Wifi name textbox to open the virtual keyboard
        onView(withId(R.id.manual_wifi_name))
                .perform(click());

        onView(withId(R.id.manual_explanation))
                .check(matches(withText(R.string.manual_explanation)))
                .check(matches(isCompletelyDisplayed()));
        onView(withId(R.id.manual_wifi_name))
                .check(matches(isCompletelyDisplayed()))
                .check(matches(isClickable()));
        onView(withId(R.id.manual_wifi_password))
                .check(matches(isCompletelyDisplayed()))
                .check(matches(isClickable()));
    }
}
