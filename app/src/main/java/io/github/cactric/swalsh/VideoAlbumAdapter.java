package io.github.cactric.swalsh;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import kotlin.NotImplementedError;

public class VideoAlbumAdapter extends RecyclerView.Adapter<VideoAlbumAdapter.ViewHolder> {
    private VideoItem[] media;
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
    public VideoAlbumAdapter(VideoItem[] media) {
        this.media = media;
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
        // Update views
        // Then set the image URI
        holder.getVideoThumbnail().setImageBitmap(media[position].thumbnail);

        // Try to parse the display name and use that as a date
        String dateStr = null;
        // Format: year, month, day, hour, minute, second, 00 - game id(?).jpg
        Calendar.Builder calBuilder = new Calendar.Builder();
        try {
            String name = media[position].display_name;
            calBuilder.set(Calendar.YEAR, Integer.parseInt(name.substring(0, 4)));
            calBuilder.set(Calendar.MONTH, Integer.parseInt(name.substring(4, 6)));
            calBuilder.set(Calendar.DAY_OF_MONTH, Integer.parseInt(name.substring(6, 8)));
            calBuilder.set(Calendar.HOUR_OF_DAY, Integer.parseInt(name.substring(8, 10)));
            calBuilder.set(Calendar.MINUTE, Integer.parseInt(name.substring(10, 12)));
            calBuilder.set(Calendar.SECOND, Integer.parseInt(name.substring(12, 14)));

            Date d = new Date();
            d.setTime(calBuilder.build().getTimeInMillis());
            DateFormat df = DateFormat.getDateTimeInstance();
            dateStr = df.format(d);
        } catch (NumberFormatException e) {
            Log.e("SwAlSh", "Failed to parse " + media[position].display_name, e);
        }

        if (dateStr == null)
            dateStr = media[position].display_name;

        Resources res = context.getResources();
        holder.getLengthText().setText(res.getString(R.string.video_text_format,
                media[position].duration_in_milliseconds / 1000.0,
                dateStr));

        holder.getVideoThumbnail().setOnClickListener(v -> {
            // When tapped, open the video in whatever app
            Intent videoIntent = new Intent(Intent.ACTION_VIEW);
            videoIntent.setDataAndType(media[position].uri, "video/mp4");
            context.startActivity(videoIntent);
        });


        holder.getShareButton().setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, media[position].uri);
            shareIntent.setType("video/mp4");
            context.startActivity(Intent.createChooser(shareIntent, null));
        });
        holder.getDeleteButton().setOnClickListener(v -> {
            throw new NotImplementedError("Deleting not implemented yet");
        });
    }

    @Override
    public int getItemCount() {
        return media.length;
    }
}