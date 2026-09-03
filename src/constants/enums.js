const UserRole = Object.freeze({
  ADMIN: 'ADMIN',
  HOD: 'HOD',
  TEACHER: 'TEACHER',
  STUDENT: 'STUDENT',
});

const REGISTERABLE_ROLES = [UserRole.HOD, UserRole.TEACHER, UserRole.STUDENT];

const UserStatus = Object.freeze({
  PENDING: 'PENDING',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
  SUSPENDED: 'SUSPENDED',
});

const MediaCategory = Object.freeze({
  PROFILE_PHOTO: 'profile_photo',
  ACADEMIC_DOCUMENT: 'academic_document',
  ASSIGNMENT: 'assignment',
  ASSIGNMENT_SUBMISSION: 'assignment_submission',
  STUDY_MATERIAL: 'study_material',
  LEAVE_ATTACHMENT: 'leave_attachment',
  MESSAGE_ATTACHMENT: 'message_attachment',
  RESULT_DOCUMENT: 'result_document',
});

// Exam result / semester result workflow.
const ResultStatus = Object.freeze({
  DRAFT: 'DRAFT',
  PENDING_HOD_APPROVAL: 'PENDING_HOD_APPROVAL',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
});

const SubjectResultStatus = Object.freeze({
  PASS: 'PASS',
  FAIL: 'FAIL',
});

const PromotionStatus = Object.freeze({
  NOT_EVALUATED: 'NOT_EVALUATED',
  ELIGIBLE_FOR_PROMOTION: 'ELIGIBLE_FOR_PROMOTION',
  NOT_PROMOTED: 'NOT_PROMOTED',
  PROMOTED: 'PROMOTED',
});

const RepeatStatus = Object.freeze({
  PENDING: 'PENDING',
  SUBMITTED: 'SUBMITTED',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
});

// A semester is blocked from promotion once this many subjects are failed (spec §9).
const MAX_ALLOWED_FAILED_SUBJECTS = 4;

// Groups NotificationType values for per-category settings/filtering (Notification Settings
// screen, Notification Center filter chips). Every NotificationType belongs to exactly one.
const NotificationCategory = Object.freeze({
  ACCOUNT: 'ACCOUNT',
  ACADEMIC_CONTENT: 'ACADEMIC_CONTENT',
  ATTENDANCE: 'ATTENDANCE',
  EXAM_RESULT: 'EXAM_RESULT',
  LEAVE: 'LEAVE',
  ANNOUNCEMENT: 'ANNOUNCEMENT',
  CALENDAR: 'CALENDAR',
  MESSAGE: 'MESSAGE',
  PROMOTION: 'PROMOTION',
  SYSTEM: 'SYSTEM',
});

// Complete notification taxonomy. Not every type has a live trigger wired yet - see
// notificationService.js callers. Types without a wired trigger are reserved for a future
// scheduled job (this backend has no cron/scheduler today) rather than faked.
const NotificationType = Object.freeze({
  // ACCOUNT
  ACCOUNT_REGISTERED: 'ACCOUNT_REGISTERED', // -> notifies Admins a new signup needs review
  ACCOUNT_APPROVED: 'ACCOUNT_APPROVED',
  ACCOUNT_REJECTED: 'ACCOUNT_REJECTED',
  ACCOUNT_SUSPENDED: 'ACCOUNT_SUSPENDED',
  ACCOUNT_REACTIVATED: 'ACCOUNT_REACTIVATED',
  ACCOUNT_ROLE_CHANGED: 'ACCOUNT_ROLE_CHANGED',
  ACCOUNT_DEPARTMENT_ASSIGNED: 'ACCOUNT_DEPARTMENT_ASSIGNED',
  PASSWORD_CHANGED: 'PASSWORD_CHANGED',
  SUBJECT_ASSIGNED: 'SUBJECT_ASSIGNED',

  // ACADEMIC_CONTENT
  ASSIGNMENT_CREATED: 'ASSIGNMENT_CREATED',
  ASSIGNMENT_SUBMITTED: 'ASSIGNMENT_SUBMITTED',
  ASSIGNMENT_GRADED: 'ASSIGNMENT_GRADED',
  ASSIGNMENT_DUE_SOON: 'ASSIGNMENT_DUE_SOON', // reserved - needs a scheduler
  QUIZ_CREATED: 'QUIZ_CREATED',
  QUIZ_SUBMITTED: 'QUIZ_SUBMITTED',
  QUIZ_GRADED: 'QUIZ_GRADED',
  QUIZ_DUE_SOON: 'QUIZ_DUE_SOON', // reserved - needs a scheduler
  MATERIAL_UPLOADED: 'MATERIAL_UPLOADED',
  TIMETABLE_UPDATED: 'TIMETABLE_UPDATED',
  TIMETABLE_SLOT_REMOVED: 'TIMETABLE_SLOT_REMOVED',
  EXAM_SCHEDULE_PUBLISHED: 'EXAM_SCHEDULE_PUBLISHED',
  EXAM_SCHEDULE_UPDATED: 'EXAM_SCHEDULE_UPDATED',

  // ATTENDANCE
  ATTENDANCE_MARKED_ABSENT: 'ATTENDANCE_MARKED_ABSENT',
  LOW_ATTENDANCE_WARNING: 'LOW_ATTENDANCE_WARNING', // reserved - needs a scheduler

  // EXAM_RESULT
  EXAM_RESULT_SUBMITTED_FOR_APPROVAL: 'EXAM_RESULT_SUBMITTED_FOR_APPROVAL',
  EXAM_RESULT_APPROVED: 'EXAM_RESULT_APPROVED',
  EXAM_RESULT_REJECTED: 'EXAM_RESULT_REJECTED',
  REPEAT_EXAM_SCHEDULED: 'REPEAT_EXAM_SCHEDULED',
  REPEAT_EXAM_MARKS_SUBMITTED: 'REPEAT_EXAM_MARKS_SUBMITTED',
  REPEAT_EXAM_APPROVED: 'REPEAT_EXAM_APPROVED',
  REPEAT_EXAM_REJECTED: 'REPEAT_EXAM_REJECTED',

  // LEAVE
  LEAVE_APPLIED: 'LEAVE_APPLIED',
  LEAVE_APPROVED: 'LEAVE_APPROVED',
  LEAVE_REJECTED: 'LEAVE_REJECTED',

  // ANNOUNCEMENT
  ANNOUNCEMENT_POSTED: 'ANNOUNCEMENT_POSTED',
  SYSTEM_ANNOUNCEMENT: 'SYSTEM_ANNOUNCEMENT',

  // CALENDAR
  CALENDAR_EVENT_ADDED: 'CALENDAR_EVENT_ADDED',

  // MESSAGE
  NEW_MESSAGE: 'NEW_MESSAGE',

  // PROMOTION
  STUDENT_PROMOTED: 'STUDENT_PROMOTED',
  STUDENT_NOT_PROMOTED: 'STUDENT_NOT_PROMOTED',
});

const NOTIFICATION_TYPE_CATEGORY = Object.freeze({
  [NotificationType.ACCOUNT_REGISTERED]: NotificationCategory.ACCOUNT,
  [NotificationType.ACCOUNT_APPROVED]: NotificationCategory.ACCOUNT,
  [NotificationType.ACCOUNT_REJECTED]: NotificationCategory.ACCOUNT,
  [NotificationType.ACCOUNT_SUSPENDED]: NotificationCategory.ACCOUNT,
  [NotificationType.ACCOUNT_REACTIVATED]: NotificationCategory.ACCOUNT,
  [NotificationType.ACCOUNT_ROLE_CHANGED]: NotificationCategory.ACCOUNT,
  [NotificationType.ACCOUNT_DEPARTMENT_ASSIGNED]: NotificationCategory.ACCOUNT,
  [NotificationType.PASSWORD_CHANGED]: NotificationCategory.ACCOUNT,
  [NotificationType.SUBJECT_ASSIGNED]: NotificationCategory.ACCOUNT,

  [NotificationType.ASSIGNMENT_CREATED]: NotificationCategory.ACADEMIC_CONTENT,
  [NotificationType.ASSIGNMENT_SUBMITTED]: NotificationCategory.ACADEMIC_CONTENT,
  [NotificationType.ASSIGNMENT_GRADED]: NotificationCategory.ACADEMIC_CONTENT,
  [NotificationType.ASSIGNMENT_DUE_SOON]: NotificationCategory.ACADEMIC_CONTENT,
  [NotificationType.QUIZ_CREATED]: NotificationCategory.ACADEMIC_CONTENT,
  [NotificationType.QUIZ_SUBMITTED]: NotificationCategory.ACADEMIC_CONTENT,
  [NotificationType.QUIZ_GRADED]: NotificationCategory.ACADEMIC_CONTENT,
  [NotificationType.QUIZ_DUE_SOON]: NotificationCategory.ACADEMIC_CONTENT,
  [NotificationType.MATERIAL_UPLOADED]: NotificationCategory.ACADEMIC_CONTENT,
  [NotificationType.TIMETABLE_UPDATED]: NotificationCategory.ACADEMIC_CONTENT,
  [NotificationType.TIMETABLE_SLOT_REMOVED]: NotificationCategory.ACADEMIC_CONTENT,
  [NotificationType.EXAM_SCHEDULE_PUBLISHED]: NotificationCategory.ACADEMIC_CONTENT,
  [NotificationType.EXAM_SCHEDULE_UPDATED]: NotificationCategory.ACADEMIC_CONTENT,

  [NotificationType.ATTENDANCE_MARKED_ABSENT]: NotificationCategory.ATTENDANCE,
  [NotificationType.LOW_ATTENDANCE_WARNING]: NotificationCategory.ATTENDANCE,

  [NotificationType.EXAM_RESULT_SUBMITTED_FOR_APPROVAL]: NotificationCategory.EXAM_RESULT,
  [NotificationType.EXAM_RESULT_APPROVED]: NotificationCategory.EXAM_RESULT,
  [NotificationType.EXAM_RESULT_REJECTED]: NotificationCategory.EXAM_RESULT,
  [NotificationType.REPEAT_EXAM_SCHEDULED]: NotificationCategory.EXAM_RESULT,
  [NotificationType.REPEAT_EXAM_MARKS_SUBMITTED]: NotificationCategory.EXAM_RESULT,
  [NotificationType.REPEAT_EXAM_APPROVED]: NotificationCategory.EXAM_RESULT,
  [NotificationType.REPEAT_EXAM_REJECTED]: NotificationCategory.EXAM_RESULT,

  [NotificationType.LEAVE_APPLIED]: NotificationCategory.LEAVE,
  [NotificationType.LEAVE_APPROVED]: NotificationCategory.LEAVE,
  [NotificationType.LEAVE_REJECTED]: NotificationCategory.LEAVE,

  [NotificationType.ANNOUNCEMENT_POSTED]: NotificationCategory.ANNOUNCEMENT,
  [NotificationType.SYSTEM_ANNOUNCEMENT]: NotificationCategory.ANNOUNCEMENT,

  [NotificationType.CALENDAR_EVENT_ADDED]: NotificationCategory.CALENDAR,

  [NotificationType.NEW_MESSAGE]: NotificationCategory.MESSAGE,

  [NotificationType.STUDENT_PROMOTED]: NotificationCategory.PROMOTION,
  [NotificationType.STUDENT_NOT_PROMOTED]: NotificationCategory.PROMOTION,
});

module.exports = {
  UserRole,
  REGISTERABLE_ROLES,
  UserStatus,
  MediaCategory,
  ResultStatus,
  SubjectResultStatus,
  PromotionStatus,
  RepeatStatus,
  MAX_ALLOWED_FAILED_SUBJECTS,
  NotificationCategory,
  NotificationType,
  NOTIFICATION_TYPE_CATEGORY,
};
