package fr.android.mhealthy.storage;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import fr.android.mhealthy.model.PendingTransaction;
import fr.android.mhealthy.model.Session;

public class PendingTransactionDAO {
    public DatabaseHelper sdb;

    public PendingTransactionDAO(Context ctx, Session s) {
        sdb = new DatabaseHelper(ctx, s);
    }

    public static long insert(SQLiteDatabase db, String type, String data) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.TRANSACTION_TYPE, type);
        values.put(DatabaseHelper.TRANSACTION_DATA, data);

        return db.insertOrThrow(DatabaseHelper.TABLE_PENDING_TRANSACTION, null, values);
    }

    @SuppressLint("Range")
    public PendingTransaction get() {
        SQLiteDatabase db = sdb.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " +
                DatabaseHelper.TABLE_PENDING_TRANSACTION + " order by " +
                DatabaseHelper.TRANSACTION_ID + " limit 1",
                null
        );

        if (cursor.moveToFirst()) {
            long idw = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.TRANSACTION_ID));
            String type = cursor.getString(cursor.getColumnIndex(DatabaseHelper.TRANSACTION_TYPE));
            String data = cursor.getString(cursor.getColumnIndex(DatabaseHelper.TRANSACTION_DATA));
            cursor.close();
            db.close();
            return new PendingTransaction(idw, type, data);
        }
        cursor.close();
        db.close();
        return null;
    }

    public void remove(long id) {
        SQLiteDatabase db = sdb.getWritableDatabase();
        db.execSQL("DELETE FROM " + DatabaseHelper.TABLE_PENDING_TRANSACTION +
                " where " + DatabaseHelper.TRANSACTION_ID + " <= ?",
                new String[]{ String.valueOf(id) });
    }
}
