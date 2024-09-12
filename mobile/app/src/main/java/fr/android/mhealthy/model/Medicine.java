package fr.android.mhealthy.model;

public class Medicine {
    public String name;
    public String dose;
    public String time;
    public long created_at;
    public long updated_at;
    public boolean active;

    public static class AddMedicineNotification {
        public Integer patient;
        public Medicine med;

        public AddMedicineNotification(Integer patient, Medicine med) {
            this.patient = patient;
            this.med     = med;
        }
    }
}
