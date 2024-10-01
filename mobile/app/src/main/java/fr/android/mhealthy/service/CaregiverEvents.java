package fr.android.mhealthy.service;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;

import fr.android.mhealthy.R;
import fr.android.mhealthy.api.ApiService;
import fr.android.mhealthy.api.CaregiverEventsReq;
import fr.android.mhealthy.api.HttpClient;
import fr.android.mhealthy.api.SSE;
import fr.android.mhealthy.api.SSEdata;
import fr.android.mhealthy.model.Instruction;
import fr.android.mhealthy.model.Patient;
import fr.android.mhealthy.model.PatientInfo;
import fr.android.mhealthy.model.Session;
import fr.android.mhealthy.storage.CaregiverDAO;
import fr.android.mhealthy.storage.LastIdDAO;
import fr.android.mhealthy.ui.CaregiverMainActivity;

public class CaregiverEvents {
    private final Gson p;
    private HashSet<Integer> tasks;

    public CaregiverEvents(Context ctx, Session s) {
        p = new Gson();
        tasks = new HashSet<>();
        EventHandlerBackground.tasks.put(Thread.currentThread(), Optional.empty());
        while (true) {
            assigned_listener(ctx, s);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private void assigned_listener(Context ctx, Session s) {
        ApiService client = HttpClient.getClient();
        SSE sse;
        try {
            sse = new SSE(client.assigned_events());
        } catch (IOException e) {
            return;
        }
        EventHandlerBackground.tasks.put(Thread.currentThread(), Optional.of(sse));
        while (true) {
            try {
                SSEdata data = sse.read_next();
                if (data.event.equals("assigned")) {
                    int patient;
                    try {
                        patient = Integer.parseInt(data.data);
                    } catch (Exception e) {
                        continue;
                    }
                    if (tasks.contains(patient)) {
                        continue;
                    }
                    Thread t = new Thread(() -> {
                        event_handler(ctx, s, patient);
                        tasks.remove(patient);
                    });
                    tasks.add(patient);
                    t.start();
                }
            } catch (Exception e) {
                break;
            }
        }
        try {
            sse.close();
        } catch (IOException e) {
            // Omit exception here
        }
    }

    static class LastId {
        int last_instruction;
        int last_patient_info;
        LastId() {
            last_instruction = 0;
            last_patient_info = 0;
        }
    }

    private void event_handler(Context ctx, Session s, int patient) {
        EventHandlerBackground.tasks.put(Thread.currentThread(), Optional.empty());
        LastId last = new LastId();
        {
            LastIdDAO ld = new LastIdDAO(ctx, s);
            last.last_instruction = ld.caregiver_instruction_last_id(patient);
            last.last_patient_info = ld.caregiver_patient_info_last_id(patient);
        }
        CaregiverDAO cd = new CaregiverDAO(ctx, s);

        ApiService client = HttpClient.getClient();
        SSE sse;
        try {
            sse = new SSE(client.caregiver_events(
                    new CaregiverEventsReq(last.last_patient_info, last.last_instruction, patient)
            ));
        } catch (IOException e) {
            return;
        }
        EventHandlerBackground.tasks.put(Thread.currentThread(), Optional.of(sse));

        while (true) {
            try {
                SSEdata data = sse.read_next();
                if (data.event.equals("patient_info")) {
                    handle_patient_info_data(cd, data, patient, last);
                } else {
                    if (handle_instruction_data(cd, data, patient, last)) {
                        break;
                    }
                }
            } catch (Exception e) {
                break;
            }
        }
        try {
            sse.close();
        } catch (IOException e) {
            // Omit exception here
        }
        EventHandlerBackground.tasks.remove(Thread.currentThread());
    }

    void handle_patient_info_data(CaregiverDAO cd, SSEdata data, int patient, LastId last) {
        PatientInfo info;
        JsonObject ons;
        String name;
        Log.d("PatientInfo", data.data);
        try {
            JsonObject obj = JsonParser.parseString(data.data).getAsJsonObject();
            int id = obj.get("id").getAsInt();
            ons = obj.get("info").getAsJsonObject();
            name = ons.get("name").getAsString();
            info = new PatientInfo(p, ons, id);
        } catch (Exception e) {
            // If it is unreadable, sadly we just skip for now
            return;
        }

        cd.add_patient_info(info, info.to_store_json_format(p).toString(), name, info.get_time(), patient);
        last.last_patient_info = info.id;
    }

    boolean handle_instruction_data(CaregiverDAO cd, SSEdata data, int patient, LastId last) {
        Instruction ins;
        JsonObject ons;
        Log.d("Instruction", data.data);
        try {
            JsonObject obj = JsonParser.parseString(data.data).getAsJsonObject();
            int caregiver = obj.get("caregiver").getAsInt();
            int id = obj.get("id").getAsInt();
            ons = obj.get("instruction").getAsJsonObject();
            ins = new Instruction(p, ons, caregiver, id);
        } catch (Exception e) {
            // If it is unreadable, sadly we just skip for now
            return false;
        }

        if (ins.type == Instruction.InstructionType.AddPatient) {
            Instruction.AddPatient inst = (Instruction.AddPatient) ins.instruction;
            cd.new_patient(inst, ons.toString(), patient, ins.id);
            EventBus.getDefault().post(new Patient(
                    inst.new_patient.id,
                    inst.new_patient.name,
                    inst.time,
                    inst.new_patient.phone,
                    true
            ));
            EventBus.getDefault().post(new EventHandlerBackground.NewNotificationTask(
                    R.string.new_patient_title,
                    R.string.new_patient,
                    inst.new_patient.name
            ));
        } else if (ins.type == Instruction.InstructionType.AddMedicine ||
                ins.type == Instruction.InstructionType.EditMedicine ||
                ins.type == Instruction.InstructionType.RemoveMedicine ||
                ins.type == Instruction.InstructionType.AddActivity ||
                ins.type == Instruction.InstructionType.EditActivity ||
                ins.type == Instruction.InstructionType.RemoveActivity) {
            cd.instruction_operation(ins, ons.toString(), null, patient, false);
        } else if (ins.type == Instruction.InstructionType.UnassignCaregiver) {
            // We ignore all notices about other caregivers as it is not supported
            Instruction.UnassignCaregiver inst = (Instruction.UnassignCaregiver) ins.instruction;
            inst.patient = patient;
            ons.addProperty("patient", patient);
            if (inst.id == null) {
                cd.unassign_patient(inst, ons.toString(), patient, ins.id);
                if (inst.stop != null && inst.stop) {
                    EventBus.getDefault().post(new CaregiverMainActivity.UnassignPatient(patient));
                    return true;
                }
            }
        }
        last.last_instruction = ins.id;
        return false;
    }
}
