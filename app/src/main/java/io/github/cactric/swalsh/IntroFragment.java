package io.github.cactric.swalsh;

import android.Manifest;
import android.app.ActivityOptions;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
        TransitionInflater ti = TransitionInflater.from(requireContext());
        setEnterTransition(ti.inflateTransition(android.R.transition.slide_right));
        setExitTransition(ti.inflateTransition(android.R.transition.fade));
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

        root.findViewById(R.id.intro_manual_button).setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_manual_entry));

        root.findViewById(R.id.intro_album_button).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AlbumActivity.class);
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(getActivity()).toBundle());
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