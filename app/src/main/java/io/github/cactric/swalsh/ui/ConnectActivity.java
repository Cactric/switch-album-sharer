package io.github.cactric.swalsh.ui;

import static android.view.View.VISIBLE;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.LiveData;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.github.cactric.swalsh.DownloadService;
import io.github.cactric.swalsh.R;
import io.github.cactric.swalsh.WifiUtils;
import io.github.cactric.swalsh.ui.album.AlbumActivity;

/*
This activity takes 2 or 3 extras:
- scanned_data (string): the Wi-Fi network data scanned from a QR code
- scan_time (long): the time (seconds since Epoch) the code was scanned. Used to prevent duplicate downloads.
- ssid (string): the ssid of the Wi-Fi network if scanned_data wasn't added.
- pass (string): the password of the Wi-Fi network if scanned_data wasn't added.
*/

public class ConnectActivity extends AppCompatActivity {
    private DownloadService.DownloadServiceBinder binder;
    private LiveData<DownloadService.State> state;
    private LiveData<Integer> errorStringIndex;
    private LiveData<Integer> numDownloaded;
    private LiveData<Integer> numFailed;
    private LiveData<Float> fileProgress;
    private int errno = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_connect);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.connect_toolbar);
        toolbar.setTitle(R.string.connect_to_console);

        if (checkWifiIsEnabled()) {
            connectToConsole();
        }
    }

    private void connectToConsole() {
        WifiNetworkSpecifier netSpec = null;
        Intent arguments = getIntent();
        if (arguments.hasExtra("scanned_data")) {
            try {
                netSpec = WifiUtils.parseNetwork(Objects.requireNonNull(arguments.getStringExtra("scanned_data")));
            } catch (IllegalArgumentException e) {
                // Not a Wi-Fi QR code
                Log.e("SwAlSh", "Invalid QR code", e);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.bad_qr_contents);
                builder.setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());
                builder.setOnDismissListener(dialog -> {
                    // Go back to code scanner
                    finish();
                });
                builder.create().show();
            }
        } else if (arguments.hasExtra("ssid") && arguments.hasExtra("pass")) {
            @NonNull String ssid = Objects.requireNonNull(arguments.getStringExtra("ssid"));
            @NonNull String pass = Objects.requireNonNull(arguments.getStringExtra("pass"));
            netSpec = WifiUtils.basicNetwork(ssid, pass);
        } else {
            throw new RuntimeException("scanned_data or ssid and pass must be added as extras to use ConnectActivity");
        }

        long scanTime = arguments.getLongExtra("scan_time", -1);
        if (scanTime == -1L) {
            throw new RuntimeException("scan_time must be added as an extra to ConnectActivity");
        }

        Intent intent = new Intent(this, DownloadService.class);
        intent.putExtra("EXTRA_NETWORK_SPECIFIER", netSpec);
        intent.putExtra("EXTRA_SCAN_TIME", scanTime);
        startService(intent);

        Button sacButton = findViewById(R.id.another_code);
        Button albumButton = findViewById(R.id.open_album);

        sacButton.setOnClickListener(v -> finish()); // Go back when "Scan another" is pressed.

        albumButton.setOnClickListener(v -> {
            Intent albumIntent = new Intent(ConnectActivity.this, AlbumActivity.class);
            if (binder.getFileType().equals("movie"))
                albumIntent.putExtra("EXTRA_STARTING_TAB", 1);
            startActivity(albumIntent);
        });

        ServiceConnection connection = new ConnectServiceConnection(
                findViewById(R.id.state_text),
                findViewById(R.id.save_location_text),
                findViewById(R.id.progressBar),
                sacButton,
                albumButton,
                findViewById(R.id.share)
        );

        bindService(intent, connection, BIND_AUTO_CREATE);

    }

    private class ConnectServiceConnection implements ServiceConnection {
        private final TextView stateText;
        private final TextView saveDirText;
        private final ProgressBar progressBar;
        private final Button sacButton;
        private final Button albumButton;
        private final Button shareButton;

        public ConnectServiceConnection(TextView stateText, TextView saveDirText, ProgressBar progressBar, Button sacButton, Button albumButton, Button shareButton) {
            this.stateText = stateText;
            this.saveDirText = saveDirText;
            this.progressBar = progressBar;
            this.sacButton = sacButton;
            this.albumButton = albumButton;
            this.shareButton = shareButton;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = (DownloadService.DownloadServiceBinder) service;
            if (binder != null) {
                state = binder.getState();
                errorStringIndex = binder.getErrorStringIndex();
                numDownloaded = binder.getNumDownloaded();
                numFailed = binder.getNumFailed();
                fileProgress = binder.getDownloadProgress();
                LiveData<Long> binderScanTime = binder.getScanTime();
                Log.d("SwAlSh", "Got binder");

                errorStringIndex.observe(ConnectActivity.this, newError -> {
                    if (newError != null) {
                        errno = newError;
                        Log.d("SwAlSh", "Set errno to " + errno);
                    }
                });

                state.observe(ConnectActivity.this, newState -> {
                    // Update the UI
                    if (state.getValue() != null)
                        formatStateText(state.getValue());
                    else
                        stateText.setText(R.string.error);

                    if (state.getValue() == DownloadService.State.DOWNLOADING) {
                        // Display total number of items and use it for the progress bar
                        if (binder.getNumToDownload() > 1)
                            progressBar.setMax(binder.getNumToDownload());
                        else
                            progressBar.setMax(100);
                    }

                    if (state.getValue() == DownloadService.State.DONE) {
                        if (numFailed != null &&
                                numFailed.getValue() != null &&
                                numFailed.getValue() > 0) {
                            // Alert the user that some failed
                            AlertDialog.Builder builder = new AlertDialog.Builder(ConnectActivity.this);
                            builder.setMessage(getResources().getQuantityString(
                                    R.plurals.n_failed_to_download_format,
                                    numFailed.getValue(), // Used for deciding which plural string to use
                                    numFailed.getValue() // Used for formatting
                            ));
                            builder.setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());
                            builder.create().show();
                        }

                        saveDirText.setVisibility(VISIBLE);
                        Integer amount = binder.getNumDownloaded().getValue();
                        if (amount == null)
                            amount = 1; // Avoid null problems by setting amount explicitly if necessary
                        if (binder.getFileType().equals("photo")) {
                            saveDirText.setText(getResources().getQuantityString(R.plurals.pictures_location_format, amount, binder.getPicturesDir()));
                        } else if (binder.getFileType().equals("movie")) {
                            saveDirText.setText(getResources().getQuantityString(R.plurals.videos_location_format, amount, binder.getVideosDir()));
                        }

                        sacButton.setVisibility(VISIBLE);
                        albumButton.setVisibility(VISIBLE);
                        shareButton.setVisibility(VISIBLE);
                        shareButton.setOnClickListener(v -> {
                            List<Uri> contentUris = binder.getSavedContentUriList();
                            if (contentUris == null || contentUris.isEmpty()) {
                                // Share... nothing?
                                Toast.makeText(ConnectActivity.this, getString(R.string.not_ready_to_share), Toast.LENGTH_SHORT).show();
                            } else if (contentUris.size() == 1) {
                                // Share one item
                                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUris.get(0));
                                if (binder.getFileType().equals("photo")) {
                                    shareIntent.setType("image/jpeg");
                                } else if (binder.getFileType().equals("movie")) {
                                    shareIntent.setType("video/mp4");
                                } else {
                                    Log.d("SwAlSh", "Unknown file type " + binder.getFileType());
                                }
                                startActivity(Intent.createChooser(shareIntent, null));
                            } else {
                                // Share multiple things
                                Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                                shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, new ArrayList<>(contentUris));
                                // Can only currently share multiple pictures, not videos
                                if (binder.getFileType().equals("photo")) {
                                    shareIntent.setType("image/jpeg");
                                } else if (binder.getFileType().equals("movie")) {
                                    shareIntent.setType("video/mp4");
                                } else {
                                    Log.d("SwAlSh", "Unknown file type " + binder.getFileType());
                                }
                                startActivity(Intent.createChooser(shareIntent, null));
                            }
                        });
                    }

                    if (state.getValue() == DownloadService.State.ERROR) {
                        // Stop the progress bar since nothing will happen now
                        progressBar.setIndeterminate(false);

                        String[] errors = getResources().getStringArray(R.array.errors);
                        AlertDialog.Builder builder = new AlertDialog.Builder(ConnectActivity.this);
                        builder.setMessage(errors[errno]);
                        builder.setTitle(R.string.comm_error);
                        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());
                        builder.setOnDismissListener(dialog -> {
                            // Go back to code scanner
                            finish();
                        });
                        Intent stopintent = new Intent(ConnectActivity.this, DownloadService.class);
                        stopService(stopintent);

                        builder.create().show();
                    }
                });

                numDownloaded.observe(ConnectActivity.this, num -> {
                    if (binder.getNumToDownload() > 1) {
                        progressBar.setIndeterminate(num <= 0);
                        progressBar.setProgress(num, true);
                    }
                    if (state.getValue() != null)
                        formatStateText(state.getValue());
                });

                fileProgress.observe(ConnectActivity.this, num -> {
                    if (binder.getNumToDownload() <= 1) {
                        progressBar.setIndeterminate(num <= 0.0f);
                        progressBar.setProgress((int) (num * 100), true);
                        if (state.getValue() != null)
                            formatStateText(state.getValue());
                    }
                });

                binderScanTime.observe(ConnectActivity.this, time -> Log.d("SwAlSh", "Scan time changed to " + time));

            }
        }

        private void formatStateText(DownloadService.State newState) {
            String[] states = getResources().getStringArray(R.array.connection_states);
            switch (newState) {
                case NOT_STARTED, CONNECTING, CONNECTED ->
                        stateText.setText(states[newState.ordinal()]);
                case DOWNLOADING, DONE -> {
                    if (binder.getNumToDownload() == 1) {
                        // Show percent downloaded if there's only one file
                        float percent = 0.0f;
                        if (binder.getDownloadProgress().getValue() != null) {
                            percent = binder.getDownloadProgress().getValue() * 100.0f;
                        }
                        String formattedStr = getString(R.string.connection_state_single,
                                states[newState.ordinal()],
                                percent);
                        stateText.setText(formattedStr);
                    } else {
                        String formattedStr = getString(R.string.connection_state,
                                states[newState.ordinal()],
                                numDownloaded.getValue(),
                                binder.getNumToDownload());
                        stateText.setText(formattedStr);
                    }
                }
                case ERROR -> {
                    if (binder.getNumToDownload() > 1) {
                        stateText.setText(states[newState.ordinal()]);
                    } else {
                        String formattedStr = getString(R.string.connection_state,
                                states[newState.ordinal()],
                                numDownloaded.getValue(),
                                binder.getNumToDownload());
                        stateText.setText(formattedStr);
                    }
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    }

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
            builder.setNeutralButton(R.string.wifi_settings, (dialog, which) -> {
                Intent wifiSettingsIntent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                if (wifiSettingsIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(wifiSettingsIntent);
                }
            });
            builder.setPositiveButton("Try again", (dialog, which) -> {
                if (checkWifiIsEnabled())
                    connectToConsole();
            });
            builder.setNegativeButton(R.string.back, (dialog, which) -> finish());
            builder.setCancelable(false);
            builder.create().show();
            return false;
        }
    }
}