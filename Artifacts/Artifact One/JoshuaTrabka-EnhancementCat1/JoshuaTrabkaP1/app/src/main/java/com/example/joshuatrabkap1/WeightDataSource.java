package com.example.joshuatrabkap1;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Handles all CRUD operations for Weight Entries and Goal Weight.
 */
public class WeightDataSource {
    private SQLiteDatabase database;
    private WeightTrackerDBHelper dbHelper;

    private String[] allWeightColumns = {
            WeightTrackerDBHelper.COLUMN_ID,
            WeightTrackerDBHelper.COLUMN_USER_ID_WEIGHT,
            WeightTrackerDBHelper.COLUMN_DATE,
            WeightTrackerDBHelper.COLUMN_WEIGHT
    };

    // Date formatter for DB storage
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
    private static final String TAG = "WeightDataSource";

    public WeightDataSource(Context context) {
        dbHelper = new WeightTrackerDBHelper(context);
    }

    public void open() {
        database = dbHelper.getWritableDatabase();
        Log.d(TAG, "WeightDataSource opened.");
    }

    public void close() {
        dbHelper.close();
        Log.d(TAG, "WeightDataSource closed.");
    }

    /**
     * Adds a new weight entry for a user using a WeightEntry object.
     */
    public WeightEntry addEntry(WeightEntry entry) {
        ContentValues values = new ContentValues();

        // Use the data from the provided WeightEntry object
        values.put(WeightTrackerDBHelper.COLUMN_USER_ID_WEIGHT, entry.getUserId());

        // Format the date object into a string for database storage
        String dateString = DATE_FORMAT.format(entry.getDate());
        values.put(WeightTrackerDBHelper.COLUMN_DATE, dateString);

        values.put(WeightTrackerDBHelper.COLUMN_WEIGHT, entry.getWeight());

        long insertId = database.insert(WeightTrackerDBHelper.TABLE_WEIGHT_ENTRIES, null, values);

        // Fetch the newly created object
        Cursor cursor = database.query(WeightTrackerDBHelper.TABLE_WEIGHT_ENTRIES,
                allWeightColumns, WeightTrackerDBHelper.COLUMN_ID + " = " + insertId, null,
                null, null, null);
        cursor.moveToFirst();
        WeightEntry newEntry = cursorToEntry(cursor);
        cursor.close();


        entry.setId(newEntry.getId());
        return newEntry;
    }

    /**
     * Deletes a weight entry by its ID.
     */
    public void deleteEntry(long entryId) {
        database.delete(WeightTrackerDBHelper.TABLE_WEIGHT_ENTRIES,
                WeightTrackerDBHelper.COLUMN_ID + " = " + entryId, null);
    }

    /**
     * Allows for updating a weight entry by its ID.
     */
    public void updateEntry(WeightEntry entry) {
        ContentValues values = new ContentValues();
        values.put(WeightTrackerDBHelper.COLUMN_DATE, DATE_FORMAT.format(entry.getDate()));
        values.put(WeightTrackerDBHelper.COLUMN_WEIGHT, entry.getWeight());

        database.update(WeightTrackerDBHelper.TABLE_WEIGHT_ENTRIES, values,
                WeightTrackerDBHelper.COLUMN_ID + " = " + entry.getId(), null);
    }

    /**
     * Retrieves all weight entries for a specific user.
     */
    public List<WeightEntry> getAllEntries(long userId) {
        List<WeightEntry> entries = new ArrayList<>();

        // Query only entries for the current user, ordered by date descending
        Cursor cursor = database.query(WeightTrackerDBHelper.TABLE_WEIGHT_ENTRIES,
                allWeightColumns, WeightTrackerDBHelper.COLUMN_USER_ID_WEIGHT + " = " + userId, null,
                null, null, WeightTrackerDBHelper.COLUMN_DATE + " DESC");

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            WeightEntry entry = cursorToEntry(cursor);
            entries.add(entry);
            cursor.moveToNext();
        }
        cursor.close();
        return entries;
    }

    // Utility to convert cursor row to WeightEntry object
    private WeightEntry cursorToEntry(Cursor cursor) {
        WeightEntry entry = new WeightEntry();
        entry.setId(cursor.getLong(0));
        entry.setUserId(cursor.getLong(1));

        // Retrieve and parse date string
        String dateString = cursor.getString(2);
        try {
            entry.setDate(DATE_FORMAT.parse(dateString));
        } catch (Exception e) {
            Log.e(TAG, "Error parsing date: " + dateString, e);
            entry.setDate(new Date(0)); // Set a default invalid date
        }

        entry.setWeight(cursor.getDouble(3));
        return entry;
    }

    /**
     * Sets or updates the goal weight for a user in TABLE_GOAL_WEIGHTS.
     */
    public void setGoalWeight(long userId, double goalWeight) {
        ContentValues values = new ContentValues();
        values.put(WeightTrackerDBHelper.COLUMN_USER_ID_GOAL, userId);
        values.put(WeightTrackerDBHelper.COLUMN_GOAL_WEIGHT, goalWeight);

        // Check if the user already has a goal set.
        long existingGoalId = getGoalId(userId);

        if (existingGoalId != -1) {
            // Update existing goal
            database.update(
                    WeightTrackerDBHelper.TABLE_GOAL_WEIGHTS,
                    values,
                    WeightTrackerDBHelper.COLUMN_ID_GOAL + " = ?",
                    new String[]{String.valueOf(existingGoalId)}
            );
            Log.d(TAG, "Updated goal weight for user ID: " + userId + " to " + goalWeight);
        } else {
            // Insert new goal
            database.insert(
                    WeightTrackerDBHelper.TABLE_GOAL_WEIGHTS,
                    null,
                    values
            );
            Log.d(TAG, "Set initial goal weight for user ID: " + userId + " to " + goalWeight);
        }
    }

    /**
     * Helper method to get the Goal's internal Primary Key ID for updating.
     */
    private long getGoalId(long userId) {
        Cursor cursor = database.query(
                WeightTrackerDBHelper.TABLE_GOAL_WEIGHTS,
                new String[]{WeightTrackerDBHelper.COLUMN_ID_GOAL},
                WeightTrackerDBHelper.COLUMN_USER_ID_GOAL + " = ?",
                new String[]{String.valueOf(userId)},
                null, null, null
        );

        long goalId = -1;
        if (cursor.moveToFirst()) {
            goalId = cursor.getLong(cursor.getColumnIndexOrThrow(WeightTrackerDBHelper.COLUMN_ID_GOAL));
        }
        cursor.close();
        return goalId;
    }

    /**
     * Retrieves the goal weight for a user.
     * @return The goal weight, or -1.0 if no goal is set.
     */
    public double getGoalWeight(long userId) {
        double goalWeight = -1.0;

        Cursor cursor = database.query(
                WeightTrackerDBHelper.TABLE_GOAL_WEIGHTS,
                new String[]{WeightTrackerDBHelper.COLUMN_GOAL_WEIGHT}, // Select only the weight column
                WeightTrackerDBHelper.COLUMN_USER_ID_GOAL + " = ?",
                new String[]{String.valueOf(userId)},
                null, null, null
        );

        if (cursor.moveToFirst()) {
            goalWeight = cursor.getDouble(cursor.getColumnIndexOrThrow(WeightTrackerDBHelper.COLUMN_GOAL_WEIGHT));
        }
        cursor.close();

        Log.d(TAG, "Retrieved goal weight for user ID: " + userId + ". Goal: " + goalWeight);
        return goalWeight;
    }
}




