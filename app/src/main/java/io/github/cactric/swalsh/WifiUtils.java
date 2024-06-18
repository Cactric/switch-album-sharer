package io.github.cactric.swalsh;

import android.net.wifi.WifiNetworkSpecifier;
import android.util.Log;

import java.util.Scanner;

public class WifiUtils {
    public static WifiNetworkSpecifier parseNetwork(String qrWifiString) throws IllegalArgumentException {

        WifiNetworkSpecifier.Builder builder = new WifiNetworkSpecifier.Builder();

        // Check it begins with "WIFI:"
        if (!qrWifiString.startsWith("WIFI:")) {
            Log.e("SwAlSh", "The scanned code" + qrWifiString + "is not a Wifi code?");
            throw new IllegalArgumentException("The scanned codes doesn't seem to be a Wifi code");
        }
        Scanner scanner = new Scanner(qrWifiString.substring(5)).useDelimiter(";");

        while(scanner.hasNext()) {
            String token = scanner.next();

            // Skip past empty tokens
            if (token.isEmpty()) {
                continue;
            }

            // SSID
            if (token.charAt(0) == 'S') {
                builder.setSsid(token.split(":", 2)[1]);
            }
            // Type (of security) (ignored, WPA2 assumed)
            //if (token.charAt(0) == 'T') {
            //}

            // Passphrase
            if (token.charAt(0) == 'P') {
                builder.setWpa2Passphrase(token.split(":", 2)[1]);
            }

            // Hidden or not. Not used in this instance
            if (token.charAt(0) == 'H') {
                builder.setIsHiddenSsid(token.split(":", 2)[1].equals("true"));
            }
        }

        return builder.build();
    }
}
