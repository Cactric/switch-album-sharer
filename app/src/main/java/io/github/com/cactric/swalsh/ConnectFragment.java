package io.github.com.cactric.swalsh;

import static android.net.NetworkCapabilities.TRANSPORT_WIFI;
import static io.github.com.cactric.swalsh.WifiUtils.parseNetwork;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ConnectFragment extends Fragment {

    private static final String ARG_SCANNED_DATA = "scanned_data";

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
                // TODO: string resource
                builder.setMessage("The scanned code does not seem to be for a Wifi network");
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
            //bindService?
        }

        return root;
    }
}