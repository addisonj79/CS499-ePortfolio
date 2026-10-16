package com.example.sensormanager;

import android.text.TextUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

// Pulled the name/date checks out into their own class instead of stuffing
// them into EventActivity - makes it easier to test and reuse.
public final class EventValidator {

    // has to match the format the db stores dates in
    public static final String DATE_PATTERN = "yyyy-MM-dd";

    public static final int MAX_NAME_LENGTH = 60;

    private EventValidator() {
    }

    // returns null if name is ok, otherwise an error message to show the user
    public static String validateName(String name) {
        if (TextUtils.isEmpty(name)) {
            return "Event name is required.";
        }
        if (name.length() > MAX_NAME_LENGTH) {
            return "Event name must be " + MAX_NAME_LENGTH + " characters or fewer.";
        }
        return null;
    }

    // same idea as validateName - null means valid
    public static String validateDate(String date) {
        if (TextUtils.isEmpty(date)) {
            return "Event date is required.";
        }

        SimpleDateFormat format = new SimpleDateFormat(DATE_PATTERN, Locale.US);
        format.setLenient(false); // reject things like 2024-13-40
        try {
            format.parse(date);
        } catch (ParseException e) {
            return "Enter the date as " + DATE_PATTERN + " (e.g. 2026-09-16).";
        }
        return null;
    }
}
