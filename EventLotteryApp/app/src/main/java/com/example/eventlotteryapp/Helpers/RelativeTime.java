package com.example.eventlotteryapp.Helpers;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class RelativeTime {
    
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