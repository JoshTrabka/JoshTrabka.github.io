package com.example.joshuatrabkap1;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;



public class WeightTrackerDBHelper extends SQLiteOpenHelper {

    private static final String TAG = "WeightTrackerDBHelper";

    // Database details
    private static final String DATABASE_NAME = "weightTracker.db";
    private static final int DATABASE_VERSION = 3;

    //  Table 1: Login Information (Users)
    public static final String TABLE_LOGIN_INFO = "login_info";
    public static final String COLUMN_ID_USER = "_id"; // Primary Key for all user-related tables
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_PASSWORD = "password";

    //  Table 2: Weight Entries
    public static final String TABLE_WEIGHT_ENTRIES = "weight_entries";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_USER_ID_WEIGHT = "user_id"; // Foreign Key to TABLE_LOGIN_INFO
    public static final String COLUMN_WEIGHT = "weight";
    public static final String COLUMN_DATE = "date"; // Stored as TEXT (MM/dd/yyyy format)

    //  Table 3: Goal Weight
    public static final String TABLE_GOAL_WEIGHTS = "goal_weights";
    public static final String COLUMN_ID_GOAL = "_id";
    public static final String COLUMN_USER_ID_GOAL = "user_id"; // Foreign Key to TABLE_LOGIN_INFO
    public static final String COLUMN_GOAL_WEIGHT = "goal_weight";


    // SQL statement for Table 1 (Login Info)
    private static final String DATABASE_CREATE_LOGIN_INFO = "create table "
            + TABLE_LOGIN_INFO + "("
            + COLUMN_ID_USER + " integer primary key autoincrement, "
            + COLUMN_USERNAME + " text not null, "
            + COLUMN_PASSWORD + " text not null"
            + ");";

    // SQL statement for Table 2 (Weight Entries)
    private static final String DATABASE_CREATE_WEIGHT_ENTRIES = "create table "
            + TABLE_WEIGHT_ENTRIES + "("
            + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_USER_ID_WEIGHT + " integer not null, "
            + COLUMN_WEIGHT + " real not null, "
            + COLUMN_DATE + " text not null, "
            + "FOREIGN KEY(" + COLUMN_USER_ID_WEIGHT + ") REFERENCES "
            + TABLE_LOGIN_INFO + "(" + COLUMN_ID_USER + ")"
            + ");";

    // SQL statement for Table 3 (Goal Weights)
    private static final String DATABASE_CREATE_GOAL_WEIGHTS = "create table "
            + TABLE_GOAL_WEIGHTS + "("
            + COLUMN_ID_GOAL + " integer primary key autoincrement, "
            + COLUMN_USER_ID_GOAL + " integer unique not null, " // Unique ensures one goal per user
            + COLUMN_GOAL_WEIGHT + " real not null, "
            + "FOREIGN KEY(" + COLUMN_USER_ID_GOAL + ") REFERENCES "
            + TABLE_LOGIN_INFO + "(" + COLUMN_ID_USER + ")"
            + ");";


    public WeightTrackerDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        Log.d(TAG, "Creating all three database tables (V" + DATABASE_VERSION + ").");
        // Create all three tables
        database.execSQL(DATABASE_CREATE_LOGIN_INFO);
        database.execSQL(DATABASE_CREATE_WEIGHT_ENTRIES);
        database.execSQL(DATABASE_CREATE_GOAL_WEIGHTS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                + newVersion + ", which will destroy all old data");


        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOGIN_INFO);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WEIGHT_ENTRIES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GOAL_WEIGHTS);

        onCreate(db);
    }

}