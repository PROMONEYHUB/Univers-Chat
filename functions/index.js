const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { logger } = require("firebase-functions");
const admin = require("firebase-admin");
const { getFirestore } = require("firebase-admin/firestore");

admin.initializeApp();

const DATABASE_ID = "ai-studio-android-pulsecha-e97cfd46-bc45-4fb2-80d8-677c380a25e2";
const MAX_MULTICAST_TOKENS = 500;
const INVALID_TOKEN_CODES = new Set([
  "messaging/registration-token-not-registered",
  "messaging/invalid-registration-token",
]);

/**
 * Firestore trigger fired whenever a new user creates their `/user_profiles/{userId}` document.
 *
 * Privacy-preserving contact discovery:
 *  - Reads the new user's SHA-256 `phoneHash` (raw phone numbers are never stored publicly).
 *  - Queries existing `/users` documents whose uploaded `contactHashes` array contains `phoneHash`.
 *  - Respects each recipient's `notificationsEnabled` flag and `blockedUserIds` list.
 *  - Sends a push notification via Firebase Cloud Messaging (`sendEachForMulticast`) in batches
 *    of 500 tokens and automatically prunes stale/unregistered FCM tokens.
 */
exports.notifyContactsOnUserJoined = onDocumentCreated(
  {
    document: "user_profiles/{userId}",
    database: DATABASE_ID,
    region: "us-central1",
  },
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) {
      logger.warn("No snapshot payload in user_profiles create event.");
      return;
    }

    const newUserId = event.params.userId;
    const newProfile = snapshot.data() || {};
    const phoneHash = typeof newProfile.phoneHash === "string" ? newProfile.phoneHash.trim() : "";

    if (!phoneHash || phoneHash.length !== 64) {
      logger.info(`User ${newUserId} created without a valid phoneHash; skipping contact alerts.`);
      return;
    }

    const displayName =
      typeof newProfile.displayName === "string" && newProfile.displayName.trim().length > 0
        ? newProfile.displayName.trim()
        : "Your contact";

    const db = getFirestore(DATABASE_ID);

    // Find all existing users whose uploaded contactHashes include the new user's phoneHash
    const matchingUsersSnap = await db
      .collection("users")
      .where("contactHashes", "array-contains", phoneHash)
      .get();

    if (matchingUsersSnap.empty) {
      logger.info(`No existing users had contact hash matching new user ${newUserId}.`);
      return;
    }

    // Collect valid FCM tokens along with owner document references for stale-token cleanup
    const tokenEntries = [];

    for (const userDoc of matchingUsersSnap.docs) {
      const recipientUid = userDoc.id;
      if (recipientUid === newUserId) continue;

      const userData = userDoc.data() || {};
      if (userData.notificationsEnabled === false) continue;

      const blockedIds = Array.isArray(userData.blockedUserIds) ? userData.blockedUserIds : [];
      if (blockedIds.includes(newUserId)) continue;

      const tokens = Array.isArray(userData.fcmTokens) ? userData.fcmTokens : [];
      for (const rawToken of tokens) {
        if (typeof rawToken === "string" && rawToken.trim().length > 0) {
          tokenEntries.push({
            token: rawToken.trim(),
            userRef: userDoc.ref,
          });
        }
      }
    }

    if (tokenEntries.length === 0) {
      logger.info(
        `Matched ${matchingUsersSnap.size} user(s) for ${newUserId}, but none had active FCM tokens.`
      );
      return;
    }

    const messaging = admin.messaging();
    const staleTokensByUserRef = new Map();
    let successCount = 0;
    let failureCount = 0;

    // Send FCM multicast notifications in chunks of 500 (FCM protocol limit)
    for (let i = 0; i < tokenEntries.length; i += MAX_MULTICAST_TOKENS) {
      const batch = tokenEntries.slice(i, i + MAX_MULTICAST_TOKENS);
      const tokens = batch.map((entry) => entry.token);

      const response = await messaging.sendEachForMulticast({
        tokens,
        notification: {
          title: `${displayName} joined Univers Chat!`,
          body: "Say hello — your contact is now on Univers Chat with end-to-end encrypted messaging.",
        },
        data: {
          type: "contact_joined",
          joinedUserId: newUserId,
          joinedDisplayName: displayName,
        },
        android: {
          priority: "high",
          notification: {
            channelId: "univers_chat_contacts",
            clickAction: "OPEN_CONTACT_PROFILE",
          },
        },
      });

      successCount += response.successCount;
      failureCount += response.failureCount;

      response.responses.forEach((sendResult, idx) => {
        if (!sendResult.success) {
          const errorCode = sendResult.error && sendResult.error.code;
          if (errorCode && INVALID_TOKEN_CODES.has(errorCode)) {
            const entry = batch[idx];
            const key = entry.userRef.path;
            if (!staleTokensByUserRef.has(key)) {
              staleTokensByUserRef.set(key, { ref: entry.userRef, tokens: [] });
            }
            staleTokensByUserRef.get(key).tokens.push(entry.token);
          }
        }
      });
    }

    // Prune invalid or expired FCM tokens from affected user documents
    const cleanupPromises = [];
    for (const { ref, tokens } of staleTokensByUserRef.values()) {
      cleanupPromises.push(
        ref.update({
          fcmTokens: admin.firestore.FieldValue.arrayRemove(...tokens),
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        })
      );
    }
    if (cleanupPromises.length > 0) {
      await Promise.allSettled(cleanupPromises);
    }

    logger.info(
      `Contact-joined push alerts for ${newUserId}: ${successCount} sent, ${failureCount} failed.`
    );
  }
);
