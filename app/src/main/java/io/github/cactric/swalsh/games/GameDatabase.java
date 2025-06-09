package io.github.cactric.swalsh.games;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Game.class}, version = 1)
public abstract class GameDatabase extends RoomDatabase {
    public abstract GameDao gameDao();


    // Instance + get method for a singleton pattern
    private static volatile GameDatabase instance;
    public static GameDatabase getDatabase(final Context context) {
        if (instance == null) {
            synchronized (GameDatabase.class) {
                instance = Room.databaseBuilder(context.getApplicationContext(),
                                GameDatabase.class,
                                "game_database")
                        .fallbackToDestructiveMigration(true)
                        .allowMainThreadQueries() // TODO: rewrite to remove this. Although the DB shouldn't get THAT big...
                        .build();
            }
        }
        return instance;
    }
}
