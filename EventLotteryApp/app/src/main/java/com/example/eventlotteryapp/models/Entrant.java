package com.example.eventlotteryapp.models;

/**
 * Represents an entrant (participant) in an event lottery.
 * Stores basic information about a person who has joined or is interested
 * in joining an event's waiting list.
 * 
 * @author Droids Team
 */
public class Entrant {
    /** The unique identifier for the entrant (typically a user ID). */
    private String id;
    
    /** The entrant's full name. */
    private String name;
    
    /** The entrant's email address. */
    private String email;

    /**
     * Default constructor required for Firestore deserialization.
     */
    public Entrant() { }

    /**
     * Constructs a new Entrant with the specified information.
     * 
     * @param id the unique identifier for the entrant
     * @param name the entrant's full name
     * @param email the entrant's email address
     */
    public Entrant(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    /**
     * Gets the entrant's unique identifier.
     * 
     * @return the entrant ID
     */
    public String getId() { return id; }
    
    /**
     * Gets the entrant's name.
     * 
     * @return the entrant's name
     */
    public String getName() { return name; }
    
    /**
     * Gets the entrant's email address.
     * 
     * @return the entrant's email
     */
    public String getEmail() { return email; }

    /**
     * Sets the entrant's unique identifier.
     * 
     * @param id the entrant ID to set
     */
    public void setId(String id) { this.id = id; }
    
    /**
     * Sets the entrant's name.
     * 
     * @param name the entrant's name to set
     */
    public void setName(String name) { this.name = name; }
    
    /**
     * Sets the entrant's email address.
     * 
     * @param email the entrant's email to set
     */
    public void setEmail(String email) { this.email = email; }
}
