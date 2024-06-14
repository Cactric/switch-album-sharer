package io.github.com.cactric.swalsh;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.Uri;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

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
    private final MutableLiveData<State> state = new MutableLiveData<>(State.NOT_STARTED);
    private final MutableLiveData<Integer> numDownloaded = new MutableLiveData<>(0);
    private int numToDownload = 0;
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            netSpec = intent.getParcelableExtra("EXTRA_NETWORK_SPECIFIER", WifiNetworkSpecifier.class);
        } else {
            netSpec = intent.getParcelableExtra("EXTRA_NETWORK_SPECIFIER");
        }
        state.setValue(State.CONNECTING);

        WorkerThread workerThread = new WorkerThread();
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
                        state.postValue(DownloadService.State.CONNECTED);

                        // Download the data.json file
                        String dataJson;
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
                            state.postValue(DownloadService.State.ERROR);
                            return;
                        }

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ignored) {}

                        // Try to parse the JSON
                        try {
                            JSONObject rootObj = new JSONObject(dataJson);

                            // I'm interested mainly in the `FileNames` array
                            // Maybe the `FileType` or `ConsoleName`...
                            JSONArray fileNames = rootObj.getJSONArray("FileNames");
                            numToDownload = fileNames.length();
                            state.postValue(DownloadService.State.DOWNLOADING);

                            for (int i = 0; i < fileNames.length(); i++) {
                                Log.d("SwAlSh", "File name " + i + " = " + fileNames.getString(i));
                                try {
                                    URL fileURL = new URL("http://192.168.0.1/img/" + fileNames.getString(i));
                                    HttpURLConnection fileConnection = (HttpURLConnection) network.openConnection(fileURL);
                                    InputStream in = new BufferedInputStream(fileConnection.getInputStream());

                                    // Try to save the picture
                                    //File dir = getAppSpecificAlbumStorageDir(getApplicationContext(), "Switch Screenshots");
                                    //File file = new File(dir, fileNames.getString(i));

                                    ContentResolver resolver = getApplicationContext().getContentResolver();
                                    // TODO: save videos elsewhere
                                    Uri pictureCollection;
                                    pictureCollection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);

                                    ContentValues pictureDetails = new ContentValues();
                                    pictureDetails.put(MediaStore.Images.Media.DISPLAY_NAME, fileNames.getString(i));
                                    pictureDetails.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Switch Screenshots");
                                    // Mark it as pending until I write the file out
                                    pictureDetails.put(MediaStore.Images.Media.IS_PENDING, 1);

                                    Uri pictureContentUri = resolver.insert(pictureCollection, pictureDetails);
                                    if (pictureContentUri == null) {
                                        Log.e("SwAlSh", "Failed to save picture - pictureContentUri is null");
                                        continue;
                                    }
                                    Log.d("SwAlSh", "Saving to " + pictureContentUri);

                                    try {
                                        OutputStream os = resolver.openOutputStream(pictureContentUri);
                                        if (os == null) {
                                            Log.e("SwAlSh", "Failed to save picture - output stream is null");
                                            continue;
                                        }
                                        boolean done = false;
                                        long bytesWritten = 0;
                                        while (!done) {
                                            byte[] data = new byte[512 * 1024];
                                            int bytesRead = in.read(data);
                                            if (bytesRead == -1)
                                                done = true;
                                            else
                                                os.write(data, 0, bytesRead);
                                            bytesWritten += bytesRead;
                                        }
                                        in.close();
                                        Log.d("SwAlSh", "Saved " + fileNames.getString(i) + "!");
                                    } catch (FileNotFoundException e) {
                                        Log.e("SwAlSh", "Failed to open output file", e);
                                    } catch (SecurityException e) {
                                        Log.e("SwAlSh", "Possibly missing permissions or something", e);
                                    }

                                    pictureDetails.clear();
                                    pictureDetails.put(MediaStore.Images.Media.IS_PENDING, 0);
                                    resolver.update(pictureContentUri, pictureDetails, null, null);
                                    if (numDownloaded.getValue() != null) {
                                        numDownloaded.postValue(numDownloaded.getValue() + 1);
                                    }
                                } catch (MalformedURLException e) {
                                    Log.e("SwAlSh", "Malformed URL, possibly unexpected data and/or an application bug", e);
                                    continue;
                                } catch (IOException e) {
                                    Log.e("SwAlSh", "Download error, file " + i + " failed to download", e);
                                    continue;
                                }
                            }

                            state.postValue(DownloadService.State.DONE);
                        } catch (JSONException e) {
                            Log.e("SwAlSh", "Failed to parse JSON data", e);
                            state.postValue(DownloadService.State.ERROR);
                            return;
                        }

                    }
                });
            } catch (SecurityException e) {
                state.postValue(DownloadService.State.ERROR);
                Log.e("SwAlSh", "Missing permissions", e);
            } catch (Exception e) {
                state.postValue(DownloadService.State.ERROR);
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
        public LiveData<State> getState() {
            return state;
        }
        public LiveData<Integer> getNumDownloaded() {
            return numDownloaded;
        }
        public int getNumToDownload() {
            return numToDownload;
        };
    }

    public enum State {
        NOT_STARTED,
        CONNECTING,
        CONNECTED,
        DOWNLOADING,
        DONE,
        ERROR
    }
}