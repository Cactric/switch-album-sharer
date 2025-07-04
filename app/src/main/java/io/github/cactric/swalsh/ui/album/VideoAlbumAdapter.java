package io.github.cactric.swalsh.ui.album;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import io.github.cactric.swalsh.MediaService;
import io.github.cactric.swalsh.games.GameUtils;
import io.github.cactric.swalsh.R;
import io.github.cactric.swalsh.VideoItem;

public class VideoAlbumAdapter extends RecyclerView.Adapter<VideoAlbumAdapter.ViewHolder> {
    private final ArrayList<VideoItem> media;
    private final MediaService.MediaBinder binder;
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView videoThumbnail;
        private final TextView lengthText;
        private final ImageButton shareButton;
        private final ImageButton deleteButton;
        public ViewHolder(View view) {
            super(view);
            videoThumbnail = view.findViewById(R.id.album_video);
            lengthText = view.findViewById(R.id.album_length_text);
            shareButton = view.findViewById(R.id.album_share_button);
            deleteButton = view.findViewById(R.id.album_delete_button);
        }

        public ImageView getVideoThumbnail() {
            return videoThumbnail;
        }

        public TextView getLengthText() {
            return lengthText;
        }

        public ImageButton getShareButton() {
            return shareButton;
        }

        public ImageButton getDeleteButton() {
            return deleteButton;
        }
    }

    // Takes an array of file paths to the pictures that should be displayed
    public VideoAlbumAdapter(ArrayList<VideoItem> media, MediaService.MediaBinder binder) {
        this.media = media;
        this.binder = binder;
    }

    // Create new views
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_vids, parent, false);
        return new ViewHolder(v);
    }

    // Replace the contents of views
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Context context = holder.getVideoThumbnail().getContext();
        VideoItem item = media.get(position);
        // Update views
        // Then set the image URI
        holder.getVideoThumbnail().setImageBitmap(item.thumbnail);

        // Try to parse the display name and use that as a date
        String dateStr = null;
        // Format: year, month, day, hour, minute, second, 00 - game id(?).jpg
        Calendar.Builder calBuilder = new Calendar.Builder();
        try {
            String name = item.display_name;
            calBuilder.set(Calendar.YEAR, Integer.parseInt(name.substring(0, 4)));
            calBuilder.set(Calendar.MONTH, Integer.parseInt(name.substring(4, 6)) - 1);
            calBuilder.set(Calendar.DAY_OF_MONTH, Integer.parseInt(name.substring(6, 8)));
            calBuilder.set(Calendar.HOUR_OF_DAY, Integer.parseInt(name.substring(8, 10)));
            calBuilder.set(Calendar.MINUTE, Integer.parseInt(name.substring(10, 12)));
            calBuilder.set(Calendar.SECOND, Integer.parseInt(name.substring(12, 14)));

            Date d = new Date();
            d.setTime(calBuilder.build().getTimeInMillis());
            DateFormat df = DateFormat.getDateTimeInstance();
            dateStr = df.format(d);
        } catch (NumberFormatException e) {
            Log.e("SwAlSh", "Failed to parse " + item.display_name, e);
        }

        if (dateStr == null)
            dateStr = item.display_name;

        GameUtils gameUtils = new GameUtils(context);
        // Use the display name to get the game ID and then try to look it up to get the game's name
        String gameId = item.display_name.substring(17,49);
        String gameName = gameUtils.lookupGameName(gameId);

        Resources res = context.getResources();
        holder.getLengthText().setText(res.getString(R.string.video_text_format,
                item.duration_in_milliseconds / 1000.0,
                gameName,
                dateStr));

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
