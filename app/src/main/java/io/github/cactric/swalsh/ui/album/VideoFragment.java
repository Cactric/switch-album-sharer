package io.github.cactric.swalsh.ui.album;

import static android.provider.MediaStore.VOLUME_EXTERNAL;

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;

import io.github.cactric.swalsh.R;
import io.github.cactric.swalsh.VideoItem;

public class VideoFragment extends Fragment {
    private final MutableLiveData<Integer> numOfVideos = new MutableLiveData<>();
    private final ArrayList<VideoItem> videoItems = new ArrayList<>();
    private String mediaSortOrder = MediaStore.Video.Media.DATE_ADDED;
    private boolean mediaSortDescending = true;
    private TextView nothingFoundText;
    private VideoAlbumAdapter adapter;

    public VideoFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_video, container, false);

        // Make the adapter, etc.
        nothingFoundText = v.findViewById(R.id.album_nothing_found);
        RecyclerView recyclerView = v.findViewById(R.id.album_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        adapter = new VideoAlbumAdapter(videoItems, numOfVideos);
        recyclerView.setAdapter(adapter);
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                nothingFoundText.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
            }
        });

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.album_video_menu, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.sort_videos) {
                    showSortItemsPopup();
                    return true;
                } else if (menuItem.getItemId() == R.id.delete_all_videos) {
                    showDeleteVideosPopup();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner());

        retrieveItemsOnSeparateThread();

        return v;
    }

    private void getVideos() {
        videoItems.clear();
        // Videos
        String[] vid_projection = new String[] {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION
        };

        try (Cursor cursor = requireContext().getContentResolver().query(
                MediaStore.Video.Media.getContentUri(VOLUME_EXTERNAL),
                vid_projection,
                null,
                null,
                mediaSortOrder + (mediaSortDescending ? " DESC" : "")
        )) {
            if (cursor == null)
                throw new NullPointerException();
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
            int displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
            int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);

            // Set count
            numOfVideos.postValue(cursor.getCount());

            // Loop through results
            while (cursor.moveToNext()) {
                long id = cursor.getLong(idColumn);
                VideoItem item = new VideoItem();
                item.id = id;
                item.uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);
                item.display_name = cursor.getString(displayNameColumn);
                item.duration_in_milliseconds = cursor.getInt(durationColumn);
                try {
                    item.thumbnail = requireContext().getContentResolver().loadThumbnail(item.uri, Size.parseSize("1280x720"), null);
                } catch (IOException e) {
                    Log.e("SwAlSh", "Error while loading thumbnail for " + item.display_name, e);
                    item.thumbnail = null;
                }
                videoItems.add(item);
            }
        }
    }

    private void retrieveItemsOnSeparateThread() {
        Thread retrieveThread = new Thread(() -> {
            getVideos();
            requireActivity().runOnUiThread(() -> {
                nothingFoundText.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                adapter.notifyDataSetChanged();
            });
        });
        retrieveThread.start();
    }

    private void showSortItemsPopup() {
        View anchor = requireActivity().findViewById(R.id.sort_videos);
        if (anchor != null) {
            PopupMenu pm = new PopupMenu(requireContext(), anchor);
            pm.inflate(R.menu.sort_menu);
            if (mediaSortDescending)
                pm.getMenu().findItem(R.id.sort_descending).setChecked(true);
            else
                pm.getMenu().findItem(R.id.sort_ascending).setChecked(true);
            pm.setOnMenuItemClickListener(sortItem -> {
                if (sortItem.getItemId() == R.id.sort_by_date_added) {
                    mediaSortOrder = MediaStore.Images.Media.DATE_ADDED;
                    retrieveItemsOnSeparateThread();
                } else if (sortItem.getItemId() == R.id.sort_by_date_taken) {
                    mediaSortOrder = MediaStore.Images.Media.DISPLAY_NAME;
                    retrieveItemsOnSeparateThread();
                } else if (sortItem.getItemId() == R.id.sort_by_game) {
                    Toast.makeText(getContext(), "Game sorting chosen, but isn't implemented yet", Toast.LENGTH_SHORT).show();
                    // TODO: replace with another activity?
                } else if (sortItem.getItemId() == R.id.sort_ascending) {
                    mediaSortDescending = false;
                    sortItem.setChecked(true);
                    retrieveItemsOnSeparateThread();
                } else if (sortItem.getItemId() == R.id.sort_descending) {
                    mediaSortDescending = true;
                    sortItem.setChecked(true);
                    retrieveItemsOnSeparateThread();
                } else {
                    return false;
                }
                return true;
            });
            pm.show();
        } else {
            Toast.makeText(getContext(), "Anchor (item.getActionView()) is null?", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteVideosPopup() {
        getVideos();
        if (videoItems.isEmpty()) {
            Toast.makeText(getContext(), "There are no videos to remove", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<Uri> uris = new ArrayList<>();
        for (VideoItem vi: videoItems) {
            uris.add(vi.uri);
        }

        AlertDialog.Builder adb = new AlertDialog.Builder(requireContext());
        adb.setTitle(getString(R.string.delete_all_videos_confirmation_formatted, videoItems.size()));
        adb.setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
        adb.setPositiveButton(R.string.yes, (dialog, which) -> {
            new Thread(() -> {
                // Delete them
                for (Uri u: uris) {
                    requireContext().getContentResolver().delete(u, MediaStore.Video.Media.OWNER_PACKAGE_NAME + " == '" + requireActivity().getPackageName() + "'", null);
                }
                getVideos();
                requireActivity().runOnUiThread(() -> {
                    adapter.notifyItemRangeRemoved(0, adapter.getItemCount());
                });
            }).start();
        });
        adb.show();
    }
}
