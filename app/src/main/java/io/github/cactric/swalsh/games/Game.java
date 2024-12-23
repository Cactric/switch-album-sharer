package io.github.cactric.swalsh.games;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

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
}
