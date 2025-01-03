package io.github.cactric.swalsh;

import static android.provider.MediaStore.VOLUME_EXTERNAL;

import android.app.Service;
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
import androidx.lifecycle.MutableLiveData;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Service to scan media items
 */
public class MediaService extends Service {
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
                item.id = id;
                item.uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                item.display_name = cursor.getString(displayNameColumn);
                pictureItems.add(item);
                gameIdsFromPictures.add(item.display_name.substring(17, 49));
            }
        }
        return pictureItems;
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
                item.id = id;
                item.uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);
                item.display_name = cursor.getString(displayNameColumn);
                item.duration_in_milliseconds = cursor.getInt(durationColumn);
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


    @Override
    public IBinder onBind(Intent intent) {
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