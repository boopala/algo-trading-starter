package com.example.algotrading.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {

    private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    public static Date parseISOOffsetDate(String timestamp) {
        try {
            return formatter.parse(timestamp);
        } catch (ParseException e) {
            throw new RuntimeException("Failed to parse timestamp: " + timestamp, e);
        }
    }
}
