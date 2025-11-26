package com.example.eventlotteryapp.EntrantView;

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

    public void setStatus(Status status) {
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }
}
