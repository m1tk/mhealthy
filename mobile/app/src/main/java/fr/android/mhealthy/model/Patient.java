package fr.android.mhealthy.model;

import java.io.Serializable;

public class Patient implements Serializable {
    public int id;
    public String name;
    public long add_time;
    public String phone;

    public Patient(int id, String name, long time, String phone) {
        this.id = id;
        this.name = name;
        this.add_time = time;
        this.phone = phone;
    }
}
