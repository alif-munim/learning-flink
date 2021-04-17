package com.flinklearn.realtime.project;

/****************************************************************************
 * Object template for browser events that will be converted from strings
 * and accessed via getter and setter methods
 ****************************************************************************/

public class BrowserEvent {

    int id;
    String user;
    String action;
    long timestamp;

    public BrowserEvent(String browserStr) {

        String[] attributes = browserStr
                .replace("\"", "")
                .split(",");

        this.id = Integer.valueOf(attributes[0]);
        this.user = attributes[1];
        this.action = attributes[2];
        this.timestamp = Long.valueOf(attributes[3]);

    }

    @Override
    public String toString() {
        return "BrowserEvent{" +
                "id='" + this.id + "\', " +
                "user='" + this.user + "\', " +
                "action='" + this.action + "\', " +
                "timestamp='" + this.timestamp +
                "}";
    }

    public String getUser() {
        return user;
    }

    public String getAction() {
        return action;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

}
