package com.example.uos_lms.core.common;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Java-callable equivalent of DateKey.kt's top-level functions, for the Java screens. The
 * Kotlin file stays as-is (still used by not-yet-converted Kotlin screens). */
public final class DateKeyUtils {

    private static final String DATE_KEY_PATTERN = "yyyy-MM-dd";
    private static final String DISPLAY_PATTERN = "dd MMM yyyy";

    private DateKeyUtils() {
    }

    public static String todayDateKey() {
        return new SimpleDateFormat(DATE_KEY_PATTERN, Locale.US).format(new Date());
    }

    public static String dateKeyToDisplay(String dateKey) {
        try {
            Date parsed = new SimpleDateFormat(DATE_KEY_PATTERN, Locale.US).parse(dateKey);
            return parsed != null ? new SimpleDateFormat(DISPLAY_PATTERN, Locale.US).format(parsed) : dateKey;
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

    public static String millisToDateKey(long millis) {
        return new SimpleDateFormat(DATE_KEY_PATTERN, Locale.US).format(new Date(millis));
    }
}
