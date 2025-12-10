package io.github.cactric.swalsh;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import io.github.cactric.swalsh.games.Game;
import io.github.cactric.swalsh.games.GameDatabase;

public class GameDatabaseTests {
    Context targetCtx;
    GameDatabase db;

    @Before
    public void clearDb() {
        targetCtx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        db = GameDatabase.getDatabase(targetCtx);
        db.clearAllTables();
        assertNotNull(db);
    }

    @Test
    public void addAndRetrieveGameTest() {
        String gameId = "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF";
        Game testGame = new Game();
        testGame.game_primary_key = 1;
        testGame.gameId = gameId;
        testGame.gameName = "Test game";
        db.gameDao().addGame(testGame);

        Game foundGame = db.gameDao().findByGameId(gameId);
        assertEquals(testGame, foundGame);
    }

    @Test
    public void getAllTest() {
        // Setup: add 20 games to the database and to an ArrayList
        ArrayList<Game> games = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            Game g = new Game();
            g.game_primary_key = i;
            g.gameId = String.format("%032X", i);
            g.gameName = String.format("Game %d", i);
            games.add(g);
            db.gameDao().addGame(g);
        }

        // Test: check gameDao().getAll returns a list with all 20 games in
        List<Game> gamesInDb = db.gameDao().getAll();
        assertEquals(games, gamesInDb);
    }

    @Test
    public void deleteTest() {
        String gameId = "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF";
        Game testGame = new Game();
        testGame.game_primary_key = 1;
        testGame.gameId = gameId;
        testGame.gameName = "Test game";
        db.gameDao().addGame(testGame);

        db.gameDao().delete(testGame);

        // Try to find it again - should fail
        Game found = db.gameDao().findByGameId(gameId);
        assertNull(found);
    }
}
