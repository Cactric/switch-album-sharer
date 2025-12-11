package io.github.cactric.swalsh.games;

import static android.provider.MediaStore.VOLUME_EXTERNAL;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.net.Uri;
import android.util.JsonReader;
import android.util.JsonWriter;

import androidx.annotation.NonNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import io.github.cactric.swalsh.R;

/**
 * Utilities for handling game IDs, e.g. for sorting by game
 */
public class GameUtils {

    private final Context ctx;
    private final GameDatabase db;

    public GameUtils(Context ctx) {
        this.ctx = ctx;
        this.db = GameDatabase.getDatabase(ctx);
    }

    public Game lookupGame(String gameId) {
        // Try finding it in the database, if it's not there, synthesise a new Game object
        // with the game ID and a generic name
        Game gameFromDb = db.gameDao().findByGameId(gameId);
        if (gameFromDb != null) {
            return gameFromDb;
        } else {
            Game newGame = new Game();
            // 0 means auto generate primary key when inserted
            newGame.game_primary_key = 0;
            newGame.gameId = gameId;
            newGame.gameName = ctx.getString(R.string.unknown_game_name_format, gameId.substring(0, 6));
            return newGame;
        }
    }

    public GameItem getTotals(Game game) {
        // This method has a lot of duplicated code with MediaService
        // But doesn't do as much
        int totalPics = 0;
        int totalVids = 0;

        final String[] selectionArgs = {""};
        selectionArgs[0] = "%" + game.gameId + "%";

        try (Cursor c = ctx.getContentResolver().query(
                MediaStore.Images.Media.getContentUri(VOLUME_EXTERNAL),
                new String[] {MediaStore.Images.Media._ID},
                MediaStore.Images.Media.DISPLAY_NAME + " LIKE ?",
                selectionArgs,
                null /* unsorted */
        )) {
            if (c != null)
                totalPics = c.getCount();
        }

        try (Cursor c = ctx.getContentResolver().query(
                MediaStore.Video.Media.getContentUri(VOLUME_EXTERNAL),
                new String[] {MediaStore.Video.Media._ID},
                MediaStore.Video.Media.DISPLAY_NAME + " LIKE ?",
                selectionArgs,
                null /* unsorted */
        )) {
            if (c != null)
                totalVids = c.getCount();
        }
        return new GameItem(game, totalPics, totalVids);
    }

    public GameItem lookupGameItem(String gameId) {
        Game g = lookupGame(gameId);
        return getTotals(g);
    }

    public String lookupGameName(String gameId) {
        return lookupGame(gameId).gameName;
    }

    public static void exportGames(List<Game> games, @NonNull OutputStream os) throws SecurityException, IOException {
        // Open the file and write the JSON
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
        writer.beginArray();
        for (Game g: games) {
            writer.beginObject();
            writer.name("game_id").value(g.gameId);
            writer.name("game_name").value(g.gameName);
            writer.endObject();
        }
        writer.endArray();
        writer.close();
    }

    public static List<Game> importGames(InputStreamReader in) throws IllegalStateException, IOException {
        JsonReader reader = new JsonReader(in);
        ArrayList<Game> foundGames = new ArrayList<>();

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

                foundGames.add(newGame);
            }
        }
        reader.endArray();

        reader.close();
        return foundGames;
    }
}
