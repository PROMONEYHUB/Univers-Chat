import assert from 'node:assert/strict';
import test from 'node:test';
import {
  handleTelegramUpdate,
  HttpError,
  MAX_OTP_ATTEMPTS,
  requestTelegramOtp,
  verifyTelegramOtp,
} from './telegramOtpService.js';

function createInMemoryStore() {
  const sessions = new Map();
  const links = new Map();
  const pendingChats = new Map();
  const rateLimits = new Map();

  return {
    async saveSession(session) {
      sessions.set(session.sessionId, { ...session });
    },
    async getSession(sessionId) {
      return sessions.has(sessionId) ? { ...sessions.get(sessionId) } : null;
    },
    async updateSession(sessionId, patch) {
      const existing = sessions.get(sessionId);
      if (existing) sessions.set(sessionId, { ...existing, ...patch });
    },
    async getPhoneLink(phoneNumber) {
      return links.get(phoneNumber) || null;
    },
    async savePhoneLink(phoneNumber, link) {
      links.set(phoneNumber, { ...link });
    },
    async setChatPendingSession(chatId, sessionId) {
      pendingChats.set(String(chatId), sessionId);
    },
    async getChatPendingSession(chatId) {
      return pendingChats.get(String(chatId)) || null;
    },
    async clearChatPendingSession(chatId) {
      pendingChats.delete(String(chatId));
    },
    async getLastRequestTimestamp(phoneNumber) {
      return rateLimits.get(phoneNumber) || null;
    },
    async setLastRequestTimestamp(phoneNumber, ts) {
      rateLimits.set(phoneNumber, ts);
    },
  };
}

function createFakeTelegramApi() {
  const sentMessages = [];
  return {
    sentMessages,
    async sendMessage(chatId, text, extra = {}) {
      sentMessages.push({ chatId, text, extra });
      return { message_id: sentMessages.length };
    },
  };
}

const TEST_JWT_SECRET = 'test-super-secret-jwt-signing-key-32bytes';

test('first-time user: request -> /start -> share contact -> OTP delivered -> verify returns JWT & custom token', async () => {
  const store = createInMemoryStore();
  const telegramApi = createFakeTelegramApi();
  let now = 1_700_000_000_000;

  const reqResult = await requestTelegramOtp({
    phoneNumber: '+91 98765 43210',
    displayName: 'Aarav',
    botUsername: '@UniversChatOtpBot',
    store,
    telegramApi,
    nowMs: () => now,
  });

  assert.equal(reqResult.deliveryMode, 'open_bot');
  assert.equal(
    reqResult.botDeepLink,
    `https://t.me/UniversChatOtpBot?start=verify_${reqResult.sessionId}`
  );

  // User taps Start inside Telegram
  const startRes = await handleTelegramUpdate({
    update: {
      message: {
        chat: { id: 777001 },
        from: { id: 555001, first_name: 'Aarav' },
        text: `/start verify_${reqResult.sessionId}`,
      },
    },
    store,
    telegramApi,
    nowMs: () => now,
  });
  assert.equal(startRes.action, 'requested_contact');
  assert.equal(telegramApi.sentMessages.length, 1);

  // User taps "Share My Phone Number"
  const contactRes = await handleTelegramUpdate({
    update: {
      message: {
        chat: { id: 777001 },
        from: { id: 555001, first_name: 'Aarav', last_name: 'Sharma' },
        contact: {
          user_id: 555001,
          phone_number: '919876543210',
          first_name: 'Aarav',
          last_name: 'Sharma',
        },
      },
    },
    store,
    telegramApi,
    nowMs: () => now,
  });
  assert.equal(contactRes.action, 'otp_delivered');

  // Extract 6-digit OTP from Telegram message
  const otpMsg = telegramApi.sentMessages[1].text;
  const match = otpMsg.match(/<code>(\d{6})<\/code>/);
  assert.ok(match, 'Expected 6-digit OTP inside <code> tag');
  const deliveredOtp = match[1];

  // Verify OTP wipes plaintext & returns tokens
  const storedAfterDelivery = await store.getSession(reqResult.sessionId);
  assert.equal(storedAfterDelivery.pendingDeliveryOtp, null);

  const verifyResult = await verifyTelegramOtp({
    sessionId: reqResult.sessionId,
    phoneNumber: '+919876543210',
    otp: deliveredOtp,
    jwtSecret: TEST_JWT_SECRET,
    store,
    nowMs: () => now + 5000,
    upsertUser: async (input) => ({
      uid: 'uid_aarav_1',
      displayName: input.displayName,
      photoUrl: '',
      isNewUser: true,
      firebaseCustomToken: 'fb-custom-token-abc',
    }),
  });

  assert.equal(verifyResult.firebaseCustomToken, 'fb-custom-token-abc');
  assert.equal(verifyResult.sessionToken.split('.').length, 3);
  assert.equal(verifyResult.user.uid, 'uid_aarav_1');
  assert.equal(verifyResult.user.phoneNumber, '+919876543210');

  // Replay attempt must fail
  await assert.rejects(
    verifyTelegramOtp({
      sessionId: reqResult.sessionId,
      phoneNumber: '+919876543210',
      otp: deliveredOtp,
      jwtSecret: TEST_JWT_SECRET,
      store,
      nowMs: () => now + 6000,
      upsertUser: async () => {
        throw new Error('Should not be called on replay');
      },
    }),
    (err) => err instanceof HttpError && err.status === 401 && err.code === 'OTP_ALREADY_USED'
  );
});

test('returning user: sends 6-digit OTP directly to linked Telegram chat without re-sharing contact', async () => {
  const store = createInMemoryStore();
  const telegramApi = createFakeTelegramApi();
  await store.savePhoneLink('+14155550199', {
    chatId: 888999,
    telegramUserId: 888999,
    firstName: 'Maya',
    lastName: 'Lin',
  });

  const reqResult = await requestTelegramOtp({
    phoneNumber: '+1 415 555 0199',
    botUsername: 'UniversChatOtpBot',
    store,
    telegramApi,
  });

  assert.equal(reqResult.deliveryMode, 'sent_directly');
  assert.equal(telegramApi.sentMessages.length, 1);
  assert.equal(telegramApi.sentMessages[0].chatId, 888999);
  const match = telegramApi.sentMessages[0].text.match(/<code>(\d{6})<\/code>/);
  assert.ok(match);

  const verifyResult = await verifyTelegramOtp({
    sessionId: reqResult.sessionId,
    phoneNumber: '+14155550199',
    otp: match[1],
    jwtSecret: TEST_JWT_SECRET,
    store,
    upsertUser: async (input) => ({
      uid: 'uid_maya',
      displayName: input.displayName,
      photoUrl: '',
      isNewUser: false,
      firebaseCustomToken: 'fb-token-maya',
    }),
  });
  assert.equal(verifyResult.user.displayName, 'Maya Lin');
  assert.equal(verifyResult.user.isNewUser, false);
});

test('security: rejects spoofed contact cards, mismatched phone numbers, wrong OTPs, and expired OTPs', async () => {
  const store = createInMemoryStore();
  const telegramApi = createFakeTelegramApi();
  let now = 1_700_000_000_000;

  const reqResult = await requestTelegramOtp({
    phoneNumber: '+447700900123',
    botUsername: 'UniversChatOtpBot',
    store,
    telegramApi,
    nowMs: () => now,
  });

  await handleTelegramUpdate({
    update: {
      message: {
        chat: { id: 1234 },
        from: { id: 9001 },
        text: `/start verify_${reqResult.sessionId}`,
      },
    },
    store,
    telegramApi,
    nowMs: () => now,
  });

  // Spoofed contact card (contact.user_id != from.id)
  const spoofRes = await handleTelegramUpdate({
    update: {
      message: {
        chat: { id: 1234 },
        from: { id: 9001 },
        contact: { user_id: 9999, phone_number: '+447700900123' },
      },
    },
    store,
    telegramApi,
    nowMs: () => now,
  });
  assert.equal(spoofRes.action, 'rejected_spoofed_contact');

  // Phone mismatch
  const mismatchRes = await handleTelegramUpdate({
    update: {
      message: {
        chat: { id: 1234 },
        from: { id: 9001 },
        contact: { user_id: 9001, phone_number: '+447700900999' },
      },
    },
    store,
    telegramApi,
    nowMs: () => now,
  });
  assert.equal(mismatchRes.action, 'phone_mismatch');

  // Deliver legitimate OTP
  await handleTelegramUpdate({
    update: {
      message: {
        chat: { id: 1234 },
        from: { id: 9001 },
        contact: { user_id: 9001, phone_number: '+447700900123' },
      },
    },
    store,
    telegramApi,
    nowMs: () => now,
  });

  // Wrong OTP attempts hit lockout
  for (let i = 0; i < MAX_OTP_ATTEMPTS; i++) {
    await assert.rejects(
      verifyTelegramOtp({
        sessionId: reqResult.sessionId,
        phoneNumber: '+447700900123',
        otp: '000000',
        jwtSecret: TEST_JWT_SECRET,
        store,
        nowMs: () => now + 1000,
        upsertUser: async () => ({}),
      }),
      (err) => err instanceof HttpError && err.status === 401
    );
  }

  await assert.rejects(
    verifyTelegramOtp({
      sessionId: reqResult.sessionId,
      phoneNumber: '+447700900123',
      otp: '000000',
      jwtSecret: TEST_JWT_SECRET,
      store,
      nowMs: () => now + 2000,
      upsertUser: async () => ({}),
    }),
    (err) => err instanceof HttpError && err.status === 429 && err.code === 'MAX_ATTEMPTS'
  );

  // Expiry check (after 6 minutes)
  await assert.rejects(
    verifyTelegramOtp({
      sessionId: reqResult.sessionId,
      phoneNumber: '+447700900123',
      otp: '123456',
      jwtSecret: TEST_JWT_SECRET,
      store,
      nowMs: () => now + 6 * 60 * 1000,
      upsertUser: async () => ({}),
    }),
    (err) => err instanceof HttpError && err.status === 410 && err.code === 'OTP_EXPIRED'
  );
});
