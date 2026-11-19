const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const ts = () => new Date(Date.now() - 1000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const CHARLIE_UID = "charlie_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

// --- 1. Unauthenticated access must fail everywhere ---

test("Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("user_profiles").where("isDiscoverable", "==", true).get()
  );
  await assertFails(
    unauthDb.collection("conversations").where("participantIds", "array-contains", ALICE_UID).get()
  );
  await assertFails(
    unauthDb
      .collection("conversations")
      .doc("conv_1")
      .collection("messages")
      .where("participantIds", "array-contains", ALICE_UID)
      .get()
  );
  await assertFails(
    unauthDb.collection("calls").where("participantIds", "array-contains", ALICE_UID).get()
  );
});

// --- 2. Private User PII isolation (/users/{uid}) ---

test("Owner can create and read own /users/{uid}; other users cannot", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      readReceiptsEnabled: true,
      notificationsEnabled: true,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "spoof@example.com",
      createdAt: ts(),
      updatedAt: ts(),
    })
  );
});

// --- 3. Public User Profiles & Query Alignment (/user_profiles/{uid}) ---

test("Signed-in user can create own profile and query discoverable profiles, but cannot spoof or shadow-update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("user_profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      handle: "alice",
      bio: "Building things",
      photoUrl: "",
      statusVibe: "Online",
      isOnline: true,
      isDiscoverable: true,
      lastSeenAt: ts(),
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Discoverable list query succeeds
  await assertSucceeds(
    bobDb.collection("user_profiles").where("isDiscoverable", "==", true).get()
  );

  // Blanket unfiltered list query fails
  await assertFails(bobDb.collection("user_profiles").get());

  // Owner can update displayName, bio, and upload a compressed JPEG data-URI profile picture
  const sampleAvatarDataUri = "data:image/jpeg;base64," + "A".repeat(4096);
  await assertSucceeds(
    aliceDb.collection("user_profiles").doc(ALICE_UID).update({
      displayName: "Alice Rivera",
      bio: "Product designer & coffee enthusiast.",
      photoUrl: sampleAvatarDataUri,
      updatedAt: ts(),
    })
  );

  // Owner can clear their profile picture back to empty string
  await assertSucceeds(
    aliceDb.collection("user_profiles").doc(ALICE_UID).update({
      photoUrl: "",
      updatedAt: ts(),
    })
  );

  // Oversized profile picture (> 60,000 chars) fails
  await assertFails(
    aliceDb.collection("user_profiles").doc(ALICE_UID).update({
      photoUrl: "data:image/jpeg;base64," + "A".repeat(60000),
      updatedAt: ts(),
    })
  );

  // Empty displayName or oversized bio (> 160 chars) fails
  await assertFails(
    aliceDb.collection("user_profiles").doc(ALICE_UID).update({
      displayName: "",
      updatedAt: ts(),
    })
  );
  await assertFails(
    aliceDb.collection("user_profiles").doc(ALICE_UID).update({
      bio: "x".repeat(161),
      updatedAt: ts(),
    })
  );

  // Cross-user update fails
  await assertFails(
    bobDb.collection("user_profiles").doc(ALICE_UID).update({
      displayName: "Hacked Alice",
      updatedAt: ts(),
    })
  );
  await assertFails(
    bobDb.collection("user_profiles").doc(ALICE_UID).update({
      photoUrl: sampleAvatarDataUri,
      updatedAt: ts(),
    })
  );

  // Shadow update with ghost field fails
  await assertFails(
    aliceDb.collection("user_profiles").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: ts(),
    })
  );
});

// --- 4. Conversations & Participant Isolation (/conversations/{cid}) ---

test("Participants can create, query, and update conversation; non-participants are denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  // Check existence of deterministic DM ID before creation
  await assertSucceeds(aliceDb.collection("conversations").doc("dm_alice_bob").get());

  await assertSucceeds(
    aliceDb.collection("conversations").doc("dm_alice_bob").set({
      type: "direct",
      title: "",
      createdBy: ALICE_UID,
      participantIds: [ALICE_UID, BOB_UID],
      participantNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob" },
      participantPhotos: {},
      lastMessageText: "",
      lastMessageSenderId: "",
      lastMessageSenderName: "",
      lastMessageType: "",
      lastMessageAt: ts(),
      unreadCounts: { [ALICE_UID]: 0, [BOB_UID]: 0 },
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Participant query aligned with rule succeeds
  await assertSucceeds(
    bobDb.collection("conversations").where("participantIds", "array-contains", BOB_UID).get()
  );

  // Unfiltered query fails
  await assertFails(bobDb.collection("conversations").get());

  // Charlie (non-participant) cannot read or update
  await assertFails(charlieDb.collection("conversations").doc("dm_alice_bob").get());
  await assertFails(
    charlieDb.collection("conversations").doc("dm_alice_bob").update({
      lastMessageText: "Intruder",
      updatedAt: ts(),
    })
  );

  // Bob can mark read (Action 2)
  await assertSucceeds(
    bobDb.collection("conversations").doc("dm_alice_bob").update({
      unreadCounts: { [ALICE_UID]: 0, [BOB_UID]: 0 },
      updatedAt: ts(),
    })
  );

  // Bob cannot mutate immutable createdBy
  await assertFails(
    bobDb.collection("conversations").doc("dm_alice_bob").update({
      createdBy: BOB_UID,
      updatedAt: ts(),
    })
  );

  // Charlie (non-participant) cannot delete DM; Bob (participant) can delete DM
  await assertFails(charlieDb.collection("conversations").doc("dm_alice_bob").delete());
  await assertSucceeds(bobDb.collection("conversations").doc("dm_alice_bob").delete());
});

// --- 5. Messages Subcollection, Reactions, Polls & Edits ---

test("Participants can send, react, vote on polls, edit own text, while non-participants and spoofers fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("conversations").doc("group_1").set({
      type: "group",
      title: "Design Club",
      topic: "Weekly sync",
      createdBy: ALICE_UID,
      participantIds: [ALICE_UID, BOB_UID],
      participantNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob" },
      participantPhotos: {},
      lastMessageText: "",
      lastMessageSenderId: "",
      lastMessageSenderName: "",
      lastMessageType: "",
      lastMessageAt: ts(),
      unreadCounts: { [ALICE_UID]: 0, [BOB_UID]: 0 },
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Alice sends text message
  await assertSucceeds(
    aliceDb
      .collection("conversations")
      .doc("group_1")
      .collection("messages")
      .doc("msg_1")
      .set({
        conversationId: "group_1",
        senderId: ALICE_UID,
        senderName: "Alice",
        senderPhotoUrl: "",
        participantIds: [ALICE_UID, BOB_UID],
        type: "text",
        text: "Hello Bob!",
        reactions: {},
        isEdited: false,
        createdAt: ts(),
        updatedAt: ts(),
      })
  );

  // Bob queries messages with participantIds filter
  await assertSucceeds(
    bobDb
      .collection("conversations")
      .doc("group_1")
      .collection("messages")
      .where("participantIds", "array-contains", BOB_UID)
      .get()
  );

  // Charlie cannot read or send messages into group_1
  await assertFails(
    charlieDb
      .collection("conversations")
      .doc("group_1")
      .collection("messages")
      .doc("msg_1")
      .get()
  );
  await assertFails(
    charlieDb
      .collection("conversations")
      .doc("group_1")
      .collection("messages")
      .doc("msg_bad")
      .set({
        conversationId: "group_1",
        senderId: CHARLIE_UID,
        senderName: "Charlie",
        participantIds: [ALICE_UID, BOB_UID, CHARLIE_UID],
        type: "text",
        text: "Bypass",
        isEdited: false,
        createdAt: ts(),
        updatedAt: ts(),
      })
  );

  // Bob can react to Alice's message (Action 2)
  await assertSucceeds(
    bobDb
      .collection("conversations")
      .doc("group_1")
      .collection("messages")
      .doc("msg_1")
      .update({
        reactions: { [BOB_UID]: "🔥" },
        updatedAt: ts(),
      })
  );

  // Bob CANNOT edit Alice's message text
  await assertFails(
    bobDb
      .collection("conversations")
      .doc("group_1")
      .collection("messages")
      .doc("msg_1")
      .update({
        text: "Edited by Bob",
        isEdited: true,
        updatedAt: ts(),
      })
  );

  // Alice CAN edit her own text message (Action 1)
  await assertSucceeds(
    aliceDb
      .collection("conversations")
      .doc("group_1")
      .collection("messages")
      .doc("msg_1")
      .update({
        text: "Hello Bob! (updated)",
        isEdited: true,
        updatedAt: ts(),
      })
  );

  // Alice creates a poll message and Bob votes (Action 3)
  await assertSucceeds(
    aliceDb
      .collection("conversations")
      .doc("group_1")
      .collection("messages")
      .doc("poll_1")
      .set({
        conversationId: "group_1",
        senderId: ALICE_UID,
        senderName: "Alice",
        participantIds: [ALICE_UID, BOB_UID],
        type: "poll",
        text: "Coffee spot?",
        pollQuestion: "Coffee spot?",
        pollOptions: ["Blue Bottle", "Sightglass"],
        pollVotes: {},
        isEdited: false,
        createdAt: ts(),
        updatedAt: ts(),
      })
  );

  await assertSucceeds(
    bobDb
      .collection("conversations")
      .doc("group_1")
      .collection("messages")
      .doc("poll_1")
      .update({
        pollVotes: { [BOB_UID]: "0" },
        updatedAt: ts(),
      })
  );

  // Bob cannot delete Alice's message; Alice can delete her own
  await assertFails(
    bobDb
      .collection("conversations")
      .doc("group_1")
      .collection("messages")
      .doc("msg_1")
      .delete()
  );
  await assertSucceeds(
    aliceDb
      .collection("conversations")
      .doc("group_1")
      .collection("messages")
      .doc("msg_1")
      .delete()
  );
});

// --- 6. WebRTC HD Calls & ICE Candidates (/calls/{callId}/candidates/{candidateId}) ---

test("Participants can initiate, accept, exchange ICE candidates, end HD calls; outsiders and post-terminal updates fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  // Seed Bob's public profile because call creation verifies callee exists
  await assertSucceeds(
    bobDb.collection("user_profiles").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      handle: "bob",
      isOnline: true,
      isDiscoverable: true,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Alice starts an HD video call with Bob
  const callRef = aliceDb.collection("calls").doc("call_hd_1");
  await assertSucceeds(
    callRef.set({
      callerId: ALICE_UID,
      callerName: "Alice",
      calleeId: BOB_UID,
      calleeName: "Bob",
      participantIds: [ALICE_UID, BOB_UID],
      conversationId: "dm_alice_bob",
      callType: "video",
      qualityPreset: "HD_720P",
      status: "ringing",
      offerSdp: "v=0\no=- 12345 2 IN IP4 127.0.0.1\ns=-\nt=0 0\n",
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Blanket unfiltered query fails
  await assertFails(bobDb.collection("calls").get());

  // Bob's aligned query succeeds
  await assertSucceeds(
    bobDb.collection("calls").where("participantIds", "array-contains", BOB_UID).get()
  );

  // Charlie cannot read, spoof, or accept Bob's call
  await assertFails(charlieDb.collection("calls").doc("call_hd_1").get());
  await assertFails(
    charlieDb.collection("calls").doc("call_hd_1").update({
      status: "accepted",
      answerSdp: "v=0\no=- 999 2 IN IP4 127.0.0.1\n",
      updatedAt: ts(),
    })
  );

  // Alice cannot self-accept her own call
  await assertFails(
    aliceDb.collection("calls").doc("call_hd_1").update({
      status: "accepted",
      answerSdp: "v=0\no=- 999 2 IN IP4 127.0.0.1\n",
      updatedAt: ts(),
    })
  );

  // Bob accepts the call with answerSdp
  await assertSucceeds(
    bobDb.collection("calls").doc("call_hd_1").update({
      status: "accepted",
      answerSdp: "v=0\no=- 67890 2 IN IP4 127.0.0.1\ns=-\nt=0 0\n",
      updatedAt: ts(),
    })
  );

  // Alice upgrades live call quality preset to Full HD 1080p
  await assertSucceeds(
    aliceDb.collection("calls").doc("call_hd_1").update({
      qualityPreset: "FHD_1080P",
      updatedAt: ts(),
    })
  );

  // Creating an ICE candidate before the parent /calls/{callId} doc exists MUST fail
  await assertFails(
    aliceDb
      .collection("calls")
      .doc("nonexistent_call_race")
      .collection("candidates")
      .doc("ice_early")
      .set({
        callId: "nonexistent_call_race",
        senderId: ALICE_UID,
        participantIds: [ALICE_UID, BOB_UID],
        sdpMid: "0",
        sdpMLineIndex: 0,
        candidate: "candidate:1 1 UDP 2122252543 192.168.1.10 54321 typ host",
        createdAt: ts(),
      })
  );

  // Both participants exchange ICE candidates (including order-independent participantIds); Charlie & invalid payloads fail
  await assertSucceeds(
    aliceDb
      .collection("calls")
      .doc("call_hd_1")
      .collection("candidates")
      .doc("ice_a1")
      .set({
        callId: "call_hd_1",
        senderId: ALICE_UID,
        participantIds: [ALICE_UID, BOB_UID],
        sdpMid: "0",
        sdpMLineIndex: 0,
        candidate: "candidate:1 1 UDP 2122252543 192.168.1.10 54321 typ host",
        createdAt: ts(),
      })
  );
  await assertSucceeds(
    bobDb
      .collection("calls")
      .doc("call_hd_1")
      .collection("candidates")
      .doc("ice_b1")
      .set({
        callId: "call_hd_1",
        senderId: BOB_UID,
        participantIds: [BOB_UID, ALICE_UID],
        sdpMid: "1",
        sdpMLineIndex: 1,
        candidate: "candidate:2 1 UDP 1686052607 203.0.113.10 55100 typ srflx",
        createdAt: ts(),
      })
  );
  await assertFails(
    aliceDb
      .collection("calls")
      .doc("call_hd_1")
      .collection("candidates")
      .doc("ice_empty")
      .set({
        callId: "call_hd_1",
        senderId: ALICE_UID,
        participantIds: [ALICE_UID, BOB_UID],
        sdpMid: "0",
        sdpMLineIndex: 0,
        candidate: "",
        createdAt: ts(),
      })
  );
  await assertFails(
    aliceDb
      .collection("calls")
      .doc("call_hd_1")
      .collection("candidates")
      .doc("ice_dup_participants")
      .set({
        callId: "call_hd_1",
        senderId: ALICE_UID,
        participantIds: [ALICE_UID, ALICE_UID],
        sdpMid: "0",
        sdpMLineIndex: 0,
        candidate: "candidate:3 1 UDP 2122252543 192.168.1.10 54322 typ host",
        createdAt: ts(),
      })
  );
  await assertSucceeds(
    bobDb
      .collection("calls")
      .doc("call_hd_1")
      .collection("candidates")
      .where("participantIds", "array-contains", BOB_UID)
      .get()
  );
  await assertFails(
    charlieDb
      .collection("calls")
      .doc("call_hd_1")
      .collection("candidates")
      .doc("ice_c1")
      .set({
        callId: "call_hd_1",
        senderId: CHARLIE_UID,
        participantIds: [ALICE_UID, BOB_UID],
        sdpMid: "0",
        sdpMLineIndex: 0,
        candidate: "candidate:1 1 UDP 2122252543 10.0.0.1 9999 typ host",
        createdAt: ts(),
      })
  );

  // Bob ends the call cleanly
  await assertSucceeds(
    bobDb.collection("calls").doc("call_hd_1").update({
      status: "ended",
      endedBy: BOB_UID,
      updatedAt: ts(),
    })
  );

  // Terminal state lock: cannot reopen an ended call
  await assertFails(
    aliceDb.collection("calls").doc("call_hd_1").update({
      qualityPreset: "HD_720P",
      updatedAt: ts(),
    })
  );
});

// --- 7. Social Messaging Options: Voice Notes, Pin/Mute, Pin Message, Forwarding & Block Users ---

test("Participants can send voice notes, pin/mute threads, pin messages, forward, and block users safely", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      readReceiptsEnabled: true,
      notificationsEnabled: true,
      blockedUserIds: [],
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Alice blocks Charlie
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      blockedUserIds: [CHARLIE_UID],
      updatedAt: ts(),
    })
  );
  // Bob cannot modify Alice's blockedUserIds
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      blockedUserIds: [],
      updatedAt: ts(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("conversations").doc("dm_social").set({
      type: "direct",
      title: "",
      createdBy: ALICE_UID,
      participantIds: [ALICE_UID, BOB_UID],
      participantNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob" },
      participantPhotos: {},
      lastMessageText: "",
      lastMessageSenderId: "",
      lastMessageSenderName: "",
      lastMessageType: "",
      lastMessageAt: ts(),
      unreadCounts: { [ALICE_UID]: 0, [BOB_UID]: 0 },
      pinnedBy: {},
      mutedBy: {},
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Voice note creation succeeds with valid audioBase64, fails when audioBase64 is empty
  await assertSucceeds(
    aliceDb
      .collection("conversations")
      .doc("dm_social")
      .collection("messages")
      .doc("voice_1")
      .set({
        conversationId: "dm_social",
        senderId: ALICE_UID,
        senderName: "Alice",
        participantIds: [ALICE_UID, BOB_UID],
        type: "audio",
        text: "Voice message (0:04)",
        audioBase64: "AAAAIGZ0eXBtcDQyAAAAAG1wNDJpc29tYXZjMQ==",
        audioDurationSec: 4,
        isForwarded: true,
        isEdited: false,
        createdAt: ts(),
        updatedAt: ts(),
      })
  );

  await assertFails(
    aliceDb
      .collection("conversations")
      .doc("dm_social")
      .collection("messages")
      .doc("voice_invalid")
      .set({
        conversationId: "dm_social",
        senderId: ALICE_UID,
        senderName: "Alice",
        participantIds: [ALICE_UID, BOB_UID],
        type: "audio",
        text: "Missing audio payload",
        audioBase64: "",
        audioDurationSec: 4,
        isEdited: false,
        createdAt: ts(),
        updatedAt: ts(),
      })
  );

  // Bob can pin/mute thread (Action 5) and pin a message (Action 6)
  await assertSucceeds(
    bobDb.collection("conversations").doc("dm_social").update({
      pinnedBy: { [BOB_UID]: true },
      mutedBy: { [BOB_UID]: true },
      updatedAt: ts(),
    })
  );
  await assertSucceeds(
    bobDb.collection("conversations").doc("dm_social").update({
      pinnedMessageId: "voice_1",
      pinnedMessagePreview: "🎤 Voice message (0:04)",
      pinnedMessageSenderName: "Alice",
      updatedAt: ts(),
    })
  );

  // Charlie cannot pin or mute Alice & Bob's chat
  await assertFails(
    charlieDb.collection("conversations").doc("dm_social").update({
      pinnedBy: { [CHARLIE_UID]: true },
      updatedAt: ts(),
    })
  );
});

// --- 8. Real-Time Typing Indicators ---

test("Participants can set and clear own typingBy timestamp without spoofing others; outsiders fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  const convRef = aliceDb.collection("conversations").doc("dm_typing");
  await assertSucceeds(
    convRef.set({
      type: "direct",
      title: "",
      createdBy: ALICE_UID,
      participantIds: [ALICE_UID, BOB_UID],
      participantNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob" },
      participantPhotos: {},
      lastMessageText: "",
      lastMessageSenderId: "",
      lastMessageSenderName: "",
      lastMessageType: "",
      lastMessageAt: ts(),
      unreadCounts: { [ALICE_UID]: 0, [BOB_UID]: 0 },
      typingBy: {},
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Alice marks herself typing
  const aliceTs = ts();
  await assertSucceeds(
    aliceDb.collection("conversations").doc("dm_typing").update({
      [`typingBy.${ALICE_UID}`]: aliceTs,
      updatedAt: ts(),
    })
  );

  // Bob marks himself typing concurrently without disturbing Alice's key
  await assertSucceeds(
    bobDb.collection("conversations").doc("dm_typing").update({
      [`typingBy.${BOB_UID}`]: ts(),
      updatedAt: ts(),
    })
  );

  // Bob CANNOT spoof or overwrite Alice's typing entry
  await assertFails(
    bobDb.collection("conversations").doc("dm_typing").update({
      [`typingBy.${ALICE_UID}`]: ts(),
      updatedAt: ts(),
    })
  );

  // Charlie (non-participant) CANNOT update typingBy
  await assertFails(
    charlieDb.collection("conversations").doc("dm_typing").update({
      [`typingBy.${CHARLIE_UID}`]: ts(),
      updatedAt: ts(),
    })
  );

  // Alice clears ONLY her own typing entry alongside sending a message (preserving Bob's timestamp)
  const snapAfterBob = await aliceDb.collection("conversations").doc("dm_typing").get();
  const preservedTyping = { ...(snapAfterBob.data().typingBy || {}) };
  delete preservedTyping[ALICE_UID];

  await assertSucceeds(
    aliceDb.collection("conversations").doc("dm_typing").update({
      lastMessageText: "Sent!",
      lastMessageSenderId: ALICE_UID,
      lastMessageSenderName: "Alice",
      lastMessageType: "text",
      lastMessageAt: ts(),
      unreadCounts: { [ALICE_UID]: 0, [BOB_UID]: 1 },
      typingBy: preservedTyping,
      updatedAt: ts(),
    })
  );

  // Bob clears ONLY his own typing entry directly (Action 7)
  await assertSucceeds(
    bobDb.collection("conversations").doc("dm_typing").update({
      typingBy: {},
      updatedAt: ts(),
    })
  );
});

// --- 9. Self-Healing Migration for Legacy Backend-Created User Docs ---

test("Owner can migrate legacy Telegram-backend /users/{uid} and /user_profiles/{uid} into canonical schema; others fail and immutability locks after migration", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore();
    await adminDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      phoneNumber: "+919706150227",
      displayName: "Member 0227",
      photoUrl: "",
      authProvider: "telegram_bot_otp",
      createdAt: ts(),
      updatedAt: ts(),
    });
    await adminDb.collection("user_profiles").doc(ALICE_UID).set({
      uid: ALICE_UID,
      displayName: "Member 0227",
      photoUrl: "",
      bio: "",
      phoneNumber: "+919706150227",
      isOnline: true,
      lastSeen: ts(),
      updatedAt: ts(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Bob CANNOT touch Alice's legacy documents
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "hijack@example.com",
      readReceiptsEnabled: true,
      notificationsEnabled: true,
      blockedUserIds: [],
      createdAt: ts(),
      updatedAt: ts(),
    })
  );
  await assertFails(
    bobDb.collection("user_profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Hijacked",
      handle: "hijacked",
      isOnline: true,
      isDiscoverable: true,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Alice CAN replace her legacy /users/{uid} with strict canonical PrivateUser schema
  const migratedCreatedAt = new Date(Date.now() - 5000);
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "+919706150227",
      readReceiptsEnabled: true,
      notificationsEnabled: true,
      blockedUserIds: [],
      createdAt: migratedCreatedAt,
      updatedAt: ts(),
    })
  );

  // Alice CAN replace her legacy /user_profiles/{uid} with strict canonical UserProfile schema (stripping phoneNumber PII)
  await assertSucceeds(
    aliceDb.collection("user_profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Member 0227",
      handle: "member0227",
      bio: "Hey there! I'm on Univers Chat.",
      photoUrl: "",
      statusVibe: "Available",
      isOnline: true,
      isDiscoverable: true,
      lastSeenAt: ts(),
      createdAt: migratedCreatedAt,
      updatedAt: ts(),
    })
  );

  // Once migrated, normal online presence update succeeds
  await assertSucceeds(
    aliceDb.collection("user_profiles").doc(ALICE_UID).update({
      isOnline: false,
      lastSeenAt: ts(),
      updatedAt: ts(),
    })
  );

  // Once migrated, createdAt is strictly immutable again
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: ts(),
      updatedAt: ts(),
    })
  );
  await assertFails(
    aliceDb.collection("user_profiles").doc(ALICE_UID).update({
      createdAt: ts(),
      updatedAt: ts(),
    })
  );
});

// --- 10. Contacts Hashes, E2EE Public Key, Read Receipts & Encrypted Messages ---

test("Owner can upload private contactHashes & publish phoneHash/e2eeIdentityKey; others cannot read private contactHashes", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const hashA = "a".repeat(64);
  const hashB = "b".repeat(64);
  const fakePubKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEtestPublicKeyBase64Value==";

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      phoneHash: hashA,
      contactHashes: [hashB],
      fcmTokens: ["fcm_token_alice_1"],
      readReceiptsEnabled: true,
      notificationsEnabled: true,
      blockedUserIds: [],
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Bob CANNOT read Alice's private contactHashes or fcmTokens
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());

  // Alice CAN publish phoneHash + e2eeIdentityKey on her public profile
  await assertSucceeds(
    aliceDb.collection("user_profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      handle: "alice",
      bio: "Encrypted chat",
      photoUrl: "",
      statusVibe: "Building",
      phoneHash: hashA,
      e2eeIdentityKey: fakePubKey,
      isOnline: true,
      isDiscoverable: true,
      lastSeenAt: ts(),
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Oversized contactHashes (> 500) rejected
  const oversized = Array.from({ length: 501 }, (_, i) => String(i).padStart(64, "0"));
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      contactHashes: oversized,
      updatedAt: ts(),
    })
  );
});

test("Participants can update ONLY their own lastReadAt read receipt timestamp and send/edit E2EE messages", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("conversations").doc("dm_e2ee_rr").set({
      type: "direct",
      title: "",
      topic: "",
      createdBy: ALICE_UID,
      participantIds: [ALICE_UID, BOB_UID],
      participantNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob" },
      participantPhotos: {},
      unreadCounts: { [ALICE_UID]: 0, [BOB_UID]: 1 },
      lastReadAt: {},
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Alice CAN send an E2EE encrypted text message
  await assertSucceeds(
    aliceDb.collection("conversations").doc("dm_e2ee_rr").collection("messages").doc("m_enc_1").set({
      conversationId: "dm_e2ee_rr",
      senderId: ALICE_UID,
      senderName: "Alice",
      participantIds: [ALICE_UID, BOB_UID],
      type: "text",
      text: "base64CiphertextPayload==",
      isEncrypted: true,
      e2eeVersion: 1,
      ephemeralPublicKey: "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEephemeralKey==",
      encryptionIv: "MTIzNDU2Nzg5MDEy",
      senderIdentityKey: "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEaliceIdKey==",
      isEdited: false,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Alice CAN edit her E2EE message with fresh ephemeral key & IV
  await assertSucceeds(
    aliceDb.collection("conversations").doc("dm_e2ee_rr").collection("messages").doc("m_enc_1").update({
      text: "updatedCiphertextPayload==",
      isEncrypted: true,
      e2eeVersion: 1,
      ephemeralPublicKey: "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEephemeralKey2==",
      encryptionIv: "OTg3NjU0MzIxMDEy",
      senderIdentityKey: "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEaliceIdKey==",
      isEdited: true,
      updatedAt: ts(),
    })
  );

  // Bob CAN stamp his own lastReadAt & reset his unread count
  await assertSucceeds(
    bobDb.collection("conversations").doc("dm_e2ee_rr").update({
      unreadCounts: { [ALICE_UID]: 0, [BOB_UID]: 0 },
      [`lastReadAt.${BOB_UID}`]: ts(),
      updatedAt: ts(),
    })
  );

  // Bob CANNOT spoof Alice's lastReadAt timestamp
  await assertFails(
    bobDb.collection("conversations").doc("dm_e2ee_rr").update({
      [`lastReadAt.${ALICE_UID}`]: ts(),
      updatedAt: ts(),
    })
  );

  // Charlie (non-participant) CANNOT update lastReadAt
  await assertFails(
    charlieDb.collection("conversations").doc("dm_e2ee_rr").update({
      [`lastReadAt.${CHARLIE_UID}`]: ts(),
      updatedAt: ts(),
    })
  );
});

// ============================================================================
// 12. Tapback Emoji Reactions (Self-only atomic toggles, switch & remove)
// ============================================================================
test("Participants can atomically add, switch, and remove only their own tapback emoji reaction; spoofing others fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("conversations").doc("dm_tapback").set({
      type: "direct",
      title: "",
      topic: "",
      createdBy: ALICE_UID,
      participantIds: [ALICE_UID, BOB_UID],
      participantNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob" },
      participantPhotos: {},
      unreadCounts: { [ALICE_UID]: 0, [BOB_UID]: 1 },
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  const msgRefAlice = aliceDb.collection("conversations").doc("dm_tapback").collection("messages").doc("m_tb_1");
  const msgRefBob = bobDb.collection("conversations").doc("dm_tapback").collection("messages").doc("m_tb_1");
  const msgRefCharlie = charlieDb.collection("conversations").doc("dm_tapback").collection("messages").doc("m_tb_1");

  await assertSucceeds(
    msgRefAlice.set({
      conversationId: "dm_tapback",
      senderId: ALICE_UID,
      senderName: "Alice",
      participantIds: [ALICE_UID, BOB_UID],
      type: "text",
      text: "Tapback test!",
      reactions: {},
      isEdited: false,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Alice adds ❤️ reaction atomically via dotted field path
  await assertSucceeds(
    msgRefAlice.update({
      [`reactions.${ALICE_UID}`]: "❤️",
      updatedAt: ts(),
    })
  );

  // Bob adds ❤️ reaction concurrently without overwriting Alice's reaction
  await assertSucceeds(
    msgRefBob.update({
      [`reactions.${BOB_UID}`]: "❤️",
      updatedAt: ts(),
    })
  );

  // Bob switches his own reaction from ❤️ to 🔥
  await assertSucceeds(
    msgRefBob.update({
      [`reactions.${BOB_UID}`]: "🔥",
      updatedAt: ts(),
    })
  );

  // Bob CANNOT spoof or overwrite Alice's reaction
  await assertFails(
    msgRefBob.update({
      [`reactions.${ALICE_UID}`]: "👎",
      updatedAt: ts(),
    })
  );

  // Bob CANNOT delete Alice's reaction
  await assertFails(
    msgRefBob.update({
      reactions: { [BOB_UID]: "🔥" },
      updatedAt: ts(),
    })
  );

  // Bob CANNOT set an empty emoji string
  await assertFails(
    msgRefBob.update({
      [`reactions.${BOB_UID}`]: "",
      updatedAt: ts(),
    })
  );

  // Bob CAN remove his own reaction while preserving Alice's
  const snapBeforeRemove = await msgRefBob.get();
  const withoutBob = { ...(snapBeforeRemove.data().reactions || {}) };
  delete withoutBob[BOB_UID];
  await assertSucceeds(
    msgRefBob.update({
      reactions: withoutBob,
      updatedAt: ts(),
    })
  );

  // Charlie (non-participant) CANNOT react
  await assertFails(
    msgRefCharlie.update({
      [`reactions.${CHARLIE_UID}`]: "👍",
      updatedAt: ts(),
    })
  );
});

// --- Voice messages with Firebase Storage audioUrl ---

test("Participants can send audio messages backed by Firebase Storage audioUrl; invalid URLs are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("conversations").doc("dm_voice_url").set({
      type: "direct",
      title: "",
      topic: "",
      createdBy: ALICE_UID,
      participantIds: [ALICE_UID, BOB_UID],
      participantNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob" },
      participantPhotos: {},
      unreadCounts: { [ALICE_UID]: 0, [BOB_UID]: 1 },
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  const storageAudioUrl =
    "https://firebasestorage.googleapis.com/v0/b/universchat.appspot.com/o/voice_messages%2Falice_123%2Fdm_voice_url%2Fnote.m4a?alt=media";

  // Valid HTTPS audioUrl voice message succeeds
  await assertSucceeds(
    aliceDb
      .collection("conversations")
      .doc("dm_voice_url")
      .collection("messages")
      .doc("m_voice_1")
      .set({
        conversationId: "dm_voice_url",
        senderId: ALICE_UID,
        senderName: "Alice",
        participantIds: [ALICE_UID, BOB_UID],
        type: "audio",
        text: storageAudioUrl,
        audioUrl: storageAudioUrl,
        audioDurationSec: 14,
        reactions: {},
        isEdited: false,
        createdAt: ts(),
        updatedAt: ts(),
      })
  );

  // Bob can read the voice message
  await assertSucceeds(
    bobDb
      .collection("conversations")
      .doc("dm_voice_url")
      .collection("messages")
      .doc("m_voice_1")
      .get()
  );

  // Audio message with non-HTTPS audioUrl fails
  await assertFails(
    aliceDb
      .collection("conversations")
      .doc("dm_voice_url")
      .collection("messages")
      .doc("m_voice_bad_scheme")
      .set({
        conversationId: "dm_voice_url",
        senderId: ALICE_UID,
        senderName: "Alice",
        participantIds: [ALICE_UID, BOB_UID],
        type: "audio",
        text: "http://insecure.example.com/voice.m4a",
        audioUrl: "http://insecure.example.com/voice.m4a",
        audioDurationSec: 5,
        reactions: {},
        isEdited: false,
        createdAt: ts(),
        updatedAt: ts(),
      })
  );

  // Audio message missing both audioUrl and audioBase64 fails
  await assertFails(
    aliceDb
      .collection("conversations")
      .doc("dm_voice_url")
      .collection("messages")
      .doc("m_voice_empty")
      .set({
        conversationId: "dm_voice_url",
        senderId: ALICE_UID,
        senderName: "Alice",
        participantIds: [ALICE_UID, BOB_UID],
        type: "audio",
        text: "Voice message",
        audioDurationSec: 5,
        reactions: {},
        isEdited: false,
        createdAt: ts(),
        updatedAt: ts(),
      })
  );
});




