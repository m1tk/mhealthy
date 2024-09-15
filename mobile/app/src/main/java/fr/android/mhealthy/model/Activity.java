package fr.android.mhealthy.model;

import android.view.LayoutInflater;
import android.view.View;

import java.io.Serializable;

import fr.android.mhealthy.R;

public class Activity implements Serializable {
    public String name;
    public String goal;
    public String time;
    public long created_at;
    public long updated_at;
    public boolean active;

    public static class AddActivityNotification {
        public Integer patient;
        public Activity act;

        public AddActivityNotification(Integer patient, Activity act) {
            this.patient = patient;
            this.act     = act;
        }
    }

    public static class EditActivityNotification {
        public Integer patient;
        public String name;
        public String goal;
        public String time;
        public long updated_at;



        public EditActivityNotification(Integer patient, String name, String goal, String time,
                                        long updated_at) {
            this.patient = patient;
            this.name    = name;
            this.goal    = goal;
            this.time    = time;
            this.updated_at = updated_at;
        }
    }

    public static class RemoveActivityNotification {
        public Integer patient;
        public String name;

        public RemoveActivityNotification(Integer patient, String name) {
            this.patient = patient;
            this.name    = name;
        }
    }
}
