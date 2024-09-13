package fr.android.mhealthy.api;

public class SSEdata {
    public String event;
    public String data;
    public SSEdata(String event, String data) {
        this.event = event;
        this.data  = data;
    }
}
