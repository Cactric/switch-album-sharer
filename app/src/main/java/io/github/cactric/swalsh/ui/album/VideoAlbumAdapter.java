package io.github.cactric.swalsh.ui.album;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import io.github.cactric.swalsh.MediaService;
import io.github.cactric.swalsh.R;
import io.github.cactric.swalsh.VideoItem;
import io.github.cactric.swalsh.databinding.RecyclerVidsBinding;

public class VideoAlbumAdapter extends RecyclerView.Adapter<VideoAlbumAdapter.ViewHolder> {
    private final ArrayList<VideoItem> media;
    private final MediaService.MediaBinder binder;
    private final LifecycleOwner owner;
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final RecyclerVidsBinding binding;
        public ViewHolder(RecyclerVidsBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public ImageView getVideoThumbnail() {
            return binding.albumVideo;
        }

        public TextView getLengthText() {
            return binding.albumLengthText;
        }

        public ImageButton getShareButton() {
            return binding.albumShareButton;
        }

        public ImageButton getDeleteButton() {
            return binding.albumDeleteButton;
        }
    }

    // Takes an array of file paths to the pictures that should be displayed
    public VideoAlbumAdapter(ArrayList<VideoItem> media, MediaService.MediaBinder binder, LifecycleOwner owner) {
        this.media = media;
        this.binder = binder;
        this.owner = owner;
    }

    // Create new views
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerVidsBinding binding = RecyclerVidsBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ViewHolder(binding);
    }

    // Replace the contents of views
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Context context = holder.getVideoThumbnail().getContext();
        VideoItem item = media.get(position);
        // Update views
        holder.getVideoThumbnail().setImageBitmap(item.thumbnail);
        item.display_text.observe(owner, text -> holder.getLengthText().setText(text));

        holder.getVideoThumbnail().setOnClickListener(v -> {
            // When tapped, open the video in whatever app
            Intent videoIntent = new Intent(Intent.ACTION_VIEW);
            videoIntent.setDataAndType(item.uri, "video/mp4");
            context.startActivity(videoIntent);
        });


        holder.getShareButton().setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, item.uri);
            shareIntent.setType("video/mp4");
            context.startActivity(Intent.createChooser(shareIntent, null));
        });
        holder.getDeleteButton().setOnClickListener(v -> {
            // Delete item
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            boolean skipConfirmation = sp.getBoolean("video_no_delete_confirmation", false);
            if (skipConfirmation) {
                // Just delete it
                deleteItem(item);
            } else {
                Resources res = context.getResources();
                CharSequence[] selectionItems = {res.getString(R.string.no_more_confirm_prompt)};
                boolean[] checkedItems = new boolean[selectionItems.length];
                AlertDialog.Builder adb = new AlertDialog.Builder(context);
                adb.setTitle(R.string.delete_confirmation);
                adb.setMultiChoiceItems(selectionItems, checkedItems, (dialog, which, isChecked) -> checkedItems[which] = isChecked);
                adb.setPositiveButton(R.string.yes, (dialog, which) -> {
                    // Check if user wants to not see this dialog again
                    if (checkedItems[0]) {
                        // Begone says thee!
                        SharedPreferences.Editor e = sp.edit();
                        e.putBoolean("video_no_delete_confirmation", checkedItems[0]);
                        e.apply();
                    }
                    // Delete!
                    deleteItem(item);
                });
                adb.setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
                adb.show();
            }
        });
    }

    private void deleteItem(VideoItem item) {
        binder.deleteVideo(item.uri);
        VideoAlbumAdapter.this.notifyItemRemoved(media.indexOf(item));
        media.remove(item);
    }

    @Override
    public int getItemCount() {
        return media.size();
    }
}
