package io.github.cactric.swalsh;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.budiyev.android.codescanner.ScanMode;
import com.google.zxing.BarcodeFormat;

import java.util.Date;
import java.util.List;

public class ScanFragment extends Fragment {
    private CodeScanner mScanner;
    private ScaleGestureDetector mScaler;

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

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final FragmentActivity activity = requireActivity();
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_scan, container, false);
        CodeScannerView scannerView = root.findViewById(R.id.scanner);
        // Create a gesture detector for pinch-to-zoom
        mScaler = new ScaleGestureDetector(requireContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(@NonNull ScaleGestureDetector detector) {
                int zoom = mScanner.getZoom();
                int zoomAmount = (int) ((detector.getScaleFactor() - 1.0f) / 0.005f);

                zoom += zoomAmount;
                if (zoom < 0)
                    zoom = 0;

                try {
                    mScanner.setZoom(zoom);
                } catch (Exception ignored) {
                }
                // TODO: add bounds on the scale factor
                return true;
            }
        });
        // Add the scale gesture detector to the scanner view
        // Supposed to do performClick too for accessibility
        // Zoom buttons will be the accessible alternative to pinch-to-zoom though
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

        // Add menu with zoom buttons (in/out/reset)
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.zoom_menu, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                final int ZOOM_AMOUNT = 3;
                if (menuItem.getItemId() == R.id.menu_zoom_in) {
                    mScanner.setZoom(mScanner.getZoom() + ZOOM_AMOUNT);
                    return true;
                } else if (menuItem.getItemId() == R.id.menu_zoom_out) {
                    mScanner.setZoom(Math.max(mScanner.getZoom() - ZOOM_AMOUNT, 0));
                    return true;
                } else if (menuItem.getItemId() == R.id.menu_zoom_reset) {
                    mScanner.setZoom(0);
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner());
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