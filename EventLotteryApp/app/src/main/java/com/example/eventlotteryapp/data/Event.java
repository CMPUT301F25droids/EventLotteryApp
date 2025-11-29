package com.example.eventlotteryapp.data;

import java.util.Date;

public class Event {

    private String title;
    private String description;
    private String location;
    private double price;
    private Date eventStartDate;
    private Date eventEndDate;
    private Date registrationOpenDate;
    private Date registrationCloseDate;
    private int maxParticipants;
    private String organizerId;

    public Event() { }

    public String getTitle() { return title != null ? title : ""; }
    public String getDescription() { return description != null ? description : ""; }
    public String getLocation() { return location != null ? location : ""; }
    public double getPrice() { return price; }
    public Date getEventStartDate() { return eventStartDate; }
    public Date getEventEndDate() { return eventEndDate; }
    public Date getRegistrationOpenDate() { return registrationOpenDate; }
    public Date getRegistrationCloseDate() { return registrationCloseDate; }
    public int getMaxParticipants() { return maxParticipants; }
    public String getOrganizerId() { return organizerId != null ? organizerId : ""; }
}
