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
import io.github.cactric.swalsh.PictureItem;
import io.github.cactric.swalsh.R;

public class PictureAlbumAdapter extends RecyclerView.Adapter<PictureAlbumAdapter.ViewHolder> {
    private final ArrayList<PictureItem> media;
    private final MediaService.MediaBinder binder;
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;
        private final TextView lengthText;
        private final ImageButton shareButton;
        private final ImageButton deleteButton;
        public ViewHolder(View view) {
            super(view);
            imageView = view.findViewById(R.id.album_picture);
            lengthText = view.findViewById(R.id.album_length_text);
            shareButton = view.findViewById(R.id.album_share_button);
            deleteButton = view.findViewById(R.id.album_delete_button);
        }

        public ImageView getImageView() {
            return imageView;
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
    public PictureAlbumAdapter(ArrayList<PictureItem> media, MediaService.MediaBinder binder) {
        this.media = media;
        this.binder = binder;
    }

    // Create new views
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_pics, parent, false);
        return new ViewHolder(v);
    }

    // Replace the contents of views
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Context context = holder.getImageView().getContext();
        PictureItem item = media.get(position);
        // Update views
        holder.getImageView().setImageURI(item.uri);
        holder.getLengthText().setText(item.display_text);

        holder.getImageView().setOnClickListener(v -> {
            // When tapped, open the picture in whatever app
            Intent pictureIntent = new Intent(Intent.ACTION_VIEW);
            pictureIntent.setDataAndType(item.uri, "image/jpeg");
            context.startActivity(pictureIntent);
        });

        holder.getShareButton().setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, item.uri);
            shareIntent.setType("image/jpeg");
            context.startActivity(Intent.createChooser(shareIntent, null));
        });

        holder.getDeleteButton().setOnClickListener(v -> {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            boolean skipConfirmation = sp.getBoolean("picture_no_delete_confirmation", false);
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
                        e.putBoolean("picture_no_delete_confirmation", checkedItems[0]);
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

    private void deleteItem(PictureItem item) {
        binder.deletePicture(item.uri);
        PictureAlbumAdapter.this.notifyItemRemoved(media.indexOf(item));
        media.remove(item);
    }

    @Override
    public int getItemCount() {
        return media.size();
    }
}
