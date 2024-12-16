package io.github.cactric.swalsh;

import com.google.zxing.Result;

public interface BarcodeDetectionListener {
    void onDecoded(Result result);
}
