package io.github.cactric.swalsh.ui.album;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import io.github.cactric.swalsh.MediaService;
import io.github.cactric.swalsh.games.GameUtils;
import io.github.cactric.swalsh.R;

public class AlbumActivity extends AppCompatActivity {
    private TabLayout tabLayout;
    private MediaService.MediaBinder binder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_album);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.album_root_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        GameUtils gameUtils = new GameUtils(this);
        String gameId = getIntent().getStringExtra("EXTRA_GAME_ID");
        int startingTab = getIntent().getIntExtra("EXTRA_STARTING_TAB", 0);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.album_toolbar);
        if (gameId == null)
            toolbar.setTitle(R.string.title_activity_album);
        else {
            // Do the DB lookup in a separate thread
            new Thread(() -> {
                String gameName = gameUtils.lookupGameName(gameId);
                runOnUiThread(() -> toolbar.setTitle(gameName));
            }).start();
        }
        setSupportActionBar(toolbar);

        // Set up tabs
        tabLayout = findViewById(R.id.album_tabs);

        // Set up View Pager
        FragmentStateAdapter adapter = new FragmentStateAdapter(this) {
            @Override
            public int getItemCount() {
                return 2;
            }

            @NonNull
            @Override
            public Fragment createFragment(int position) {
                // Create corresponding fragment based on position
                if (position == 1) {
                    return VideoFragment.newInstance(gameId);
                } else {
                    return PictureFragment.newInstance(gameId);
                }
            }
        };

        ViewPager2 pager = findViewById(R.id.album_pager);
        pager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, pager, (tab, pos) -> {
            if (pos == 0) {
                tab.setText(R.string.pictures);
                tab.setTag("picture_tab");
            }
            if (pos == 1) {
                tab.setText(R.string.videos);
                tab.setTag("video_tab");
            }
        }).attach();

        tabLayout.selectTab(tabLayout.getTabAt(startingTab));

        // Setup connection to MediaScanService
        // (in this activity, it just updates the numbers in the tab labels)
        ServiceConnection connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder iBinder) {
                binder = (MediaService.MediaBinder) iBinder;
                binder.getNumOfPictures().observe(AlbumActivity.this, num -> {
                    TabLayout.Tab tab = tabLayout.getTabAt(0);
                    if (num != null & tab != null) {
                        tab.setText(getString(R.string.pictures_format_str, num));
                    }
                });
                binder.getNumOfVideos().observe(AlbumActivity.this, num -> {
                    TabLayout.Tab tab = tabLayout.getTabAt(1);
                    if (num != null & tab != null) {
                        tab.setText(getString(R.string.videos_format_str, num));
                    }
                });

                // Be cheeky and fetch the data for the other tab so that the number in the tab is populated
                if (startingTab == 0)
                    binder.scanVideos(gameId, MediaStore.Video.Media.DATE_ADDED, true, items -> {});
                else
                    binder.scanPictures(gameId, MediaStore.Images.Media.DATE_ADDED, true, items -> {});
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                /* do nothing */
            }
        };

        Intent mssIntent = new Intent(this, MediaService.class);
        bindService(mssIntent, connection, BIND_AUTO_CREATE);
    }
}
