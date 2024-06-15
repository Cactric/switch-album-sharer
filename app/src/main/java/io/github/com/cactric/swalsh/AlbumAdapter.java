package io.github.com.cactric.swalsh;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.ViewHolder> {
    private String[] pictures;
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;
        private final TextView length_text;
        private final ImageButton shareButton;
        private final ImageButton deleteButton;
        public ViewHolder(View view) {
            super(view);
            imageView = view.findViewById(R.id.album_picture);
            length_text = view.findViewById(R.id.album_length_text);
            shareButton = view.findViewById(R.id.album_share_button);
            deleteButton = view.findViewById(R.id.album_delete_button);
        }
    }

    // Takes an array of file paths to the pictures that should be displayed
    public AlbumAdapter(String[] pictures) {
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
        // TODO: Update views
    }

    @Override
    public int getItemCount() {
        return pictures.length;
    }
}
