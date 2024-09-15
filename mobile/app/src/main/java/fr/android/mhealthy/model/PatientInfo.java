package fr.android.mhealthy.model;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class PatientInfo {
    public Object info;
    public PatientInfoType type;
    public int id;
    public String name;
    private long time;

    public enum PatientInfoType {
        MedicineTaken,
        ActivityFinished
    }

    public static class MedicineTaken {
        public long time;
    }

    public static class ActivityFinished {
        public long time;
        public String value;
    }

    public PatientInfo(Gson p, JsonObject ins, int id) {
        switch (ins.get("type").getAsString()) {
            case "medicine_taken":
                this.type = PatientInfoType.MedicineTaken;
                this.info = p.fromJson(ins.toString(), MedicineTaken.class);
                break;
            case "activity_finished":
                this.type = PatientInfoType.ActivityFinished;
                this.info = p.fromJson(ins.toString(), ActivityFinished.class);
                break;
            default:
                throw new InstantiationError("Unknown instruction type");
        }
        this.name = ins.get("name").getAsString();
        this.id = id;
        this.time = ins.get("time").getAsLong();
    }

    /// This is used for internal usage only and should not be sent to server
    public PatientInfo(PatientInfoType type, Object info, String name, int id) {
        this.name = name;
        this.type = type;
        this.info = info;
        this.id = id;
    }

    public JsonObject to_store_json_format(Gson p) {
        JsonObject obj = p.toJsonTree(this.info)
                .getAsJsonObject();
        // We must also insert the type
        String type = get_type();
        obj.addProperty("type", type);
        return obj;
    }

    public String get_type() {
        String type;
        switch (this.type) {
            case MedicineTaken:
                type = "medicine_taken";
                break;
            case ActivityFinished:
                type = "activity_finished";
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

    public JsonObject to_server_json_format(Gson p) {
        JsonObject o = new JsonObject();
        o.add("data", to_store_json_format(p));
        o.getAsJsonObject("data").addProperty("name", this.name);
        return o;
    }
}
