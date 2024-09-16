package fr.android.mhealthy.model;

public class History {
    public Object info;
    public Integer patient;
    public String name;
    public HistoryType type;

    public History(HistoryType type, Object info, String name, Integer patient) {
        this.info = info;
        this.type = type;
        this.name = name;
        this.patient = patient;
    }

    public enum HistoryType {
        Medicine,
        Activity,
        Assignment
    }
}
