package com.example.uos_lms.core.domain.model;

import java.util.Arrays;
import java.util.List;

/**
 * HEC (Pakistan) style absolute grading scale - the only grading-scale
 * definition in the app. Bands are checked top-down against percentage, so
 * order matters (highest minPercent first).
 */
public final class GradeScale {

    private static final class Band {
        final double minPercent;
        final String letter;
        final double gpaPoint;

        Band(double minPercent, String letter, double gpaPoint) {
            this.minPercent = minPercent;
            this.letter = letter;
            this.gpaPoint = gpaPoint;
        }
    }

    private static final List<Band> BANDS = Arrays.asList(
            new Band(85.0, "A", 4.00),
            new Band(80.0, "A-", 3.66),
            new Band(75.0, "B+", 3.33),
            new Band(71.0, "B", 3.00),
            new Band(68.0, "B-", 2.66),
            new Band(64.0, "C+", 2.33),
            new Band(61.0, "C", 2.00),
            new Band(58.0, "C-", 1.66),
            new Band(54.0, "D+", 1.33),
            new Band(50.0, "D", 1.00),
            new Band(0.0, "F", 0.00)
    );

    private GradeScale() {
    }

    private static Band bandFor(double percentage) {
        for (Band band : BANDS) {
            if (percentage >= band.minPercent) return band;
        }
        return BANDS.get(BANDS.size() - 1);
    }

    public static String letterFor(double percentage) {
        return bandFor(percentage).letter;
    }

    public static double gpaPointFor(double percentage) {
        return bandFor(percentage).gpaPoint;
    }
}
