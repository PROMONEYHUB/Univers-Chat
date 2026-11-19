# Univers Chat Cloud Functions

## `notifyContactsOnUserJoined`
Triggered automatically on Firestore `onDocumentCreated` at `user_profiles/{userId}` on database `ai-studio-android-pulsecha-e97cfd46-bc45-4fb2-80d8-677c380a25e2`.

### How it works
1. Reads the newly created user's SHA-256 `phoneHash` and `displayName`.
2. Queries `/users` where `contactHashes` (`array-contains`) matches `phoneHash`.
3. Skips the new user themselves, users who disabled notifications, and users who blocked the new user.
4. Sends push notifications via `admin.messaging().sendEachForMulticast(...)` in chunks of 500 tokens and prunes stale FCM tokens automatically.

### Deploy
```bash
cd functions
npm install
firebase deploy --only functions
```
