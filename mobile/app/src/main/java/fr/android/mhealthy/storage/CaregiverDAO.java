package fr.android.mhealthy.storage;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import fr.android.mhealthy.model.Instruction;
import fr.android.mhealthy.model.Patient;
import fr.android.mhealthy.model.Session;

public class CaregiverDAO {
    public DatabaseHelper sdb;

    public CaregiverDAO(Context ctx, Session s) {
        sdb = new DatabaseHelper(ctx, s);
    }

    private void update_last_instruction_id(SQLiteDatabase db, int patient, int last_id) {
        update_last_id(db, patient, "instruction", last_id);
    }

    private void update_last_patient_info_id(SQLiteDatabase db, int patient, int last_id) {
        update_last_id(db, patient, "patient_info", last_id);
    }

    private void update_last_id(SQLiteDatabase db, int patient, String type, int last_id) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.LAST_ID, last_id);
        db.update(
                DatabaseHelper.TABLE_LAST_ID,
                values,
                DatabaseHelper.LAST_ID_USER + " = ? and " + DatabaseHelper.LAST_ID_TYPE + " = ?",
                new String[]{ String.valueOf(patient), type }
        );
    }

    public void new_patient(Instruction.AddPatient add, int patient,
                            int last_id) {
        SQLiteDatabase db = sdb.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.USER_ID, add.new_patient.id);
            values.put(DatabaseHelper.USER_NAME, add.new_patient.name);
            values.put(DatabaseHelper.USER_PHONE, add.new_patient.phone);
            values.put(DatabaseHelper.USER_ADDED_DATE, add.time);
            values.putNull(DatabaseHelper.USER_ADDED_BY);
            db.insertOrThrow(DatabaseHelper.TABLE_USER, null, values);
            update_last_instruction_id(db, patient, last_id);
            db.setTransactionSuccessful();
            db.endTransaction();
            db.close();
        } catch (Exception e) {
            db.endTransaction();
            db.close();
            throw e;
        }
    }

    @SuppressLint("Range")
    public List<Patient> get_all_patients() {
        SQLiteDatabase db   = sdb.getReadableDatabase();
        List<Patient> patients = new ArrayList<>();

        Cursor cursor = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_USER + " ORDER BY " +
                DatabaseHelper.USER_ADDED_DATE +" DESC", null);

        if (cursor.moveToFirst()) {
            do {
                int id      = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.USER_ID));
                String name = cursor.getString(cursor.getColumnIndex(DatabaseHelper.USER_NAME));
                int time    = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.USER_ADDED_DATE));
                String phone = cursor.getString(cursor.getColumnIndex(DatabaseHelper.USER_PHONE));
                Patient patient = new Patient(id, name, time, phone);
                patients.add(patient);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return patients;
    }

    public void medicine_operation(Instruction op, String json, int patient) {
        SQLiteDatabase db = sdb.getWritableDatabase();
        db.beginTransaction();
        try {
            switch (op.type) {
                case AddMedicine:
                    Instruction.AddMedicine add = (Instruction.AddMedicine)op.instruction;
                    PatientDAO.add_medicine_inner(db, add, op.caregiver, patient, json);
                    break;
                case EditMedicine:
                    Instruction.EditMedicine edit = (Instruction.EditMedicine)op.instruction;
                    PatientDAO.edit_medicine_inner(db, edit, op.caregiver, patient, json);
                    break;
                case RemoveMedicine:
                    Instruction.RemoveMedicine rm = (Instruction.RemoveMedicine)op.instruction;
                    PatientDAO.remove_medicine_inner(db, rm, op.caregiver, patient, json);
                    break;
                default:
                    // This should be unreachable
                    return;
            }
            update_last_instruction_id(db, patient, op.id);
            db.setTransactionSuccessful();
            db.endTransaction();
            db.close();
        } catch (Exception e) {
            db.endTransaction();
            db.close();
            throw e;
        }
    }
}
