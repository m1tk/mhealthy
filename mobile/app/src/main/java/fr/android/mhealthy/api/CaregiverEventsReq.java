package fr.android.mhealthy.api;

public class CaregiverEventsReq {
    public int last_info;
    public int last_instruction;
    public int patient;

    public CaregiverEventsReq(int last_info, int last_instruction, int patient) {
        this.last_info = last_info;
        this.last_instruction = last_instruction;
        this.patient = patient;
    }
}
