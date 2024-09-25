package fr.android.mhealthy.storage;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import fr.android.mhealthy.model.Session;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "{account_id}.db";
    private static final int DATABASE_VERSION = 1;
    // Table and Column Names
    public static final String TABLE_USER = "user";
    public static final String USER_ID = "user_id";
    public static final String USER_NAME = "name";
    public static final String USER_PHONE = "phone";
    public static final String USER_ADDED_DATE = "added_date";
    public static final String USER_ADDED_BY = "added_by";
    public static final String USER_ACTIVE = "active";

    public static final String TABLE_ASSIGN_HISTORY = "assign_history";
    public static final String ASSIGN_HISTORY_ID = "history_id";
    public static final String ASSIGN_HISTORY_DATA = "data";
    public static final String ASSIGN_HISTORY_UPDATE_TIME = "update_time";

    public static final String TABLE_MEDICATION = "medication";
    public static final String MEDICATION_NAME = "name";
    public static final String MEDICATION_DOSE = "medication_dose";
    public static final String MEDICATION_TIME = "medication_time";
    public static final String MEDICATION_CREATED_AT = "created_at";
    public static final String MEDICATION_UPDATED_AT = "updated_at";
    public static final String MEDICATION_USER = "user";
    public static final String MEDICATION_ACTIVE = "active";

    public static final String TABLE_MEDICATION_HISTORY = "medication_history";
    public static final String HISTORY_ID = "history_id";
    public static final String HISTORY_MEDICATION = "medication";
    public static final String HISTORY_DATA = "data";
    public static final String HISTORY_UPDATE_TIME = "update_time";
    public static final String HISTORY_USER = "user";

    public static final String TABLE_ACTIVITY = "activity";
    public static final String ACTIVITY_NAME = "name";
    public static final String ACTIVITY_DESC = "activity";
    public static final String ACTIVITY_TIME = "activity_time";
    public static final String ACTIVITY_CREATED_AT = "created_at";
    public static final String ACTIVITY_UPDATED_AT = "updated_at";
    public static final String ACTIVITY_USER = "user";
    public static final String ACTIVITY_ACTIVE = "active";

    public static final String TABLE_ACTIVITY_HISTORY = "activity_history";
    public static final String ACTIVITY_HISTORY_ID = "history_id";
    public static final String ACTIVITY_HISTORY_NAME = "activity";
    public static final String ACTIVITY_HISTORY_DATA = "data";
    public static final String ACTIVITY_HISTORY_UPDATE_TIME = "update_time";
    public static final String ACTIVITY_HISTORY_USER = "user";

    public static final String TABLE_PENDING_TRANSACTION = "pending_transaction";
    public static final String TRANSACTION_ID = "transaction_id";
    public static final String TRANSACTION_TYPE = "type";
    public static final String TRANSACTION_DATA = "trans";

    public static final String TABLE_LAST_ID = "last_id";
    public static final String LAST_ID = "id";
    public static final String LAST_ID_TYPE = "type";
    public static final String LAST_ID_USER = "user";

    public static final String TABLE_SECRET = "secret";
    public static final String SECRET_TYPE = "type";
    public static final String SECRET_SECRET = "secret";


    public DatabaseHelper(Context context, Session s) {
        super(
                context,
                DATABASE_NAME.replace("{account_id}", String.valueOf(s.id)),
                null,
                DATABASE_VERSION
        );
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_USER + " (" +
                USER_ID + " INTEGER PRIMARY KEY," +
                USER_NAME + " TEXT," +
                USER_ADDED_DATE + " INTEGER," +
                USER_ADDED_BY + " INTEGER," +
                USER_PHONE + " TEXT," +
                USER_ACTIVE + " INTEGER DEFAULT 1);");

        db.execSQL("CREATE TABLE " + TABLE_ASSIGN_HISTORY + " (" +
                ASSIGN_HISTORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                ASSIGN_HISTORY_DATA + " TEXT," +
                ASSIGN_HISTORY_UPDATE_TIME + " INTEGER);");

        db.execSQL("CREATE TABLE " + TABLE_MEDICATION + " (" +
                MEDICATION_NAME + " TEXT," +
                MEDICATION_DOSE + " TEXT," +
                MEDICATION_TIME + " INTEGER," +
                MEDICATION_CREATED_AT + " INTEGER," +
                MEDICATION_UPDATED_AT + " DATETIME," +
                MEDICATION_USER + " INTEGER," +
                MEDICATION_ACTIVE + " INTEGER DEFAULT 1," +
                "PRIMARY KEY (" + MEDICATION_NAME + "," + MEDICATION_USER + ")," +
                "FOREIGN KEY (" + MEDICATION_USER + ") REFERENCES " + TABLE_USER + "(" + USER_ID + ") ON DELETE CASCADE);");

        db.execSQL("CREATE TABLE " + TABLE_MEDICATION_HISTORY + " (" +
                HISTORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                HISTORY_MEDICATION + " TEXT," +
                HISTORY_DATA + " TEXT," +
                HISTORY_UPDATE_TIME + " INTEGER," +
                HISTORY_USER + " INTEGER," +
                "FOREIGN KEY (" + HISTORY_USER + ") REFERENCES " + TABLE_USER + "(" + USER_ID + ") ON DELETE CASCADE," +
                "FOREIGN KEY (" + HISTORY_MEDICATION + ") REFERENCES " + TABLE_MEDICATION + "(" + MEDICATION_NAME + ") ON DELETE CASCADE);");

        db.execSQL("CREATE TABLE " + TABLE_ACTIVITY + " (" +
                ACTIVITY_NAME + " TEXT," +
                ACTIVITY_DESC + " TEXT," +
                ACTIVITY_TIME + " INTEGER," +
                ACTIVITY_CREATED_AT + " INTEGER," +
                ACTIVITY_UPDATED_AT + " DATETIME," +
                ACTIVITY_USER + " INTEGER," +
                ACTIVITY_ACTIVE + " INTEGER DEFAULT 1," +
                "PRIMARY KEY (" + ACTIVITY_NAME + "," + ACTIVITY_USER + ")," +
                "FOREIGN KEY (" + ACTIVITY_USER + ") REFERENCES " + TABLE_USER + "(" + USER_ID + ") ON DELETE CASCADE);");

        db.execSQL("CREATE TABLE " + TABLE_ACTIVITY_HISTORY + " (" +
                ACTIVITY_HISTORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                ACTIVITY_HISTORY_NAME + " TEXT," +
                ACTIVITY_HISTORY_DATA + " TEXT," +
                ACTIVITY_HISTORY_UPDATE_TIME + " INTEGER," +
                ACTIVITY_HISTORY_USER + " INTEGER," +
                "FOREIGN KEY (" + ACTIVITY_HISTORY_NAME + ") REFERENCES " + TABLE_ACTIVITY + "(" + ACTIVITY_NAME +
                ") ON DELETE CASCADE," +
                "FOREIGN KEY (" + ACTIVITY_HISTORY_USER + ") REFERENCES " + TABLE_USER + "(" + USER_ID + ") ON DELETE CASCADE);");

        db.execSQL("CREATE TABLE " + TABLE_PENDING_TRANSACTION + " (" +
                TRANSACTION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                TRANSACTION_TYPE + " TEXT NOT NULL," +
                TRANSACTION_DATA + " TEXT);");

        db.execSQL("CREATE TABLE " + TABLE_LAST_ID + " (" +
                LAST_ID + " INTEGER," +
                LAST_ID_TYPE + " TEXT," +
                LAST_ID_USER + " INTEGER," +
                "PRIMARY KEY (" + LAST_ID_USER + "," + LAST_ID_TYPE + "));");

        db.execSQL("CREATE TABLE " + TABLE_SECRET + " (" +
                SECRET_TYPE + " TEXT PRIMARY KEY," +
                SECRET_SECRET + " TEXT);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}