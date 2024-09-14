package fr.android.mhealthy.storage;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import fr.android.mhealthy.model.Activity;
import fr.android.mhealthy.model.Instruction;
import fr.android.mhealthy.model.Medicine;
import fr.android.mhealthy.model.Session;

public class PatientDAO {
    public DatabaseHelper sdb;

    public PatientDAO(Context ctx, Session s) {
        sdb = new DatabaseHelper(ctx, s);
    }

    public void new_caregiver(Instruction.AddCaregiver add_caregiver,
                              int by_caregiver, int last_id) {
        SQLiteDatabase db = sdb.getWritableDatabase();
        db.beginTransaction();
        try {
            new_caregiver_inner(db, add_caregiver, by_caregiver);
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

    static void new_caregiver_inner(SQLiteDatabase db, Instruction.AddCaregiver add_caregiver,
                       int by_caregiver) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.USER_ID, add_caregiver.new_caregiver.id);
        values.put(DatabaseHelper.USER_NAME, add_caregiver.new_caregiver.name);
        values.put(DatabaseHelper.USER_PHONE, add_caregiver.new_caregiver.phone);
        values.put(DatabaseHelper.USER_ADDED_DATE, add_caregiver.time);
        if (by_caregiver != add_caregiver.new_caregiver.id) {
            values.put(DatabaseHelper.USER_ADDED_BY, by_caregiver);
        }
        db.insertOrThrow(DatabaseHelper.TABLE_USER, null, values);
    }

    private void update_last_id(SQLiteDatabase db, int last_id) {
        ContentValues values2 = new ContentValues();
        values2.put(DatabaseHelper.LAST_ID, last_id);
        db.update(DatabaseHelper.TABLE_LAST_ID, values2, null, null);
    }

    public void instruction_operation(Instruction op, String json) {
        SQLiteDatabase db = sdb.getWritableDatabase();
        db.beginTransaction();
        try {
            switch (op.type) {
                case AddMedicine:
                    Instruction.AddMedicine add = (Instruction.AddMedicine)op.instruction;
                    add_medicine_inner(db, add, op.caregiver, null, json);
                    break;
                case EditMedicine:
                    Instruction.EditMedicine edit = (Instruction.EditMedicine)op.instruction;
                    edit_medicine_inner(db, edit, op.caregiver, null, json);
                    break;
                case RemoveMedicine:
                    Instruction.RemoveMedicine rm = (Instruction.RemoveMedicine)op.instruction;
                    remove_medicine_inner(db, rm, op.caregiver, null, json);
                    break;
                case AddActivity:
                    Instruction.AddActivity adda = (Instruction.AddActivity)op.instruction;
                    add_activity_inner(db, adda, op.caregiver, null, json);
                    break;
                case EditActivity:
                    Instruction.EditActivity edita = (Instruction.EditActivity)op.instruction;
                    edit_activity_inner(db, edita, op.caregiver, null, json);
                    break;
                case RemoveActivity:
                    Instruction.RemoveActivity rma = (Instruction.RemoveActivity)op.instruction;
                    remove_activity_inner(db, rma, op.caregiver, null, json);
                    break;
                default:
                    // This should be unreachable
                    return;
            }
            update_last_id(db, op.id);
            db.setTransactionSuccessful();
            db.endTransaction();
            db.close();
        } catch (Exception e) {
            db.endTransaction();
            db.close();
            throw e;
        }
    }

    static void add_medicine_inner(SQLiteDatabase db, Instruction.AddMedicine add,
                            Integer src, Integer dst, String json) {
        ContentValues vals = new ContentValues();
        vals.put(DatabaseHelper.MEDICATION_DOSE, add.dose);
        vals.put(DatabaseHelper.MEDICATION_TIME, add.dose_time);
        vals.put(DatabaseHelper.MEDICATION_UPDATED_AT, add.time);
        vals.put(DatabaseHelper.MEDICATION_ACTIVE, 1);

        int up = db.update(
                DatabaseHelper.TABLE_MEDICATION,
                vals,
                DatabaseHelper.MEDICATION_NAME + " = ? and " +
                DatabaseHelper.MEDICATION_USER + " is " + (dst == null ? "null" : String.valueOf(dst)) + " and " +
                DatabaseHelper.MEDICATION_ACTIVE + " = 0",
                new String[]{ add.name }
        );

        if (up <= 0) {
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.MEDICATION_NAME, add.name);
            values.put(DatabaseHelper.MEDICATION_DOSE, add.dose);
            values.put(DatabaseHelper.MEDICATION_TIME, add.dose_time);
            values.put(DatabaseHelper.MEDICATION_CREATED_AT, add.time);
            values.put(DatabaseHelper.MEDICATION_UPDATED_AT, add.time);
            if (dst != null) {
                values.put(DatabaseHelper.MEDICATION_USER, dst);
            }

            db.insertOrThrow(DatabaseHelper.TABLE_MEDICATION, null, values);
        }
        add_medicine_history(db, json, add.name, add.time, src);

        // Notifying of new message
        Medicine med = new Medicine();
        med.name = add.name;
        med.dose = add.dose;
        med.time = add.dose_time;
        med.created_at = add.time;
        med.updated_at = add.time;
        med.active = true;
        EventBus.getDefault().post(new Medicine.AddMedicineNotification(dst, med));
    }

    static void edit_medicine_inner(SQLiteDatabase db, Instruction.EditMedicine edit,
                             Integer src, Integer dst, String json) {
        SQLiteStatement stmt = db.compileStatement("update "+ DatabaseHelper.TABLE_MEDICATION +
                " set " + DatabaseHelper.MEDICATION_DOSE + " = ?, " +
                DatabaseHelper.MEDICATION_TIME + " = ?, " +
                DatabaseHelper.MEDICATION_UPDATED_AT + " = ? where " +
                DatabaseHelper.MEDICATION_NAME + " = ? and " +
                DatabaseHelper.MEDICATION_USER + " is ?;");
        stmt.bindString(1, edit.dose);
        stmt.bindString(2, edit.dose_time);
        stmt.bindLong(3, edit.time);
        stmt.bindString(4, edit.name);
        if (dst != null) {
            stmt.bindLong(5, dst);
        } else {
            stmt.bindNull(5);
        }
        stmt.execute();
        add_medicine_history(db, json, edit.name, edit.time, src);

        EventBus.getDefault().post(new Medicine.EditMedicineNotification(dst, edit.name, edit.dose,
                edit.dose_time, edit.time));
    }

    static void remove_medicine_inner(SQLiteDatabase db, Instruction.RemoveMedicine rm,
                               Integer src, Integer dst, String json) {
        SQLiteStatement stmt = db.compileStatement("update "+ DatabaseHelper.TABLE_MEDICATION +
                " set " + DatabaseHelper.MEDICATION_ACTIVE + " = ? where " +
                DatabaseHelper.MEDICATION_NAME + " = ? and " +
                DatabaseHelper.MEDICATION_USER + " is ?;");
        stmt.bindLong(1, 0);
        stmt.bindString(2, rm.name);
        if (dst != null) {
            stmt.bindLong(3, dst);
        } else {
            stmt.bindNull(3);
        }
        stmt.execute();
        add_medicine_history(db, json, rm.name, rm.time, src);

        EventBus.getDefault().post(new Medicine.RemoveMedicineNotification(dst, rm.name));
    }

    static void add_medicine_history(SQLiteDatabase db, String json,
                              String name, long time, Integer patient) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.HISTORY_MEDICATION, name);
        values.put(DatabaseHelper.HISTORY_DATA, json);
        values.put(DatabaseHelper.HISTORY_UPDATE_TIME, time);
        if (patient != null) {
            values.put(DatabaseHelper.HISTORY_USER, patient);
        }
        db.insertOrThrow(DatabaseHelper.TABLE_MEDICATION_HISTORY, null, values);
    }

    @SuppressLint("Range")
    public List<Medicine> get_all_meds(Integer user) {
        SQLiteDatabase db = sdb.getReadableDatabase();
        List<Medicine> meds = new ArrayList<>();

        Cursor cursor = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_MEDICATION + " WHERE " +
                DatabaseHelper.MEDICATION_USER + " is " + (user == null ? "null" : String.valueOf(user)) +
                " ORDER BY " + DatabaseHelper.MEDICATION_CREATED_AT + " DESC", null);

        if (cursor.moveToFirst()) {
            do {
                Medicine med = new Medicine();
                med.name = cursor.getString(cursor.getColumnIndex(DatabaseHelper.MEDICATION_NAME));
                med.dose = cursor.getString(cursor.getColumnIndex(DatabaseHelper.MEDICATION_DOSE));
                med.time = cursor.getString(cursor.getColumnIndex(DatabaseHelper.MEDICATION_TIME));
                med.created_at = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.MEDICATION_CREATED_AT));
                med.updated_at = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.MEDICATION_UPDATED_AT));
                med.active = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.MEDICATION_ACTIVE)) == 1;
                meds.add(med);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return meds;
    }

    static void add_activity_inner(SQLiteDatabase db, Instruction.AddActivity add,
                                   Integer src, Integer dst, String json) {
        ContentValues vals = new ContentValues();
        vals.put(DatabaseHelper.ACTIVITY_DESC, add.goal);
        vals.put(DatabaseHelper.ACTIVITY_TIME, add.activity_time);
        vals.put(DatabaseHelper.ACTIVITY_UPDATED_AT, add.time);
        vals.put(DatabaseHelper.ACTIVITY_ACTIVE, 1);

        int up = db.update(
                DatabaseHelper.TABLE_ACTIVITY,
                vals,
                DatabaseHelper.ACTIVITY_NAME + " = ? and " +
                        DatabaseHelper.ACTIVITY_USER + " is " + (dst == null ? "null" : String.valueOf(dst)) + " and " +
                        DatabaseHelper.ACTIVITY_ACTIVE + " = 0",
                new String[]{ add.name }
        );

        if (up <= 0) {
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.ACTIVITY_NAME, add.name);
            values.put(DatabaseHelper.ACTIVITY_DESC, add.goal);
            values.put(DatabaseHelper.ACTIVITY_TIME, add.activity_time);
            values.put(DatabaseHelper.ACTIVITY_CREATED_AT, add.time);
            values.put(DatabaseHelper.ACTIVITY_UPDATED_AT, add.time);
            if (dst != null) {
                values.put(DatabaseHelper.ACTIVITY_USER, dst);
            }

            db.insertOrThrow(DatabaseHelper.TABLE_ACTIVITY, null, values);
        }
        add_activity_history(db, json, add.name, add.time, src);

        // Notifying of new message
        Activity act = new Activity();
        act.name = add.name;
        act.goal = add.goal;
        act.time = add.activity_time;
        act.created_at = add.time;
        act.updated_at = add.time;
        act.active = true;
        EventBus.getDefault().post(new Activity.AddActivityNotification(dst, act));
    }

    static void edit_activity_inner(SQLiteDatabase db, Instruction.EditActivity edit,
                                    Integer src, Integer dst, String json) {
        SQLiteStatement stmt = db.compileStatement("update "+ DatabaseHelper.TABLE_ACTIVITY +
                " set " + DatabaseHelper.ACTIVITY_DESC + " = ?, " +
                DatabaseHelper.ACTIVITY_TIME + " = ?, " +
                DatabaseHelper.ACTIVITY_UPDATED_AT + " = ? where " +
                DatabaseHelper.ACTIVITY_NAME + " = ? and " +
                DatabaseHelper.ACTIVITY_USER + " is ?;");
        stmt.bindString(1, edit.goal);
        stmt.bindString(2, edit.activity_time);
        stmt.bindLong(3, edit.time);
        stmt.bindString(4, edit.name);
        if (dst != null) {
            stmt.bindLong(5, dst);
        } else {
            stmt.bindNull(5);
        }
        stmt.execute();
        add_activity_history(db, json, edit.name, edit.time, src);

        EventBus.getDefault().post(new Activity.EditActivityNotification(dst, edit.name, edit.goal,
                edit.activity_time, edit.time));
    }

    static void remove_activity_inner(SQLiteDatabase db, Instruction.RemoveActivity rm,
                                      Integer src, Integer dst, String json) {
        SQLiteStatement stmt = db.compileStatement("update "+ DatabaseHelper.TABLE_ACTIVITY +
                " set " + DatabaseHelper.ACTIVITY_ACTIVE + " = ? where " +
                DatabaseHelper.ACTIVITY_NAME + " = ? and " +
                DatabaseHelper.ACTIVITY_USER + " is ?;");
        stmt.bindLong(1, 0);
        stmt.bindString(2, rm.name);
        if (dst != null) {
            stmt.bindLong(3, dst);
        } else {
            stmt.bindNull(3);
        }
        stmt.execute();
        add_activity_history(db, json, rm.name, rm.time, src);

        EventBus.getDefault().post(new Activity.RemoveActivityNotification(dst, rm.name));
    }

    static void add_activity_history(SQLiteDatabase db, String json,
                                     String name, long time, Integer patient) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.ACTIVITY_HISTORY_NAME, name);
        values.put(DatabaseHelper.ACTIVITY_HISTORY_DATA, json);
        values.put(DatabaseHelper.ACTIVITY_HISTORY_UPDATE_TIME, time);
        if (patient != null) {
            values.put(DatabaseHelper.ACTIVITY_HISTORY_USER, patient);
        }
        db.insertOrThrow(DatabaseHelper.TABLE_ACTIVITY_HISTORY, null, values);
    }

    @SuppressLint("Range")
    public List<Activity> get_all_activities(Integer user) {
        SQLiteDatabase db = sdb.getReadableDatabase();
        List<Activity> acts = new ArrayList<>();

        Cursor cursor = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_ACTIVITY + " WHERE " +
                DatabaseHelper.ACTIVITY_USER + " is " + (user == null ? "null" : String.valueOf(user)) +
                " ORDER BY " + DatabaseHelper.ACTIVITY_CREATED_AT + " DESC", null);

        if (cursor.moveToFirst()) {
            do {
                Activity act = new Activity();
                act.name = cursor.getString(cursor.getColumnIndex(DatabaseHelper.ACTIVITY_NAME));
                act.goal = cursor.getString(cursor.getColumnIndex(DatabaseHelper.ACTIVITY_DESC));
                act.time = cursor.getString(cursor.getColumnIndex(DatabaseHelper.ACTIVITY_TIME));
                act.created_at = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.ACTIVITY_CREATED_AT));
                act.updated_at = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.ACTIVITY_UPDATED_AT));
                act.active = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.ACTIVITY_ACTIVE)) == 1;
                acts.add(act);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return acts;
    }
}
