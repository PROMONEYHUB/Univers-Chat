import crypto from 'node:crypto';

export const OTP_TTL_SECONDS = 300; // 5 minutes
export const MAX_OTP_ATTEMPTS = 5;
export const REQUEST_COOLDOWN_MS = 30_000; // 30s resend cooldown

export class HttpError extends Error {
  constructor(status, message, code = 'BAD_REQUEST') {
    super(message);
    this.status = status;
    this.code = code;
  }
}

export function normalizeE164Phone(raw) {
  if (typeof raw !== 'string') return '';
  const digits = raw.replace(/\D/g, '');
  if (digits.length < 7 || digits.length > 15) return '';
  return `+${digits}`;
}

export function sanitizeBotUsername(raw) {
  return String(raw || '').trim().replace(/^@+/, '');
}

export function generateNumericOtp() {
  return String(crypto.randomInt(100000, 1000000));
}

export function hashOtp(otp, salt) {
  return crypto
    .createHash('sha256')
    .update(`${salt}:${otp}`)
    .digest('hex');
}

export function verifyOtpHash(otp, salt, expectedHash) {
  const actual = Buffer.from(hashOtp(otp, salt), 'hex');
  const expected = Buffer.from(String(expectedHash || ''), 'hex');
  if (actual.length !== expected.length || actual.length === 0) return false;
  return crypto.timingSafeEqual(actual, expected);
}

/**
 * Signs a compact HS256 JWT without external dependencies so unit tests work offline.
 */
export function signSessionJwt(payload, secret, expiresInSeconds = 7 * 24 * 3600) {
  if (!secret || secret.length < 16) {
    throw new HttpError(500, 'JWT_SECRET must be configured with at least 16 characters.', 'CONFIG_ERROR');
  }
  const now = Math.floor(Date.now() / 1000);
  const header = Buffer.from(JSON.stringify({ alg: 'HS256', typ: 'JWT' })).toString('base64url');
  const body = Buffer.from(
    JSON.stringify({ ...payload, iat: now, exp: now + expiresInSeconds })
  ).toString('base64url');
  const signingInput = `${header}.${body}`;
  const signature = crypto
    .createHmac('sha256', secret)
    .update(signingInput)
    .digest('base64url');
  return `${signingInput}.${signature}`;
}

export function createTelegramApiClient({ botToken, fetchImpl = fetch, timeoutMs = 10_000 }) {
  if (!botToken) {
    throw new HttpError(500, 'TELEGRAM_BOT_TOKEN is not configured.', 'CONFIG_ERROR');
  }
  const apiBase = `https://api.telegram.org/bot${botToken}`;

  async function callMethod(method, body) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeoutMs);
    try {
      const response = await fetchImpl(`${apiBase}/${method}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
        signal: controller.signal,
      });
      const json = await response.json().catch(() => ({}));
      if (!response.ok || !json.ok) {
        throw new HttpError(
          502,
          json.description || `Telegram Bot API ${method} failed (HTTP ${response.status}).`,
          'TELEGRAM_API_ERROR'
        );
      }
      return json.result;
    } catch (err) {
      if (err instanceof HttpError) throw err;
      if (err?.name === 'AbortError') {
        throw new HttpError(504, 'Telegram Bot API timed out.', 'TELEGRAM_TIMEOUT');
      }
      throw new HttpError(502, 'Unable to reach Telegram Bot API.', 'TELEGRAM_NETWORK_ERROR');
    } finally {
      clearTimeout(timer);
    }
  }

  return {
    sendMessage: (chatId, text, extra = {}) =>
      callMethod('sendMessage', {
        chat_id: chatId,
        text,
        parse_mode: 'HTML',
        ...extra,
      }),
    getUpdates: (offset, timeoutSec = 25) =>
      callMethod('getUpdates', {
        offset,
        timeout: timeoutSec,
        allowed_updates: ['message'],
      }),
  };
}

/**
 * Step 1 (Android App -> POST /api/auth/telegram/request):
 * Creates a cryptographically random 6-digit OTP session.
 * - If this phone number already linked a Telegram chat_id, pushes the OTP straight to Telegram.
 * - Otherwise returns a deep link `https://t.me/<bot>?start=verify_<sessionId>` so the user can
 *   verify their number in 1 tap inside Telegram.
 */
export async function requestTelegramOtp({
  phoneNumber,
  displayName = '',
  botUsername,
  store,
  telegramApi,
  nowMs = () => Date.now(),
}) {
  const normalizedPhone = normalizeE164Phone(phoneNumber);
  if (!normalizedPhone) {
    throw new HttpError(400, 'Invalid phone number. Include country code (E.164 format).', 'INVALID_PHONE');
  }

  const bot = sanitizeBotUsername(botUsername);
  if (!bot) {
    throw new HttpError(500, 'TELEGRAM_BOT_USERNAME is not configured on the server.', 'CONFIG_ERROR');
  }

  const now = nowMs();
  const lastRequestedAt = await store.getLastRequestTimestamp(normalizedPhone);
  if (lastRequestedAt && now - lastRequestedAt < REQUEST_COOLDOWN_MS) {
    const waitSec = Math.ceil((REQUEST_COOLDOWN_MS - (now - lastRequestedAt)) / 1000);
    throw new HttpError(
      429,
      `Please wait ${waitSec}s before requesting another Telegram OTP.`,
      'RATE_LIMITED'
    );
  }

  const sessionId = `otp_${crypto.randomBytes(12).toString('hex')}`;
  const otp = generateNumericOtp();
  const salt = crypto.randomBytes(16).toString('hex');
  const otpHash = hashOtp(otp, salt);
  const expiresAt = now + OTP_TTL_SECONDS * 1000;

  const existingLink = await store.getPhoneLink(normalizedPhone);
  let deliveryMode = 'open_bot';
  let deliveredAt = null;
  let botDeepLink = `https://t.me/${bot}?start=verify_${sessionId}`;

  if (existingLink?.chatId) {
    try {
      await telegramApi.sendMessage(
        existingLink.chatId,
        `🔐 <b>Univers Chat Verification Code</b>\n\n` +
          `Your 6-digit login code is:\n<code>${otp}</code>\n\n` +
          `Tap the code to copy it. Valid for 5 minutes. Never share this code with anyone.`
      );
      deliveryMode = 'sent_directly';
      deliveredAt = now;
      botDeepLink = `https://t.me/${bot}`;
    } catch {
      // User may have blocked/cleared the bot; fall back cleanly to open_bot flow
      deliveryMode = 'open_bot';
    }
  }

  await store.saveSession({
    sessionId,
    phoneNumber: normalizedPhone,
    requestedDisplayName: String(displayName || '').trim().slice(0, 60),
    otpHash,
    salt,
    // Plaintext OTP is held transiently ONLY until delivered to Telegram on first-time contact share,
    // then immediately wiped from the session record.
    pendingDeliveryOtp: deliveryMode === 'open_bot' ? otp : null,
    deliveredAt,
    attempts: 0,
    consumed: false,
    createdAt: now,
    expiresAt,
    telegramChatId: existingLink?.chatId || null,
    telegramFirstName: existingLink?.firstName || '',
    telegramLastName: existingLink?.lastName || '',
  });
  await store.setLastRequestTimestamp(normalizedPhone, now);

  return {
    sessionId,
    deliveryMode,
    botDeepLink,
    expiresInSeconds: OTP_TTL_SECONDS,
  };
}

/**
 * Step 2 (Telegram Bot Webhook / Polling Update Handler):
 * Handles:
 *  - `/start verify_<sessionId>` -> prompts 1-tap "Share My Phone Number" keyboard button
 *  - `message.contact` -> verifies anti-spoofing (`contact.user_id === from.id`), checks phone match,
 *    saves permanent `phone -> chatId` link, wipes pending plaintext OTP, and sends the 6-digit OTP.
 */
export async function handleTelegramUpdate({
  update,
  store,
  telegramApi,
  nowMs = () => Date.now(),
}) {
  const message = update?.message;
  if (!message || !message.chat?.id) return { handled: false };

  const chatId = message.chat.id;
  const fromId = message.from?.id;
  const text = String(message.text || '').trim();

  // Case 1: Deep-linked /start verify_<sessionId>
  if (text.startsWith('/start')) {
    const payload = text.split(/\s+/)[1] || '';
    if (!payload.startsWith('verify_')) {
      await telegramApi.sendMessage(
        chatId,
        `👋 Welcome to <b>Univers Chat OTP Bot</b>!\n\n` +
          `Open the <b>Univers Chat</b> app, enter your phone number, and tap <b>Get OTP on Telegram</b> to receive your instant verification code.`
      );
      return { handled: true, action: 'welcome' };
    }

    const sessionId = payload.slice('verify_'.length).trim();
    const session = await store.getSession(sessionId);
    if (!session || session.consumed || nowMs() > session.expiresAt) {
      await telegramApi.sendMessage(
        chatId,
        `⏱ This verification link has expired. Please return to <b>Univers Chat</b> and tap <b>Resend OTP</b>.`
      );
      return { handled: true, action: 'expired_link' };
    }

    await store.setChatPendingSession(chatId, sessionId);
    await telegramApi.sendMessage(
      chatId,
      `🔐 <b>Univers Chat Verification</b>\n\n` +
        `Tap the button below to confirm your phone number (<b>${session.phoneNumber}</b>) and receive your 6-digit OTP.`,
      {
        reply_markup: {
          keyboard: [[{ text: '📱 Share My Phone Number', request_contact: true }]],
          resize_keyboard: true,
          one_time_keyboard: true,
        },
      }
    );
    return { handled: true, action: 'requested_contact', sessionId };
  }

  // Case 2: User tapped "Share My Phone Number"
  if (message.contact) {
    const contact = message.contact;
    // Security: Prevent forwarding someone else's contact card
    if (!fromId || contact.user_id !== fromId) {
      await telegramApi.sendMessage(
        chatId,
        `⚠️ Please use the <b>Share My Phone Number</b> button so Telegram verifies your own account number.`,
        { reply_markup: { remove_keyboard: true } }
      );
      return { handled: true, action: 'rejected_spoofed_contact' };
    }

    const sharedPhone = normalizeE164Phone(contact.phone_number);
    const pendingSessionId = await store.getChatPendingSession(chatId);
    const session = pendingSessionId ? await store.getSession(pendingSessionId) : null;

    if (!session || session.consumed || nowMs() > session.expiresAt) {
      await telegramApi.sendMessage(
        chatId,
        `⏱ Your verification session expired. Please request a fresh OTP from <b>Univers Chat</b>.`,
        { reply_markup: { remove_keyboard: true } }
      );
      return { handled: true, action: 'expired_session' };
    }

    if (sharedPhone !== session.phoneNumber) {
      await telegramApi.sendMessage(
        chatId,
        `❌ Your Telegram number (<b>${sharedPhone}</b>) does not match the number you entered in Univers Chat (<b>${session.phoneNumber}</b>).\n\n` +
          `Please enter <b>${sharedPhone}</b> in Univers Chat and try again.`,
        { reply_markup: { remove_keyboard: true } }
      );
      return { handled: true, action: 'phone_mismatch' };
    }

    const otpToSend = session.pendingDeliveryOtp;
    if (!otpToSend) {
      await telegramApi.sendMessage(
        chatId,
        `ℹ️ Your code was already delivered. If you need a new one, tap <b>Resend OTP</b> in Univers Chat.`,
        { reply_markup: { remove_keyboard: true } }
      );
      return { handled: true, action: 'already_delivered' };
    }

    const firstName = String(contact.first_name || message.from?.first_name || '').trim();
    const lastName = String(contact.last_name || message.from?.last_name || '').trim();

    await store.savePhoneLink(sharedPhone, {
      chatId,
      telegramUserId: fromId,
      firstName,
      lastName,
      linkedAt: nowMs(),
    });

    await store.updateSession(session.sessionId, {
      pendingDeliveryOtp: null,
      deliveredAt: nowMs(),
      telegramChatId: chatId,
      telegramFirstName: firstName,
      telegramLastName: lastName,
    });
    await store.clearChatPendingSession(chatId);

    await telegramApi.sendMessage(
      chatId,
      `✅ <b>Number Verified!</b>\n\n` +
        `Your Univers Chat 6-digit login code is:\n<code>${otpToSend}</code>\n\n` +
        `Tap the code to copy it and return to Univers Chat. Valid for 5 minutes.`,
      { reply_markup: { remove_keyboard: true } }
    );
    return { handled: true, action: 'otp_delivered', sessionId: session.sessionId };
  }

  return { handled: false };
}

/**
 * Step 3 (Android App -> POST /api/auth/telegram/verify):
 * Verifies the 6-digit OTP against the salted hash, enforces expiry & max attempts,
 * upserts the user in Firebase Auth + Firestore, and returns a Firebase Custom Token + HS256 JWT.
 */
export async function verifyTelegramOtp({
  sessionId,
  phoneNumber,
  otp,
  displayName = '',
  jwtSecret,
  store,
  upsertUser,
  nowMs = () => Date.now(),
}) {
  const cleanSessionId = String(sessionId || '').trim();
  const normalizedPhone = normalizeE164Phone(phoneNumber);
  const cleanOtp = String(otp || '').replace(/\D/g, '');

  if (!cleanSessionId || !normalizedPhone || cleanOtp.length !== 6) {
    throw new HttpError(400, 'sessionId, valid E.164 phoneNumber, and 6-digit otp are required.', 'INVALID_INPUT');
  }

  const session = await store.getSession(cleanSessionId);
  if (!session || session.phoneNumber !== normalizedPhone) {
    throw new HttpError(401, 'Invalid verification session. Please request a new Telegram OTP.', 'INVALID_SESSION');
  }

  if (session.consumed) {
    throw new HttpError(401, 'This verification code has already been used. Request a new one.', 'OTP_ALREADY_USED');
  }

  if (nowMs() > session.expiresAt) {
    throw new HttpError(410, 'This Telegram OTP has expired. Please tap Resend OTP.', 'OTP_EXPIRED');
  }

  if (!session.deliveredAt) {
    throw new HttpError(
      400,
      'Please open the Telegram Bot, tap Start, and share your phone number first to receive your code.',
      'OTP_NOT_DELIVERED'
    );
  }

  if (session.attempts >= MAX_OTP_ATTEMPTS) {
    throw new HttpError(429, 'Too many failed attempts. Please request a fresh Telegram OTP.', 'MAX_ATTEMPTS');
  }

  const isValid = verifyOtpHash(cleanOtp, session.salt, session.otpHash);
  if (!isValid) {
    const nextAttempts = session.attempts + 1;
    await store.updateSession(cleanSessionId, { attempts: nextAttempts });
    const remaining = Math.max(0, MAX_OTP_ATTEMPTS - nextAttempts);
    throw new HttpError(
      401,
      remaining > 0
        ? `Incorrect code. ${remaining} attempt${remaining === 1 ? '' : 's'} remaining.`
        : 'Too many failed attempts. Please request a new Telegram OTP.',
      'INVALID_OTP'
    );
  }

  await store.updateSession(cleanSessionId, { consumed: true, pendingDeliveryOtp: null });

  const resolvedFirstName = session.telegramFirstName || '';
  const resolvedLastName = session.telegramLastName || '';
  const resolvedDisplayName =
    String(displayName || session.requestedDisplayName || '').trim() ||
    [resolvedFirstName, resolvedLastName].filter(Boolean).join(' ') ||
    normalizedPhone;

  const account = await upsertUser({
    phoneNumber: normalizedPhone,
    firstName: resolvedFirstName,
    lastName: resolvedLastName,
    displayName: resolvedDisplayName,
    photoUrl: '',
  });

  const sessionToken = signSessionJwt(
    {
      sub: account.uid,
      phone_number: normalizedPhone,
      auth_provider: 'telegram_bot_otp',
    },
    jwtSecret
  );

  return {
    sessionToken,
    firebaseCustomToken: account.firebaseCustomToken || null,
    user: {
      uid: account.uid,
      phoneNumber: normalizedPhone,
      firstName: resolvedFirstName,
      lastName: resolvedLastName,
      displayName: account.displayName || resolvedDisplayName,
      photoUrl: account.photoUrl || '',
      isNewUser: Boolean(account.isNewUser),
    },
  };
}
