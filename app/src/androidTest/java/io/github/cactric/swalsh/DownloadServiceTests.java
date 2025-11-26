package io.github.cactric.swalsh;

import static org.junit.Assert.assertNotEquals;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.IBinder;

import androidx.lifecycle.Observer;
import androidx.test.espresso.action.MotionEvents;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ServiceTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Date;

public class DownloadServiceTests {
    @Rule
    public final ServiceTestRule serviceRule = new ServiceTestRule();

    @Test
    public void normalDownloadTest() {
        // TODO: Follow https://developer.android.com/training/testing/other-components/services#java more closely
        Context targetCtx = InstrumentationRegistry.getInstrumentation().getTargetContext();

        Intent intent = new Intent(targetCtx, DownloadService.class);
        WifiNetworkSpecifier netSpec = new WifiNetworkSpecifier.Builder()
                .setSsid("AndroidWifi")
                .build();
        intent.putExtra("EXTRA_NETWORK_SPECIFIER", netSpec);
        intent.putExtra("EXTRA_SCAN_TIME", new Date().getTime());
        intent.putExtra("EXTRA_ALT_URL", "http://10.0.2.2:8080/");
        targetCtx.startService(intent);

        targetCtx.bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                DownloadService.DownloadServiceBinder binder = (DownloadService.DownloadServiceBinder) iBinder;

                Observer<DownloadService.State> testObserver = state -> {
                    assertNotEquals(DownloadService.State.ERROR, state);

                    if (state == DownloadService.State.DONE) {
                        // TODO: Check it wrote the files
                    }
                };

                binder.getState().observeForever(testObserver);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                throw new RuntimeException("Unreachable?");
            }
        }, Context.BIND_AUTO_CREATE);

    }
}
