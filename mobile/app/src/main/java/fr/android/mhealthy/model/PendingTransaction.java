package fr.android.mhealthy.model;

public class PendingTransaction {
    public long id;
    public String type;
    public String data;
    public PendingTransaction(long id, String type, String data) {
        this.id = id;
        this.type = type;
        this.data = data;
    }
}