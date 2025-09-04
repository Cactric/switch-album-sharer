package io.github.cactric.swalsh.ui.album;

import android.os.Bundle;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.MenuProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Objects;

import io.github.cactric.swalsh.games.Game;
import io.github.cactric.swalsh.games.GameDatabase;
import io.github.cactric.swalsh.games.GameItem;
import io.github.cactric.swalsh.games.GameUtils;
import io.github.cactric.swalsh.R;

public class GamePickerActivity extends AppCompatActivity {
    private final ArrayList<GameItem> gameItems = new ArrayList<>();
    GamePickerAdapter adapter;
    TextView nothingFoundText;
    RecyclerView recyclerView;


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

        // Get hopefully supplied argument with game IDs in
        String[] gameIds = getIntent().getStringArrayExtra("EXTRA_GAME_ID_LIST");

        new Thread(() -> {
            loadGameListFromDb(gameIds);
            runOnUiThread(() -> {
                // Check if there are no games, show placeholder text if so
                showOrHidePlaceholder();
                // Set adapter
                adapter = new GamePickerAdapter(gameItems, getResources());
                recyclerView.setAdapter(adapter);
            });
        }).start();

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.gp_toolbar);
        toolbar.setTitle(R.string.title_activity_game_picker);
        addMenuProvider(new PickerMenuProvider(), this);
        setSupportActionBar(toolbar);

        // Set up recycler view
        nothingFoundText = findViewById(R.id.gp_nothing);
        recyclerView = findViewById(R.id.gp_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    // Loads the game list from the database, should be run on a separate thread than the UI thread
    private void loadGameListFromDb(String[] gameIds) {
        GameUtils gameUtils = new GameUtils(this);
        if (gameIds != null) {
            for (String id: gameIds) {
                gameItems.add(gameUtils.lookupGameItem(id));
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
            boolean found = false;
            for (GameItem item: gameItems) {
                if (item.game().equals(g)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                gameItems.add(gameUtils.getTotals(g));
            }
        }
    }

    private void showOrHidePlaceholder() {
        if (gameItems.isEmpty()) {
            nothingFoundText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            nothingFoundText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private class PickerMenuProvider implements MenuProvider {
        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            menuInflater.inflate(R.menu.picker_menu, menu);
        }

        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
            if (menuItem.getItemId() == R.id.picker_menu_import) {
                importLauncher.launch(new String[]{"application/json"});
            } else if (menuItem.getItemId() == R.id.picker_menu_export) {
                exportLauncher.launch("SwAlSh Export.json");
            }
            return false;
        }
    }

    private final ActivityResultLauncher<String> exportLauncher = registerForActivityResult(
        new ActivityResultContracts.CreateDocument("application/json"),
    contentUri -> {
        if (contentUri == null)
            return;
        // Get the database object and the games in it
        GameDatabase db = GameDatabase.getDatabase(this);
        new Thread(() -> {
            try {
                // Open the file and write the csv header
                try (OutputStream os = getContentResolver().openOutputStream(contentUri)) {
                    if (os == null) {
                        return;
                    }

                    JsonWriter writer = new JsonWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
                    writer.beginArray();
                    for (Game g: db.gameDao().getAll()) {
                        writer.beginObject();
                        writer.name("game_id").value(g.gameId);
                        writer.name("game_name").value(g.gameName);
                        writer.endObject();
                    }
                    writer.endArray();
                    writer.close();
                }
            } catch (SecurityException | IOException e) {
                Log.e("SwAlSh", "Error while exporting", e);
                Toast.makeText(this, "Error while exporting", Toast.LENGTH_SHORT).show();
            }

            runOnUiThread(() -> {
                // Tell the user it's done
                AlertDialog.Builder adb = new AlertDialog.Builder(this);
                adb.setTitle(R.string.export_complete);
                adb.setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());
                adb.show();
            });
        }).start();
    });

    private final ActivityResultLauncher<String[]> importLauncher = registerForActivityResult(
        new ActivityResultContracts.OpenDocument(),
        contentUri -> {
            GameDatabase db = GameDatabase.getDatabase(this);
            GameUtils gameUtils = new GameUtils(this);
            new Thread(() -> {
                Log.d("SwAlSh", "User picked " + contentUri);
                if (contentUri == null)
                    return;
                ArrayList<GameItem> newGameItems = new ArrayList<>();
                // Read the file
                try (InputStreamReader in = new InputStreamReader(getContentResolver().openInputStream(contentUri), StandardCharsets.UTF_8);
                     JsonReader reader = new JsonReader(in)) {
                    reader.beginArray();
                    while (reader.hasNext()) {
                        String gameId = null;
                        String gameName = null;

                        reader.beginObject();
                        while (reader.hasNext()) {
                            String name = reader.nextName();
                            if (name.equals("game_id")) {
                                gameId = reader.nextString();
                            } else if (name.equals("game_name")) {
                                gameName = reader.nextString();
                            } else {
                                reader.skipValue();
                            }
                        }
                        reader.endObject();

                        if (gameId != null && gameName != null) {
                            Game newGame = new Game();
                            newGame.game_primary_key = 0; // auto-generate when inserted
                            newGame.gameId = gameId;
                            newGame.gameName = gameName;

                            Game oldGame = db.gameDao().findByGameId(gameId);
                            if (oldGame != null) {
                                // if the gameId already exists in the database, copy the primary key
                                // so that it gets replaced
                                newGame.game_primary_key = oldGame.game_primary_key;
                            }

                            db.gameDao().addGame(newGame);
                            // Re-search the database so that we have the primary key populated
                            newGameItems.add(gameUtils.getTotals(db.gameDao().findByGameId(newGame.gameId)));
                        }
                    }
                    reader.endArray();
                } catch (IllegalStateException | IOException e) {
                    Log.e("SwAlSh", "Failed to read/parse JSON", e);
                    runOnUiThread(() -> Toast.makeText(this, R.string.failed_to_import_game_list, Toast.LENGTH_SHORT).show());
                    return;
                }

                // Should be done, tell the user and refresh(?)
                runOnUiThread(() -> {
                    AlertDialog.Builder adb = new AlertDialog.Builder(this);
                    adb.setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());
                    adb.setTitle("Import completed");
                    adb.show();

                    // Make the changes appear
                    int numOfGames = gameItems.size();
                    for (GameItem ng: newGameItems) {
                        boolean inserted = false;
                        // Check if the game ID appears in the games array
                        for (int i = 0; i < numOfGames; i++) {
                            GameItem ogi = gameItems.get(i);
                            if (Objects.equals(ng.game().gameId, ogi.game().gameId)) {
                                gameItems.set(i, ng);
                                adapter.notifyItemChanged(i);
                                inserted = true;
                                break;
                            }
                        }

                        // If it doesn't, add it to the array
                        if (!inserted) {
                            gameItems.add(ng);
                            Log.d("SwAlSh", "Added game " +
                                    ng.game().game_primary_key + "/" +
                                    ng.game().gameId + ": " +
                                    ng.game().gameName);
                            adapter.notifyItemInserted(gameItems.size());
                        }
                    }
                    showOrHidePlaceholder();
                });
            }).start();
        }
    );
}