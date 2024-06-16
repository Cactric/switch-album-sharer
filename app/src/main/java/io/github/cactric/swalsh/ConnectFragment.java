package io.github.cactric.swalsh;

import static android.content.Context.BIND_AUTO_CREATE;
import static android.view.View.VISIBLE;
import static io.github.cactric.swalsh.WifiUtils.parseNetwork;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ConnectFragment extends Fragment {

    private static final String ARG_SCANNED_DATA = "scanned_data";
    private DownloadService.DownloadServiceBinder binder;
    private LiveData<DownloadService.State> state;
    private LiveData<Integer> numDownloaded;

    public static ConnectFragment newInstance(String scannedData) {
        ConnectFragment fragment = new ConnectFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SCANNED_DATA, scannedData);
        fragment.setArguments(args);
        return fragment;
    }

    public ConnectFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
            if (!success) {
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setMessage(requireContext().getResources().getString(R.string.bad_qr_contents));
                builder.setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());
                builder.setOnDismissListener(dialog -> {
                    // Go back to code scanner
                    NavHostFragment navHostFragment = (NavHostFragment)
                            requireActivity().getSupportFragmentManager().findFragmentById(R.id.mainFragmentContainer);
                    NavController navController = navHostFragment.getNavController();
                    navController.popBackStack();
                });
                builder.create().show();
            }

            // Start the download service
            Intent intent = new Intent(getContext(), DownloadService.class);
            intent.putExtra("EXTRA_NETWORK_SPECIFIER", netSpec);
            requireContext().startService(intent);

            TextView stateText = root.findViewById(R.id.state_text);
            ProgressBar progressBar = root.findViewById(R.id.progressBar);

            Button sacButton = root.findViewById(R.id.another_code);
            Button albumButton = root.findViewById(R.id.open_album);

            sacButton.setOnClickListener(v -> {
                // Go back to code scanner
                NavHostFragment navHostFragment = (NavHostFragment)
                        requireActivity().getSupportFragmentManager().findFragmentById(R.id.mainFragmentContainer);
                NavController navController = navHostFragment.getNavController();
                navController.popBackStack();
            });

            albumButton.setOnClickListener(v -> {
                Intent albumIntent = new Intent(getActivity(), AlbumActivity.class);
                startActivity(albumIntent);
            });

            ServiceConnection connection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    binder = (DownloadService.DownloadServiceBinder) service;
                    if (binder != null) {
                        state = binder.getState();
                        numDownloaded = binder.getNumDownloaded();
                        Log.d("SwAlSh", "Got binder");

                        state.observe(getViewLifecycleOwner(), newState -> {
                            // Update the UI
                            if (state.getValue() != null) {
                                String formattedStr = getString(R.string.connection_state,
                                        getResources().getStringArray(R.array.connection_states)[state.getValue().ordinal()],
                                        numDownloaded.getValue(), binder.getNumToDownload());
                                stateText.setText(formattedStr);
                            } else
                                stateText.setText(R.string.error);

                            if (state.getValue() == DownloadService.State.DOWNLOADING) {
                                // Display total number of items and use it for the progress bar
                                progressBar.setMax(binder.getNumToDownload());
                            }

                            if (state.getValue() == DownloadService.State.DONE) {
                                sacButton.setVisibility(VISIBLE);
                                albumButton.setVisibility(VISIBLE);
                            }
                        });

                        numDownloaded.observe(getViewLifecycleOwner(), num -> {
                            progressBar.setIndeterminate(num <= 0);
                            progressBar.setProgress(num, true);
                            if (state.getValue() != null) {
                                String formattedStr = getString(R.string.connection_state,
                                        getResources().getStringArray(R.array.connection_states)[state.getValue().ordinal()],
                                        numDownloaded.getValue(), binder.getNumToDownload());
                                stateText.setText(formattedStr);
                            }
                        });
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {

                }
            };

            requireContext().bindService(intent, connection, BIND_AUTO_CREATE);
        }

        return root;
    }
}