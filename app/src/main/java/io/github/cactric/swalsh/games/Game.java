package io.github.cactric.swalsh.games;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Objects;

@Entity
public class Game {
    // Primary key for use in the database
    @PrimaryKey(autoGenerate = true)
    public int game_primary_key;

    // Game ID as used in file names from the Switch and used for sorting by game
    @ColumnInfo(name = "game_id")
    public String gameId;

    @ColumnInfo(name = "game_name")
    public String gameName;

    // TODO: add more fields for localised names
    // (at least if I want to pre-populate them at some point)
    // The gameName field would probably remain as the user-set one though

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof Game other) {
            return Objects.equals(this.gameId, other.gameId) &&
                    Objects.equals(this.gameName, other.gameName) &&
            this.game_primary_key == other.game_primary_key;
        } else {
            return false;
        }
    }

    // The equals() documentation says I should override this too
    @Override
    public int hashCode() {
        return gameId.hashCode() + gameName.hashCode() + game_primary_key;
    }
}
