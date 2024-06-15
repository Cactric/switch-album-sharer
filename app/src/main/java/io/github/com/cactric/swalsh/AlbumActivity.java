package io.github.com.cactric.swalsh;

import static android.provider.MediaStore.VOLUME_EXTERNAL;

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class AlbumActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_album);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ArrayList<Uri> uris = new ArrayList<>();

        // Do it twice, once for pictures, once for videos
        // TODO: Videos
        // Which columns from the query?
        String[] projection = new String[] {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA
        };

        try (Cursor cursor = getContentResolver().query(
                MediaStore.Images.Media.getContentUri(VOLUME_EXTERNAL),
                projection,
                null,
                null,
                MediaStore.Images.Media.DATE_ADDED + " ASC"
        )) {
            if (cursor == null)
                throw new NullPointerException();
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

            // Loop through results
            while (cursor.moveToNext()) {
                long id = cursor.getLong(idColumn);
                Uri contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                uris.add(contentUri);
            }
        }


        // Make the adapter, etc.
        RecyclerView recyclerView = findViewById(R.id.album_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        AlbumAdapter adapter = new AlbumAdapter(uris.toArray(new Uri[]{}));
        recyclerView.setAdapter(adapter);
    }
}