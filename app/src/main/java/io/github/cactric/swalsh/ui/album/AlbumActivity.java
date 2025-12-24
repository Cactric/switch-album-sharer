package io.github.cactric.swalsh.ui.album;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;

import androidx.activity.EdgeToEdge;
import androidx.activity.FullyDrawnReporter;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import io.github.cactric.swalsh.MediaService;
import io.github.cactric.swalsh.databinding.ActivityAlbumBinding;
import io.github.cactric.swalsh.games.GameUtils;
import io.github.cactric.swalsh.R;

public class AlbumActivity extends AppCompatActivity {
    private MediaService.MediaBinder binder;
    private FullyDrawnReporter reporter;
    private ActivityAlbumBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityAlbumBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        reporter = getFullyDrawnReporter();

        GameUtils gameUtils = new GameUtils(this);
        String gameId = getIntent().getStringExtra("EXTRA_GAME_ID");
        int startingTab = getIntent().getIntExtra("EXTRA_STARTING_TAB", 0);

        // Set up toolbar
        if (gameId == null)
            binding.albumToolbar.setTitle(R.string.title_activity_album);
        else {
            reporter.addReporter();
            // Do the DB lookup in a separate thread
            new Thread(() -> {
                String gameName = gameUtils.lookupGameName(gameId);
                runOnUiThread(() -> {
                    binding.albumToolbar.setTitle(gameName);
                    reporter.removeReporter();
                });
            }).start();
        }
        setSupportActionBar(binding.albumToolbar);

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

        binding.albumPager.setAdapter(adapter);

        new TabLayoutMediator(binding.albumTabs, binding.albumPager, (tab, pos) -> {
            if (pos == 0) {
                tab.setText(R.string.pictures);
                tab.setTag("picture_tab");
            }
            if (pos == 1) {
                tab.setText(R.string.videos);
                tab.setTag("video_tab");
            }
        }).attach();

        binding.albumTabs.selectTab(binding.albumTabs.getTabAt(startingTab));

        // Setup connection to MediaScanService
        // (in this activity, it just updates the numbers in the tab labels)
        reporter.addReporter();
        reporter.addReporter();
        ServiceConnection connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder iBinder) {
                binder = (MediaService.MediaBinder) iBinder;
                binder.getNumOfPictures().observe(AlbumActivity.this, num -> {
                    TabLayout.Tab tab = binding.albumTabs.getTabAt(0);
                    if (num != null & tab != null) {
                        tab.setText(getString(R.string.pictures_format_str, num));
                    }
                    reporter.removeReporter();
                });
                binder.getNumOfVideos().observe(AlbumActivity.this, num -> {
                    TabLayout.Tab tab = binding.albumTabs.getTabAt(1);
                    if (num != null & tab != null) {
                        tab.setText(getString(R.string.videos_format_str, num));
                    }
                    reporter.removeReporter();
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
