package io.github.com.cactric.swalsh;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadService extends Service {
    private boolean started = false;
    private boolean connected = false;
    private boolean error = false;
    private int numDownloaded = 0;
    private WorkerThread workerThread;
    private WifiNetworkSpecifier netSpec;

    public DownloadService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        netSpec = intent.getParcelableExtra("EXTRA_NETWORK_SPECIFIER");
        started = true;

        workerThread = new WorkerThread();
        workerThread.start();

        return START_NOT_STICKY;
    }

    private class WorkerThread extends Thread {
        @Override
        public void run() {
            // Try to connect to the network
            // Ooh, exciting
            try {
                NetworkRequest request = new NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .setNetworkSpecifier(netSpec)
                        .build();
                ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                connectivityManager.requestNetwork(request, new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(@NonNull Network network) {
                        Log.d("SwAlSh", "Connected!");
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ignored) {}

                        // Download the data.json file I guess
                        // TODO: rewrite since it fails
                        try {
                            URL url = new URL("http://192.168.0.1/data.json");
                            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                            try {
                                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                                StringBuilder sb = new StringBuilder();

                                int read;
                                do {
                                    read = in.read();
                                    sb.append(read);
                                } while (read != -1);
                                in.close();

                                Log.d("SwAlSh", "data.json is" + sb);
                            } finally {
                                urlConnection.disconnect();
                            }
                        } catch (Exception e) {
                            Log.e("SwAlSh", "Download error", e);
                        }
                    }
                });
            } catch (SecurityException e) {
                error = true;
                Log.e("SwAlSh", "Missing permissions", e);
            } catch (Exception e) {
                error = true;
                Log.e("SwAlSh", "Other error", e);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new DownloadServiceBinder();
    }

    public class DownloadServiceBinder extends Binder {
        public DownloadService getService() {
            return DownloadService.this;
        }
        public boolean hasStarted() {
            return started;
        }
        public boolean isConnected() {
            return connected;
        }
        public boolean hasErrorOccurred() {
            return error;
        }
        public int getNumDownloaded() {
            return numDownloaded;
        }
    }
}