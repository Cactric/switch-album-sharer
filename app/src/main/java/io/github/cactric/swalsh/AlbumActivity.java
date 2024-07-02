package io.github.cactric.swalsh;

import static android.provider.MediaStore.VOLUME_EXTERNAL;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Intent;
import android.content.IntentSender;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;

import java.io.IOException;
import java.util.ArrayList;

public class AlbumActivity extends AppCompatActivity {
    private final ArrayList<PictureItem> pictureItems = new ArrayList<>();
    private final ArrayList<VideoItem> videoItems = new ArrayList<>();
    private static final int PICTURE_DELETION_REQUEST_CODE = 8583; // chosen by echo $RANDOM
    private static final int VIDEO_DELETION_REQUEST_CODE = 30202; // chosen by echo $RANDOM

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_album);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.album_toolbar);
        toolbar.setTitle(R.string.app_name_long);
        setSupportActionBar(toolbar);

        // Set up tabs
        TabLayout tabLayout = findViewById(R.id.album_tabs);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    setupRecyclerForPictures();
                } else if (tab.getPosition() == 1) {
                    setupRecyclerForVideos();
                } else {
                    Log.e("SwAlSh", "Unknown tab position " + tab.getPosition());
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        // On Pictures initially
        setupRecyclerForPictures();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.album_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.delete_all_pictures) {
            getPictures();
            if (pictureItems.isEmpty()) {
                Toast.makeText(this, "There are no pictures to remove", Toast.LENGTH_SHORT).show();
                return true;
            }

            ArrayList<Uri> uris = new ArrayList<>();
            for (PictureItem pi: pictureItems) {
                uris.add(pi.uri);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Use the native delete UI
                PendingIntent editPendingIntent = MediaStore.createDeleteRequest(getContentResolver(), uris);
                try {
                    startIntentSenderForResult(editPendingIntent.getIntentSender(), PICTURE_DELETION_REQUEST_CODE, null, 0, 0, 0);
                } catch (IntentSender.SendIntentException e) {
                    Log.e("SwAlSh", "Couldn't ask to delete pictures", e);
                }
            } else {
                AlertDialog.Builder adb = new AlertDialog.Builder(this);
                adb.setTitle(getString(R.string.delete_all_pictures_confirmation_formatted, pictureItems.size()));
                adb.setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
                adb.setPositiveButton(R.string.yes, (dialog, which) -> {
                    // Delete them
                    for (Uri u: uris) {
                        getContentResolver().delete(u, null, null);
                    }
                    TabLayout tb = findViewById(R.id.album_tabs);
                    if (tb.getSelectedTabPosition() == 0)
                        // If pictures is the selected tab, refresh it
                        setupRecyclerForPictures();
                });
                adb.show();
            }
            return true;
        } else if (item.getItemId() == R.id.delete_all_videos) {
            getVideos();
            if (videoItems.isEmpty()) {
                Toast.makeText(this, "There are no videos to remove", Toast.LENGTH_SHORT).show();
                return true;
            }

            ArrayList<Uri> uris = new ArrayList<>();
            for (VideoItem vi: videoItems) {
                uris.add(vi.uri);
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                // Use the native delete UI
                PendingIntent editPendingIntent = MediaStore.createDeleteRequest(getContentResolver(), uris);
                try {
                    startIntentSenderForResult(editPendingIntent.getIntentSender(), VIDEO_DELETION_REQUEST_CODE, null, 0, 0, 0);
                } catch (IntentSender.SendIntentException e) {
                    Log.e("SwAlSh", "Couldn't ask to delete videos", e);
                }
            } else {
                AlertDialog.Builder adb = new AlertDialog.Builder(this);
                adb.setTitle(getString(R.string.delete_all_videos_confirmation_formatted, videoItems.size()));
                adb.setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
                adb.setPositiveButton(R.string.yes, (dialog, which) -> {
                    // Delete them
                    for (Uri u: uris) {
                        getContentResolver().delete(u, null, null);
                    }
                    TabLayout tb = findViewById(R.id.album_tabs);
                    if (tb.getSelectedTabPosition() == 1)
                        // If videos is the selected tab, refresh it
                        setupRecyclerForVideos();
                });
                adb.show();
            }
            return true;
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == VIDEO_DELETION_REQUEST_CODE) {
                TabLayout tb = findViewById(R.id.album_tabs);
                if (tb.getSelectedTabPosition() == 1)
                    // If videos is the selected tab, refresh it
                    setupRecyclerForVideos();
            } else if (requestCode == PICTURE_DELETION_REQUEST_CODE) {
                TabLayout tb = findViewById(R.id.album_tabs);
                if (tb.getSelectedTabPosition() == 0)
                    // If pictures is the selected tab, refresh it
                    setupRecyclerForPictures();
            }
        } else {
            Toast.makeText(this, "They were not deleted", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupRecyclerForPictures() {
        // Make the adapter, etc.
        TextView nothingFoundText = findViewById(R.id.album_nothing_found);
        RecyclerView recyclerView = findViewById(R.id.album_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        getPictures();

        PictureAlbumAdapter adapter = new PictureAlbumAdapter(pictureItems.toArray(new PictureItem[0]));
        recyclerView.setAdapter(adapter);
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if (adapter.getItemCount() == 0) {
                    recyclerView.setVisibility(View.GONE);
                    nothingFoundText.setVisibility(View.VISIBLE);
                }
            }
        });

        if (pictureItems.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            nothingFoundText.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            nothingFoundText.setVisibility(View.GONE);
        }
    }

    private void setupRecyclerForVideos() {
        // Make the adapter, etc.
        TextView nothingFoundText = findViewById(R.id.album_nothing_found);
        RecyclerView recyclerView = findViewById(R.id.album_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        getVideos();

        VideoAlbumAdapter adapter = new VideoAlbumAdapter(videoItems.toArray(new VideoItem[0]));
        recyclerView.setAdapter(adapter);
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if (adapter.getItemCount() == 0) {
                    recyclerView.setVisibility(View.GONE);
                    nothingFoundText.setVisibility(View.VISIBLE);
                }
            }
        });

        if (videoItems.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            nothingFoundText.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            nothingFoundText.setVisibility(View.GONE);
        }
    }

    private void getPictures() {
        // Pictures:
        pictureItems.clear();
        // Which columns from the query?
        String[] pics_projection = new String[] {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
        };

        try (Cursor cursor = getContentResolver().query(
                MediaStore.Images.Media.getContentUri(VOLUME_EXTERNAL),
                pics_projection,
                null,
                null,
                MediaStore.Images.Media.DATE_ADDED + " DESC"
        )) {
            if (cursor == null)
                throw new NullPointerException();
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            int displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);

            // Loop through results
            while (cursor.moveToNext()) {
                long id = cursor.getLong(idColumn);
                PictureItem item = new PictureItem();
                item.id = id;
                item.uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                item.display_name = cursor.getString(displayNameColumn);
                pictureItems.add(item);
            }
        }
    }

    private void getVideos() {
        videoItems.clear();
        // Videos
        String[] vid_projection = new String[] {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION
        };

        try (Cursor cursor = getContentResolver().query(
                MediaStore.Video.Media.getContentUri(VOLUME_EXTERNAL),
                vid_projection,
                null,
                null,
                MediaStore.Video.Media.DATE_ADDED + " DESC"
        )) {
            if (cursor == null)
                throw new NullPointerException();
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
            int displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
            int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);

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
            }
        }
    }
}