package fr.android.mhealthy.model;

import java.io.Serializable;

public class Medicine implements Serializable {
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

    public static class EditMedicineNotification {
        public Integer patient;
        public String name;
        public String dose;
        public String time;
        public long updated_at;

        public EditMedicineNotification(Integer patient, String name, String dose, String time,
                                        long updated_at) {
            this.patient = patient;
            this.name    = name;
            this.dose    = dose;
            this.time    = time;
            this.updated_at = updated_at;
        }
    }

    public static class RemoveMedicineNotification {
        public Integer patient;
        public String name;

        public RemoveMedicineNotification(Integer patient, String name) {
            this.patient = patient;
            this.name    = name;
        }
    }
}
