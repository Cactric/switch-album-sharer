package io.github.cactric.swalsh;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.cactric.swalsh.ui.MainActivity;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityTests {
    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void threeButtons() {
        onView(withId(R.id.intro_scan_button))
                .check(matches(withText(R.string.scan_code)))
                .check(matches(isCompletelyDisplayed()))
                .check(matches(isClickable()));
        onView(withId(R.id.intro_manual_button))
                .check(matches(withText(R.string.manual_entry)))
                .check(matches(isCompletelyDisplayed()))
                .check(matches(isClickable()));
        onView(withId(R.id.intro_album_button))
                .check(matches(withText(R.string.open_album)))
                .check(matches(isCompletelyDisplayed()))
                .check(matches(isClickable()));
    }
}
