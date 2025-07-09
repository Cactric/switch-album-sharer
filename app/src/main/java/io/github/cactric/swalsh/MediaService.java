package io.github.cactric.swalsh;

import static android.provider.MediaStore.VOLUME_EXTERNAL;

import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.ArraySet;
import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleService;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import io.github.cactric.swalsh.games.GameDatabase;

/**
 * Service to scan media items
 */
public class MediaService extends LifecycleService {
    private final MediaBinder binder = new MediaBinder();
    private final MutableLiveData<Integer> numOfPictures = new MutableLiveData<>();
    private final MutableLiveData<Integer> numOfVideos = new MutableLiveData<>();
    private final ArraySet<String> gameIdsFromPictures = new ArraySet<>();
    private final ArraySet<String> gameIdsFromVideos = new ArraySet<>();

    public MediaService() {
    }

    private ArrayList<PictureItem> getPictures(@Nullable String gameId, String mediaSortOrder, boolean mediaSortDescending) {
        // Pictures:
        ArrayList<PictureItem> pictureItems = new ArrayList<>();
        gameIdsFromPictures.clear();
        // Which columns from the query?
        String[] pics_projection = new String[] {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME
        };

        // Select all pictures when game ID is null; select only pictures from the specified game when not null
        String selection = null;
        final String[] selectionArgs = {""};
        if (gameId != null) {
            selection = MediaStore.Images.Media.DISPLAY_NAME + " LIKE ?";
            selectionArgs[0] = "%" + gameId + "%";
        }

        try (Cursor cursor = getContentResolver().query(
                MediaStore.Images.Media.getContentUri(VOLUME_EXTERNAL),
                pics_projection,
                selection,
                gameId == null ? null : selectionArgs,
                mediaSortOrder + (mediaSortDescending ? " DESC" : "")
        )) {
            if (cursor == null)
                throw new NullPointerException();
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            int displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);

            // Set count
            numOfPictures.postValue(cursor.getCount());

            // Loop through results
            while (cursor.moveToNext()) {
                long id = cursor.getLong(idColumn);
                PictureItem item = new PictureItem();
                item.uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                item.display_name = cursor.getString(displayNameColumn);
                // Parse the display name into the string that'll be shown in the UI
                item.display_text = parsePictureName(item.display_name);
                pictureItems.add(item);
                gameIdsFromPictures.add(item.display_name.substring(17, 49));
            }
        }
        return pictureItems;
    }

    /**
     * Format the filename of a picture into the text that'll be shown in the UI.
     * Calls the games database, so it shouldn't be called on the UI thread
     * @param disName The filename of the picture
     * @return The formatted string, suitable for showing in the UI
     */
    private LiveData<String> parsePictureName(String disName) {
        // Use the display name to get the game ID and try to look it up to get the game's name
        String gameId = disName.substring(17,49);
        GameDatabase db = GameDatabase.getDatabase(this);
        LiveData<String> gameName = db.gameDao().getGameNameLD(gameId);
        String placeholder = getString(R.string.unknown_game_name_format, gameId.substring(0, 6));

        // Try to parse the display name and use that as a date
        String dateStr = null;
        // Format: year, month, day, hour, minute, second, 00 - game id(?).jpg
        Calendar.Builder calBuilder = new Calendar.Builder();
        try {
            calBuilder.set(Calendar.YEAR, Integer.parseInt(disName.substring(0, 4)));
            calBuilder.set(Calendar.MONTH, Integer.parseInt(disName.substring(4, 6)) - 1);
            calBuilder.set(Calendar.DAY_OF_MONTH, Integer.parseInt(disName.substring(6, 8)));
            calBuilder.set(Calendar.HOUR_OF_DAY, Integer.parseInt(disName.substring(8, 10)));
            calBuilder.set(Calendar.MINUTE, Integer.parseInt(disName.substring(10, 12)));
            calBuilder.set(Calendar.SECOND, Integer.parseInt(disName.substring(12, 14)));

            Date d = new Date();
            d.setTime(calBuilder.build().getTimeInMillis());
            DateFormat df = DateFormat.getDateTimeInstance();
            dateStr = df.format(d);
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            Log.e("SwAlSh", "Failed to parse " + disName, e);
        }

        if (dateStr == null)
            dateStr = disName;

        MutableLiveData<String> ret = new MutableLiveData<>();
        final String finalDateStr = dateStr;
        getMainExecutor().execute(() -> gameName.observe(
                this,
                s -> ret.postValue(getString(R.string.picture_text_format,
                        s == null ? placeholder : s,
                        finalDateStr))));
        return ret;
    }

    private ArrayList<VideoItem> getVideos(@Nullable String gameId, String mediaSortOrder, boolean mediaSortDescending) {
        ArrayList<VideoItem> videoItems = new ArrayList<>();
        gameIdsFromVideos.clear();
        // Videos
        String[] vid_projection = new String[] {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION
        };

        // Select all videos when game ID is null; select only pictures from the specified game when not null
        String selection = null;
        final String[] selectionArgs = {""};
        if (gameId != null) {
            selection = MediaStore.Images.Media.DISPLAY_NAME + " LIKE ?";
            selectionArgs[0] = "%" + gameId + "%";
        }


        try (Cursor cursor = getContentResolver().query(
                MediaStore.Video.Media.getContentUri(VOLUME_EXTERNAL),
                vid_projection,
                selection,
                gameId == null ? null : selectionArgs,
                mediaSortOrder + (mediaSortDescending ? " DESC" : "")
        )) {
            if (cursor == null)
                throw new NullPointerException();
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
            int displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
            int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);

            // Set count
            numOfVideos.postValue(cursor.getCount());

            // Loop through results
            while (cursor.moveToNext()) {
                long id = cursor.getLong(idColumn);
                VideoItem item = new VideoItem();
                item.uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);
                item.display_name = cursor.getString(displayNameColumn);
                item.duration_in_milliseconds = cursor.getInt(durationColumn);
                item.display_text = parseVideoName(item.display_name, item.duration_in_milliseconds);
                try {
                    item.thumbnail = getContentResolver().loadThumbnail(item.uri, Size.parseSize("1280x720"), null);
                } catch (IOException e) {
                    Log.e("SwAlSh", "Error while loading thumbnail for " + item.display_name, e);
                    item.thumbnail = null;
                }
                videoItems.add(item);
                gameIdsFromVideos.add(item.display_name.substring(17, 49));
            }
        }
        return videoItems;
    }

    /**
     * Format the filename of a video into the text that'll be shown in the UI.
     * Calls the games database, so it shouldn't be called on the UI thread
     * @param disName Filename of the video to parse
     * @param duration Duration in milliseconds
     * @return A string suitable for showing in the UI
     */
    private LiveData<String> parseVideoName(String disName, long duration) {
        // Use the display name to get the game ID and then try to look it up to get the game's name
        String gameId = disName.substring(17,49);
        GameDatabase db = GameDatabase.getDatabase(this);
        LiveData<String> gameName = db.gameDao().getGameNameLD(gameId);
        String placeholder = getString(R.string.unknown_game_name_format, gameId.substring(0, 6));

        // Try to parse the display name and use that as a date
        String dateStr = null;
        // Format: year, month, day, hour, minute, second, 00 - game id(?).jpg
        Calendar.Builder calBuilder = new Calendar.Builder();
        try {
            calBuilder.set(Calendar.YEAR, Integer.parseInt(disName.substring(0, 4)));
            calBuilder.set(Calendar.MONTH, Integer.parseInt(disName.substring(4, 6)) - 1);
            calBuilder.set(Calendar.DAY_OF_MONTH, Integer.parseInt(disName.substring(6, 8)));
            calBuilder.set(Calendar.HOUR_OF_DAY, Integer.parseInt(disName.substring(8, 10)));
            calBuilder.set(Calendar.MINUTE, Integer.parseInt(disName.substring(10, 12)));
            calBuilder.set(Calendar.SECOND, Integer.parseInt(disName.substring(12, 14)));

            Date d = new Date();
            d.setTime(calBuilder.build().getTimeInMillis());
            DateFormat df = DateFormat.getDateTimeInstance();
            dateStr = df.format(d);
        } catch (NumberFormatException e) {
            Log.e("SwAlSh", "Failed to parse " + disName, e);
        }

        if (dateStr == null)
            dateStr = disName;

        MutableLiveData<String> ret = new MutableLiveData<>();
        final String finalDateStr = dateStr;
        getMainExecutor().execute(() -> gameName.observe(
                this,
                s -> ret.postValue(
                        getString(R.string.video_text_format,
                                duration / 1000.0,
                                s == null ? placeholder : s,
                                finalDateStr))));
        return ret;
    }


    @Override
    public IBinder onBind(@NonNull Intent intent) {
        super.onBind(intent);
        return binder;
    }

    public class MediaBinder extends Binder {
        /**
         * Scan pictures on a separate thread, use the listener as a callback for the results
         * Note: the results listener will be called on that separate thread.
         */
        public void scanPictures(@Nullable String gameId, String mediaSortOrder, boolean mediaSortDescending, PictureScanListener resultListener) {
            new Thread(() -> {
                ArrayList<PictureItem> results = getPictures(gameId, mediaSortOrder, mediaSortDescending);
                resultListener.onPicturesReady(results);
            }).start();
        }

        public void scanVideos(@Nullable String gameId, String mediaSortOrder, boolean mediaSortDescending, VideoScanListener resultListener) {
            new Thread(() -> {
                ArrayList<VideoItem> results = getVideos(gameId, mediaSortOrder, mediaSortDescending);
                resultListener.onVideosReady(results);
            }).start();
        }

        public ArraySet<String> getFoundGameIds() {
            ArraySet<String> combinedGameIds = new ArraySet<>();
            combinedGameIds.addAll(gameIdsFromPictures);
            combinedGameIds.addAll(gameIdsFromVideos);
            return combinedGameIds;
        }

        @NonNull
        public MutableLiveData<Integer> getNumOfPictures() {
            return numOfPictures;
        }
        @NonNull
        public MutableLiveData<Integer> getNumOfVideos() {
            return numOfVideos;
        }

        public void deletePicture(Uri uri) {
            getContentResolver().delete(uri, null, null);
            if (numOfPictures.getValue() != null)
                numOfPictures.postValue(numOfPictures.getValue() - 1);
        }
        public void deleteVideo(Uri uri) {
            getContentResolver().delete(uri, null, null);
            if (numOfVideos.getValue() != null)
                numOfVideos.postValue(numOfVideos.getValue() - 1);
        }

        public void deleteAllPictures() {
            Uri uri = MediaStore.Images.Media.getContentUri(VOLUME_EXTERNAL);
            // Delete all pictures in one go
            getContentResolver().delete(uri, MediaStore.Images.Media.OWNER_PACKAGE_NAME + " == ?", new String[]{getPackageName()});
            numOfPictures.postValue(0);
        }
        public void deleteAllVideos() {
            Uri uri = MediaStore.Video.Media.getContentUri(VOLUME_EXTERNAL);
            getContentResolver().delete(uri, MediaStore.Video.Media.OWNER_PACKAGE_NAME + " == ?", new String[]{getPackageName()});
            numOfVideos.postValue(0);
        }
    }

    public interface PictureScanListener {
        void onPicturesReady(@NonNull ArrayList<PictureItem> items);
    }
    public interface VideoScanListener {
        void onVideosReady(@NonNull ArrayList<VideoItem> items);
    }
}