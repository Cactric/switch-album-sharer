package io.github.cactric.swalsh;

import android.util.Log;

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
    private final QRCodeReader reader = new QRCodeReader();
    private final BarcodeDetectionListener listener;
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
                imageData = new byte[planeBuffer.limit()];
                planeBuffer.get(imageData);
            }

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
            listener.onDecoded(r);
        } catch (NotFoundException ignored) {
        } catch (Exception e) {
            Log.e("SwAlSh", "Exception while analysing image", e);
        }
    }
}
