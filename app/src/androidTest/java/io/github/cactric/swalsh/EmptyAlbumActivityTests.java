package io.github.cactric.swalsh;

import static android.provider.MediaStore.VOLUME_EXTERNAL;
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
import org.junit.Before;
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

    @Before
    public void clearAlbum() {
        // Warning: Destructive!
        ContentResolver resolver = InstrumentationRegistry.getInstrumentation().getTargetContext().getContentResolver();
        String targetPackage = InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageName();
        int deleted = 0;

        Uri pic_uri = MediaStore.Images.Media.getContentUri(VOLUME_EXTERNAL);
        // Delete all pictures in one go
        deleted += resolver.delete(pic_uri, MediaStore.Images.Media.OWNER_PACKAGE_NAME + " == ?", new String[]{targetPackage});
        Uri vid_uri = MediaStore.Video.Media.getContentUri(VOLUME_EXTERNAL);
        deleted += resolver.delete(vid_uri, MediaStore.Video.Media.OWNER_PACKAGE_NAME + " == ?", new String[]{targetPackage});

        if (deleted > 0) {
            // Restart the activity if media was deleted
            activityRule.getScenario().recreate();
        }
    }

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
