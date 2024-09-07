package fr.android.mhealthy.service;

import java.io.IOException;

import fr.android.mhealthy.api.ApiService;
import fr.android.mhealthy.api.HttpClient;
import fr.android.mhealthy.api.SSE;
import fr.android.mhealthy.api.SSEdata;
import fr.android.mhealthy.model.Session;



public class CaregiverEvents {
    public CaregiverEvents(Session s) {
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
            } catch (IOException e) {
                break;
            }
        }
        try {
            sse.close();
        } catch (IOException e) {
            // Omit exception here
        }
    }
}
