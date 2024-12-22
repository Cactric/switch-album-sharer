package io.github.cactric.swalsh.ui.scan_flow_fragments;

import android.Manifest;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.provider.Settings;
import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import io.github.cactric.swalsh.R;
import io.github.cactric.swalsh.ui.album.AlbumActivity;
import io.github.cactric.swalsh.ui.InfoActivity;

public class IntroFragment extends Fragment {
    public IntroFragment() {
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
        View root = inflater.inflate(R.layout.fragment_intro, container, false);

        // Setup button functionality
        root.findViewById(R.id.intro_scan_button).setOnClickListener(v -> {
            if (checkWifiIsEnabled()) {
                storedNavController = Navigation.findNavController(v);
                requestPermLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        root.findViewById(R.id.intro_manual_button).setOnClickListener(v -> {
            if (checkWifiIsEnabled())
                Navigation.findNavController(v).navigate(R.id.action_manual_entry);
        });

        root.findViewById(R.id.intro_album_button).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AlbumActivity.class);
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(getActivity()).toBundle());
        });

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.main_menu, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.info_option) {
                    Intent intent = new Intent(getContext(), InfoActivity.class);
                    startActivity(intent);
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner());
        return root;
    }

    private NavController storedNavController;
    private final ActivityResultLauncher<String> requestPermLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
        if (granted) {
            if (storedNavController != null)
                storedNavController.navigate(R.id.action_scan_code);
        } else {
            Toast.makeText(getContext(), "Camera permission is needed to scan the QR code", Toast.LENGTH_SHORT).show();
        }
    });

    /**
     * Checks if Wifi is enabled. If it is, it returns true, otherwise it alerts the user to turn it
     * on and returns false.
     * @return True if Wifi is enabled
     */
    private boolean checkWifiIsEnabled() {
        WifiManager wifiManager = (WifiManager) requireContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager.isWifiEnabled()) {
            return true;
        } else {
            // Alert the user and then return false
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setMessage(R.string.wifi_disabled_message);
            builder.setTitle(R.string.wifi_disabled);
            builder.setPositiveButton(R.string.wifi_settings, (dialog, which) -> {
                Intent wifiSettingsIntent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                if (wifiSettingsIntent.resolveActivity(requireContext().getPackageManager()) != null) {
                    startActivity(wifiSettingsIntent);
                }
            });
            builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss());
            builder.create().show();
            return false;
        }
    }
}