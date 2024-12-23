package io.github.cactric.swalsh.games;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface GameDao {
    @Query("SELECT * FROM game")
    List<Game> getAll();

    @Query("SELECT * FROM game WHERE game_primary_key == :primaryKey")
    Game findByPrimaryKey(int primaryKey);

    @Query("SELECT * FROM game WHERE game_id LIKE :gameId LIMIT 1")
    Game findByGameId(String gameId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addGame(Game game);

    @Delete
    void delete(Game game);
}
