package com.example.uos_lms.core.common;

import java.util.Locale;

/** Formats a minutes-since-midnight int (the app's plain-int time-of-day convention, matching
 * how AttendanceRecord.dateKey avoids java.time) as a 12-hour clock string. */
public final class TimeOfDayUtils {

    private TimeOfDayUtils() {
    }

    public static String format(int minutesSinceMidnight) {
        int hour24 = (minutesSinceMidnight / 60) % 24;
        int minute = minutesSinceMidnight % 60;
        int hour12 = hour24 % 12 == 0 ? 12 : hour24 % 12;
        String amPm = hour24 < 12 ? "AM" : "PM";
        return String.format(Locale.US, "%d:%02d %s", hour12, minute, amPm);
    }

    public static String formatRange(int startMinutes, int endMinutes) {
        return format(startMinutes) + " - " + format(endMinutes);
    }
}
