package fr.android.mhealthy.service;

import android.content.Context;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import fr.android.mhealthy.api.ApiService;
import fr.android.mhealthy.api.HttpClient;
import fr.android.mhealthy.api.InstructionReq;
import fr.android.mhealthy.api.PatientInfoReq;
import fr.android.mhealthy.model.PendingTransaction;
import fr.android.mhealthy.model.Session;
import fr.android.mhealthy.storage.PendingTransactionDAO;
import okhttp3.ResponseBody;
import retrofit2.Response;

public class TransactionHandler {
    public static AtomicLong update_id = new AtomicLong(0);
    Gson p;
    private long last_id;
    public TransactionHandler(Context ctx, Session s) {
        EventHandlerBackground.tasks.put(Thread.currentThread(), Optional.empty());

        p = new Gson();
        last_id = 0;
        PendingTransactionDAO db = new PendingTransactionDAO(ctx, s);
        ApiService client        = HttpClient.getClient();

        transmit(db, client);

        while (true) {
            if (update_id.get() > last_id) {
                transmit(db, client);
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private void transmit(PendingTransactionDAO db, ApiService client) {
        PendingTransaction pending;
        while ((pending = db.get()) != null) {
            Response<ResponseBody> resp;
            try {
                switch (pending.type) {
                    case "instruction":
                        InstructionReq ins = p.fromJson(pending.data, InstructionReq.class);
                        resp = client.instruction(ins).execute();
                        break;
                    case "patient_info":
                        PatientInfoReq inf = p.fromJson(pending.data, PatientInfoReq.class);
                        resp = client.patient_info(inf).execute();
                        break;
                    default:
                        // Should not happen
                        return;
                }
            } catch (IOException e) {
                return;
            }
            if (!resp.isSuccessful()) {
                return;
            }
            db.remove(pending.id);
            last_id = pending.id;
        }
    }
}
