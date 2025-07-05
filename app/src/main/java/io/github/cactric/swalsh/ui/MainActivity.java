package io.github.cactric.swalsh.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.MenuProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.Navigation;

import io.github.cactric.swalsh.R;
import io.github.cactric.swalsh.ui.album.AlbumActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_linear_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.app_name_long);
        setSupportActionBar(toolbar);

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
        findViewById(R.id.intro_scan_button).setOnClickListener(v -> {
            if (checkWifiIsEnabled()) {
                requestPermLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        findViewById(R.id.intro_manual_button).setOnClickListener(v -> {
            if (checkWifiIsEnabled()) {
                Intent intent = new Intent(this, ManualActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.intro_album_button).setOnClickListener(v -> {
            Intent intent = new Intent(this, AlbumActivity.class);
            startActivity(intent);
        });

    }

    // TODO: maybe move this logic to the scanning activity
    private final ActivityResultLauncher<String> requestPermLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
        if (granted) {
            Intent intent = new Intent(this, ScanActivity.class);
            startActivity(intent);
        } else {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
            alertBuilder.setMessage("Camera permission is needed to scan the QR code");
            alertBuilder.setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());
            alertBuilder.setOnDismissListener(d -> {});
            alertBuilder.show();
        }
    });

    /**
     * Checks if Wifi is enabled. If it is, it returns true, otherwise it alerts the user to turn it
     * on and returns false.
     * @return True if Wifi is enabled
     */
    private boolean checkWifiIsEnabled() {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (wifiManager.isWifiEnabled()) {
            return true;
        } else {
            // Alert the user and then return false
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.wifi_disabled_message);
            builder.setTitle(R.string.wifi_disabled);
            builder.setPositiveButton(R.string.wifi_settings, (dialog, which) -> {
                Intent wifiSettingsIntent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                if (wifiSettingsIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(wifiSettingsIntent);
                }
            });
            builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss());
            builder.create().show();
            return false;
        }
    }
}