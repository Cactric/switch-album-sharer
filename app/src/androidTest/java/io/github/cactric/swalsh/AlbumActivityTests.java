package io.github.cactric.swalsh;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withTagValue;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.android.material.textview.MaterialTextView;

import org.hamcrest.core.StringStartsWith;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.cactric.swalsh.ui.album.AlbumActivity;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AlbumActivityTests {
    @Rule
    public ActivityScenarioRule<AlbumActivity> activityRule =
            new ActivityScenarioRule<>(AlbumActivity.class);

    @Test
    public void tabChangeTest() {
        // Checks that switching tabs works
        // Also relies on the "No Pictures"/"No Videos" text being displayed

        onView(withId(R.id.album_nothing_found))
                .check(matches(isDisplayed()));

        StringStartsWith startsWithPictures = new StringStartsWith("Pictures");
        StringStartsWith startsWithVideos = new StringStartsWith("Videos");

        // Check we can go to the videos tab
        onView(allOf(instanceOf(MaterialTextView.class), isDescendantOfA(withId(R.id.album_tabs)), withText(startsWithVideos)))
                .perform(click());
        onView(withId(R.id.album_nothing_found))
                .check(matches(isDisplayed()))
                .check(matches(withText(R.string.no_videos)));

        // ... and that we can go back
        onView(allOf(instanceOf(MaterialTextView.class), isDescendantOfA(withId(R.id.album_tabs)), withText(startsWithPictures)))
                .perform(click());
        onView(withId(R.id.album_nothing_found))
                .check(matches(isDisplayed()))
                .check(matches(withText(R.string.no_pictures)));
    }
}
