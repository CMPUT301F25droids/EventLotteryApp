package com.example.eventlotteryapp.EntrantView.placeholder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlaceholderContent {

    /**
     * A list of sample events.
     */
    public static final List<EventItem> ITEMS = new ArrayList<>();

    /**
     * A map of sample events by ID.
     */
    public static final Map<String, EventItem> ITEM_MAP = new HashMap<>();

    private static final int COUNT = 10; // how many sample events to create

    static {
        // Add some sample event items
        for (int i = 1; i <= COUNT; i++) {
            addItem(createEventItem(i));
        }
    }

    private static void addItem(EventItem item) {
        ITEMS.add(item);
        ITEM_MAP.put(item.id, item);
    }

    private static EventItem createEventItem(int position) {
        return new EventItem();
    }

    /**
     * A model class representing one event.
     */
    public static class EventItem {
        private String id;
        private String Name;
        private String Organizer;
        private double Cost;
        private String Image;

        public EventItem() {
            this.id = "";
            this.Name = "";
            this.Organizer = "";
            this.Cost = 0;
            this.Image = "";
        } // Firestore needs this empty constructor

        public String getName() {
            return Name;
        }

        public String getOrganizer() {
            return Organizer;
        }

        public double getCost() {
            return Cost;
        }

        public String getImage() {
            return Image;
        }
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

    }
}