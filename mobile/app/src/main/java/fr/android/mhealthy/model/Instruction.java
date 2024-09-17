package fr.android.mhealthy.model;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import fr.android.mhealthy.R;

public class Instruction {
    public InstructionType type;
    public Object instruction;
    public int caregiver;
    public int id;
    private long time;

    public enum InstructionType {
        AddCaregiver,
        AddPatient,
        AddMedicine,
        EditMedicine,
        RemoveMedicine,
        AddActivity,
        EditActivity,
        RemoveActivity
    }

    public static class AddPatient {
        public long time;
        public NewPatient new_patient;

        public static class NewPatient {
            public int id;
            public String name;
            public String phone;
        }
    }

    public static class AddCaregiver {
        public long time;
        public NewCaregiver new_caregiver;

        public static class NewCaregiver {
            public int id;
            public String name;
            public String phone;
        }
    }

    public static class AddMedicine {
        public long time;
        public String name;
        public String dose;
        public String dose_time;
    }

    public static class EditMedicine {
        public long time;
        public String name;
        public String dose;
        public String dose_time;
    }

    public static class RemoveMedicine {
        public long time;
        public String name;
    }

    public static class AddActivity {
        public long time;
        public String name;
        public String goal;
        public String activity_time;
    }

    public static class EditActivity {
        public long time;
        public String name;
        public String goal;
        public String activity_time;
    }

    public static class RemoveActivity {
        public long time;
        public String name;
    }

    public Instruction(Gson p, JsonObject ins, int caregiver, int id) {
        switch (ins.get("type").getAsString()) {
            case "assign_caregiver":
                if (ins.has("new_patient")) {
                    this.type = InstructionType.AddPatient;
                    this.instruction = p.fromJson(ins.toString(), AddPatient.class);
                } else {
                    this.type = InstructionType.AddCaregiver;
                    this.instruction = p.fromJson(ins.toString(), AddCaregiver.class);
                }
                break;
            case "add_medicine":
                this.type = InstructionType.AddMedicine;
                this.instruction = p.fromJson(ins.toString(), AddMedicine.class);
                break;
            case "edit_medicine":
                this.type = InstructionType.EditMedicine;
                this.instruction = p.fromJson(ins.toString(), EditMedicine.class);
                break;
            case "remove_medicine":
                this.type = InstructionType.RemoveMedicine;
                this.instruction = p.fromJson(ins.toString(), RemoveMedicine.class);
                break;
            case "add_activity":
                this.type = InstructionType.AddActivity;
                this.instruction = p.fromJson(ins.toString(), AddActivity.class);
                break;
            case "edit_activity":
                this.type = InstructionType.EditActivity;
                this.instruction = p.fromJson(ins.toString(), EditActivity.class);
                break;
            case "remove_activity":
                this.type = InstructionType.RemoveActivity;
                this.instruction = p.fromJson(ins.toString(), RemoveActivity.class);
                break;
            default:
                throw new InstantiationError("Unknown instruction type");
        }
        this.caregiver = caregiver;
        this.id = id;
        this.time = ins.get("time").getAsLong();
    }

    /// This is used for internal usage only and should not be sent to server
    public Instruction(InstructionType type, Object ins, int caregiver, int id) {
        this.type = type;
        this.instruction = ins;
        this.caregiver = caregiver;
        this.id = id;
    }

    public JsonObject to_store_json_format(Gson p) {
        JsonObject obj = p.toJsonTree(this.instruction)
                .getAsJsonObject();
        // We must also insert the type
        String type = get_type();
        obj.addProperty("type", type);
        return obj;
    }

    public String get_type() {
        String type;
        switch (this.type) {
            case AddCaregiver:
                type = "assign_caregiver";
                break;
            case AddMedicine:
                type = "add_medicine";
                break;
            case EditMedicine:
                type = "edit_medicine";
                break;
            case RemoveMedicine:
                type = "remove_medicine";
                break;
            case AddActivity:
                type = "add_activity";
                break;
            case EditActivity:
                type = "edit_activity";
                break;
            case RemoveActivity:
                type = "remove_activity";
                break;
            default:
                // This should not happen
                throw new InstantiationError("Unknown instruction type");
        }
        return type;
    }

    public long get_time() {
        return this.time;
    }

    public JsonObject to_server_json_format(Gson p, int patient) {
        JsonObject o = new JsonObject();
        o.add("data", to_store_json_format(p));
        o.addProperty("patient", patient);
        return o;
    }

    public String get_action_string(Context ctx) {
        switch (this.type) {
            case AddPatient:
                return ctx.getString(R.string.new_patient_hist, ((AddPatient)instruction).new_patient);
            case AddCaregiver:
                return ctx.getString(R.string.new_caregiver_hist, ((AddCaregiver)instruction).new_caregiver.name);
            case AddMedicine:
                return ctx.getString(R.string.new_med_hist);
            case EditMedicine:
                return ctx.getString(R.string.edit_med_hist,
                        ((EditMedicine)instruction).dose, ((EditMedicine)instruction).dose_time);
            case RemoveMedicine:
                return ctx.getString(R.string.remove_med_hist);
            case AddActivity:
                return ctx.getString(R.string.new_act_hist);
            case EditActivity:
                EditActivity e = (EditActivity) instruction;
                if (e.goal.isEmpty()) {
                    return ctx.getString(R.string.edit_act_hist, e.activity_time);
                } else {
                    return ctx.getString(R.string.edit_act_hist_goal, e.goal, e.activity_time);
                }
            case RemoveActivity:
                return ctx.getString(R.string.remove_act_hist);
            default:
                // This should not happen
                throw new InstantiationError("Unknown instruction type");
        }
    }
}
