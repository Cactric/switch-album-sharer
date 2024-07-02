package io.github.cactric.swalsh;

import android.Manifest;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import android.transition.TransitionInflater;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

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
            ScanOptions options = new ScanOptions();
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
             options.setOrientationLocked(false);
            options.setBeepEnabled(false);
            options.setPrompt(getString(R.string.scan_prompt));
            barcodeLauncher.launch(options);
        });

        root.findViewById(R.id.intro_manual_button).setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_manual_entry));

        root.findViewById(R.id.intro_album_button).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AlbumActivity.class);
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(getActivity()).toBundle());
        });
        return root;
    }

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
        if (result.getContents() == null) {
            Toast.makeText(getContext(), "Didn't scan anything?", Toast.LENGTH_SHORT).show();
        } else {
            // Go to connect fragment with the result
            NavHostFragment navHostFragment = (NavHostFragment)
                    requireActivity().getSupportFragmentManager().findFragmentById(R.id.mainFragmentContainer);
            NavController navController;
            if (navHostFragment != null) {
                navController = navHostFragment.getNavController();
                Bundle bundle = new Bundle();
                bundle.putString("scanned_data", result.getContents());
                navController.navigate(R.id.action_scan_code, bundle);
            }
        }
    });
}