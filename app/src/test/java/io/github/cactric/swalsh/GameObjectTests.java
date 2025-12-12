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
        gameA.gameId = "4CE9651EE88A979D41F24CE8D6EA1C23";
        gameA.gameName = "Spoon Tree";

        Game gameB = new Game();
        gameB.game_primary_key = 0;
        gameB.gameId = "4CE9651EE88A979D41F24CE8D6EA1C23";
        gameB.gameName = "Spoon Tree";

        // Normal
        assertEquals(gameA, gameB);
        // Reflexive
        assertEquals(gameA, gameA);
        // Symmetric
        assertEquals(gameB, gameA);
        // Transitive
        // TODO
        // Consistent
        // TODO

        // Null
        assertNotEquals(null, gameA);
    }
}
