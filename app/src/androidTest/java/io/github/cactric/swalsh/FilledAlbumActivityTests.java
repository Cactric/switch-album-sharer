package io.github.cactric.swalsh;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
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

import org.junit.After;
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
public class FilledAlbumActivityTests {
    @Rule
    public ActivityScenarioRule<AlbumActivity> activityRule;

    private final Context targetCtx = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private final Context testCtx = InstrumentationRegistry.getInstrumentation().getContext();

    public FilledAlbumActivityTests() throws IOException {
        // Write out the picture before the activity gets started
        writePicture("2025121212105900-FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF.jpg");
        activityRule = new ActivityScenarioRule<>(AlbumActivity.class);
    }

    @After
    public void cleanupPictures() {
        // Delete the picture afterwards
        deletePicture("2025121212105900-FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF.jpg");
    }

    @Test
    public void pictureShowsUpTest() throws IOException {
        onView(withId(R.id.album_nothing_found))
            .check(matches(not(isDisplayed())));
    }

    public void writePicture(String filename) throws IOException {
        // Write a picture into the app

        ContentResolver targetContentResolver = targetCtx.getContentResolver();
        Uri collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + targetCtx.getString(R.string.captured_pictures_dir));
        Uri picUri = targetContentResolver.insert(collection, values);
        assertNotNull(picUri);

        try (OutputStream out = targetContentResolver.openOutputStream(picUri);
             InputStream in = testCtx.getResources().openRawResource(io.github.cactric.swalsh.test.R.raw.test_picture1)
        ) {
            assertNotNull(out);
            assertNotNull(in);

            boolean done = false;
            while (!done) {
                byte[] data = new byte[512 * 1024];
                int bytesRead = in.read(data);
                if (bytesRead == -1)
                    done = true;
                else {
                    out.write(data, 0, bytesRead);
                }
            }
        }
    }

    public void deletePicture(String filename) {
        ContentResolver targetContentResolver = targetCtx.getContentResolver();
        targetContentResolver.delete(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), "_display_name == ?", new String[]{filename});
    }
}
