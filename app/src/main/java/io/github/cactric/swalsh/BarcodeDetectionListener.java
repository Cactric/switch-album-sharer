package io.github.cactric.swalsh;

import com.google.zxing.Result;

public interface BarcodeDetectionListener {
    void onBarcodeFound(Result result);
}
