package com.example.eventlotteryapp.data;

import java.util.Date;

/**
 * Represents an event in the Event Lottery application.
 * Contains all information about an event including title, description, location,
 * pricing, dates, participant limits, and organizer information.
 * This class is used for both creating new events and retrieving event data from Firestore.
 * 
 * @author Droids Team
 */
public class Event {

    /** The title or name of the event. */
    private String title;
    
    /** A detailed description of the event. */
    private String description;
    
    /** The physical location where the event will take place. */
    private String location;
    
    /** The price to attend the event. */
    private double price;
    
    /** The date and time when the event starts. */
    private Date eventStartDate;
    
    /** The date and time when the event ends. */
    private Date eventEndDate;
    
    /** The date and time when registration opens for the event. */
    private Date registrationOpenDate;
    
    /** The date and time when registration closes for the event. */
    private Date registrationCloseDate;
    
    /** The maximum number of participants allowed for the event. */
    private int maxParticipants;
    
    /** The unique identifier of the organizer who created this event. */
    private String organizerId;

    /** The event poster image stored as a base64-encoded string (Firestore field). */
    private String Image;

    /**
     * Default constructor required for Firestore deserialization.
     */
    public Event() { }

    /**
     * Gets the event title.
     * 
     * @return the event title
     */
    public String getTitle() { return title; }
    
    /**
     * Gets the event description.
     * 
     * @return the event description
     */
    public String getDescription() { return description; }
    
    /**
     * Gets the event location.
     * 
     * @return the event location
     */
    public String getLocation() { return location; }
    
    /**
     * Gets the event price.
     * 
     * @return the event price as a double value
     */
    public double getPrice() { return price; }
    
    /**
     * Gets the event start date and time.
     * 
     * @return the event start date, or null if not set
     */
    public Date getEventStartDate() { return eventStartDate; }
    
    /**
     * Gets the event end date and time.
     * 
     * @return the event end date, or null if not set
     */
    public Date getEventEndDate() { return eventEndDate; }
    
    /**
     * Gets the registration open date and time.
     * 
     * @return the registration open date, or null if not set
     */
    public Date getRegistrationOpenDate() { return registrationOpenDate; }
    
    /**
     * Gets the registration close date and time.
     * 
     * @return the registration close date, or null if not set
     */
    public Date getRegistrationCloseDate() { return registrationCloseDate; }
    
    /**
     * Gets the maximum number of participants allowed.
     * 
     * @return the maximum number of participants
     */
    public int getMaxParticipants() { return maxParticipants; }
    
    /**
     * Gets the organizer's unique identifier.
     * 
     * @return the organizer ID
     */
    public String getOrganizerId() { return organizerId; }

    /**
     * Gets the event poster image as a base64-encoded string.
     * 
     * @return the base64-encoded image string, or null if no image is set
     */
    public String getImage() {
        return Image;
    }

    /**
     * Sets the event poster image as a base64-encoded string.
     * 
     * @param image the base64-encoded image string to set
     */
    public void setImage(String image) {
        this.Image = image;
    }
}
