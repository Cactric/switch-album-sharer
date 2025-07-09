package io.github.cactric.swalsh;

import android.net.Uri;

import androidx.lifecycle.LiveData;

public class PictureItem {
    public Uri uri;
    public String display_name;
    public LiveData<String> display_text;
}
