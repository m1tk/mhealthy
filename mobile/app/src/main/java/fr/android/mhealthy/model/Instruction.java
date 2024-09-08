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
        public int time;
        public NewPatient new_patient;

        public static class NewPatient {
            public int id;
            public String name;
            public String phone;
        }
    }

    public static class AddCaregiver {
        public int time;
        public NewCaregiver new_caregiver;

        public static class NewCaregiver {
            public int id;
            public String name;
            public String phone;
        }
    }

    public static class AddMedicine {
        public int time;
        public String name;
        public String dose;
        public String dose_time;
    }

    public static class EditMedicine {
        public int time;
        public String name;
        public String dose;
        public String dose_time;
    }

    public static class RemoveMedicine {
        public int time;
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
}
