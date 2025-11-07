package com.example.eventlotteryapp.models;
import java.io.Serializable;

/**
 * Represents a user in Event Lottery app.
 * Stores profile information like UID, name, email, and phone number.
 * Used by both entrants and organizers.
 */
public class User implements Serializable {
    private String uid;
    private String name;
    private String email;
    private String phone;

    public User() {}

    public User(String uid, String name, String email, String phone) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
