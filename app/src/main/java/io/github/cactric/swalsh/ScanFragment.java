package io.github.cactric.swalsh;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraProvider;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.core.impl.CameraConfig;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Date;
import java.util.concurrent.ExecutionException;

public class ScanFragment extends Fragment {
    private CodeScanner scanner;
    private ScaleGestureDetector pinchDetector;

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

        scanner = new CodeScanner(result -> activity.runOnUiThread(() -> {
            // Change fragment to connect fragment with the scanned data
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

        PreviewView previewView = root.findViewById(R.id.camera_preview);

        CameraXConfig camConfig = Camera2Config.defaultConfig();
        ListenableFuture<ProcessCameraProvider> camProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        camProviderFuture.addListener(() -> {
            try {
                Log.d("SwAlSh", "In CamProviderFuture");
                ProcessCameraProvider camProvider = camProviderFuture.get();
                camProvider.unbindAll();

                Preview preview = new Preview.Builder().build();
                // TODO: allow user to change to front camera?
                // Not sure how necessary that would be though
                CameraSelector camSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().build();
                imageAnalysis.setAnalyzer(requireContext().getMainExecutor(), scanner);

                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                Camera cam = camProvider.bindToLifecycle(getViewLifecycleOwner(),
                        camSelector,
                        preview,
                        imageAnalysis);
                // TODO: autofocus
            } catch (ExecutionException | InterruptedException e) {
                Log.e("SwAlSh", "Binding failed", e);
            }
        }, requireContext().getMainExecutor());

        // Create a gesture detector for pinch-to-zoom
        pinchDetector = new ScaleGestureDetector(requireContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(@NonNull ScaleGestureDetector detector) {
//                int zoom = scanner.getZoom();
//                int zoomAmount = (int) ((detector.getScaleFactor() - 1.0f) / 0.005f);
//
//                zoom += zoomAmount;
//                if (zoom < 0)
//                    zoom = 0;
//
//                try {
//                    scanner.setZoom(zoom);
//                } catch (Exception ignored) {
//                }
                // TODO: add bounds on the scale factor
                return true;
            }
        });
        // Add the scale gesture detector to the scanner view
        // Supposed to do performClick too for accessibility
        // Zoom buttons will be the accessible alternative to pinch-to-zoom though
        previewView.setOnTouchListener((v, event) -> {
            pinchDetector.onTouchEvent(event);
            return true;
        });

        // Add menu with zoom buttons (in/out/reset)
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.zoom_menu, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
//                final int ZOOM_AMOUNT = 3;
//                if (menuItem.getItemId() == R.id.menu_zoom_in) {
//                    scanner.setZoom(scanner.getZoom() + ZOOM_AMOUNT);
//                    return true;
//                } else if (menuItem.getItemId() == R.id.menu_zoom_out) {
//                    scanner.setZoom(Math.max(scanner.getZoom() - ZOOM_AMOUNT, 0));
//                    return true;
//                } else if (menuItem.getItemId() == R.id.menu_zoom_reset) {
//                    scanner.setZoom(0);
//                    return true;
//                }
                return false;
            }
        }, getViewLifecycleOwner());
        return root;
    }
}