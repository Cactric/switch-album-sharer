package io.github.cactric.swalsh;

import android.content.Intent;
import android.net.Network;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.net.URL;

/**
 * Version of DownloadService that doesn't actually connect to the console, used for testing
 */
public class MockDownloadService extends DownloadService {
    private String mockJson;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("SwAlSh_Tests", "MockDownloadService starting");
        mockJson = intent.getStringExtra("EXTRA_MOCK_JSON");

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected String getDataJson(@NonNull Network network) {
        return mockJson;
    }

    @Override
    protected void downloadMedia(@NonNull Network network, @NonNull URL sourceURL, @NonNull Uri destinationUri) throws IOException {
        Log.d("SwAlSh_Tests", "Network is " + network + ", sourceURL is " + sourceURL + ", destinationUri is " + destinationUri);
    }
}
