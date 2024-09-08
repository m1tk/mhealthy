package fr.android.mhealthy.model;

public class Patient {
    public int id;
    public String name;
    public int add_time;
    public String phone;

    public Patient(int id, String name, int time, String phone) {
        this.id = id;
        this.name = name;
        this.add_time = time;
        this.phone = phone;
    }
}
