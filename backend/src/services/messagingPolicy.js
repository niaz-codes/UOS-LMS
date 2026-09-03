function inSameDepartment(userA, userB) {
  const idsOf = (u) => {
    const ids = (u.departmentIds || []).map((id) => id.toString());
    if (u.departmentId) ids.push(u.departmentId.toString());
    return ids;
  };
  const a = idsOf(userA);
  const b = idsOf(userB);
  return a.some((id) => b.includes(id));
}

/** Re-derives each side's role from their own live User doc rather than trusting anything
 * client-supplied - matches the pre-migration firestore.rules' isValidMessagingPair, which
 * deliberately never trusted denormalized role fields on the conversation doc itself. Shared
 * between messagingController (send/create) and userController (contact list) so both sides
 * of the policy can never drift apart. */
function isValidMessagingPair(userA, userB) {
  // Admin oversees the whole university, not one department - unlike HOD/Teacher, no
  // same-department check applies to them. Previously Admin could only message a HOD, which
  // made the Admin panel's message/compose screen look broken: Teachers and Students never
  // appeared as contacts no matter what.
  if (userA.role === 'ADMIN' || userB.role === 'ADMIN') return true;
  const pair = [userA.role, userB.role].sort().join('-');
  if (pair === 'STUDENT-TEACHER') return inSameDepartment(userA, userB);
  if (pair === 'HOD-TEACHER') return inSameDepartment(userA, userB);
  return false;
}

module.exports = { inSameDepartment, isValidMessagingPair };
