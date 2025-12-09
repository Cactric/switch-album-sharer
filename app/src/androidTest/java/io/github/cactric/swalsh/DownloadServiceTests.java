package io.github.cactric.swalsh;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Network;
import android.net.Uri;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.Observer;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ServiceTestRule;

import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

public class DownloadServiceTests {
    @Rule
    public final ServiceTestRule serviceRule = new ServiceTestRule();
    // Json string that can be replaced
    private int jsonIndex;

    @Test
    public void normalDownloadTest() {
        Context targetCtx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Context testCtx = InstrumentationRegistry.getInstrumentation().getContext();
        CountDownLatch latch = new CountDownLatch(1);

        Intent intent = new Intent(targetCtx, MockDownloadService.class);
        WifiNetworkSpecifier netSpec = new WifiNetworkSpecifier.Builder()
                .setSsid("AndroidWifi")
                .build();
        intent.putExtra("EXTRA_NETWORK_SPECIFIER", netSpec);
        intent.putExtra("EXTRA_SCAN_TIME", new Date().getTime());
        intent.putExtra("EXTRA_MOCK_JSON", testCtx.getResources().getStringArray(
                io.github.cactric.swalsh.test.R.array.test_json_strings)[0]
        );
        intent.putExtra("EXTRA_ALT_URL", "http://10.0.2.2:8080/");

        ComponentName service = targetCtx.startService(intent);
        Log.d("SwAlSh_Tests", "startService returned " + service);
        // Make sure startService succeeded
        assertNotNull(service);

        targetCtx.bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                DownloadService.DownloadServiceBinder binder = (DownloadService.DownloadServiceBinder) iBinder;

                Observer<DownloadService.State> testObserver = state -> {
                    if (state == DownloadService.State.ERROR) {
                        Log.e("SwAlSh_Tests", "Error state: reason is " + binder.getErrorType().getValue());
                    }
                    assertNotEquals(DownloadService.State.ERROR, state);

                    if (state == DownloadService.State.DONE) {
                        latch.countDown();
                    }
                };

                binder.getState().observeForever(testObserver);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                throw new RuntimeException("Unreachable?");
            }
        }, Context.BIND_AUTO_CREATE);

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // TODO: Check it wrote the files
        // TODO: Start a mock endpoint
        // TODO: accept connect to device prompt
    }
}
