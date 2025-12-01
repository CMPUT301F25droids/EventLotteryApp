package com.example.eventlotteryapp.Helpers;

import com.google.type.DateTime;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Locale;

/**
 * Utility class for converting between Date objects and Firebase-compatible string formats.
 * Provides methods to format dates for storage in Firestore and parse them back to Date objects.
 * Uses a consistent date format: "EEE MMM dd HH:mm:ss z yyyy" with Canadian locale.
 * 
 * @author Droids Team
 */
public class DateTimeFormat {
    /**
     * Gets a SimpleDateFormat formatter configured for Firebase date storage.
     * 
     * @return a SimpleDateFormat instance with format "EEE MMM dd HH:mm:ss z yyyy" and Canadian locale
     */
    private static SimpleDateFormat getFormatter() {
        return new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.CANADA);
    }

    /**
     * Converts a Date object to a Firebase-compatible timestamp string.
     * 
     * @param ts the Date object to convert
     * @return formatted date string in "EEE MMM dd HH:mm:ss z yyyy" format
     */
    public static String toFireBaseDate(Date ts) {
        SimpleDateFormat formatter = getFormatter();
        return formatter.format(ts);
    }

    /**
     * Converts a Firebase timestamp string back to a Date object.
     * 
     * @param date the date string from Firestore in "EEE MMM dd HH:mm:ss z yyyy" format
     * @return the parsed Date object
     * @throws RuntimeException if the date string cannot be parsed
     */
    public static Date toDate(String date) {
        SimpleDateFormat formatter = getFormatter();
        try {
            return formatter.parse(date);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

}
