package io.github.cactric.swalsh.ui.scan_flow_fragments;

import static android.content.Context.BIND_AUTO_CREATE;
import static android.view.View.VISIBLE;
import static io.github.cactric.swalsh.WifiUtils.parseNetwork;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Bundle;
import android.os.IBinder;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import java.util.ArrayList;
import java.util.List;

import io.github.cactric.swalsh.DownloadService;
import io.github.cactric.swalsh.R;
import io.github.cactric.swalsh.ui.album.AlbumActivity;

public class ConnectFragment extends Fragment {

    private static final String ARG_SCANNED_DATA = "scanned_data";
    private static final String ARG_SCAN_TIME = "scan_time";
    private DownloadService.DownloadServiceBinder binder;
    private LiveData<DownloadService.State> state;
    private LiveData<Integer> errorStringIndex;
    private LiveData<Integer> numDownloaded;
    private LiveData<Integer> numFailed;
    private LiveData<Float> fileProgress;
    private int errno = 0;

    public ConnectFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TransitionInflater ti = TransitionInflater.from(requireContext());
        setEnterTransition(ti.inflateTransition(android.R.transition.slide_right));
        setExitTransition(ti.inflateTransition(android.R.transition.fade));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_connect, container, false);

        if (getArguments() != null) {
            String scannedData = getArguments().getString(ARG_SCANNED_DATA);
            WifiNetworkSpecifier netSpec = null;
            boolean success = false;
            if (scannedData != null)
                try {
                    netSpec = parseNetwork(scannedData);
                    success = true;
                } catch (IllegalArgumentException e) {
                    // Not a Wifi code?
                }
            if (success) {
                long scanTime = getArguments().getLong(ARG_SCAN_TIME, -1);
                if (scanTime == -1)
                    Log.w("SwAlSh", "ARG_SCAN_TIME was not set - download service may not restart on subsequent scans");

                // Start the download service
                Intent intent = new Intent(getContext(), DownloadService.class);
                intent.putExtra("EXTRA_NETWORK_SPECIFIER", netSpec);
                intent.putExtra("EXTRA_SCAN_TIME", scanTime);
                requireContext().startService(intent);

                TextView stateText = root.findViewById(R.id.state_text);
                TextView saveDirText = root.findViewById(R.id.save_location_text);
                ProgressBar progressBar = root.findViewById(R.id.progressBar);

                Button sacButton = root.findViewById(R.id.another_code);
                Button albumButton = root.findViewById(R.id.open_album);
                Button shareButton = root.findViewById(R.id.share);

                sacButton.setOnClickListener(v -> {
                    // Go back to code scanner
                    NavHostFragment navHostFragment = (NavHostFragment)
                            requireActivity().getSupportFragmentManager().findFragmentById(R.id.mainFragmentContainer);
                    NavController navController;
                    if (navHostFragment != null) {
                        navController = navHostFragment.getNavController();
                        navController.popBackStack();
                    }
                });

                albumButton.setOnClickListener(v -> {
                    Intent albumIntent = new Intent(getActivity(), AlbumActivity.class);
                    if (binder.getFileType().equals("movie"))
                        albumIntent.putExtra("EXTRA_STARTING_TAB", 1);
                    startActivity(albumIntent);
                });

                shareButton.setOnClickListener(v -> Toast.makeText(requireContext(),
                        getString(R.string.not_ready_to_share), Toast.LENGTH_SHORT).show());

                ServiceConnection connection = new ServiceConnection() {
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

                            errorStringIndex.observe(getViewLifecycleOwner(), newError -> {
                                if (newError != null) {
                                    errno = newError;
                                    Log.d("SwAlSh", "Set errno to " + errno);
                                }
                            });

                            state.observe(getViewLifecycleOwner(), newState -> {
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
                                        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
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
                                            Toast.makeText(requireContext(), getString(R.string.not_ready_to_share), Toast.LENGTH_SHORT).show();
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
                                    AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                                    builder.setMessage(errors[errno]);
                                    builder.setTitle(R.string.comm_error);
                                    builder.setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());
                                    builder.setOnDismissListener(dialog -> {
                                        // Go back to code scanner
                                        NavHostFragment navHostFragment = (NavHostFragment)
                                                requireActivity().getSupportFragmentManager().findFragmentById(R.id.mainFragmentContainer);
                                        NavController navController;
                                        if (navHostFragment != null) {
                                            navController = navHostFragment.getNavController();
                                            navController.popBackStack();
                                        }
                                    });

                                    builder.create().show();
                                }
                            });

                            numDownloaded.observe(getViewLifecycleOwner(), num -> {
                                if (binder.getNumToDownload() > 1) {
                                    progressBar.setIndeterminate(num <= 0);
                                    progressBar.setProgress(num, true);
                                }
                                if (state.getValue() != null)
                                    formatStateText(state.getValue());
                            });

                            fileProgress.observe(getViewLifecycleOwner(), num -> {
                                if (binder.getNumToDownload() <= 1) {
                                    progressBar.setIndeterminate(num <= 0.0f);
                                    progressBar.setProgress((int) (num * 100), true);
                                    if (state.getValue() != null)
                                        formatStateText(state.getValue());
                                }
                            });

                            binderScanTime.observe(getViewLifecycleOwner(), time -> {
                                Log.d("SwAlSh", "Scan time changed to " + time);
                            });

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
                };

                requireContext().bindService(intent, connection, BIND_AUTO_CREATE);
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setMessage(R.string.bad_qr_contents);
                builder.setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());
                builder.setOnDismissListener(dialog -> {
                    // Go back to code scanner
                    NavHostFragment navHostFragment = (NavHostFragment)
                            requireActivity().getSupportFragmentManager().findFragmentById(R.id.mainFragmentContainer);
                    NavController navController;
                    if (navHostFragment != null) {
                        navController = navHostFragment.getNavController();
                        navController.popBackStack();
                    }
                });
                builder.create().show();
            }
        }
        return root;
    }
}