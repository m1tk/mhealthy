package fr.android.mhealthy.service;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.HashMap;

import fr.android.mhealthy.api.ApiService;
import fr.android.mhealthy.api.CaregiverEventsReq;
import fr.android.mhealthy.api.HttpClient;
import fr.android.mhealthy.api.PatientEventReq;
import fr.android.mhealthy.api.SSE;
import fr.android.mhealthy.api.SSEdata;
import fr.android.mhealthy.model.Instruction;
import fr.android.mhealthy.model.Session;
import fr.android.mhealthy.storage.CaregiverDAO;
import fr.android.mhealthy.storage.LastIdDAO;

public class CaregiverEvents {
    private final Gson p;
    private HashMap<Integer, Thread> tasks;

    public CaregiverEvents(Context ctx, Session s) {
        p = new Gson();
        tasks = new HashMap<>();
        while (true) {
            assigned_listener(ctx, s);
            try {
                Thread.sleep(5000);
            } catch (Exception e) {}
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
                    if (tasks.containsKey(patient)) {
                        continue;
                    }
                    Thread t = new Thread(() -> {
                        instruction_event_handler(ctx, s, patient);
                    });
                    tasks.put(patient, t);
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

    private void instruction_event_handler(Context ctx, Session s, int patient) {
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

        while (true) {
            try {
                SSEdata data = sse.read_next();
                handle_instruction_data(cd, data, patient, last);
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

    void handle_instruction_data(CaregiverDAO cd, SSEdata data, int patient, LastId last) {
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
            return;
        }

        if (ins.type == Instruction.InstructionType.AddPatient) {
            Instruction.AddPatient inst = (Instruction.AddPatient) ins.instruction;
            cd.new_patient(inst, patient, ins.id);
        } else if (ins.type == Instruction.InstructionType.AddMedicine ||
                ins.type == Instruction.InstructionType.EditMedicine ||
                ins.type == Instruction.InstructionType.RemoveMedicine) {
            cd.medicine_operation(ins, ons.toString(), patient);
        }
        last.last_instruction = ins.id;
    }
}
