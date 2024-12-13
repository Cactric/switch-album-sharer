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
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;

import java.io.IOException;
import java.util.ArrayList;

public class AlbumActivity extends AppCompatActivity {
    private final ArrayList<PictureItem> pictureItems = new ArrayList<>();
    private final ArrayList<VideoItem> videoItems = new ArrayList<>();
    private final MutableLiveData<Integer> numOfPictures = new MutableLiveData<Integer>();
    private final MutableLiveData<Integer> numOfVideos = new MutableLiveData<Integer>();
    private static final int PICTURE_DELETION_REQUEST_CODE = 8583; // chosen by echo $RANDOM
    private static final int VIDEO_DELETION_REQUEST_CODE = 30202; // chosen by echo $RANDOM
    private String mediaSortOrder = MediaStore.Images.Media.DATE_ADDED;
    private boolean mediaSortDescending = true;
    private TabLayout tabLayout;

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
        tabLayout = findViewById(R.id.album_tabs);
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
        numOfPictures.observe(this, num -> {
            TabLayout.Tab tab = tabLayout.getTabAt(0);
            if (num != null && tab != null) {
                tab.setText(getString(R.string.pictures_format_str, num));
            }
        });
        numOfVideos.observe(this, num -> {
            TabLayout.Tab tab = tabLayout.getTabAt(1);
            if (num != null && tab != null)
                tab.setText(getString(R.string.videos_format_str, num));
        });

        retrieveItemsOnSeparateThread();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.album_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.sort_items) {
            View anchor = findViewById(R.id.sort_items);
            if (anchor != null) {
                PopupMenu pm = new PopupMenu(this, anchor);
                pm.inflate(R.menu.sort_menu);
                if (mediaSortDescending)
                    pm.getMenu().findItem(R.id.sort_descending).setChecked(true);
                else
                    pm.getMenu().findItem(R.id.sort_ascending).setChecked(true);
                pm.setOnMenuItemClickListener(sortItem -> {
                    if (sortItem.getItemId() == R.id.sort_by_date_added) {
                        mediaSortOrder = MediaStore.Images.Media.DATE_ADDED;
                        retrieveItemsOnSeparateThread();
                    } else if (sortItem.getItemId() == R.id.sort_by_date_taken) {
                        mediaSortOrder = MediaStore.Images.Media.DISPLAY_NAME;
                        retrieveItemsOnSeparateThread();
                    } else if (sortItem.getItemId() == R.id.sort_by_game) {
                        Toast.makeText(this, "Game sorting chosen, but isn't implemented yet", Toast.LENGTH_SHORT).show();
                        // TODO: replace with another activity?
                    } else if (sortItem.getItemId() == R.id.sort_ascending) {
                        mediaSortDescending = false;
                        sortItem.setChecked(true);
                        retrieveItemsOnSeparateThread();
                    } else if (sortItem.getItemId() == R.id.sort_descending) {
                        mediaSortDescending = true;
                        sortItem.setChecked(true);
                        retrieveItemsOnSeparateThread();
                    } else {
                        return false;
                    }
                    return true;
                });
                pm.show();
            } else {
                Toast.makeText(this, "Anchor (item.getActionView()) is null?", Toast.LENGTH_SHORT).show();
            }
        } else if (item.getItemId() == R.id.delete_all_pictures) {
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
                    new Thread(() -> {
                        // Delete them
                        for (Uri u: uris) {
                            getContentResolver().delete(u, null, null);
                        }
                        TabLayout tb = findViewById(R.id.album_tabs);
                        if (tb.getSelectedTabPosition() == 0) {
                            // If pictures is the selected tab, refresh it
                            getPictures();
                            runOnUiThread(this::setupRecyclerForPictures);
                        }
                    }).start();
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
                    new Thread(() -> {
                        // Delete them
                        for (Uri u: uris) {
                            getContentResolver().delete(u, null, null);
                        }
                        TabLayout tb = findViewById(R.id.album_tabs);
                        if (tb.getSelectedTabPosition() == 1) {
                            // If videos is the selected tab, refresh it
                            getVideos();
                            runOnUiThread(this::setupRecyclerForVideos);
                        }

                    }).start();
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
            if (requestCode == PICTURE_DELETION_REQUEST_CODE) {
                TabLayout tb = findViewById(R.id.album_tabs);
                if (tb.getSelectedTabPosition() == 0) {
                    // If pictures is the selected tab, refresh it
                    getPictures();
                    setupRecyclerForPictures();
                }
            } else if (requestCode == VIDEO_DELETION_REQUEST_CODE) {
                TabLayout tb = findViewById(R.id.album_tabs);
                if (tb.getSelectedTabPosition() == 1) {
                    // If videos is the selected tab, refresh it
                    getVideos();
                    setupRecyclerForVideos();
                }
            }
        }
    }

    private void setupRecyclerForPictures() {
        // Make the adapter, etc.
        TextView nothingFoundText = findViewById(R.id.album_nothing_found);
        RecyclerView recyclerView = findViewById(R.id.album_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        PictureAlbumAdapter adapter = new PictureAlbumAdapter(pictureItems.toArray(new PictureItem[0]), numOfPictures);
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

        VideoAlbumAdapter adapter = new VideoAlbumAdapter(videoItems.toArray(new VideoItem[0]), numOfVideos);
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
            }
        }
    }

    private void retrieveItemsOnSeparateThread() {
        Thread retrieveThread = new Thread(() -> {
            // TODO: synchronise adding to array list
            getPictures();
            getVideos();

            runOnUiThread(() -> {
                // On Pictures initially, unless there aren't any pictures and there are some videos
                Integer pics = numOfPictures.getValue();
                Integer vids = numOfVideos.getValue();
                if ((pics != null && pics == 0) &&
                        (vids != null && vids > 0)) {
                    if (tabLayout != null)
                        tabLayout.selectTab(tabLayout.getTabAt(1));
                    setupRecyclerForVideos();
                } else {
                    setupRecyclerForPictures();
                }
            });
        });
        retrieveThread.start();
    }
}