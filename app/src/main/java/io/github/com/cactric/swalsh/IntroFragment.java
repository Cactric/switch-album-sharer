package io.github.com.cactric.swalsh;

import android.Manifest;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
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