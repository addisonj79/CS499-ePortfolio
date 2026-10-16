package com.example.sensormanager;


public class EventItem {
    private int id;
    private String name;
    private String date;

    public EventItem(int id, String name, String date) {
        this.id = id;
        this.name = name;
        this.date = date;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDate() {
        return date;
    }
}
