package io.github.com.cactric.swalsh;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;

public class ScanFragment extends Fragment {
    private CodeScanner mScanner;
    public ScanFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final FragmentActivity activity = requireActivity();
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_scan, container, false);
        CodeScannerView scannerView = root.findViewById(R.id.scanner);
        mScanner = new CodeScanner(activity, scannerView);
        mScanner.setDecodeCallback(result -> {
            activity.runOnUiThread(() -> {
                Log.d("SwAlSh", "Code result: " + result.getText());

                // Change fragment
                NavHostFragment navHostFragment = (NavHostFragment)
                        activity.getSupportFragmentManager().findFragmentById(R.id.mainFragmentContainer);
                NavController navController = navHostFragment.getNavController();
                navController.navigate(R.id.action_destination_scan_to_connectFragment);
            });
        });
        scannerView.setOnClickListener(v -> {
            mScanner.startPreview();
        });
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        mScanner.startPreview();
    }

    @Override
    public void onPause() {
        mScanner.releaseResources();
        super.onPause();
    }
}