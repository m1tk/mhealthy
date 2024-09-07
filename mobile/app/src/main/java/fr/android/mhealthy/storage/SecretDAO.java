package fr.android.mhealthy.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import fr.android.mhealthy.model.Session;

public class SecretDAO {
    public DatabaseHelper sdb;

    public SecretDAO(Context ctx, Session s) {
        sdb = new DatabaseHelper(ctx, s);
    }

    public void update_token(String token) {
        update_inner("token", token);
    }

    public void update_inner(String type, String secret) {
        SQLiteDatabase db = sdb.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.SECRET_TYPE, type);
        values.put(DatabaseHelper.SECRET_SECRET, secret);
        db.insertWithOnConflict(DatabaseHelper.TABLE_SECRET, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }
}
