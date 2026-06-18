package com.example.joshuatrabkap1;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Handles all CRUD operations for the LOGIN_INFO table (User registration and login).

 */
public class UserDataSource {

    private static final String TAG = "UserDataSource";
    private SQLiteDatabase database;
    private final WeightTrackerDBHelper dbHelper;


    private final String[] allColumns = {
            WeightTrackerDBHelper.COLUMN_ID_USER, // Primary key
            WeightTrackerDBHelper.COLUMN_USERNAME,
            WeightTrackerDBHelper.COLUMN_PASSWORD
    };

    public UserDataSource(Context context) {
        dbHelper = new WeightTrackerDBHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
        Log.d(TAG, "Database opened for writing.");
    }

    public void close() {
        dbHelper.close();
        Log.d(TAG, "Database closed.");
    }

    /**
     * Registers a new user.
     * @return The user's ID if successful, or -1 if the username already exists.
     */
    public long registerUser(String username, String password) {
        ContentValues values = new ContentValues();
        values.put(WeightTrackerDBHelper.COLUMN_USERNAME, username);
        values.put(WeightTrackerDBHelper.COLUMN_PASSWORD, password);

        try {

            long insertId = database.insertOrThrow(WeightTrackerDBHelper.TABLE_LOGIN_INFO, null, values);
            Log.d(TAG, "User registered with ID: " + insertId);
            return insertId;
        } catch (Exception e) {
            //Error for usernames already existing
            Log.e(TAG, "Error registering user or username already exists: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Attempts to log in a user.
     * @return The user's ID (COLUMN_ID_USER) if credentials match, or -1 otherwise.
     */
    public long loginUser(String username, String password) {
        Cursor cursor = database.query(
                WeightTrackerDBHelper.TABLE_LOGIN_INFO,
                allColumns,
                WeightTrackerDBHelper.COLUMN_USERNAME + " = ? AND " + WeightTrackerDBHelper.COLUMN_PASSWORD + " = ?",
                new String[]{username, password},
                null, null, null
        );

        long userId = -1;
        if (cursor.moveToFirst()) {
            userId = cursor.getLong(cursor.getColumnIndexOrThrow(WeightTrackerDBHelper.COLUMN_ID_USER));
        }
        cursor.close();

        Log.d(TAG, "Login attempt for user: " + username + ". Result ID: " + userId);
        return userId;
    }

    /**
     * Checks if a username already exists during registration.
     */
    public boolean usernameExists(String username) {
        Cursor cursor = database.query(
                WeightTrackerDBHelper.TABLE_LOGIN_INFO,
                new String[]{WeightTrackerDBHelper.COLUMN_ID_USER}, // Select only the ID column
                WeightTrackerDBHelper.COLUMN_USERNAME + " = ?",
                new String[]{username},
                null, null, null
        );
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }
}

