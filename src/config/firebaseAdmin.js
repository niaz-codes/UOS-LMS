const admin = require('firebase-admin');

let app = null;
let attempted = false;

/** Lazily initializes firebase-admin from a service account JSON in FIREBASE_SERVICE_ACCOUNT_JSON
 * (the whole JSON key file's contents, as a single env var string). Returns null - never throws -
 * if that env var isn't set or fails to parse, so the rest of the app (and the in-app Notification
 * Center, which doesn't depend on push) keeps working before/without a Firebase project being
 * configured. This is firebase-admin only (server-side push sender) - no relation to the
 * client-side Firebase Auth/Firestore SDKs this project removed during the MongoDB migration. */
function initFirebaseAdmin() {
  if (app || attempted) return app;
  attempted = true;

  const raw = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
  if (!raw) {
    console.warn('FIREBASE_SERVICE_ACCOUNT_JSON not set - push notifications are disabled (in-app notifications still work).');
    return null;
  }

  try {
    const credentials = JSON.parse(raw);
    // firebase-admin v14 flattened the top-level export - admin.cert(), not the older
    // admin.credential.cert() (admin.credential is undefined in this version).
    app = admin.initializeApp({ credential: admin.cert(credentials) });
    console.log('firebase-admin initialized - push notifications enabled.');
    return app;
  } catch (err) {
    console.error('Failed to initialize firebase-admin (push notifications disabled):', err.message);
    return null;
  }
}

module.exports = { admin, initFirebaseAdmin };
