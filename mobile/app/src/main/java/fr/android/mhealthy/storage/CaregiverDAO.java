package fr.android.mhealthy.storage;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import fr.android.mhealthy.model.Instruction;
import fr.android.mhealthy.model.Patient;
import fr.android.mhealthy.model.PatientInfo;
import fr.android.mhealthy.model.PendingTransactionNotification;
import fr.android.mhealthy.model.Session;
import fr.android.mhealthy.service.PatientAlarmScheduler;

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
        if (last_id == 0) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.LAST_ID, last_id);
        db.update(
                DatabaseHelper.TABLE_LAST_ID,
                values,
                DatabaseHelper.LAST_ID_USER + " = ? and " + DatabaseHelper.LAST_ID_TYPE + " = ?",
                new String[]{ String.valueOf(patient), type }
        );
    }

    public void new_patient(Instruction.AddPatient add, String json, int patient,
                            int last_id) {
        SQLiteDatabase db = sdb.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.USER_ID, add.new_patient.id);
            values.put(DatabaseHelper.USER_NAME, add.new_patient.name);
            values.put(DatabaseHelper.USER_PHONE, add.new_patient.phone);
            values.put(DatabaseHelper.USER_ADDED_DATE, add.time);
            values.put(DatabaseHelper.USER_ACTIVE, 1);
            values.putNull(DatabaseHelper.USER_ADDED_BY);
            db.insertWithOnConflict(DatabaseHelper.TABLE_USER, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            PatientDAO.add_assign_history(db, json, add.time);
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

    public void unassign_patient(Instruction.UnassignCaregiver edit, String json, Integer patient,
                                 int last_id) {
        SQLiteDatabase db = sdb.getWritableDatabase();
        db.beginTransaction();
        try {
            PatientDAO.unassign_inner(sdb.getWritableDatabase(), edit, json, patient);
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
                boolean active = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.USER_ACTIVE)) == 1;
                Patient patient = new Patient(id, name, time, phone, active);
                patients.add(patient);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return patients;
    }

    public void instruction_operation(Instruction op, String json, String trans_json,
                                      int patient, boolean self_care) {
        SQLiteDatabase db = sdb.getWritableDatabase();
        long pending = 0;
        db.beginTransaction();
        try {
            if (history_exists(db, op, patient)) {
                update_last_instruction_id(db, patient, op.id);
                db.setTransactionSuccessful();
                db.endTransaction();
                db.close();
                return;
            }
            switch (op.type) {
                case AddMedicine:
                    Instruction.AddMedicine add = (Instruction.AddMedicine)op.instruction;
                    PatientDAO.add_medicine_inner(db, op, add, patient, patient, json);
                    break;
                case EditMedicine:
                    Instruction.EditMedicine edit = (Instruction.EditMedicine)op.instruction;
                    PatientDAO.edit_medicine_inner(db, op, edit, patient, patient, json);
                    break;
                case RemoveMedicine:
                    Instruction.RemoveMedicine rm = (Instruction.RemoveMedicine)op.instruction;
                    PatientDAO.remove_medicine_inner(db, op, rm, patient, patient, json);
                    break;
                case AddActivity:
                    Instruction.AddActivity adda = (Instruction.AddActivity)op.instruction;
                    PatientDAO.add_activity_inner(db, op, adda, patient, patient, json);
                    break;
                case EditActivity:
                    Instruction.EditActivity edita = (Instruction.EditActivity)op.instruction;
                    PatientDAO.edit_activity_inner(db, op, edita, patient, patient, json);
                    break;
                case RemoveActivity:
                    Instruction.RemoveActivity rma = (Instruction.RemoveActivity)op.instruction;
                    PatientDAO.remove_activity_inner(db, op, rma, patient, patient, json);
                    break;
                default:
                    // This should be unreachable
                    return;
            }
            update_last_instruction_id(db, patient, op.id);
            if (trans_json != null && !self_care) {
                pending = PendingTransactionDAO.insert(db, "instruction", trans_json);
            }
            db.setTransactionSuccessful();
            db.endTransaction();
            db.close();
        } catch (Exception e) {
            db.endTransaction();
            db.close();
            throw e;
        }
        if (pending != 0) {
            EventBus.getDefault().post(new PendingTransactionNotification(pending));
        }
        if (self_care) {
            EventBus.getDefault().post(new PatientAlarmScheduler.Updated());
        }
    }

    private boolean history_exists(SQLiteDatabase db, Object op, int patient) {
        String table;
        String user;
        String up_time;
        String data;

        String type;
        long time;
        boolean is_med;

        if (op instanceof Instruction) {
            Instruction ins = (Instruction) op;
            if (ins.type == Instruction.InstructionType.AddMedicine ||
                    ins.type == Instruction.InstructionType.EditMedicine ||
                    ins.type == Instruction.InstructionType.RemoveMedicine) {
                is_med = true;
            } else {
                is_med = false;
            }
            type = ins.get_type();
            time = ins.get_time();
        } else {
            PatientInfo info = (PatientInfo) op;
            if (info.type == PatientInfo.PatientInfoType.MedicineTaken) {
                is_med = true;
            } else {
                is_med = false;
            }
            type = info.get_type();
            time = info.get_time();
        }

        if (is_med) {
            table = DatabaseHelper.TABLE_MEDICATION_HISTORY;
            user = DatabaseHelper.HISTORY_USER;
            up_time = DatabaseHelper.HISTORY_UPDATE_TIME;
            data = DatabaseHelper.HISTORY_DATA;
        } else {
            table = DatabaseHelper.TABLE_ACTIVITY_HISTORY;
            user = DatabaseHelper.ACTIVITY_HISTORY_USER;
            up_time = DatabaseHelper.ACTIVITY_HISTORY_UPDATE_TIME;
            data = DatabaseHelper.ACTIVITY_HISTORY_DATA;
        }

        Cursor cursor = db.rawQuery("SELECT 1 from " + table +
                        " WHERE " + user + " = ? and " +
                        up_time + " = ? and " +
                        data + " like '%\"" + type + "\"%'",
                new String[]{ String.valueOf(patient), String.valueOf(time) });
        if (cursor == null) {
            return false;
        }
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public void add_patient_info(PatientInfo op, String json,
                              String name, long time, Integer patient) {
        SQLiteDatabase db = sdb.getWritableDatabase();
        db.beginTransaction();
        try {
            if (history_exists(db, op, patient)) {
                update_last_patient_info_id(db, patient, op.id);
                db.setTransactionSuccessful();
                db.endTransaction();
                db.close();
                return;
            }
            if (op.type == PatientInfo.PatientInfoType.MedicineTaken) {
                PatientDAO.add_medicine_history(db, op, json, name, time, patient);
            } else {
                PatientDAO.add_activity_history(db, op, json, name, time, patient);
            }
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
