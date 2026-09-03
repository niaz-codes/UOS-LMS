// HEC (Pakistan) style absolute grading scale - the single source of truth for marks -> grade/
// gpa everywhere in the backend (per the spec: "not hardcoded in multiple places"). Ported
// exactly from the pre-migration app's GradeScale.java so results stay consistent with every
// grade already recorded there. Bands are checked top-down against percentage.
const BANDS = [
  { minPercent: 85.0, grade: 'A', gpa: 4.0 },
  { minPercent: 80.0, grade: 'A-', gpa: 3.66 },
  { minPercent: 75.0, grade: 'B+', gpa: 3.33 },
  { minPercent: 71.0, grade: 'B', gpa: 3.0 },
  { minPercent: 68.0, grade: 'B-', gpa: 2.66 },
  { minPercent: 64.0, grade: 'C+', gpa: 2.33 },
  { minPercent: 61.0, grade: 'C', gpa: 2.0 },
  { minPercent: 58.0, grade: 'C-', gpa: 1.66 },
  { minPercent: 54.0, grade: 'D+', gpa: 1.33 },
  { minPercent: 50.0, grade: 'D', gpa: 1.0 },
  { minPercent: 0.0, grade: 'F', gpa: 0.0 },
];

const FAILING_GRADE = 'F';

function bandFor(marks) {
  return BANDS.find((band) => marks >= band.minPercent) || BANDS[BANDS.length - 1];
}

/** marks: 0-100. Returns { grade, gpa, status }, never trust a client-submitted value. */
function gradeFor(marks) {
  const band = bandFor(marks);
  return { grade: band.grade, gpa: band.gpa, status: band.grade === FAILING_GRADE ? 'FAIL' : 'PASS' };
}

module.exports = { gradeFor, FAILING_GRADE };
