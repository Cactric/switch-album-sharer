package io.github.cactric.swalsh;

import android.hardware.Camera;
import android.os.Bundle;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.budiyev.android.codescanner.ScanMode;
import com.google.zxing.BarcodeFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class ScanFragment extends Fragment {
    private CodeScanner mScanner;
    private ScaleGestureDetector mScaler;
    private float mScaleFactor = 1.f / 50.f;

    public ScanFragment() {
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
        final FragmentActivity activity = requireActivity();
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_scan, container, false);
        CodeScannerView scannerView = root.findViewById(R.id.scanner);
        mScaler = new ScaleGestureDetector(requireContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(@NonNull ScaleGestureDetector detector) {
                mScaleFactor *= detector.getScaleFactor();
                try {
                    mScanner.setZoom(Math.round(50.f * mScaleFactor));
                } catch (Exception ignored) {
                }
                // TODO: add bounds on the scale factor
                return true;
            }
        });
        scannerView.setOnTouchListener((v, event) -> {
            mScaler.onTouchEvent(event);
            return true;
        });
        mScanner = new CodeScanner(activity, scannerView);
        // Just scan for QR codes
        mScanner.setFormats(List.of(BarcodeFormat.QR_CODE));
        mScanner.setScanMode(ScanMode.SINGLE);
        mScanner.setTouchFocusEnabled(true);
        mScanner.setDecodeCallback(result -> activity.runOnUiThread(() -> {
            // Change fragment
            NavHostFragment navHostFragment = (NavHostFragment)
                    activity.getSupportFragmentManager().findFragmentById(R.id.mainFragmentContainer);
            NavController navController;
            if (navHostFragment != null) {
                navController = navHostFragment.getNavController();
                Bundle bundle = new Bundle();
                bundle.putString("scanned_data", result.getText());
                Date d = new Date();
                bundle.putLong("scan_time", d.getTime());
                navController.navigate(R.id.action_destination_scan_to_connectFragment, bundle);
            }

        }));
        scannerView.setOnClickListener(v -> mScanner.startPreview());
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