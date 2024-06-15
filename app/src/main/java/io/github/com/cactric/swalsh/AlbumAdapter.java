package io.github.com.cactric.swalsh;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import kotlin.NotImplementedError;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.ViewHolder> {
    private Uri[] pictures;
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;
        private final VideoView videoView;
        private final TextView lengthText;
        private final ImageButton shareButton;
        private final ImageButton deleteButton;
        public ViewHolder(View view) {
            super(view);
            imageView = view.findViewById(R.id.album_picture);
            videoView = view.findViewById(R.id.album_video);
            lengthText = view.findViewById(R.id.album_length_text);
            shareButton = view.findViewById(R.id.album_share_button);
            deleteButton = view.findViewById(R.id.album_delete_button);
        }

        public ImageView getImageView() {
            return imageView;
        }

        public VideoView getVideoView() {
            return videoView;
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
    public AlbumAdapter(Uri[] pictures) {
        this.pictures = pictures;
    }

    // Create new views
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_album, parent, false);
        return new ViewHolder(v);
    }

    // Replace the contents of views
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Update views
        String filename = pictures[position].getLastPathSegment();
        if (filename != null && filename.endsWith(".mp4")) {
            // Hide the image view; show the video view
            holder.getImageView().setVisibility(View.GONE);
            holder.getVideoView().setVisibility(View.VISIBLE);
            // Then set the video URI
            holder.getVideoView().setVideoURI(pictures[position]);
        } else {
            // Show the image view; hide the video view
            holder.getImageView().setVisibility(View.VISIBLE);
            holder.getVideoView().setVisibility(View.GONE);
            // Then set the image URI
            holder.getImageView().setImageURI(pictures[position]);
        }

        holder.getLengthText().setText("AAA");
        holder.getShareButton().setOnClickListener(v -> {
            throw new NotImplementedError("Sharing not implemented yet");
        });
        holder.getDeleteButton().setOnClickListener(v -> {
            throw new NotImplementedError("Deleting not implemented yet");
        });
    }

    @Override
    public int getItemCount() {
        return pictures.length;
    }
}
