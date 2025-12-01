package com.example.eventlotteryapp.Helpers;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for calculating and formatting relative time strings.
 * Converts Date objects to human-readable relative time descriptions
 * (e.g., "Just now", "5m ago", "2h ago", "3d ago", "2w ago", "3mo ago").
 * 
 * @author Droids Team
 */
public class RelativeTime {
    
    /**
     * Calculates and returns a human-readable relative time string for a given past date.
     * Returns different formats based on the time elapsed:
     * - Less than 60 seconds: "Just now"
     * - Less than 60 minutes: "Xm ago"
     * - Less than 24 hours: "Xh ago"
     * - Less than 7 days: "Xd ago"
     * - Less than 4 weeks: "Xw ago"
     * - Otherwise: "Xmo ago"
     * 
     * @param pastDate the date to calculate relative time from
     * @return formatted relative time string (e.g., "Just now", "5m ago", "2h ago", "3d ago")
     */
    public static String getRelativeTime(Date pastDate) {
        Date now = new Date();
        long diffInMillis = now.getTime() - pastDate.getTime();
        
        long seconds = TimeUnit.MILLISECONDS.toSeconds(diffInMillis);
        if (seconds < 60) {
            return "Just now";
        }
        
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis);
        if (minutes < 60) {
            return minutes + "m ago";
        }
        
        long hours = TimeUnit.MILLISECONDS.toHours(diffInMillis);
        if (hours < 24) {
            return hours + "h ago";
        }
        
        long days = TimeUnit.MILLISECONDS.toDays(diffInMillis);
        if (days < 7) {
            return days + "d ago";
        }
        
        long weeks = days / 7;
        if (weeks < 4) {
            return weeks + "w ago";
        }
        
        long months = days / 30;
        return months + "mo ago";
    }
}