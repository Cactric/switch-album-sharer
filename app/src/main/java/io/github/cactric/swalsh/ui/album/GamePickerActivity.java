package io.github.cactric.swalsh.ui.album;

import android.os.Bundle;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.github.cactric.swalsh.games.Game;
import io.github.cactric.swalsh.games.GameDatabase;
import io.github.cactric.swalsh.games.GameUtils;
import io.github.cactric.swalsh.R;

public class GamePickerActivity extends AppCompatActivity {
    private final ArrayList<Game> games = new ArrayList<>();
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
                adapter = new GamePickerAdapter(games);
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
    }

    private void showOrHidePlaceholder() {
        if (games.isEmpty()) {
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
                importLauncher.launch(new String[]{"text/*"});
            } else if (menuItem.getItemId() == R.id.picker_menu_export) {
                exportLauncher.launch("SwAlSh Export.csv");
            }
            return false;
        }
    }

    private final ActivityResultLauncher<String> exportLauncher = registerForActivityResult(
        new ActivityResultContracts.CreateDocument("text/csv"),
    contentUri -> {
        if (contentUri == null)
            return;
        // Get the database object and the games in it
        GameDatabase db = GameDatabase.getDatabase(this);
        new Thread(() -> {
            try {
                // Open the file and write the csv header
                OutputStream os = getContentResolver().openOutputStream(contentUri);
                if (os == null) {
                    return;
                }

                String header = "game_id,game_name\n";
                os.write(header.getBytes(StandardCharsets.UTF_8));

                // Write the games
                for (Game g: db.gameDao().getAll()) {
                    os.write(g.gameId.getBytes(StandardCharsets.UTF_8));
                    os.write(',');
                    // Remove commas and new lines in the game name to avoid issues parsing the output
                    os.write(g.gameName.replace(",", "").replace("\n","").getBytes(StandardCharsets.UTF_8));
                    os.write('\n');
                }

                os.close();
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
            new Thread(() -> {
                Log.d("SwAlSh", "User picked " + contentUri);
                if (contentUri == null)
                    return;
                // Read the file
                try (ParcelFileDescriptor fd = getContentResolver().openFileDescriptor(contentUri, "r");
                    FileReader fr = new FileReader(Objects.requireNonNull(fd).getFileDescriptor())) {
                    BufferedReader reader = new BufferedReader(fr);

                    String header = reader.readLine();
                    // Parse the header
                    String[] columns = header.split(",");

                    String line;
                    ArrayList<Game> newGames = new ArrayList<>();
                    while (true) {
                        String gameId = null;
                        String gameName = null;

                        line = reader.readLine();
                        if (line == null)
                            break;

                        String[] data = line.split(",");
                        for (int i = 0; i < data.length; i++) {
                            if (Objects.equals(columns[i], "game_id")) {
                                gameId = data[i];
                            } else if (Objects.equals(columns[i], "game_name")) {
                                gameName = data[i];
                            }
                        }

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
                            newGames.add(db.gameDao().findByGameId(newGame.gameId));
                        }
                    }

                    // Should be done, tell the user and refresh(?)
                    runOnUiThread(() -> {
                        AlertDialog.Builder adb = new AlertDialog.Builder(this);
                        adb.setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());
                        adb.setTitle("Import completed");
                        adb.show();

                        // Make the changes appear
                        int numOfGames = games.size();
                        for (Game ng: newGames) {
                            boolean inserted = false;
                            // Check if the game ID appears in the games array
                            for (int i = 0; i < numOfGames; i++) {
                                Game og = games.get(i);
                                if (Objects.equals(ng.gameId, og.gameId)) {
                                    games.set(i, ng);
                                    adapter.notifyItemChanged(i);
                                    inserted = true;
                                    break;
                                }
                            }

                            // If it doesn't, add it to the array
                            if (!inserted) {
                                games.add(ng);
                                Log.d("SwAlSh", "Added game " + ng.game_primary_key + "/" + ng.gameId + ": " + ng.gameName);
                                adapter.notifyItemInserted(games.size());
                            }
                        }
                        showOrHidePlaceholder();
                    });
                } catch (NullPointerException | IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
    );
}