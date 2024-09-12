package fr.android.mhealthy.model;

public class Activity {
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
}
