package com.tejus.shavedogmusic.resources;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class ShaveDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "shavedb";

    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_CREATE = "create table friends (_id integer primary key autoincrement, "
            + "username text not null, address text not null, status text not null);";

    public ShaveDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
    }

    
    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion,
            int newVersion) {
        Log.w(ShaveDbHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        database.execSQL("DROP TABLE IF EXISTS friends");
        onCreate(database);
    }
}
