package fr.android.mhealthy.service;

import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import fr.android.mhealthy.api.ApiService;
import fr.android.mhealthy.api.HttpClient;
import fr.android.mhealthy.model.Session;
import okhttp3.ResponseBody;
import retrofit2.Response;


public class CaregiverEvents {
    public CaregiverEvents(Session s) {
        ApiService client = HttpClient.getClient();

        Response<ResponseBody> resp;
        try {
            resp = client.assigned_events().execute();
            Log.d("fuuuuuu", resp.toString());
        } catch (IOException e) {
            Log.d("fuuuuuu", e.toString());
            return;
        }
        if (!resp.isSuccessful()) {
            return;
        }

        InputStream input = resp.body().byteStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(input));
        try {
            while (true) {
                String line = br.readLine();
                Log.d("fuuuuuu", line);
            }
        } catch (IOException e) {
            Log.d("fuuuuuu", e.toString());
        } finally {
            try {
                br.close();
            } catch (IOException e) {}
        }
    }
}
