package io.github.cactric.swalsh.ui.album;

import static android.provider.MediaStore.VOLUME_EXTERNAL;

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import io.github.cactric.swalsh.PictureItem;
import io.github.cactric.swalsh.R;

public class PictureFragment extends Fragment {
    private final MutableLiveData<Integer> numOfPictures = new MutableLiveData<>();
    private final ArrayList<PictureItem> pictureItems = new ArrayList<>();
    private PictureAlbumAdapter adapter;
    private TextView nothingFoundText;
    private String mediaSortOrder = MediaStore.Images.Media.DATE_ADDED;
    private boolean mediaSortDescending = true;
    private final PictureMenuProvider pictureMenuProvider = new PictureMenuProvider();

    public PictureFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_picture, container, false);

        // Make the adapter, etc.
        nothingFoundText = v.findViewById(R.id.album_nothing_found);
        RecyclerView recyclerView = v.findViewById(R.id.album_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        adapter = new PictureAlbumAdapter(pictureItems, numOfPictures);
        recyclerView.setAdapter(adapter);
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                nothingFoundText.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
            }
        });

        retrieveItemsOnSeparateThread();

        return v;
    }

    @Override
    public void onPause() {
        super.onPause();
        requireActivity().removeMenuProvider(pictureMenuProvider);
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().addMenuProvider(pictureMenuProvider, getViewLifecycleOwner());
    }

    private class PictureMenuProvider implements MenuProvider {
        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            menuInflater.inflate(R.menu.album_picture_menu, menu);
        }

        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
            if (menuItem.getItemId() == R.id.sort_pictures) {
                showSortItemsPopup();
                return true;
            } else if (menuItem.getItemId() == R.id.delete_all_pictures) {
                showDeletePicturesPopup();
                return true;
            }
            return false;
        }
    }

    private void getPictures() {
        // Pictures:
        pictureItems.clear();
        // Which columns from the query?
        String[] pics_projection = new String[] {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME
        };

        try (Cursor cursor = requireContext().getContentResolver().query(
                MediaStore.Images.Media.getContentUri(VOLUME_EXTERNAL),
                pics_projection,
                null,
                null,
                mediaSortOrder + (mediaSortDescending ? " DESC" : "")
        )) {
            if (cursor == null)
                throw new NullPointerException();
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            int displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);

            // Set count
            numOfPictures.postValue(cursor.getCount());

            // Loop through results
            while (cursor.moveToNext()) {
                long id = cursor.getLong(idColumn);
                PictureItem item = new PictureItem();
                item.id = id;
                item.uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                item.display_name = cursor.getString(displayNameColumn);
                pictureItems.add(item);
            }
        }
    }

    public LiveData<Integer> getNumOfPictures() {
        return numOfPictures;
    }

    private void retrieveItemsOnSeparateThread() {
        Thread retrieveThread = new Thread(() -> {
            getPictures();
            requireActivity().runOnUiThread(() -> {
                nothingFoundText.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                adapter.notifyDataSetChanged();
            });
        });
        retrieveThread.start();
    }

    private void showSortItemsPopup() {
        View anchor = requireActivity().findViewById(R.id.sort_pictures);
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

    private void showDeletePicturesPopup() {
        getPictures();
        if (pictureItems.isEmpty()) {
            Toast.makeText(getContext(), "There are no pictures to remove", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<Uri> uris = new ArrayList<>();
        for (PictureItem pi: pictureItems) {
            uris.add(pi.uri);
        }

        AlertDialog.Builder adb = new AlertDialog.Builder(requireContext());
        adb.setTitle(getResources().getQuantityString(
                R.plurals.delete_all_pictures_confirmation_formatted,
                pictureItems.size(), // Used for deciding which plural string to use
                pictureItems.size() // Used for formatting
        ));
        adb.setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
        adb.setPositiveButton(R.string.yes, (dialog, which) -> new Thread(() -> {
            // Delete them
            for (Uri u: uris) {
                requireContext().getContentResolver().delete(u, MediaStore.Images.Media.OWNER_PACKAGE_NAME + " == '" + requireActivity().getPackageName() + "'", null);
            }
            getPictures();
            requireActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());
        }).start());
        adb.show();

    }
}