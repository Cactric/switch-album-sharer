package io.github.cactric.swalsh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import android.net.wifi.WifiNetworkSpecifier;

import org.junit.Test;

// These are instrumented tests to allow the use of WifiNetworkSpecifier
public class WifiUtilsTests {

    // Check output for a well-formed QR Wifi string
    @Test
    public void parseNetwork_works() {
        String input = "WIFI:S:switch_6CA04F0100J;T:WPA;P:qvk22ssf;;";
        WifiNetworkSpecifier.Builder expected = new WifiNetworkSpecifier.Builder();
        expected.setSsid("switch_6CA04F0100J");
        expected.setWpa2Passphrase("qvk22ssf");
        expected.setIsHiddenSsid(false);
        WifiNetworkSpecifier actualOutput = WifiUtils.parseNetwork(input);

        assertEquals(expected.build(), actualOutput);
    }

    // Check it fails with IllegalArgumentException if bad input is provided
    @Test
    public void parseNetwork_failsOnBadInput() {
        String[] badInputs = {
                "spaghetti",
                "https://github.com/cactric",
                "WIFI:S:" // cut off
        };

        for (String bad: badInputs) {
            assertThrows(IllegalArgumentException.class, () -> WifiUtils.parseNetwork(bad));
        }
    }
}
