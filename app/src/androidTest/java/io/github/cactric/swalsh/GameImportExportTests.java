package io.github.cactric.swalsh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.util.MalformedJsonException;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.cactric.swalsh.games.Game;
import io.github.cactric.swalsh.games.GameUtils;

/**
 * Tests for importGames and exportGames in GameUtils.
 * This is an instrumented test since JsonReader is not mocked.
 */
public class GameImportExportTests {
    @Test
    public void singleImportTest() throws IOException {
        String jsonToImportFrom = "[{\"game_id\":\"FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF\",\"game_name\":\"Test game\"}]";

        ArrayList<Game> expectedOutput = new ArrayList<>();
        Game g = new Game();
        g.gameId = "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF";
        g.gameName = "Test game";
        expectedOutput.add(g);

        List<Game> actualOutput = GameUtils.importGames(
                new InputStreamReader(
                        new ByteArrayInputStream(
                                jsonToImportFrom.getBytes(StandardCharsets.UTF_8))));
        assertEquals(expectedOutput, actualOutput);
    }

    @Test
    public void multipleImportTest() throws IOException {
        String jsonToImportFrom = "[{\"game_id\":\"00000000000000000000000000000001\",\"game_name\":\"Game 1\"},{\"game_id\":\"00000000000000000000000000000002\",\"game_name\":\"Game 2\"},{\"game_id\":\"00000000000000000000000000000003\",\"game_name\":\"Game 3\"},{\"game_id\":\"00000000000000000000000000000004\",\"game_name\":\"Game 4\"},{\"game_id\":\"00000000000000000000000000000005\",\"game_name\":\"Game 5\"},{\"game_id\":\"00000000000000000000000000000006\",\"game_name\":\"Game 6\"},{\"game_id\":\"00000000000000000000000000000007\",\"game_name\":\"Game 7\"},{\"game_id\":\"00000000000000000000000000000008\",\"game_name\":\"Game 8\"},{\"game_id\":\"00000000000000000000000000000009\",\"game_name\":\"Game 9\"},{\"game_id\":\"0000000000000000000000000000000A\",\"game_name\":\"Game 10\"},{\"game_id\":\"0000000000000000000000000000000B\",\"game_name\":\"Game 11\"},{\"game_id\":\"0000000000000000000000000000000C\",\"game_name\":\"Game 12\"},{\"game_id\":\"0000000000000000000000000000000D\",\"game_name\":\"Game 13\"},{\"game_id\":\"0000000000000000000000000000000E\",\"game_name\":\"Game 14\"},{\"game_id\":\"0000000000000000000000000000000F\",\"game_name\":\"Game 15\"},{\"game_id\":\"00000000000000000000000000000010\",\"game_name\":\"Game 16\"},{\"game_id\":\"00000000000000000000000000000011\",\"game_name\":\"Game 17\"},{\"game_id\":\"00000000000000000000000000000012\",\"game_name\":\"Game 18\"},{\"game_id\":\"00000000000000000000000000000013\",\"game_name\":\"Game 19\"},{\"game_id\":\"00000000000000000000000000000014\",\"game_name\":\"Game 20\"}]";

        ArrayList<Game> expectedOutput = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            Game g = new Game();
            g.game_primary_key = 0;
            g.gameId = String.format("%032X", i);
            g.gameName = String.format("Game %d", i);
            expectedOutput.add(g);
        }

        List<Game> actualOutput = GameUtils.importGames(
                new InputStreamReader(
                        new ByteArrayInputStream(
                                jsonToImportFrom.getBytes(StandardCharsets.UTF_8))));
        assertEquals(expectedOutput, actualOutput);
    }

    @Test
    public void invalidImportTest() {
        String badJson = "'SwAlSh' is not json";
        InputStreamReader r = new InputStreamReader(
                new ByteArrayInputStream(
                        badJson.getBytes(StandardCharsets.UTF_8)));

        assertThrows(MalformedJsonException.class, () -> GameUtils.importGames(r));
    }

    @Test
    public void emptyImportTest() throws IOException {
        // An empty array is "valid" (just returns an empty list)
        // Fun fact: empty input throws an EOFException
        String badJson = "[]";
        InputStreamReader r = new InputStreamReader(
                new ByteArrayInputStream(
                        badJson.getBytes(StandardCharsets.UTF_8)));

        List<Game> empty = GameUtils.importGames(r);
        assertTrue(empty.isEmpty());
    }

    @Test
    public void singleExportTest() throws IOException {
        Game testGame = new Game();
        testGame.game_primary_key = 1;
        testGame.gameId = "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF";
        testGame.gameName = "Test game";

        OutputStream output = new ByteArrayOutputStream();
        List<Game> input = List.of(testGame);
        GameUtils.exportGames(input, output);

        String expectedJson = String.format("[{\"game_id\":\"%s\",\"game_name\":\"%s\"}]", testGame.gameId, testGame.gameName);
        assertEquals(expectedJson, output.toString());
    }

    @Test
    public void multipleExportTest() throws IOException {
        String expectedJson = "[{\"game_id\":\"00000000000000000000000000000001\",\"game_name\":\"Game 1\"},{\"game_id\":\"00000000000000000000000000000002\",\"game_name\":\"Game 2\"},{\"game_id\":\"00000000000000000000000000000003\",\"game_name\":\"Game 3\"},{\"game_id\":\"00000000000000000000000000000004\",\"game_name\":\"Game 4\"},{\"game_id\":\"00000000000000000000000000000005\",\"game_name\":\"Game 5\"},{\"game_id\":\"00000000000000000000000000000006\",\"game_name\":\"Game 6\"},{\"game_id\":\"00000000000000000000000000000007\",\"game_name\":\"Game 7\"},{\"game_id\":\"00000000000000000000000000000008\",\"game_name\":\"Game 8\"},{\"game_id\":\"00000000000000000000000000000009\",\"game_name\":\"Game 9\"},{\"game_id\":\"0000000000000000000000000000000A\",\"game_name\":\"Game 10\"},{\"game_id\":\"0000000000000000000000000000000B\",\"game_name\":\"Game 11\"},{\"game_id\":\"0000000000000000000000000000000C\",\"game_name\":\"Game 12\"},{\"game_id\":\"0000000000000000000000000000000D\",\"game_name\":\"Game 13\"},{\"game_id\":\"0000000000000000000000000000000E\",\"game_name\":\"Game 14\"},{\"game_id\":\"0000000000000000000000000000000F\",\"game_name\":\"Game 15\"},{\"game_id\":\"00000000000000000000000000000010\",\"game_name\":\"Game 16\"},{\"game_id\":\"00000000000000000000000000000011\",\"game_name\":\"Game 17\"},{\"game_id\":\"00000000000000000000000000000012\",\"game_name\":\"Game 18\"},{\"game_id\":\"00000000000000000000000000000013\",\"game_name\":\"Game 19\"},{\"game_id\":\"00000000000000000000000000000014\",\"game_name\":\"Game 20\"}]";

        ArrayList<Game> input = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            Game g = new Game();
            g.game_primary_key = 0;
            g.gameId = String.format("%032X", i);
            g.gameName = String.format("Game %d", i);
            input.add(g);
        }

        OutputStream output = new ByteArrayOutputStream();
        GameUtils.exportGames(input, output);
        assertEquals(expectedJson, output.toString());
    }

    @Test
    public void emptyExportTest() throws IOException {
        OutputStream output = new ByteArrayOutputStream();
        GameUtils.exportGames(new ArrayList<>(), output);
        assertEquals("[]", output.toString());
    }
}
