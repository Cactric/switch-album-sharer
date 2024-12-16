package io.github.cactric.swalsh;

import android.graphics.Matrix;
import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.nio.ByteBuffer;

public class CodeScanner implements ImageAnalysis.Analyzer {
    // Use the QR specific reader since I only care about QR codes specifically
    private QRCodeReader reader = new QRCodeReader();
    private BarcodeDetectionListener listener;
    public CodeScanner(BarcodeDetectionListener listener) {
        this.listener = listener;
    }

    @Override
    public void analyze(@NonNull ImageProxy image) {
        try {
            ByteBuffer planeBuffer = image.getPlanes()[0].getBuffer();
            byte[] imageData;
            // If the plane's buffer has an underlying array, just use it
            // Otherwise, make a new array and copy the contents
            if (planeBuffer.hasArray()) {
                imageData = planeBuffer.array();
            } else {
                Log.d("SwAlSh", "Copying array: Limit: " + planeBuffer.limit());
                imageData = new byte[planeBuffer.limit()];
                planeBuffer.get(imageData);
            }

            Log.d("SwAlSh", "Image crop is " + image.getCropRect());

            PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                    imageData,
                    image.getWidth(),
                    image.getHeight(),
                    0, // TODO: crop image?
                    0,
                    image.getWidth(),
                    image.getHeight(),
                    false
            );
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result r = reader.decode(bitmap);
            listener.onBarcodeFound(r);
        } catch (NotFoundException ignored) {
        } catch (Exception e) {
            Log.e("SwAlSh", "Exception while analysing image", e);
        }
    }
}
