package io.github.cactric.swalsh.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ScaleGestureDetector;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.core.ZoomState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.graphics.Insets;
import androidx.core.view.MenuProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Date;
import java.util.concurrent.ExecutionException;

import io.github.cactric.swalsh.CodeScanner;
import io.github.cactric.swalsh.R;

public class ScanActivity extends AppCompatActivity {
    private CodeScanner scanner;
    private ScaleGestureDetector pinchDetector;
    private Camera cam;
    private ProcessCameraProvider camProvider;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_scan);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        requestPermLauncher.launch(Manifest.permission.CAMERA);

        scanner = new CodeScanner(result -> runOnUiThread(() -> {
            Intent intent = new Intent(ScanActivity.this, ConnectActivity.class);
            intent.putExtra("scanned_data", result.getText());
            intent.putExtra("scan_time", new Date().getTime());
            startActivity(intent);
            if (camProvider != null)
                camProvider.unbindAll();
        }));

        Toolbar toolbar = findViewById(R.id.scan_toolbar);
        toolbar.setTitle(R.string.scan_code);
        toolbar.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.zoom_menu, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                final float ZOOM_AMOUNT = 0.2f;
                if (menuItem.getItemId() == R.id.menu_zoom_in) {
                    changeZoom(ZOOM_AMOUNT);
                    return true;
                } else if (menuItem.getItemId() == R.id.menu_zoom_out) {
                    changeZoom(-ZOOM_AMOUNT);
                    return true;
                } else if (menuItem.getItemId() == R.id.menu_zoom_reset) {
                    // Set to the furthest out it can be
                    cam.getCameraControl().setLinearZoom(0.0f);
                    return true;
                }
                return false;
            }
        });


        // Create a gesture detector for pinch-to-zoom
        pinchDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(@NonNull ScaleGestureDetector detector) {
                float zoomAmount = (detector.getScaleFactor() - 1.0f) / 0.4f;
                changeZoom(zoomAmount);
                return true;
            }
        });
        // Add the scale gesture detector to the scanner view
        // Supposed to do performClick too for accessibility
        // Zoom buttons will be the accessible alternative to pinch-to-zoom though
        PreviewView previewView = findViewById(R.id.camera_preview);
        previewView.setOnTouchListener((v, event) -> {
            pinchDetector.onTouchEvent(event);
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        PreviewView previewView = findViewById(R.id.camera_preview);
        ListenableFuture<ProcessCameraProvider> camProviderFuture = ProcessCameraProvider.getInstance(this);
        camProviderFuture.addListener(() -> {
            try {
                camProvider = camProviderFuture.get();
                camProvider.unbindAll();

                Preview preview = new Preview.Builder().build();
                // TODO: allow user to change to front camera?
                // Not sure how necessary that would be though
                CameraSelector camSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                imageAnalysis.setAnalyzer(getMainExecutor(), scanner);

                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                cam = camProvider.bindToLifecycle(this,
                        camSelector,
                        preview,
                        imageAnalysis);
            } catch (ExecutionException | InterruptedException e) {
                Log.e("SwAlSh", "Binding failed", e);
            }
        }, getMainExecutor());
    }

    /**
     * Zoom in or out the camera by adding amount to the zoom ratio, then applying bounds checks
     * @param amount Amount to add to the zoom ratio
     */
    private void changeZoom(float amount) {
        ZoomState zoomState = cam.getCameraInfo().getZoomState().getValue();
        if (zoomState != null) {
            float zoomRatio = zoomState.getZoomRatio();
            zoomRatio += amount;
            // Apply bounds
            zoomRatio = Float.max(zoomState.getMinZoomRatio(), zoomRatio);
            zoomRatio = Float.min(zoomState.getMaxZoomRatio(), zoomRatio);
            cam.getCameraControl().setZoomRatio(zoomRatio);
        } else {
            Log.e("SwAlSh", "Zoom state obtained from Camera Info is null - zoom not applied");
        }
    }

    private final ActivityResultLauncher<String> requestPermLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
        if (!granted) {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
            alertBuilder.setMessage("Camera permission is needed to scan the QR code");
            alertBuilder.setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());
            alertBuilder.setOnDismissListener(d -> finish());
            alertBuilder.show();
            // TODO: add a button to go to settings
        }
    });

}