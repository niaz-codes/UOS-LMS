package com.example.uos_lms.core.common;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class DateUtils {

    private static final String DATE_KEY_PATTERN = "yyyy-MM-dd";
    private static final String DISPLAY_PATTERN = "dd MMM yyyy";

    private DateUtils() {
    }

    /** Today's date as a plain "yyyy-MM-dd" key — the unit attendance sessions are keyed by. */
    public static String todayDateKey() {
        return new SimpleDateFormat(DATE_KEY_PATTERN, Locale.US).format(new Date());
    }

    public static String dateKeyToDisplay(String dateKey) {
        try {
            Date parsed = new SimpleDateFormat(DATE_KEY_PATTERN, Locale.US).parse(dateKey);
            if (parsed != null) {
                return new SimpleDateFormat(DISPLAY_PATTERN, Locale.US).format(parsed);
            }
            return dateKey;
        } catch (ParseException e) {
            return dateKey;
        }
    }

    public static long dateKeyToMillis(String dateKey) {
        try {
            Date parsed = new SimpleDateFormat(DATE_KEY_PATTERN, Locale.US).parse(dateKey);
            return parsed != null ? parsed.getTime() : System.currentTimeMillis();
        } catch (ParseException e) {
            return System.currentTimeMillis();
        }
    }

    public static String millisToDisplay(long millis) {
        return new SimpleDateFormat(DISPLAY_PATTERN, Locale.US).format(new Date(millis));
    }
}
