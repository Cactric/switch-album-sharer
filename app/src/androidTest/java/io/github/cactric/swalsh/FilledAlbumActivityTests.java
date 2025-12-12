package io.github.cactric.swalsh;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertNotNull;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.test.espresso.contrib.RecyclerViewActions;
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
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import io.github.cactric.swalsh.games.GameUtils;
import io.github.cactric.swalsh.ui.album.AlbumActivity;
import io.github.cactric.swalsh.ui.album.PictureAlbumAdapter;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class FilledAlbumActivityTests {
    @Rule
    public ActivityScenarioRule<AlbumActivity> activityRule = new ActivityScenarioRule<>(AlbumActivity.class);

    private final Context targetCtx = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private final Context testCtx = InstrumentationRegistry.getInstrumentation().getContext();

    @Before
    public void setupPictures() throws IOException {
        // Write out the picture and restart the activity
        writePicture("2025121212105900-FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF.jpg");
        activityRule.getScenario().recreate();
    }

    @After
    public void cleanupPictures() {
        // Delete the picture afterwards
        deletePicture("2025121212105900-FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF.jpg");
    }

    @Test
    public void pictureShowsUpTest() {
        // Make sure the placeholder text is gone
        onView(withId(R.id.album_nothing_found))
            .check(matches(not(isDisplayed())));

        // Build the text that should be displayed
        GameUtils utils = new GameUtils(targetCtx);
        String gameName = utils.lookupGameName("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        String date = parseIntoDate("2025121212105900-FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF.jpg");
        String displayedName = targetCtx.getString(R.string.picture_text_format, gameName, date);

        // Make sure the picture shows up
        onView(withId(R.id.album_recycler))
                .perform(RecyclerViewActions.scrollTo(
                        hasDescendant(withText(displayedName))
                ));
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

    public String parseIntoDate(String disName) {
        // Parse the display name and use that as a date

        // Format: year, month, day, hour, minute, second, 00 - game id(?).jpg
        Calendar.Builder calBuilder = new Calendar.Builder();
        calBuilder.set(Calendar.YEAR, Integer.parseInt(disName.substring(0, 4)));
        calBuilder.set(Calendar.MONTH, Integer.parseInt(disName.substring(4, 6)) - 1);
        calBuilder.set(Calendar.DAY_OF_MONTH, Integer.parseInt(disName.substring(6, 8)));
        calBuilder.set(Calendar.HOUR_OF_DAY, Integer.parseInt(disName.substring(8, 10)));
        calBuilder.set(Calendar.MINUTE, Integer.parseInt(disName.substring(10, 12)));
        calBuilder.set(Calendar.SECOND, Integer.parseInt(disName.substring(12, 14)));

        Date d = new Date();
        d.setTime(calBuilder.build().getTimeInMillis());
        DateFormat df = DateFormat.getDateTimeInstance();
        return df.format(d);
    }
}
