package io.github.cactric.swalsh;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

public class IntroFragment extends Fragment {
    public IntroFragment() {
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
        View root = inflater.inflate(R.layout.fragment_intro, container, false);
        root.findViewById(R.id.intro_scan_button).setOnClickListener(v -> {
            storedNavController = Navigation.findNavController(v);
            requestPermLauncher.launch(Manifest.permission.CAMERA);
        });

        root.findViewById(R.id.intro_manual_button).setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_manual_entry));

        root.findViewById(R.id.intro_album_button).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AlbumActivity.class);
            startActivity(intent);
        });
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
}