package com.lovellfelix.caribbeannewsstand.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.os.Handler;

import com.lovellfelix.caribbeannewsstand.MainApplication;
import com.lovellfelix.caribbeannewsstand.R;
import com.lovellfelix.caribbeannewsstand.parser.OPML;
import com.lovellfelix.caribbeannewsstand.provider.FeedData.EntryColumns;
import com.lovellfelix.caribbeannewsstand.provider.FeedData.FeedColumns;
import com.lovellfelix.caribbeannewsstand.provider.FeedData.FilterColumns;
import com.lovellfelix.caribbeannewsstand.provider.FeedData.TaskColumns;

import java.io.File;

class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "CaribbeanNewsStand.db";
    private static final int DATABASE_VERSION = 5;

    private static final String ALTER_TABLE = "ALTER TABLE ";
    private static final String ADD = " ADD ";

    private final Handler mHandler;

    public DatabaseHelper(Handler handler, Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mHandler = handler;
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(createTable(FeedColumns.TABLE_NAME, FeedColumns.COLUMNS));
        database.execSQL(createTable(FilterColumns.TABLE_NAME, FilterColumns.COLUMNS));
        database.execSQL(createTable(EntryColumns.TABLE_NAME, EntryColumns.COLUMNS));
        database.execSQL(createTable(TaskColumns.TABLE_NAME, TaskColumns.COLUMNS));

        // Check if we need to import the backup
        File backupFile = new File(OPML.BACKUP_OPML);
        final boolean hasBackup = backupFile.exists();
        mHandler.post(new Runnable() { // In order to it after the database is created
            @Override
            public void run() {
                new Thread(new Runnable() { // To not block the UI
                    @Override
                    public void run() {
                        try {
                            if (hasBackup) {
                                // Perform an automated import of the backup
                                OPML.importFromFile(OPML.BACKUP_OPML);
                            } else {
                                // No database and no backup, automatically add the default feeds
                                OPML.importFromFile(MainApplication.getContext().getResources().openRawResource(R.raw.default_feeds));
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }).start();
            }
        });
    }

    public void exportToOPML() {
        try {
            OPML.exportToFile(OPML.BACKUP_OPML);
        } catch (Exception ignored) {
        }
    }

    private String createTable(String tableName, String[][] columns) {
        if (tableName == null || columns == null || columns.length == 0) {
            throw new IllegalArgumentException("Invalid parameters for creating table " + tableName);
        } else {
            StringBuilder stringBuilder = new StringBuilder("CREATE TABLE ");

            stringBuilder.append(tableName);
            stringBuilder.append(" (");
            for (int n = 0, i = columns.length; n < i; n++) {
                if (n > 0) {
                    stringBuilder.append(", ");
                }
                stringBuilder.append(columns[n][0]).append(' ').append(columns[n][1]);
            }
            return stringBuilder.append(");").toString();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            executeCatchedSQL(database, ALTER_TABLE + FeedColumns.TABLE_NAME + ADD + FeedColumns.REAL_LAST_UPDATE + ' ' + FeedData.TYPE_DATE_TIME);
        }
        if (oldVersion < 3) {
            executeCatchedSQL(database, ALTER_TABLE + FeedColumns.TABLE_NAME + ADD + FeedColumns.RETRIEVE_FULLTEXT + ' ' + FeedData.TYPE_BOOLEAN);
        }
        if (oldVersion < 4) {
            executeCatchedSQL(database, createTable(TaskColumns.TABLE_NAME, TaskColumns.COLUMNS));
            // Remove old CaribbeanNewsStand directory (now useless)
            try {
                deleteFileOrDir(new File(Environment.getExternalStorageDirectory() + "/CaribbeanNewsStand/"));
            } catch (Exception ignored) {
            }
        }
        if (oldVersion < 5) {
            executeCatchedSQL(database, ALTER_TABLE + TaskColumns.TABLE_NAME + ADD + "UNIQUE(" + TaskColumns.ENTRY_ID + ", " + TaskColumns.IMG_URL_TO_DL + ") ON CONFLICT IGNORE");
        }
    }

    private void executeCatchedSQL(SQLiteDatabase database, String query) {
        try {
            database.execSQL(query);
        } catch (Exception ignored) {
        }
    }

    private void deleteFileOrDir(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteFileOrDir(child);

        fileOrDirectory.delete();
    }
}
