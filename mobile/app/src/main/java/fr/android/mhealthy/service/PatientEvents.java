package fr.android.mhealthy.service;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import fr.android.mhealthy.api.ApiService;
import fr.android.mhealthy.api.HttpClient;
import fr.android.mhealthy.api.PatientEventReq;
import fr.android.mhealthy.api.SSE;
import fr.android.mhealthy.api.SSEdata;
import fr.android.mhealthy.model.Instruction;
import fr.android.mhealthy.model.Session;
import fr.android.mhealthy.storage.LastIdDAO;
import fr.android.mhealthy.storage.PatientDAO;


public class PatientEvents {
    private final Gson p;
    private int last;

    public PatientEvents(Context ctx, Session s) {
        {
            LastIdDAO last_db = new LastIdDAO(ctx, s);
            last = last_db.patient_last_id();
            p = new Gson();
        }
        PatientDAO pd = new PatientDAO(ctx, s);

        while (true) {
            event_handler(pd);
            try {
                Thread.sleep(5000);
            } catch (Exception e) {}
        }
    }

    private void event_handler(PatientDAO pd) {
        ApiService client = HttpClient.getClient();
        SSE sse;
        try {
            sse = new SSE(client.patient_events(new PatientEventReq(last)));
        } catch (IOException e) {
            return;
        }

        while (true) {
            try {
                SSEdata data = sse.read_next();
                handle_data(pd, data);
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

    void handle_data(PatientDAO pd, SSEdata data) {
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

        if (ins.type == Instruction.InstructionType.AddCaregiver) {
            Instruction.AddCaregiver inst = (Instruction.AddCaregiver) ins.instruction;
            pd.new_caregiver(inst, ins.caregiver, ins.id);
        } else if (ins.type == Instruction.InstructionType.AddMedicine ||
                   ins.type == Instruction.InstructionType.EditMedicine ||
                   ins.type == Instruction.InstructionType.RemoveMedicine) {
            pd.medicine_operation(ins, ons.toString());
        }
        last = ins.id;
    }
}
