import 'dotenv/config';
import express from 'express';
import admin from 'firebase-admin';
import { getFirestore } from 'firebase-admin/firestore';
import {
  createTelegramApiClient,
  handleTelegramUpdate,
  HttpError,
  requestTelegramOtp,
  verifyTelegramOtp,
} from './telegramOtpService.js';

function initFirebaseAdmin() {
  if (admin.apps.length > 0) return admin.app();
  const rawJson = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
  if (rawJson) {
    const serviceAccount = JSON.parse(rawJson);
    return admin.initializeApp({
      credential: admin.credential.cert(serviceAccount),
    });
  }
  return admin.initializeApp({
    credential: admin.credential.applicationDefault(),
  });
}

function createFirestoreOtpStore(db) {
  const sessionsCol = db.collection('telegram_otp_sessions');
  const linksCol = db.collection('telegram_phone_links');
  const pendingChatCol = db.collection('telegram_chat_pending');
  const rateLimitCol = db.collection('telegram_otp_ratelimits');

  return {
    async saveSession(session) {
      await sessionsCol.doc(session.sessionId).set(session);
    },
    async getSession(sessionId) {
      const snap = await sessionsCol.doc(sessionId).get();
      return snap.exists ? snap.data() : null;
    },
    async updateSession(sessionId, patch) {
      await sessionsCol.doc(sessionId).set(patch, { merge: true });
    },
    async getLatestPendingSessionByPhone(phoneNumber, nowMs) {
      const snap = await sessionsCol
        .where('phoneNumber', '==', phoneNumber)
        .get();
      let latest = null;
      for (const doc of snap.docs) {
        const data = doc.data();
        if (!data.consumed && data.pendingDeliveryOtp && data.expiresAt >= nowMs) {
          if (!latest || (data.createdAt || 0) > (latest.createdAt || 0)) {
            latest = data;
          }
        }
      }
      return latest;
    },
    async getRecentDeliveredSessionsByPhone(phoneNumber, nowMs) {
      const snap = await sessionsCol
        .where('phoneNumber', '==', phoneNumber)
        .get();
      const valid = [];
      for (const doc of snap.docs) {
        const data = doc.data();
        if (!data.consumed && data.deliveredAt && data.expiresAt >= nowMs) {
          valid.push(data);
        }
      }
      valid.sort((a, b) => (b.createdAt || 0) - (a.createdAt || 0));
      return valid;
    },
    async getPhoneLink(phoneNumber) {
      const snap = await linksCol.doc(phoneNumber).get();
      return snap.exists ? snap.data() : null;
    },
    async savePhoneLink(phoneNumber, link) {
      await linksCol.doc(phoneNumber).set(link, { merge: true });
    },
    async setChatPendingSession(chatId, sessionId) {
      await pendingChatCol.doc(String(chatId)).set({ sessionId, updatedAt: Date.now() });
    },
    async getChatPendingSession(chatId) {
      const snap = await pendingChatCol.doc(String(chatId)).get();
      return snap.exists ? snap.data().sessionId : null;
    },
    async clearChatPendingSession(chatId) {
      await pendingChatCol.doc(String(chatId)).delete().catch(() => {});
    },
    async getLastRequestTimestamp(phoneNumber) {
      const snap = await rateLimitCol.doc(phoneNumber).get();
      return snap.exists ? snap.data().lastRequestedAt : null;
    },
    async setLastRequestTimestamp(phoneNumber, timestamp) {
      await rateLimitCol.doc(phoneNumber).set({ lastRequestedAt: timestamp });
    },
  };
}

function createFirebaseUserUpserter(auth, db) {
  return async function upsertUser({ phoneNumber, displayName, photoUrl }) {
    let userRecord;
    let isNewUser = false;

    try {
      userRecord = await auth.getUserByPhoneNumber(phoneNumber);
    } catch (err) {
      if (err?.code === 'auth/user-not-found') {
        userRecord = await auth.createUser({
          phoneNumber,
          displayName: displayName || phoneNumber,
          ...(photoUrl ? { photoURL: photoUrl } : {}),
        });
        isNewUser = true;
      } else {
        throw err;
      }
    }

    const uid = userRecord.uid;
    const now = admin.firestore.FieldValue.serverTimestamp();
    const privateRef = db.collection('users').doc(uid);
    const publicRef = db.collection('user_profiles').doc(uid);

    const [existingPrivate, existingPublic] = await Promise.all([
      privateRef.get(),
      publicRef.get(),
    ]);
    const privateData = existingPrivate.exists ? existingPrivate.data() || {} : {};
    const publicData = existingPublic.exists ? existingPublic.data() || {} : {};

    const fallbackMemberName = `Member ${String(phoneNumber).slice(-4)}`;
    const resolvedName = String(
      publicData.displayName ||
      (userRecord.displayName && userRecord.displayName !== phoneNumber ? userRecord.displayName : '') ||
      displayName ||
      fallbackMemberName
    ).trim().slice(0, 60) || fallbackMemberName;

    const resolvedPhoto = String(
      publicData.photoUrl ||
      userRecord.photoURL ||
      photoUrl ||
      ''
    ).slice(0, 60000);

    const rawHandle = String(publicData.handle || resolvedName)
      .toLowerCase()
      .replace(/[^a-z0-9_]/g, '')
      .slice(0, 28);
    const resolvedHandle = rawHandle.length >= 2 ? rawHandle : `u_${uid.slice(0, 6)}`;

    const resolvedEmail = String(
      userRecord.email ||
      (typeof privateData.email === 'string' && privateData.email.length >= 3 ? privateData.email : '') ||
      phoneNumber ||
      `${uid}@universchat.local`
    ).slice(0, 320);

    await privateRef.set({
      userId: uid,
      email: resolvedEmail,
      readReceiptsEnabled: typeof privateData.readReceiptsEnabled === 'boolean' ? privateData.readReceiptsEnabled : true,
      notificationsEnabled: typeof privateData.notificationsEnabled === 'boolean' ? privateData.notificationsEnabled : true,
      blockedUserIds: Array.isArray(privateData.blockedUserIds) ? privateData.blockedUserIds.slice(0, 100) : [],
      createdAt: privateData.createdAt || now,
      updatedAt: now,
    }, { merge: true });

    await publicRef.set({
      userId: uid,
      displayName: resolvedName,
      handle: resolvedHandle,
      bio: String(publicData.bio || "Hey there! I'm on Univers Chat.").slice(0, 160),
      photoUrl: resolvedPhoto,
      statusVibe: String(publicData.statusVibe || 'Available').slice(0, 60),
      isOnline: true,
      isDiscoverable: typeof publicData.isDiscoverable === 'boolean' ? publicData.isDiscoverable : true,
      lastSeenAt: now,
      createdAt: publicData.createdAt || now,
      updatedAt: now,
    }, { merge: true });

    const firebaseCustomToken = await auth.createCustomToken(uid, {
      phone_number: phoneNumber,
      auth_provider: 'telegram_bot_otp',
    });

    return {
      uid,
      displayName: resolvedName,
      photoUrl: resolvedPhoto,
      isNewUser,
      firebaseCustomToken,
    };
  };
}

export function createApp({ store, telegramApi, upsertUser }) {
  const app = express();
  app.use(express.json({ limit: '64kb' }));

  app.get('/', (_req, res) => {
    res.json({
      status: 'ok',
      service: 'univers-chat-telegram-otp-bot',
      health: '/health',
    });
  });

  app.get('/health', (_req, res) => {
    res.json({ status: 'ok', service: 'univers-chat-telegram-otp-bot' });
  });

  app.post('/api/auth/telegram/request', async (req, res) => {
    try {
      const result = await requestTelegramOtp({
        phoneNumber: req.body?.phoneNumber,
        displayName: req.body?.displayName,
        botUsername: process.env.TELEGRAM_BOT_USERNAME,
        store,
        telegramApi,
      });
      res.status(200).json(result);
    } catch (err) {
      if (err instanceof HttpError) {
        return res.status(err.status).json({ error: err.code, message: err.message });
      }
      console.error('[POST /api/auth/telegram/request] Unexpected error:', err);
      res.status(500).json({ error: 'INTERNAL_ERROR', message: 'Failed to create Telegram OTP session.' });
    }
  });

  app.post('/api/auth/telegram/verify', async (req, res) => {
    try {
      const result = await verifyTelegramOtp({
        sessionId: req.body?.sessionId,
        phoneNumber: req.body?.phoneNumber,
        otp: req.body?.otp,
        displayName: req.body?.displayName,
        jwtSecret: process.env.JWT_SECRET,
        store,
        upsertUser,
      });
      res.status(200).json(result);
    } catch (err) {
      if (err instanceof HttpError) {
        return res.status(err.status).json({ error: err.code, message: err.message });
      }
      console.error('[POST /api/auth/telegram/verify] Unexpected error:', err);
      res.status(500).json({ error: 'INTERNAL_ERROR', message: 'Failed to verify Telegram OTP.' });
    }
  });

  app.post('/api/telegram/webhook', async (req, res) => {
    const expectedSecret = process.env.TELEGRAM_WEBHOOK_SECRET;
    if (expectedSecret) {
      const providedSecret = req.header('x-telegram-bot-api-secret-token');
      if (providedSecret !== expectedSecret) {
        return res.status(401).json({ error: 'UNAUTHORIZED_WEBHOOK' });
      }
    }
    try {
      await handleTelegramUpdate({ update: req.body, store, telegramApi });
      res.status(200).json({ ok: true });
    } catch (err) {
      console.error('[POST /api/telegram/webhook] Update error:', err);
      res.status(200).json({ ok: true }); // Acknowledge to prevent infinite Telegram retries
    }
  });

  return app;
}

async function startPollingLoop({ store, telegramApi }) {
  let offset = 0;
  console.log('Telegram Bot long-polling started...');
  while (true) {
    try {
      const updates = await telegramApi.getUpdates(offset, 25);
      for (const update of updates) {
        offset = Math.max(offset, update.update_id + 1);
        await handleTelegramUpdate({ update, store, telegramApi });
      }
    } catch (err) {
      console.error('[Telegram Polling] Error:', err.message);
      await new Promise((resolve) => setTimeout(resolve, 3000));
    }
  }
}

if (import.meta.url === `file://${process.argv[1]}`) {
  const firebaseApp = initFirebaseAdmin();
  const auth = admin.auth(firebaseApp);
  const databaseId =
    process.env.FIREBASE_DATABASE_ID || 'ai-studio-android-pulsecha-e97cfd46-bc45-4fb2-80d8-677c380a25e2';
  const db = getFirestore(firebaseApp, databaseId);

  const store = createFirestoreOtpStore(db);
  const telegramApi = createTelegramApiClient({ botToken: process.env.TELEGRAM_BOT_TOKEN });
  const upsertUser = createFirebaseUserUpserter(auth, db);

  const app = createApp({ store, telegramApi, upsertUser });
  const port = Number(process.env.PORT || 8080);
  app.listen(port, () => {
    console.log(`Univers Chat Telegram OTP Backend listening on port ${port}`);
  });

  if (String(process.env.TELEGRAM_POLLING || 'false').toLowerCase() === 'true') {
    startPollingLoop({ store, telegramApi });
  }
}
