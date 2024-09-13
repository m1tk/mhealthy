package fr.android.mhealthy.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

public class SSE {
    private final BufferedReader stream;

    public SSE(Call<ResponseBody> method) throws IOException {
        Response<ResponseBody> resp = method.execute();
        if (!resp.isSuccessful()) {
            throw new IOException("Wrong response returned");
        }

        InputStream input = resp.body().byteStream();
        stream = new BufferedReader(new InputStreamReader(input));
    }

    public SSEdata read_next() throws IOException {
        String event = null;
        String data  = null;
        while (true) {
            String line = stream.readLine();
            if (line.startsWith(":")) {
                // This is a heartbeat skip it
                stream.readLine();
                continue;
            }
            int pos = line.indexOf(':');
            if (pos == -1 ||
                (event == null && !line.substring(0, pos).equals("event")) ||
                (event != null && !line.substring(0, pos).equals("data")) ||
                line.length() < pos + 2) {
                throw new IOException("SSE invalid format");
            }
            if (event == null) {
                event = line.substring(pos + 1).trim();
            } else {
                data = line.substring(pos + 1).trim();
                stream.readLine();
                return new SSEdata(event, data);
            }
        }
    }

    public void close() throws IOException {
        stream.close();
    }
}
