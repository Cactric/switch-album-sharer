package io.github.cactric.swalsh.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.MenuProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import io.github.cactric.swalsh.R;
import io.github.cactric.swalsh.databinding.ActivityMainBinding;
import io.github.cactric.swalsh.ui.album.AlbumActivity;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        binding.toolbar.setTitle(R.string.app_name_long);
        setSupportActionBar(binding.toolbar);

        addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.main_menu, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.info_option) {
                    Intent intent = new Intent(MainActivity.this, InfoActivity.class);
                    startActivity(intent);
                    return true;
                }
                return false;
            }
        });

        // Setup button functionality
        binding.introScanButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ScanActivity.class);
            startActivity(intent);
        });

        binding.introManualButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ManualActivity.class);
            startActivity(intent);
        });

        binding.introAlbumButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AlbumActivity.class);
            startActivity(intent);
        });

        reportFullyDrawn();
    }
}