package fr.android.mhealthy.service;

import android.content.Context;
import android.content.Intent;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import fr.android.mhealthy.api.LoginResp;
import fr.android.mhealthy.model.Session;
import fr.android.mhealthy.model.SessionStore;
import fr.android.mhealthy.storage.SecretDAO;

public class SessionManager {
    private static final String SESSION_MANAGER_PATH = "sessions.json";
    private final Context ctx;
    private boolean is_logged;
    private SessionStore session;

    public SessionManager(Context _ctx) throws IOException {
        ctx       = _ctx;
        is_logged = false;
        try {
            session = this.read_session_file();
        } catch (FileNotFoundException e) {
            session = new SessionStore();
            this.write_session_file(session);
        }
        if (session.active_account_id != -1) {
            is_logged = true;
        }
    }

    public Session login(LoginResp acc, String token) throws Exception {
        int count = 0;
        for (Session s : session.sessions) {
            if (s.cin.equals(acc.cin)) {
                session.active_account_id = count;
                return s;
            }
            count += 1;
        }
        Session s = new Session();
        s.id = session.sessions.size();
        s.name = acc.name;
        s.cin = acc.cin;
        s.account_type = acc.account_type;
        session.sessions.add(s);
        session.active_account_id = s.id;
        this.write_session_file(session);
        // New account, we need to create db
        SecretDAO db = new SecretDAO(ctx, s);
        db.update_token(token);
        return s;
    }

    public Session get_logged_session() {
        if (is_logged) {
            return session.sessions.get(session.active_account_id);
        }
        return null;
    }

    void write_session_file(SessionStore session) throws IOException {
        Gson gson = new Gson();
        String s  = gson.toJson(session);

        FileOutputStream fos = ctx.openFileOutput(SESSION_MANAGER_PATH, Context.MODE_PRIVATE);
        fos.write(s.getBytes());
        fos.close();
    }

    SessionStore read_session_file() throws IOException {
        FileInputStream fis = ctx.openFileInput(SESSION_MANAGER_PATH);
        InputStreamReader isr = new InputStreamReader(fis);
        BufferedReader bufferedReader = new BufferedReader(isr);
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line);
        }

        Gson ser = new Gson();
        SessionStore session = ser.fromJson(stringBuilder.toString(), SessionStore.class);
        bufferedReader.close();
        return session;
    }
}
