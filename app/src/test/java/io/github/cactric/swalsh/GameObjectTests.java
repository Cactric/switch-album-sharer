package io.github.cactric.swalsh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import io.github.cactric.swalsh.games.Game;

/**
 * Tests for the `Game` class
 */
public class GameObjectTests {
    @Test
    public void equalsWorks() {
        Game gameA = new Game();
        gameA.game_primary_key = 0;
        gameA.gameId = "00000000000000000000000000000000";
        gameA.gameName = "Test game";

        Game gameB = new Game();
        gameB.game_primary_key = 0;
        gameB.gameId = "00000000000000000000000000000000";
        gameB.gameName = "Test game";

        // Normal
        assertEquals(gameA, gameB);
        // Reflexive
        assertEquals(gameA, gameA);
        // Symmetric
        assertEquals(gameB, gameA);

        // Make sure gameA != null
        assertNotEquals(null, gameA);

        // Different primary key - assert gameC and gameA are not equal
        Game gameC = new Game();
        gameC.game_primary_key = 1;
        gameC.gameId = gameA.gameId;
        gameC.gameName = gameA.gameName;
        assertNotEquals(gameA, gameC);

        // Different game ID - assert gameD and gameA are not equal
        Game gameD = new Game();
        gameD.game_primary_key = 0;
        gameD.gameId = "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF";
        gameD.gameName = gameA.gameName;
        assertNotEquals(gameD, gameA);

        // Different game ID - assert gameD and gameA are not equal
        Game gameE = new Game();
        gameE.game_primary_key = 0;
        gameE.gameId = gameA.gameId;
        gameE.gameName = "A different test game";
        assertNotEquals(gameE, gameA);
    }
}
