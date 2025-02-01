package io.github.cactric.swalsh.ui.album;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import io.github.cactric.swalsh.MediaService;
import io.github.cactric.swalsh.R;

public class VideoFragment extends Fragment {
    private String mediaSortOrder = MediaStore.Video.Media.DATE_ADDED;
    private boolean mediaSortDescending = true;
    private TextView nothingFoundText;
    private VideoAlbumAdapter adapter;
    private RecyclerView recyclerView;
    private final VideoMenuProvider videoMenuProvider = new VideoMenuProvider();

    private final static String PARAM_GAME_ID = "param_game_id";
    private String gameId;
    private MediaService.MediaBinder binder;

    private boolean wentToGamePickerActivity = false;
    private Integer oldNumOfPictures;
    private Integer oldNumOfVideos;

    public VideoFragment() {
    }

    /**
     * Get an instance with the game ID parameter set to filter by that game ID
     * @param gameId Game ID to filter by
     * @return A new instance of the fragment which only shows videos from the specified game (by ID)
     */
    public static VideoFragment newInstance(@Nullable String gameId) {
        VideoFragment f = new VideoFragment();
        Bundle args = new Bundle();
        args.putString(PARAM_GAME_ID, gameId);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            gameId = getArguments().getString(PARAM_GAME_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_video, container, false);

        nothingFoundText = v.findViewById(R.id.album_nothing_found);
        recyclerView = v.findViewById(R.id.album_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        // Bind to the media service
        Intent msIntent = new Intent(requireContext(), MediaService.class);
        ServiceConnection connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder ibinder) {
                binder = (MediaService.MediaBinder) ibinder;
                binder.getNumOfVideos().observe(requireActivity(), num -> {
                    nothingFoundText.setVisibility(num == 0 ? View.VISIBLE : View.GONE);
                });
                retrieveItemsOnSeparateThread();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                binder = null;
            }
        };
        requireContext().bindService(msIntent, connection, Context.BIND_AUTO_CREATE);

        return v;
    }

    @Override
    public void onPause() {
        super.onPause();
        requireActivity().removeMenuProvider(videoMenuProvider);
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().addMenuProvider(videoMenuProvider, getViewLifecycleOwner());
    }

    @Override
    public void onStart() {
        super.onStart();
        if (wentToGamePickerActivity)
            // If the user went to the game picker activity, they might have changed the names,
            // so tell the adapter about it
            if (adapter.getItemCount() > 0) {
                binder.getNumOfPictures().setValue(oldNumOfPictures);
                binder.getNumOfVideos().setValue(oldNumOfVideos);
                adapter.notifyItemRangeChanged(0, adapter.getItemCount());
                wentToGamePickerActivity = false;
            }
    }

    private class VideoMenuProvider implements MenuProvider {
        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            menuInflater.inflate(gameId == null ? R.menu.album_video_menu : R.menu.by_game_album_video_menu, menu);
        }

        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
            if (menuItem.getItemId() == R.id.sort_videos) {
                showSortItemsPopup();
                return true;
            } else if (menuItem.getItemId() == R.id.filter_by_game) {
                Intent intent = new Intent(getActivity(), GamePickerActivity.class);
                intent.putExtra("EXTRA_GAME_ID_LIST", binder.getFoundGameIds().toArray(new String[]{}));
                wentToGamePickerActivity = true;
                oldNumOfPictures = binder.getNumOfPictures().getValue();
                oldNumOfVideos = binder.getNumOfVideos().getValue();
                startActivity(intent);
            } else if (menuItem.getItemId() == R.id.delete_all_videos) {
                showDeleteVideosPopup();
                return true;
            }
            return false;
        }
    }

    private void retrieveItemsOnSeparateThread() {
        if (binder != null) {
            binder.scanVideos(gameId, mediaSortOrder, mediaSortDescending, items -> {
                requireActivity().runOnUiThread(() -> {
                    adapter = new VideoAlbumAdapter(items, binder);
                    recyclerView.setAdapter(adapter);
                });
            });
        }
    }

    private void showSortItemsPopup() {
        View anchor = requireActivity().findViewById(R.id.sort_videos);
        if (anchor != null) {
            PopupMenu pm = new PopupMenu(requireContext(), anchor);
            pm.inflate(gameId == null ? R.menu.sort_menu : R.menu.by_game_sort_menu);
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

    @SuppressLint("NotifyDataSetChanged")
    private void showDeleteVideosPopup() {
        if (binder == null) {
            Toast.makeText(getContext(), getString(R.string.error_deleting_items), Toast.LENGTH_SHORT).show();
            return;
        }

        binder.scanVideos(gameId, mediaSortOrder, mediaSortDescending, videoItems -> {
            requireActivity().runOnUiThread(() -> {
                if (videoItems.isEmpty()) {
                    Toast.makeText(getContext(), getString(R.string.error_deleting_items), Toast.LENGTH_SHORT).show();
                    return;
                }

                AlertDialog.Builder adb = new AlertDialog.Builder(requireContext());
                adb.setTitle(getResources().getQuantityString(
                        R.plurals.delete_all_videos_confirmation_formatted,
                        videoItems.size(), // Used for deciding which plural string to use
                        videoItems.size() // Use for formatting
                ));
                adb.setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
                adb.setPositiveButton(R.string.yes, (dialog, which) -> new Thread(() -> {
                    Integer oldNumOfVideos = binder.getNumOfVideos().getValue();
                    if (oldNumOfVideos == null) {
                        Toast.makeText(getContext(), getString(R.string.error_deleting_items), Toast.LENGTH_SHORT).show();
                        Log.e("SwAlSh", "Refusing to delete all videos since oldNumOfPictures is null");
                        return;
                    }
                    binder.deleteAllVideos();
                    requireActivity().runOnUiThread(this::retrieveItemsOnSeparateThread);
                }).start());
                adb.show();
            });
        });
    }
}
