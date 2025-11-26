package com.example.eventlotteryapp.Helpers;

import com.google.type.DateTime;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Locale;

public class DateTimeFormat {
    private static SimpleDateFormat getFormatter() {
        return new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.CANADA);
    }

    // Convert a Date object into timestamp string to be stored by firebase db
    public static String toFireBaseDate(Date ts) {
        SimpleDateFormat formatter = getFormatter();
        return formatter.format(ts);
    }

    // Convert timestamp string fetched from firebase db to Date
    public static Date toDate(String date) {
        SimpleDateFormat formatter = getFormatter();
        try {
            return formatter.parse(date);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

}
