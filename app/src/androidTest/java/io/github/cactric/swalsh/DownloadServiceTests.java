package io.github.cactric.swalsh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.IBinder;
import android.util.Log;

import androidx.lifecycle.Observer;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ServiceTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;

@LargeTest
public class DownloadServiceTests {
    @Rule
    public final ServiceTestRule serviceRule = new ServiceTestRule();
    private final ArrayList<Uri> savedUris = new ArrayList<>();
    private Context targetCtx;
    private Context testCtx;

    @Before
    public void setup() {
        targetCtx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        testCtx = InstrumentationRegistry.getInstrumentation().getContext();
    }

    @Test(timeout = 20000)
    public void singlePictureDownloadTest() throws TimeoutException {
        normalDownloadTestCore(0, 1);
    }

    @Test(timeout = 20000)
    public void multiPictureDownloadTest() throws TimeoutException {
        normalDownloadTestCore(1, 10);
    }

    @Test(timeout = 20000)
    public void singleVideoDownloadTest() throws TimeoutException {
        normalDownloadTestCore(2, 1);
    }

    @Test(timeout = 20000)
    public void multiVideoDownloadTest() throws TimeoutException {
        normalDownloadTestCore(3, 10);
    }

    public void normalDownloadTestCore(int jsonIndex, int expectedNumOfFiles) throws TimeoutException {
        CountDownLatch latch = new CountDownLatch(1);

        Intent intent = new Intent(targetCtx, MockDownloadService.class);
        WifiNetworkSpecifier netSpec = new WifiNetworkSpecifier.Builder()
                .setSsid("AndroidWifi")
                .build();
        intent.putExtra("EXTRA_NETWORK_SPECIFIER", netSpec);
        intent.putExtra("EXTRA_SCAN_TIME", new Date().getTime());
        intent.putExtra("EXTRA_MOCK_JSON", testCtx.getResources().getStringArray(
                io.github.cactric.swalsh.test.R.array.test_json_strings)[jsonIndex]
        );
        intent.putExtra("EXTRA_ALT_URL", "http://10.0.2.2:8080/");
        intent.putExtra("EXTRA_STAY_CONNECTED", true);

        serviceRule.startService(intent);

        targetCtx.bindService(intent, new ServiceConnection() {
            boolean done = false;
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                DownloadService.DownloadServiceBinder binder = (DownloadService.DownloadServiceBinder) iBinder;

                Observer<DownloadService.State> testObserver = state -> {
                    if (done) {
                        Log.d("SwAlSh_Tests", "Duplicate state observer callback!");
                        return;
                    }
                    if (state == DownloadService.State.ERROR) {
                        Log.e("SwAlSh_Tests", "Error state: reason is " + binder.getErrorType().getValue());
                    }
                    assertNotEquals(DownloadService.State.ERROR, state);

                    if (state == DownloadService.State.DONE) {
                        assertEquals(expectedNumOfFiles, binder.getSavedContentUriList().size());
                        savedUris.addAll(binder.getSavedContentUriList());
                        done = true;
                        latch.countDown();
                    }
                };

                binder.getState().observe(binder.getService(), testObserver);
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

        // TODO: accept connect to device prompt automatically, if possible
    }

    @After
    public void cleanupDownloadedFiles() {
        ContentResolver cr = targetCtx.getContentResolver();
        for (Uri uri: savedUris) {
            Log.d("SwAlSh_Tests", "Will cleanup Uri: " + uri);
            try {
                cr.delete(uri, null, null);
            } catch (Exception e) {
                Log.e("SwAlSh_Tests", "Failed to delete " + uri + ", but not considering it a failed test");
            }
        }
        savedUris.clear();
    }
}
