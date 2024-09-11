package fr.android.mhealthy.model;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Instruction {
    public InstructionType type;
    public Object instruction;
    public int caregiver;
    public int id;

    public enum InstructionType {
        AddCaregiver,
        AddPatient,
        AddMedicine,
        EditMedicine,
        RemoveMedicine
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
            default:
                throw new InstantiationError("Unknown instruction type");
        }
        this.caregiver = caregiver;
        this.id = id;
    }

    public Instruction(Gson p, String ins, int caregiver, int id) {
        this(p, JsonParser.parseString(ins).getAsJsonObject(), caregiver, id);
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
            default:
                // This should not happen
                throw new InstantiationError("Unknown instruction type");
        }
        obj.addProperty("type", type);
        return obj;
    }

    public JsonObject to_server_json_format(Gson p, int patient) {
        JsonObject o = new JsonObject();
        o.add("data", to_store_json_format(p));
        o.addProperty("patient", patient);
        return o;
    }
}
