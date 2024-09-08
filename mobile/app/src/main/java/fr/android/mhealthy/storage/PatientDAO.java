package fr.android.mhealthy.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import fr.android.mhealthy.model.Instruction;
import fr.android.mhealthy.model.Session;

public class PatientDAO {
    public DatabaseHelper sdb;

    public PatientDAO(Context ctx, Session s) {
        sdb = new DatabaseHelper(ctx, s);
    }

    public void new_caregiver(Instruction.AddCaregiver add_caregiver,
                              int by_caregiver, int last_id) {
        SQLiteDatabase db = sdb.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.USER_ID, add_caregiver.new_caregiver.id);
        values.put(DatabaseHelper.USER_NAME, add_caregiver.new_caregiver.name);
        values.put(DatabaseHelper.USER_PHONE, add_caregiver.new_caregiver.phone);
        values.put(DatabaseHelper.USER_ADDED_DATE, add_caregiver.time);
        if (by_caregiver != add_caregiver.new_caregiver.id) {
            values.put(DatabaseHelper.USER_ADDED_BY, by_caregiver);
        }
        db.beginTransaction();
        try {
            db.insertOrThrow(DatabaseHelper.TABLE_USER, null, values);
            update_last_id(db, last_id);
            db.setTransactionSuccessful();
            db.endTransaction();
            db.close();
        } catch (Exception e) {
            db.endTransaction();
            db.close();
            throw e;
        }
    }

    private void update_last_id(SQLiteDatabase db, int last_id) {
        ContentValues values2 = new ContentValues();
        values2.put(DatabaseHelper.LAST_ID, last_id);
        db.update(DatabaseHelper.TABLE_LAST_ID, values2, null, null);
    }
}
