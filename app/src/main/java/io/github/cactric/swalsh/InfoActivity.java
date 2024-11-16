package io.github.cactric.swalsh;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class InfoActivity extends AppCompatActivity {

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
                Log.e("SwAlSh", "No browser?", e);
            }
        });

    }
}