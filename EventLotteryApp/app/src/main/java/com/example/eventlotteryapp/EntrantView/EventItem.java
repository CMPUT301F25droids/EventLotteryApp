package com.example.eventlotteryapp.EntrantView;

public class EventItem {
    private String id;
    private String Name;
    private String Organizer;
    private String Cost;
    private String Image;

    public EventItem() {
    } // Firestore needs this empty constructor

    public String getName() {
        return Name;
    }

    public String getOrganizer() {
        return Organizer;
    }

    public String getCost() {
        return Cost;
    }

    public String getImage() {
        return Image;
    }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

}
