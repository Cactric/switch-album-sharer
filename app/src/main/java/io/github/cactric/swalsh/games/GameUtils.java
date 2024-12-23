package io.github.cactric.swalsh.games;

import android.content.Context;

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

    public String lookupGameName(String gameId) {
        return lookupGame(gameId).gameName;
    }
}
