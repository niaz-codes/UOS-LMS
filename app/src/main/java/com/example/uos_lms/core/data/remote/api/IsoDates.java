package com.example.uos_lms.core.data.remote.api;

import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/** Parses the ISO-8601 timestamps Mongoose's `timestamps: true` produces
 * (e.g. "2026-08-12T14:50:36.955Z") into epoch millis for the app's Java-side domain models,
 * which predate this migration and store timestamps as {@code long}. */
public final class IsoDates {

    private IsoDates() {
    }

    public static long toMillis(@Nullable String iso) {
        if (iso == null) return 0L;
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            return format.parse(iso).getTime();
        } catch (Exception e) {
            return 0L;
        }
    }

    /** Inverse of toMillis() - needed wherever a timestamp goes into a query param rather than
     * a JSON number field (Mongoose casts numbers to Date automatically on write, but
     * `new Date(req.query.since)` on the read side needs a real ISO string). */
    public static String toIso(long millis) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new java.util.Date(millis));
    }
}
