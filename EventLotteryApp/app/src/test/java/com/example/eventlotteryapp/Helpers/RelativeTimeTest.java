package com.example.eventlotteryapp.Helpers;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Unit tests for RelativeTime helper class.
 * Tests relative time calculation for different time periods.
 */
public class RelativeTimeTest {
    
    @Test
    public void testJustNow() {
        Date now = new Date();
        String relative = RelativeTime.getRelativeTime(now);
        assertEquals("Just now", relative);
    }

    @Test
    public void testMinutesAgo() {
        Date thirtySecondsAgo = new Date(System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(90));
        String relative = RelativeTime.getRelativeTime(thirtySecondsAgo);
        assertTrue(relative.contains("m ago"));
        assertTrue(relative.matches("\\d+m ago"));
    }

    @Test
    public void testHoursAgo() {
        Date twoHoursAgo = new Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(2));
        String relative = RelativeTime.getRelativeTime(twoHoursAgo);
        assertTrue(relative.contains("h ago"));
        assertTrue(relative.matches("\\d+h ago"));
    }

    @Test
    public void testDaysAgo() {
        Date threeDaysAgo = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3));
        String relative = RelativeTime.getRelativeTime(threeDaysAgo);
        assertTrue(relative.contains("d ago"));
        assertTrue(relative.matches("\\d+d ago"));
    }

    @Test
    public void testWeeksAgo() {
        Date twoWeeksAgo = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(14));
        String relative = RelativeTime.getRelativeTime(twoWeeksAgo);
        assertTrue(relative.contains("w ago"));
        assertTrue(relative.matches("\\d+w ago"));
    }

    @Test
    public void testMonthsAgo() {
        Date twoMonthsAgo = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(60));
        String relative = RelativeTime.getRelativeTime(twoMonthsAgo);
        assertTrue(relative.contains("mo ago"));
        assertTrue(relative.matches("\\d+mo ago"));
    }

    @Test
    public void testRelativeTimeOrder() {
        Date oneHourAgo = new Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1));
        Date twoHoursAgo = new Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(2));
        
        String relative1 = RelativeTime.getRelativeTime(oneHourAgo);
        String relative2 = RelativeTime.getRelativeTime(twoHoursAgo);
        
        // Both should be "h ago" format
        assertTrue(relative1.contains("h ago"));
        assertTrue(relative2.contains("h ago"));
        
        // Extract numbers and verify order
        int hours1 = Integer.parseInt(relative1.replace("h ago", "").trim());
        int hours2 = Integer.parseInt(relative2.replace("h ago", "").trim());
        assertTrue(hours1 <= hours2);
    }
}
