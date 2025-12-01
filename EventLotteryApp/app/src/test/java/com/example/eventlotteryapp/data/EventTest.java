package com.example.eventlotteryapp.data;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Date;

/**
 * Unit tests for the Event class.
 * Tests all getters, setters, and default constructor.
 * Related to user stories: US 02.01.01, US 02.01.04
 */
public class EventTest {
    private Event event;
    private Date testDate;
    private Date futureDate;

    @Before
    public void setUp() {
        event = new Event();
        testDate = new Date();
        futureDate = new Date(testDate.getTime() + 86400000); // 1 day later
    }

    @Test
    public void testDefaultConstructor() {
        Event defaultEvent = new Event();
        assertNotNull(defaultEvent);
        assertNull(defaultEvent.getTitle());
        assertNull(defaultEvent.getDescription());
        assertNull(defaultEvent.getLocation());
        assertEquals(0.0, event.getPrice(), 0.001);
        assertEquals(0, event.getMaxParticipants());
    }

    @Test
    public void testTitleGetter() {
        // Event is populated from Firestore, so we test getters work
        // Note: Setters are not available as Event is designed for Firestore deserialization
        assertNull(event.getTitle());
    }

    @Test
    public void testDescriptionGetter() {
        assertNull(event.getDescription());
    }

    @Test
    public void testLocationGetter() {
        assertNull(event.getLocation());
    }

    @Test
    public void testPriceGetter() {
        assertEquals(0.0, event.getPrice(), 0.001);
    }

    @Test
    public void testEventStartDateGetter() {
        assertNull(event.getEventStartDate());
    }

    @Test
    public void testEventEndDateGetter() {
        assertNull(event.getEventEndDate());
    }

    @Test
    public void testRegistrationOpenDateGetter() {
        assertNull(event.getRegistrationOpenDate());
    }

    @Test
    public void testRegistrationCloseDateGetter() {
        assertNull(event.getRegistrationCloseDate());
    }

    @Test
    public void testMaxParticipantsGetter() {
        assertEquals(0, event.getMaxParticipants());
    }

    @Test
    public void testOrganizerIdGetter() {
        assertNull(event.getOrganizerId());
    }

    @Test
    public void testCancelledGetter() {
        assertFalse(event.isCancelled()); // Default should be false
    }

    @Test
    public void testImageGetterAndSetter() {
        String imageBase64 = "base64encodedstring";
        event.setImage(imageBase64);
        assertEquals(imageBase64, event.getImage());
        
        event.setImage(null);
        assertNull(event.getImage());
    }

    @Test
    public void testEventGettersWithDefaultValues() {
        // Test that all getters work with default values
        assertNull(event.getTitle());
        assertNull(event.getDescription());
        assertNull(event.getLocation());
        assertEquals(0.0, event.getPrice(), 0.001);
        assertNull(event.getEventStartDate());
        assertNull(event.getEventEndDate());
        assertNull(event.getRegistrationOpenDate());
        assertNull(event.getRegistrationCloseDate());
        assertEquals(0, event.getMaxParticipants());
        assertNull(event.getOrganizerId());
        assertFalse(event.isCancelled());
        assertNull(event.getImage());
    }
}
