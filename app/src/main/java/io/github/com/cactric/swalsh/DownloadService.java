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
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
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

    @Nullable
    File getAppSpecificAlbumStorageDir(Context context, String albumName) {
        // Get the pictures directory that's inside the app-specific directory on
        // external storage.
        File file = new File(context.getExternalFilesDir(
                Environment.DIRECTORY_PICTURES), albumName);
        if (!file.exists() && !file.mkdirs()) {
            Log.e("SwAlSh", "Directory not created");
        }
        return file;
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

                        String dataJson;

                        // Download the data.json file
                        try {
                            URL url = new URL("http://192.168.0.1/data.json");
                            HttpURLConnection urlConnection = (HttpURLConnection) network.openConnection(url);
                            try {
                                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                                StringBuilder sb = new StringBuilder();

                                int read;
                                while (true) {
                                    read = in.read();
                                    if (read == -1)
                                        break;
                                    sb.appendCodePoint(read);
                                }
                                in.close();

                                Log.d("SwAlSh", "data.json is " + sb);
                                dataJson = sb.toString();
                            } finally {
                                urlConnection.disconnect();
                            }
                        } catch (Exception e) {
                            Log.e("SwAlSh", "Download error", e);
                            return;
                        }

                        // Try to parse the JSON
                        try {
                            JSONObject rootObj = new JSONObject(dataJson);

                            // I'm interested mainly in the `FileNames` array
                            // Maybe the `FileType` or `ConsoleName`...
                            JSONArray fileNames = rootObj.getJSONArray("FileNames");

                            for (int i = 0; i < fileNames.length(); i++) {
                                Log.d("SwAlSh", "File name " + i + " = " + fileNames.getString(i));
                                try {
                                    URL fileURL = new URL("http://192.168.0.1/img/" + fileNames.getString(i));
                                    HttpURLConnection fileConnection = (HttpURLConnection) network.openConnection(fileURL);
                                    InputStream in = new BufferedInputStream(fileConnection.getInputStream());

                                    // Try to save the picture
                                    File dir = getAppSpecificAlbumStorageDir(getApplicationContext(), "Switch Screenshots");
                                    File file = new File(dir, fileNames.getString(i));
                                    try {
                                        FileOutputStream fos = new FileOutputStream(file);
                                        boolean done = false;
                                        while (!done) {
                                            byte[] data = new byte[512];
                                            int bytesRead = in.read(data);
                                            if (bytesRead == 0)
                                                done = true;
                                            else
                                                fos.write(bytesRead);
                                        }
                                        in.close();
                                        fos.close();
                                        Log.d("SwAlSh", "Saved " + fileNames.getString(i) + " to " + file.getAbsolutePath() + "!");
                                    } catch (FileNotFoundException e) {
                                        Log.e("SwAlSh", "Failed to open output file", e);
                                    } catch (SecurityException e) {
                                        Log.e("SwAlSh", "Possibly missing permissions or something", e);
                                    }

                                } catch (MalformedURLException e) {
                                    Log.e("SwAlSh", "Malformed URL, possibly unexpected data and/or an application bug", e);
                                    continue;
                                } catch (IOException e) {
                                    Log.e("SwAlSh", "Download error, file " + i + " failed to download", e);
                                    continue;
                                }
                            }
                        } catch (JSONException e) {
                            Log.e("SwAlSh", "Failed to parse JSON data", e);
                            return;
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