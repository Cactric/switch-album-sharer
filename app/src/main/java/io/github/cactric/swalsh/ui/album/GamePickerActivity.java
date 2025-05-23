package io.github.cactric.swalsh.ui.album;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.MenuProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import io.github.cactric.swalsh.games.Game;
import io.github.cactric.swalsh.games.GameDatabase;
import io.github.cactric.swalsh.games.GameUtils;
import io.github.cactric.swalsh.R;

public class GamePickerActivity extends AppCompatActivity {
    private final ArrayList<Game> games = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_game_picker);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.gp_root_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        GameUtils gameUtils = new GameUtils(this);

        // Get hopefully supplied argument with game IDs in
        String[] gameIds = getIntent().getStringArrayExtra("EXTRA_GAME_ID_LIST");
        if (gameIds != null) {
            for (String id: gameIds) {
                games.add(gameUtils.lookupGame(id));
            }
        } else {
            // Maybe this should look up the list itself but I can't be bothered to duplicate code
            Log.e("SwAlSh", "GamePickerActivity needs IDs of the games to be shown supplied in EXTRA_GAME_ID_LIST");
            finish();
            return;
        }
        // Add games in database
        GameDatabase db = GameDatabase.getDatabase(this);
        for (Game g: db.gameDao().getAll()) {
            if (!games.contains(g))
                games.add(g);
        }

        // Debug: print games list
        /*for (Game g: games) {
            System.out.println(g.game_primary_key + ": " + "id = " + g.gameId + " / " + g.gameName);
        }*/

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.gp_toolbar);
        toolbar.setTitle(R.string.title_activity_game_picker);
        addMenuProvider(new PickerMenuProvider(), this);
        setSupportActionBar(toolbar);

        // Set up recycler view
        TextView nothingFoundText = findViewById(R.id.gp_nothing);
        RecyclerView recyclerView = findViewById(R.id.gp_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Check if there are no games, show placeholder text if so
        if (games.isEmpty()) {
            nothingFoundText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            nothingFoundText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }

        // Set adapter
        GamePickerAdapter adapter = new GamePickerAdapter(games);
        recyclerView.setAdapter(adapter);
    }

    private class PickerMenuProvider implements MenuProvider {
        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            menuInflater.inflate(R.menu.picker_menu, menu);
            Log.d("SwAlSh", "Picker menu inflated");
        }

        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
            if (menuItem.getItemId() == R.id.picker_menu_import) {
                Toast.makeText(GamePickerActivity.this, "Import not implemented yet", Toast.LENGTH_SHORT).show();
            } else if (menuItem.getItemId() == R.id.picker_menu_export) {
                Toast.makeText(GamePickerActivity.this, "Export not implemented yet", Toast.LENGTH_SHORT).show();
            }
            return false;
        }
    }
}