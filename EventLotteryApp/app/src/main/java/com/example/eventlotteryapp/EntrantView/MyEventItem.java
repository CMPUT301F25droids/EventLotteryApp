package com.example.eventlotteryapp.EntrantView;

import com.google.firebase.firestore.DocumentSnapshot;

public class MyEventItem extends EventItem{
    public enum Status {
        PENDING,
        SELECTED,
        NOT_SELECTED,
        UNKNOWN
    }

    private Status status;

    public MyEventItem() {
        super();
    }

    public static void fromDocument(DocumentSnapshot doc) {

    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }
}
