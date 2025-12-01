package com.example.eventlotteryapp.Helpers;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Date;

/**
 * Unit tests for DateTimeFormat helper class.
 * Tests date formatting and parsing functionality.
 */
public class DateTimeFormatTest {
    
    @Test
    public void testToFireBaseDate() {
        Date testDate = new Date();
        String formatted = DateTimeFormat.toFireBaseDate(testDate);
        assertNotNull(formatted);
        assertFalse(formatted.isEmpty());
    }

    @Test
    public void testToFireBaseDateAndBack() {
        Date originalDate = new Date();
        String formatted = DateTimeFormat.toFireBaseDate(originalDate);
        Date parsedDate = DateTimeFormat.toDate(formatted);
        
        // Allow for small time difference due to formatting precision
        long diff = Math.abs(originalDate.getTime() - parsedDate.getTime());
        assertTrue("Dates should be within 1 second of each other", diff < 1000);
    }

    @Test
    public void testToDateValidFormat() {
        // Create a date and format it, then parse it back
        Date original = new Date();
        String formatted = DateTimeFormat.toFireBaseDate(original);
        Date parsed = DateTimeFormat.toDate(formatted);
        
        assertNotNull(parsed);
        // Should be able to parse what we formatted
        assertTrue(Math.abs(original.getTime() - parsed.getTime()) < 1000);
    }

    @Test(expected = RuntimeException.class)
    public void testToDateInvalidFormat() {
        // Should throw RuntimeException for invalid date format
        DateTimeFormat.toDate("invalid-date-format");
    }

    @Test
    public void testDateConsistency() {
        Date date1 = new Date();
        String formatted1 = DateTimeFormat.toFireBaseDate(date1);
        
        // Wait a bit to ensure different timestamps
        try {
            Thread.sleep(1100); // Wait more than 1 second to ensure different formatted strings
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        Date date2 = new Date();
        String formatted2 = DateTimeFormat.toFireBaseDate(date2);
        
        // Formatted strings should be different (with 1+ second difference)
        assertNotEquals("Formatted dates should be different when more than 1 second apart", formatted1, formatted2);
        
        // But when parsed back, dates should maintain order
        Date parsed1 = DateTimeFormat.toDate(formatted1);
        Date parsed2 = DateTimeFormat.toDate(formatted2);
        
        // Verify that parsed dates maintain chronological order
        assertTrue("Parsed date1 should be before or equal to parsed date2", 
                   parsed1.getTime() <= parsed2.getTime());
    }
}
