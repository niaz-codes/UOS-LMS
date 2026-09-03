const { UserRole } = require('../constants/enums');

function isHodOfDepartment(user, departmentId) {
  return user.role === UserRole.HOD && user.departmentId && departmentId
    && user.departmentId.toString() === departmentId.toString();
}

/** Teacher is multi-department (departmentIds); checks both that array and the legacy
 * scalar mirror, matching the pre-migration firestore.rules' isTeacherInDepartment. */
function isTeacherInDepartment(user, departmentId) {
  if (user.role !== UserRole.TEACHER || !departmentId) return false;
  const target = departmentId.toString();
  if (user.departmentId && user.departmentId.toString() === target) return true;
  return (user.departmentIds || []).some((id) => id.toString() === target);
}

/** Same membership rule as subjectController.mine: a Student can see a subject's content
 * either because it's their current department+semester cohort, or because it's on their
 * retakeSubjectIds list (a subject from a past semester they're repeating). */
function isStudentEnrolledInSubject(user, subject) {
  if (user.role !== UserRole.STUDENT || !subject) return false;
  const sameCohort = subject.departmentId && user.departmentId && subject.semesterId && user.currentSemesterId
    && subject.departmentId.toString() === user.departmentId.toString()
    && subject.semesterId.toString() === user.currentSemesterId.toString();
  if (sameCohort) return true;
  return (user.retakeSubjectIds || []).some((id) => id.toString() === subject._id.toString());
}

module.exports = { isHodOfDepartment, isTeacherInDepartment, isStudentEnrolledInSubject };
