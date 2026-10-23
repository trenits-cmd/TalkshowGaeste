package de.trenits.talkshowgaeste.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "talkshow.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE shows (" +
                "id TEXT PRIMARY KEY," +
                "name TEXT NOT NULL," +
                "broadcaster TEXT," +
                "weekdays TEXT," +
                "time TEXT," +
                "frequency TEXT," +
                "color TEXT," +
                "description TEXT," +
                "is_local INTEGER NOT NULL DEFAULT 0," +
                "is_customized INTEGER NOT NULL DEFAULT 0," +
                "is_deleted INTEGER NOT NULL DEFAULT 0)");

        db.execSQL("CREATE TABLE episodes (" +
                "show_id TEXT NOT NULL," +
                "date TEXT NOT NULL," +
                "time TEXT," +
                "topic TEXT," +
                "guests TEXT," +
                "PRIMARY KEY (show_id, date))");

        db.execSQL("CREATE INDEX idx_episode_date ON episodes(date)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Version 1 – bei künftigen Schemaänderungen hier migrieren.
    }
}
