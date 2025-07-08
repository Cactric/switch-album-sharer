package io.github.cactric.swalsh;

import android.graphics.Bitmap;
import android.net.Uri;

import androidx.lifecycle.LiveData;

public class VideoItem {
    public long id;
    public Uri uri;
    public String display_name;
    public LiveData<String> display_text;
    public long duration_in_milliseconds;
    public Bitmap thumbnail;
}
