package com.example.eventlotteryapp.Admin;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for the AdminEvent model class.
 */
public class AdminEventTest {

    @Test
    public void testInitializationStoresValuesCorrectly() {
        AdminEvent event = new AdminEvent(
                "E123",
                "Sample Event",
                "Edmonton"
        );

        assertEquals("E123", event.getEventId());
        assertEquals("Sample Event", event.getTitle());
        assertEquals("Edmonton", event.getLocation());
    }

    @Test
    public void testAllowsNullValues() {
        AdminEvent event = new AdminEvent(
                null,
                null,
                null
        );

        assertNull(event.getEventId());
        assertNull(event.getTitle());
        assertNull(event.getLocation());
    }

    @Test
    public void testDifferentEventsNotEqual() {
        AdminEvent event1 = new AdminEvent("1", "Title A", "Location A");
        AdminEvent event2 = new AdminEvent("2", "Title B", "Location B");

        // They are different objects with different data.
        assertNotEquals(event1.getEventId(), event2.getEventId());
        assertNotEquals(event1.getTitle(), event2.getTitle());
        assertNotEquals(event1.getLocation(), event2.getLocation());
    }
}