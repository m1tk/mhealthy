package fr.android.mhealthy.model;

import java.util.Vector;

public class SessionStore {
    public int active_account_id;
    public Vector<Session> sessions;

    public SessionStore() {
        sessions          = new Vector<>();
        active_account_id = -1;
    }
}