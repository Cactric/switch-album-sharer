package io.github.cactric.swalsh;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DownloadService extends Service {
    private final ArrayList<Uri> savedContentUris = new ArrayList<>();
    private String fileType = null;
    private MutableLiveData<State> state = new MutableLiveData<>(State.NOT_STARTED);
    private MutableLiveData<Integer> numDownloaded = new MutableLiveData<>(0);
    private MutableLiveData<Integer> numFailed = new MutableLiveData<>(0);
    private MutableLiveData<Float> downloadProgress = new MutableLiveData<>(0.0f);
    private int numToDownload = 0;
    private WifiNetworkSpecifier netSpec;
    private MutableLiveData<Long> scanTime = new MutableLiveData<>(-1L);

    public DownloadService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Long scanTimeFromIntent = intent.getLongExtra("EXTRA_SCAN_TIME", -1);
        if (Objects.equals(scanTimeFromIntent, scanTime.getValue())) {
            // If the scan time is the same, assume it's the same scan and the service just got restarted for some reason
            Log.w("SwAlSh", "DownloadService: Same scan time as the last start command (" + scanTimeFromIntent + ") - ignoring");
            return START_NOT_STICKY;
        } else {
            scanTime.setValue(scanTimeFromIntent);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            netSpec = intent.getParcelableExtra("EXTRA_NETWORK_SPECIFIER", WifiNetworkSpecifier.class);
        } else {
            netSpec = intent.getParcelableExtra("EXTRA_NETWORK_SPECIFIER");
        }
        state.setValue(State.CONNECTING);
        numDownloaded.setValue(0);
        numToDownload = 0;
        downloadProgress.setValue(0.0f);
        numFailed.setValue(0);
        savedContentUris.clear();
        fileType = null;

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

                            // Stop duplicate callbacks
                            connectivityManager.unregisterNetworkCallback(this);

                            stopSelf();
                            return;
                        }

                        // Try to parse the JSON
                        try {
                            JSONObject rootObj = new JSONObject(dataJson);

                            // I'm interested mainly in the `FileNames` array
                            // Maybe the `FileType` or `ConsoleName`...
                            fileType = rootObj.getString("FileType");
                            JSONArray fileNames = rootObj.getJSONArray("FileNames");
                            numToDownload = fileNames.length();
                            state.postValue(DownloadService.State.DOWNLOADING);

                            for (int i = 0; i < fileNames.length(); i++) {
                                Log.d("SwAlSh", "File name " + i + " = " + fileNames.getString(i));
                                ContentResolver resolver = getApplicationContext().getContentResolver();
                                Uri contentUri = null;
                                try {
                                    URL fileURL = new URL("http://192.168.0.1/img/" + fileNames.getString(i));
                                    HttpURLConnection fileConnection = (HttpURLConnection) network.openConnection(fileURL);
                                    long contentLength = fileConnection.getContentLengthLong();
                                    InputStream in = new BufferedInputStream(fileConnection.getInputStream());

                                    // Try to save the picture
                                    Uri contentCollection;
                                    ContentValues contentDetails = new ContentValues();
                                    if (fileType.equals("photo")) {
                                        contentCollection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);

                                        contentDetails.put(MediaStore.Images.Media.DISPLAY_NAME, fileNames.getString(i));
                                        contentDetails.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Switch Screenshots");
                                        // Mark it as pending until I write the file out
                                        contentDetails.put(MediaStore.Images.Media.IS_PENDING, 1);
                                    } else if (fileType.equals("movie")) {
                                        contentCollection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                                        contentDetails.put(MediaStore.Video.Media.DISPLAY_NAME, fileNames.getString(i));
                                        contentDetails.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/Switch Clips");
                                        contentDetails.put(MediaStore.Video.Media.IS_PENDING, 1);
                                    } else {
                                        Log.e("SwAlSh", "Unknown file type '" + fileType + "'");
                                        continue;
                                    }

                                    contentUri = resolver.insert(contentCollection, contentDetails);
                                    if (contentUri == null) {
                                        Log.e("SwAlSh", "Failed to save picture - contentUri is null");
                                        continue;
                                    }
                                    Log.d("SwAlSh", "Saving to " + contentUri);

                                    try {
                                        OutputStream os = resolver.openOutputStream(contentUri);
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
                                            else {
                                                os.write(data, 0, bytesRead);
                                                bytesWritten += bytesRead;
                                            }
                                            downloadProgress.postValue(((float) bytesWritten) / ((float) contentLength));
                                        }
                                        in.close();
                                        os.close();
                                        Log.d("SwAlSh", "Saved " + fileNames.getString(i) + "!");
                                    } catch (FileNotFoundException e) {
                                        Log.e("SwAlSh", "Failed to open output file", e);
                                    } catch (SecurityException e) {
                                        Log.e("SwAlSh", "Possibly missing permissions or something", e);
                                    }

                                    contentDetails.clear();
                                    if (fileType.equals("photo"))
                                        contentDetails.put(MediaStore.Images.Media.IS_PENDING, 0);
                                    else
                                        contentDetails.put(MediaStore.Video.Media.IS_PENDING, 0);
                                    resolver.update(contentUri, contentDetails, null, null);
                                    savedContentUris.add(contentUri);
                                    if (numDownloaded.getValue() != null) {
                                        numDownloaded.postValue(numDownloaded.getValue() + 1);
                                    }
                                } catch (MalformedURLException e) {
                                    Log.e("SwAlSh", "Malformed URL, possibly unexpected data and/or an application bug", e);
                                    if (numFailed.getValue() != null) {
                                        numFailed.postValue(numFailed.getValue() + 1);
                                    }
                                } catch (IOException e) {
                                    Log.e("SwAlSh", "Download error, file " + i + " failed to download", e);
                                    if (numFailed.getValue() != null) {
                                        numFailed.postValue(numFailed.getValue() + 1);
                                    }
                                }
                            }

                            state.postValue(DownloadService.State.DONE);
                        } catch (JSONException e) {
                            Log.e("SwAlSh", "Failed to parse JSON data", e);
                            state.postValue(DownloadService.State.ERROR);
                        }

                        // Stop duplicate callbacks
                        connectivityManager.unregisterNetworkCallback(this);

                        stopSelf();
                    }

                    @Override
                    public void onLost(@NonNull Network network) {
                        Log.d("SwAlSh", "Lost network");
                        if (state.getValue() != DownloadService.State.DONE)
                            state.postValue(DownloadService.State.ERROR);
                        stopSelf();
                    }

                    @Override
                    public void onUnavailable() {
                        Log.d("SwAlSh", "Network is unavailable");
                        if (state.getValue() != DownloadService.State.DONE)
                            state.postValue(DownloadService.State.ERROR);
                        stopSelf();
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
        public LiveData<State> getState() {
            return state;
        }
        public LiveData<Integer> getNumDownloaded() {
            return numDownloaded;
        }
        public LiveData<Integer> getNumFailed() {
            return numFailed;
        }
        public int getNumToDownload() {
            return numToDownload;
        }
        public LiveData<Float> getDownloadProgress() {
            return downloadProgress;
        }
        public List<Uri> getSavedContentUriList() {
            return savedContentUris;
        }
        public String getFileType() {
            return fileType;
        }
        public LiveData<Long> getScanTime() {
            return scanTime;
        }
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