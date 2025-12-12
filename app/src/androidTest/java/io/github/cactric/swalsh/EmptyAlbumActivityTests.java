package io.github.cactric.swalsh;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertNotNull;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.material.textview.MaterialTextView;

import org.hamcrest.core.StringStartsWith;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.github.cactric.swalsh.ui.album.AlbumActivity;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class EmptyAlbumActivityTests {
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
