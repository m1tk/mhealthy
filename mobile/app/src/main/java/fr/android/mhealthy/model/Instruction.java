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
        AddCaregiver
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

    public Instruction(Gson p, JsonObject ins, int caregiver, int id) {
        if (ins.get("type").getAsString().equals("assign_caregiver")) {
            this.type        = InstructionType.AddCaregiver;
            this.instruction = p.fromJson(ins.toString(), AddCaregiver.class);
        } else {
            throw new InstantiationError("Unknown instruction type");
        }
        this.caregiver = caregiver;
        this.id = id;
    }

    public Instruction(Gson p, String ins, int caregiver, int id) {
        this(p, JsonParser.parseString(ins).getAsJsonObject(), caregiver, id);
    }
}
