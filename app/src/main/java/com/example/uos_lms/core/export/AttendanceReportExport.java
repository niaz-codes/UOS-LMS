package com.example.uos_lms.core.export;

import com.example.uos_lms.core.domain.model.AttendanceRecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Builds the header/row shape shared by every attendance report screen's export (Teacher/HOD/
 * Admin), from the same List&lt;AttendanceRecord&gt; each report ViewModel already exposes via
 * getRecords() - used for both the real CSV file and the PDF export. */
public final class AttendanceReportExport {

    private AttendanceReportExport() {
    }

    public static List<String> headers() {
        return Arrays.asList("Student Name", "Subject ID", "Date", "Status");
    }

    public static List<List<String>> rows(List<AttendanceRecord> records) {
        List<List<String>> rows = new ArrayList<>();
        for (AttendanceRecord record : records) {
            rows.add(Arrays.asList(record.getStudentName(), record.getSubjectId(), record.getDateKey(), record.getStatus().name()));
        }
        return rows;
    }

    public static String csv(List<AttendanceRecord> records) {
        StringBuilder sb = new StringBuilder(String.join(",", headers()));
        for (List<String> row : rows(records)) {
            sb.append('\n').append(String.join(",", row));
        }
        return sb.toString();
    }
}
