package io.github.cactric.swalsh.ui.album;

import android.os.Bundle;

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

import io.github.cactric.swalsh.R;

public class AlbumActivity extends AppCompatActivity {
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
        toolbar.setTitle(R.string.title_activity_album);
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
                    VideoFragment f = new VideoFragment();
                    f.getNumOfVideos().observe(AlbumActivity.this, num -> {
                        TabLayout.Tab tab = tabLayout.getTabAt(1);
                        if (num != null & tab != null) {
                            tab.setText(getString(R.string.videos_format_str, num));
                        }
                    });
                    return f;
                } else {
                    PictureFragment f = new PictureFragment();
                    f.getNumOfPictures().observe(AlbumActivity.this, num -> {
                        TabLayout.Tab tab = tabLayout.getTabAt(0);
                        if (num != null & tab != null) {
                            tab.setText(getString(R.string.pictures_format_str, num));
                        }
                    });
                    return f;
                }
            }
        };
        ViewPager2 pager = findViewById(R.id.album_pager);
        pager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, pager, (tab, pos) -> {
            if (pos == 0)
                tab.setText(R.string.pictures);
            if (pos == 1)
                tab.setText(R.string.videos);
        }).attach();
    }
}