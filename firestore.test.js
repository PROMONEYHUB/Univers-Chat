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

  // Cross-user update fails
  await assertFails(
    bobDb.collection("user_profiles").doc(ALICE_UID).update({
      displayName: "Hacked Alice",
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
