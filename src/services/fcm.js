const { getMessaging } = require('firebase-admin/messaging');
const { initFirebaseAdmin } = require('../config/firebaseAdmin');

/** Best-effort push send - returns which tokens were invalid so the caller can prune them from
 * the recipient's User.fcmTokens. Silently no-ops (zero successes, no invalid tokens) if
 * firebase-admin isn't configured or there are no tokens to send to, so the rest of the
 * notification pipeline (in-app Notification records) keeps working even before a Firebase
 * project is wired up. */
async function sendPushToTokens(tokens, { title, body, data }) {
  const uniqueTokens = [...new Set((tokens || []).filter(Boolean))];
  if (uniqueTokens.length === 0) {
    return { successCount: 0, invalidTokens: [] };
  }

  const app = initFirebaseAdmin();
  if (!app) {
    return { successCount: 0, invalidTokens: [] };
  }

  // Data-only (no top-level `notification` block) is deliberate: with a `notification` block
  // present, the OS builds and shows the tray notification itself whenever the app isn't in
  // the foreground, WITHOUT ever invoking the app's FirebaseMessagingService.onMessageReceived
  // - which would mean no channel routing, no tap-to-open-the-right-screen, no grouping. A
  // data-only message always reaches onMessageReceived, in every app state (foreground,
  // background, killed), so the app can build the notification itself every time.
  // FCM data payloads must be flat string->string maps, hence title/body live in `data` too.
  const stringData = Object.fromEntries(
    Object.entries({ ...data, title, body })
      .filter(([, value]) => value !== undefined)
      .map(([key, value]) => [key, value === null ? '' : String(value)])
  );

  const message = {
    tokens: uniqueTokens,
    data: stringData,
    android: {
      priority: 'high',
    },
  };

  try {
    // firebase-admin v14's flattened default export doesn't have admin.messaging() (that's
    // undefined in this version) - the modular getMessaging(app) from firebase-admin/messaging
    // is the current API.
    const response = await getMessaging(app).sendEachForMulticast(message);
    const invalidTokens = [];
    response.responses.forEach((result, index) => {
      if (!result.success) {
        const code = result.error && result.error.code;
        if (code === 'messaging/registration-token-not-registered' || code === 'messaging/invalid-argument') {
          invalidTokens.push(uniqueTokens[index]);
        }
      }
    });
    return { successCount: response.successCount, invalidTokens };
  } catch (err) {
    console.error('FCM send failed:', err.message);
    return { successCount: 0, invalidTokens: [] };
  }
}

module.exports = { sendPushToTokens };
