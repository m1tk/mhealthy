package fr.android.mhealthy.storage;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import fr.android.mhealthy.model.Session;

public class LastIdDAO {
    public DatabaseHelper sdb;

    public LastIdDAO(Context ctx, Session s) {
        sdb = new DatabaseHelper(ctx, s);
    }

    public int patient_last_id() {
        SQLiteDatabase db = sdb.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + DatabaseHelper.LAST_ID + " FROM " +
                DatabaseHelper.TABLE_LAST_ID, new String[]{});
        if (cursor.getCount() == 0) {
            cursor.close();
            db.execSQL("INSERT INTO " + DatabaseHelper.TABLE_LAST_ID + " (" +
                    DatabaseHelper.LAST_ID + "," +
                    DatabaseHelper.LAST_ID_TYPE + "," +
                    DatabaseHelper.LAST_ID_USER + ") values (0, null, null)");
            return 0;
        } else {
            cursor.moveToFirst();
            int last = cursor.getInt(0);
            cursor.close();
            return last;
        }
    }

    public int caregiver_instruction_last_id(int patient) {
        return last_id_inner(patient, "instruction");
    }

    public int caregiver_patient_info_last_id(int patient) {
        return last_id_inner(patient, "patient_info");
    }

    private int last_id_inner(Integer user, String type) {
        SQLiteDatabase db = sdb.getWritableDatabase();
        String s = user == null ? null : String.valueOf(user);
        Cursor cursor = db.rawQuery("SELECT " + DatabaseHelper.LAST_ID + " FROM " +
                DatabaseHelper.TABLE_LAST_ID + " WHERE " +
                DatabaseHelper.LAST_ID_TYPE + " = ? AND " +
                DatabaseHelper.LAST_ID_USER + " = ?;",
                new String[]{ type, s });
        if (cursor.getCount() == 0) {
            cursor.close();
            db.execSQL("INSERT INTO " + DatabaseHelper.TABLE_LAST_ID + " (" +
                    DatabaseHelper.LAST_ID + "," +
                    DatabaseHelper.LAST_ID_TYPE + "," +
                    DatabaseHelper.LAST_ID_USER + ") values (0, ?, ?)",
                    new String[]{ type, s });
            return 0;
        } else {
            cursor.moveToFirst();
            int last = cursor.getInt(0);
            cursor.close();
            return last;
        }
    }
}
