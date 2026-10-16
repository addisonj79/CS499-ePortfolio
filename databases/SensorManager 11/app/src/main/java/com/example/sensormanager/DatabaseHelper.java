package com.example.sensormanager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "events.db";
    private static final int DATABASE_VERSION = 3;

    // Users table constants
    public static final String TABLE_USERS = "users";
    public static final String COLUMN_USER_ID = "id";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_PASSWORD = "password";

    // Events table constants
    public static final String TABLE_EVENTS = "events";
    public static final String COLUMN_EVENT_ID = "id";
    public static final String COLUMN_EVENT_NAME = "name";
    public static final String COLUMN_EVENT_DATE = "date";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create users table
        String createUsersTable = "CREATE TABLE " + TABLE_USERS + " (" +
                COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_USERNAME + " TEXT UNIQUE, " +
                COLUMN_PASSWORD + " TEXT)";
        db.execSQL(createUsersTable);

        // Create events table
        String createEventsTable = "CREATE TABLE " + TABLE_EVENTS + " (" +
                COLUMN_EVENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_EVENT_NAME + " TEXT NOT NULL, " +
                COLUMN_EVENT_DATE + " TEXT NOT NULL)";
        db.execSQL(createEventsTable);

        createEventIndexes(db);
    }

    private void createEventIndexes(SQLiteDatabase db) {
        // Add indexes for the fields used when searching and sorting events.
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_events_name ON " +
                TABLE_EVENTS + "(" + COLUMN_EVENT_NAME + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_events_date ON " +
                TABLE_EVENTS + "(" + COLUMN_EVENT_DATE + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Keep the existing data when the database is updated.
        if (oldVersion < 3) {
            createEventIndexes(db);
        }
    }

    // --- User Operations ---

    public boolean addUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_PASSWORD, password);

        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    public boolean checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COLUMN_USERNAME + "=? AND " + COLUMN_PASSWORD + "=?";
        String[] selectionArgs = {username, password};
        Cursor cursor = db.query(TABLE_USERS, null, selection, selectionArgs, null, null, null);

        int count = cursor.getCount();
        cursor.close();
        return count > 0;
    }

    // --- Event Operations ---

    public boolean addEvent(String name, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_EVENT_NAME, name);
        values.put(COLUMN_EVENT_DATE, date);

        long result = db.insert(TABLE_EVENTS, null, values);
        return result != -1;
    }

    public boolean updateEvent(int id, String name, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_EVENT_NAME, name);
        values.put(COLUMN_EVENT_DATE, date);

        int result = db.update(TABLE_EVENTS, values, COLUMN_EVENT_ID + "=?",
                new String[]{String.valueOf(id)});
        return result > 0;
    }

    public boolean deleteEvent(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_EVENTS, COLUMN_EVENT_ID + "=?",
                new String[]{String.valueOf(id)});
        return result > 0;
    }

    public Cursor searchEvents(String searchText, String dateFilter, String sortOption) {
        SQLiteDatabase db = this.getReadableDatabase();

        StringBuilder selection = new StringBuilder();
        ListSelectionArgs args = new ListSelectionArgs();

        if (searchText != null && !searchText.trim().isEmpty()) {
            selection.append("LOWER(").append(COLUMN_EVENT_NAME).append(") LIKE ?");
            args.add("%" + searchText.trim().toLowerCase() + "%");
        }

        if ("UPCOMING".equals(dateFilter) || "PAST".equals(dateFilter)) {
            if (selection.length() > 0) {
                selection.append(" AND ");
            }
            selection.append(COLUMN_EVENT_DATE)
                    .append(" ")
                    .append("UPCOMING".equals(dateFilter) ? ">= ?" : "< ?");
            args.add(new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                    .format(new java.util.Date()));
        }

        String orderBy;
        switch (sortOption) {
            case "NAME_DESC":
                orderBy = COLUMN_EVENT_NAME + " COLLATE NOCASE DESC";
                break;
            case "DATE_ASC":
                orderBy = COLUMN_EVENT_DATE + " ASC, " + COLUMN_EVENT_NAME + " COLLATE NOCASE ASC";
                break;
            case "DATE_DESC":
                orderBy = COLUMN_EVENT_DATE + " DESC, " + COLUMN_EVENT_NAME + " COLLATE NOCASE ASC";
                break;
            case "NAME_ASC":
            default:
                orderBy = COLUMN_EVENT_NAME + " COLLATE NOCASE ASC";
                break;
        }

        return db.query(
                TABLE_EVENTS,
                null,
                selection.length() == 0 ? null : selection.toString(),
                args.toArray(),
                null,
                null,
                orderBy
        );
    }

    public Cursor getAllEvents() {
        return searchEvents("", "ALL", "DATE_ASC");
    }

    private static class ListSelectionArgs {
        private final java.util.ArrayList<String> values = new java.util.ArrayList<>();

        void add(String value) {
            values.add(value);
        }

        String[] toArray() {
            return values.toArray(new String[0]);
        }
    }
}
