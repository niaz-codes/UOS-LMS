package com.example.uos_lms.feature.teacher.presentation.examresult.workspace;

import com.example.uos_lms.core.domain.model.DepartmentOptions;
import com.example.uos_lms.core.domain.model.Subject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import lombok.Builder;
import lombok.Getter;

/** UI state for the dedicated Exam Result screen. Two top-level views share this one state:
 * the "My Assigned Courses" grid ({@link #showCourseGrid}) and, once a course is opened, the
 * marks-entry sheet for that course (department/session/semester/subject/exam-type selections
 * are still tracked so the existing roster-loading plumbing keeps working, but the cascade tabs
 * themselves are no longer part of the visible flow - see TeacherExamResultWorkspaceFragment). */
@Getter
@Builder(toBuilder = true)
public class TeacherExamResultWorkspaceUiState {

    public static final String EXAM_TYPE_CURRENT = "current";
    public static final String EXAM_TYPE_REPEAT = "repeat";
    public static final String EXAM_TYPE_PREVIOUS = "previous";

    public enum StudentFilter { ALL, ENTERED, PENDING, PASSED, FAILED }

    public enum StudentSort { ROLL, NAME, MARKS, GRADE, GPA }

    @Builder.Default
    private final boolean loadingOptions = true;
    private final List<DepartmentOptions> departmentOptions;
    private final String optionsError;

    // ---- "My Assigned Courses" overview ----
    @Builder.Default
    private final boolean showCourseGrid = true;
    @Builder.Default
    private final boolean loadingCourses = false;
    private final String coursesError;
    @Builder.Default
    private final List<TeacherCourseSummary> courses = Collections.emptyList();
    /** The course the teacher opened from the grid - drives the detail-view breadcrumb/header. */
    private final TeacherCourseSummary activeCourse;

    private final String selectedDepartmentId;
    private final String selectedSessionId;
    private final String selectedSemesterId;
    private final String selectedSubjectId;
    @Builder.Default
    private final String selectedExamType = EXAM_TYPE_CURRENT;

    @Builder.Default
    private final boolean loadingSubjects = false;
    @Builder.Default
    private final boolean loadingRoster = false;
    @Builder.Default
    private final boolean saving = false;
    private final String sectionError;
    private final String actionMessage;

    @Builder.Default
    private final List<Subject> subjects = Collections.emptyList();
    @Builder.Default
    private final List<ResultEntryRow> rows = Collections.emptyList();
    @Builder.Default
    private final String totalMarksInput = "100";

    // ---- Roster search / filter / sort ----
    @Builder.Default
    private final String studentSearchQuery = "";
    @Builder.Default
    private final StudentFilter studentFilter = StudentFilter.ALL;
    @Builder.Default
    private final StudentSort studentSort = StudentSort.ROLL;

    public DepartmentOptions getSelectedDepartment() {
        return selectedDepartmentId == null ? null : DepartmentOptions.findById(departmentOptions, selectedDepartmentId);
    }

    public List<String> getDepartmentIds() {
        return idsOfDepartments(departmentOptions);
    }

    public List<String> getSessionIds() {
        DepartmentOptions dept = getSelectedDepartment();
        return dept == null ? Collections.emptyList() : dept.getSessionIds();
    }

    public List<String> getSemesterIds() {
        DepartmentOptions dept = getSelectedDepartment();
        return dept == null ? Collections.emptyList() : dept.getSemesterIds();
    }

    public List<String> getSubjectIds() {
        List<String> ids = new ArrayList<>();
        for (Subject subject : subjects) {
            ids.add(subject.getId());
        }
        return ids;
    }

    public int getTotalMarksValue() {
        Integer total = ResultEntryRow.parseIntOrNull(totalMarksInput);
        return total != null && total > 0 ? total : 100;
    }

    public boolean canSubmit() {
        if (rows.isEmpty()) return false;
        if (!EXAM_TYPE_REPEAT.equals(selectedExamType)) {
            Integer total = ResultEntryRow.parseIntOrNull(totalMarksInput);
            if (total == null || total <= 0) return false;
        }
        int total = getTotalMarksValue();
        for (ResultEntryRow row : rows) {
            if (!row.hasValidMarks(selectedExamType, total)) return false;
        }
        return true;
    }

    public boolean isTotalMarksLocked() {
        if (rows.isEmpty()) return false;
        for (ResultEntryRow row : rows) {
            if (!row.isEditable(selectedExamType)) return true;
        }
        return false;
    }

    /** Applies the search query and filter, then sorts - all client-side over the already-
     * loaded roster (rows), so switching search/filter/sort never re-hits the network. */
    public List<ResultEntryRow> getVisibleRows() {
        int total = getTotalMarksValue();
        String query = studentSearchQuery == null ? "" : studentSearchQuery.trim().toLowerCase(Locale.getDefault());
        List<ResultEntryRow> visible = new ArrayList<>();
        for (ResultEntryRow row : rows) {
            if (!matchesFilter(row, total)) continue;
            if (!query.isEmpty() && !matchesQuery(row, query)) continue;
            visible.add(row);
        }
        visible.sort((a, b) -> compareForSort(a, b, total));
        return visible;
    }

    private boolean matchesFilter(ResultEntryRow row, int total) {
        boolean hasValid = row.hasValidMarks(selectedExamType, total);
        switch (studentFilter) {
            case ENTERED:
                return hasValid;
            case PENDING:
                return !hasValid;
            case PASSED:
                return hasValid && !"F".equals(row.displayGrade(selectedExamType, total));
            case FAILED:
                return hasValid && "F".equals(row.displayGrade(selectedExamType, total));
            case ALL:
            default:
                return true;
        }
    }

    private boolean matchesQuery(ResultEntryRow row, String query) {
        String name = row.getSource().getStudent().getFullName();
        String roll = row.getSource().getStudent().getRollNumber();
        return (name != null && name.toLowerCase(Locale.getDefault()).contains(query))
                || (roll != null && roll.toLowerCase(Locale.getDefault()).contains(query));
    }

    private int compareForSort(ResultEntryRow a, ResultEntryRow b, int total) {
        switch (studentSort) {
            case NAME:
                return a.getSource().getStudent().getFullName().compareToIgnoreCase(b.getSource().getStudent().getFullName());
            case MARKS: {
                Integer ma = ResultEntryRow.parseIntOrNull(a.getMarksInput());
                Integer mb = ResultEntryRow.parseIntOrNull(b.getMarksInput());
                return Integer.compare(mb != null ? mb : -1, ma != null ? ma : -1);
            }
            case GRADE:
                return a.displayGrade(selectedExamType, total).compareTo(b.displayGrade(selectedExamType, total));
            case GPA:
                return Double.compare(b.displayGpa(selectedExamType, total), a.displayGpa(selectedExamType, total));
            case ROLL:
            default:
                int cmp = compareRoll(a.getSource().getStudent().getRollNumber(), b.getSource().getStudent().getRollNumber());
                if (cmp == 0) {
                    cmp = a.getSource().getStudent().getFullName()
                            .compareToIgnoreCase(b.getSource().getStudent().getFullName());
                }
                return cmp;
        }
    }

    public boolean isSearchOrFilterActive() {
        return (studentSearchQuery != null && !studentSearchQuery.trim().isEmpty()) || studentFilter != StudentFilter.ALL;
    }

    // ---- Course performance analytics (computed from the full, unfiltered roster) ----

    public boolean hasAnalytics() {
        return getEnteredCount() > 0;
    }

    public double getClassAverage() {
        int total = getTotalMarksValue();
        double sum = 0;
        int count = 0;
        for (ResultEntryRow row : rows) {
            if (row.hasValidMarks(selectedExamType, total)) {
                sum += row.displayPercentage(selectedExamType, total);
                count++;
            }
        }
        return count == 0 ? 0.0 : sum / count;
    }

    public double getHighestPercentage() {
        int total = getTotalMarksValue();
        double max = -1;
        for (ResultEntryRow row : rows) {
            if (row.hasValidMarks(selectedExamType, total)) {
                max = Math.max(max, row.displayPercentage(selectedExamType, total));
            }
        }
        return Math.max(max, 0.0);
    }

    public double getLowestPercentage() {
        int total = getTotalMarksValue();
        double min = -1;
        for (ResultEntryRow row : rows) {
            if (row.hasValidMarks(selectedExamType, total)) {
                double pct = row.displayPercentage(selectedExamType, total);
                min = min < 0 ? pct : Math.min(min, pct);
            }
        }
        return Math.max(min, 0.0);
    }

    public int getPassPercent() {
        int entered = getEnteredCount();
        return entered == 0 ? 0 : Math.round(getPassedCount() * 100f / entered);
    }

    public int getFailPercent() {
        int entered = getEnteredCount();
        return entered == 0 ? 0 : Math.round(getFailedCount() * 100f / entered);
    }

    // ---- "My Assigned Courses" aggregate summary ----

    public int getAssignedCourseCount() {
        return courses.size();
    }

    public int getTotalStudentsAcrossCourses() {
        int total = 0;
        for (TeacherCourseSummary course : courses) total += course.getTotalStudents();
        return total;
    }

    public int getResultsEnteredAcrossCourses() {
        int total = 0;
        for (TeacherCourseSummary course : courses) total += course.getResultsEntered();
        return total;
    }

    public int getPendingResultsAcrossCourses() {
        int total = 0;
        for (TeacherCourseSummary course : courses) total += course.getResultsPendingEntry();
        return total;
    }

    public int getTotalStudents() {
        return rows.size();
    }

    public int getEnteredCount() {
        int total = getTotalMarksValue();
        int count = 0;
        for (ResultEntryRow row : rows) {
            if (row.hasValidMarks(selectedExamType, total)) count++;
        }
        return count;
    }

    public int getPassedCount() {
        int total = getTotalMarksValue();
        int count = 0;
        for (ResultEntryRow row : rows) {
            if (row.hasValidMarks(selectedExamType, total) && !"F".equals(row.displayGrade(selectedExamType, total))) {
                count++;
            }
        }
        return count;
    }

    public int getFailedCount() {
        int total = getTotalMarksValue();
        int count = 0;
        for (ResultEntryRow row : rows) {
            if (row.hasValidMarks(selectedExamType, total) && "F".equals(row.displayGrade(selectedExamType, total))) {
                count++;
            }
        }
        return count;
    }

    private static int compareRoll(String a, String b) {
        if (a == null && b == null) return 0;
        if (a == null) return 1;
        if (b == null) return -1;
        try {
            return Integer.compare(Integer.parseInt(a), Integer.parseInt(b));
        } catch (NumberFormatException e) {
            return a.compareTo(b);
        }
    }

    private static List<String> idsOfDepartments(List<DepartmentOptions> departments) {
        if (departments == null) return Collections.emptyList();
        List<String> ids = new ArrayList<>();
        for (DepartmentOptions department : departments) {
            ids.add(department.getDepartment().getId());
        }
        return ids;
    }
}
