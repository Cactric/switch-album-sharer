package io.github.cactric.swalsh.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import io.github.cactric.swalsh.R;

public class InfoActivity extends AppCompatActivity {
    private final Library[] libraries = {
            new Library("Material Icons", Uri.parse("https://fonts.google.com/icons"), "Apache 2.0", Library.APACHE_2_URI),
            new Library("ZXing", Uri.parse("https://github.com/zxing/zxing"), "Apache 2.0", Library.APACHE_2_URI),
            new Library("Android Jetpack (AndroidX)", Uri.parse("https://developer.android.com/jetpack"), "Apache 2.0", Library.APACHE_2_URI)
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_info);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.info_root_linearlayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button source_button = findViewById(R.id.info_goto_source_button);
        source_button.setOnClickListener(v -> {
            // Go to repo page
            Uri repoUri = Uri.parse("https://github.com/Cactric/switch-album-sharer");
            Intent intent = new Intent(Intent.ACTION_VIEW, repoUri);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, getString(R.string.no_browser), Toast.LENGTH_SHORT).show();
                Log.e("SwAlSh", "No browser?", e);
            }
        });

        // Add libraries to list view
        LinearLayout libList = findViewById(R.id.library_list);
        for (Library l: libraries) {
            // Add a separator if it's not the first element
            if (libList.getChildCount() > 0) {
                TypedArray attributes = obtainStyledAttributes(new int[]{android.R.attr.listDivider});
                Drawable bg = attributes.getDrawable(0);
                attributes.recycle();
                View divider = new View(this);
                divider.setBackground(bg);
                libList.addView(divider);
            }

            View v = getLayoutInflater().inflate(R.layout.library_list_element, libList, false);

            TextView libName = v.findViewById(R.id.library_name);
            libName.setText(l.name);
            TextView libLicense = v.findViewById(R.id.library_license);
            libLicense.setText(l.license);
            ImageButton libWebButton = v.findViewById(R.id.library_website_button);
            libWebButton.setOnClickListener(button -> {
                // Go to library website
                Intent intent = new Intent(Intent.ACTION_VIEW, l.website);
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, getString(R.string.no_browser), Toast.LENGTH_SHORT).show();
                    Log.e("SwAlSh", "No browser?", e);
                }
            });
            ImageButton libLicenseButton = v.findViewById(R.id.library_license_button);
            libLicenseButton.setOnClickListener(button -> {
                // Go to license website
                Intent intent = new Intent(Intent.ACTION_VIEW, l.license_link);
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, getString(R.string.no_browser), Toast.LENGTH_SHORT).show();
                    Log.e("SwAlSh", "No browser?", e);
                }
            });
            libList.addView(v);
        }
    }

    private static class Library {
        String name;
        Uri website;
        String license;
        Uri license_link;

        static final Uri APACHE_2_URI = Uri.parse("http://www.apache.org/licenses/LICENSE-2.0.html");

        public Library(String name, Uri website, String license, Uri license_link) {
            this.name = name;
            this.website = website;
            this.license = license;
            this.license_link = license_link;
        }
    }
}