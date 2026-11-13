package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class MessagingRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun syncCurrentUserProfile_createsPrivateAndPublicDocs_andAllowsOwnerRead() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val user = checkNotNull(auth.currentUser)
        val repo = MessagingRepository(firestore, auth)

        val syncResult = withTimeout(DEFAULT_TIMEOUT_MS) { repo.syncCurrentUserProfile(user) }
        assertTrue("Sync failed: ${syncResult.exceptionOrNull()}", syncResult.isSuccess)

        val privateUser = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observePrivateUser(uid).first { it != null }
        }
        assertNotNull(privateUser)
        assertEquals(uid, privateUser?.userId)

        val publicProfile = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeUserProfile(uid).first { it != null }
        }
        assertNotNull(publicProfile)
        assertTrue(publicProfile?.isDiscoverable == true)
    }

    @Test
    fun observePrivateUser_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceUser = checkNotNull(auth.currentUser)
        val aliceRepo = MessagingRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.syncCurrentUserProfile(aliceUser).getOrThrow() }

        signInTestUser(BOB_EMAIL)
        val bobRepo = MessagingRepository(firestore, auth)
        try {
            withTimeout(FLOW_TIMEOUT_MS) { bobRepo.observePrivateUser(aliceUid).first() }
            fail("Expected PERMISSION_DENIED when reading another user's private doc")
        } catch (e: Exception) {
            val firestoreException = checkNotNull(e.unwrapFirestoreException()) {
                "Expected FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
        }
    }

    @Test
    fun directChatAndMessaging_endToEndFlow_worksForParticipantsAndDeniesOutsiders() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = MessagingRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.syncCurrentUserProfile(checkNotNull(auth.currentUser)).getOrThrow()
        }
        val aliceProfile = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeUserProfile(aliceUid).first { it != null }!!
        }

        val bobUid = signInTestUser(BOB_EMAIL)
        val bobRepo = MessagingRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.syncCurrentUserProfile(checkNotNull(auth.currentUser)).getOrThrow()
        }
        val bobProfile = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeUserProfile(bobUid).first { it != null }!!
        }

        // Alice starts a DM with Bob
        signInTestUser(ALICE_EMAIL)
        val convId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.getOrCreateDirectConversation(aliceProfile, bobProfile).getOrThrow()
        }
        val conv = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeConversation(convId).first { it != null }!!
        }

        // Alice sends a text message
        val uniqueText = "Hey Bob ${UUID.randomUUID()}"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.sendTextMessage(conv, aliceProfile, uniqueText).getOrThrow()
        }

        // Bob observes the message in real time and reacts
        signInTestUser(BOB_EMAIL)
        val bobMessages = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeMessages(convId, bobUid).first { list -> list.any { it.text == uniqueText } }
        }
        val targetMsg = bobMessages.first { it.text == uniqueText }
        withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.toggleReaction(convId, targetMsg, "🔥").getOrThrow()
        }

        // Charlie (outsider) sees zero messages on his own filtered query and gets PERMISSION_DENIED on Alice's query or direct doc read
        val charlieUid = signInTestUser(CHARLIE_EMAIL)
        val charlieRepo = MessagingRepository(firestore, auth)
        val charlieVisible = withTimeout(FLOW_TIMEOUT_MS) {
            charlieRepo.observeMessages(convId, charlieUid).first()
        }
        assertTrue(charlieVisible.isEmpty())

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                charlieRepo.observeMessages(convId, aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED when Charlie queries Alice's messages")
        } catch (e: Exception) {
            val firestoreException = checkNotNull(e.unwrapFirestoreException()) {
                "Expected FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
        }

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                charlieRepo.observeConversation(convId).first()
            }
            fail("Expected PERMISSION_DENIED when Charlie reads Alice & Bob's conversation")
        } catch (e: Exception) {
            val firestoreException = checkNotNull(e.unwrapFirestoreException()) {
                "Expected FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
        }
    }

    @Test
    fun groupChat_createPollAndVote_succeedsForParticipants() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = MessagingRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.syncCurrentUserProfile(checkNotNull(auth.currentUser)).getOrThrow()
        }
        val aliceProfile = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeUserProfile(aliceUid).first { it != null }!!
        }

        val bobUid = signInTestUser(BOB_EMAIL)
        val bobRepo = MessagingRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.syncCurrentUserProfile(checkNotNull(auth.currentUser)).getOrThrow()
        }
        val bobProfile = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeUserProfile(bobUid).first { it != null }!!
        }

        signInTestUser(ALICE_EMAIL)
        val groupId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createGroupConversation(
                title = "Weekend Trip ${UUID.randomUUID().toString().take(6)}",
                topic = "Planning",
                currentProfile = aliceProfile,
                selectedMembers = listOf(bobProfile)
            ).getOrThrow()
        }
        val groupConv = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeConversation(groupId).first { it != null }!!
        }

        val pollQuestion = "Where to eat?"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.sendPollMessage(groupConv, aliceProfile, pollQuestion, listOf("Tacos", "Ramen")).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val messages = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeMessages(groupId, bobUid).first { list -> list.any { it.pollQuestion == pollQuestion } }
        }
        val pollMsg = messages.first { it.pollQuestion == pollQuestion }
        withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.votePoll(groupId, pollMsg, 1).getOrThrow()
        }
        val updated = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeMessages(groupId, bobUid).first { list ->
                list.any { it.id == pollMsg.id && it.pollVotes[bobUid] == "1" }
            }
        }
        assertFalse(updated.isEmpty())
    }

    @Test
    fun observeConversations_unauthenticatedUser_throwsIllegalStateOrPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = MessagingRepository(firestore, auth)
        try {
            withTimeout(FLOW_TIMEOUT_MS) { repo.observeConversations("someone").first() }
            fail("Expected failure when unauthenticated")
        } catch (e: Exception) {
            assertTrue(
                e is IllegalStateException || e.unwrapFirestoreException()?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
            )
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice_pulse@test.com"
        const val BOB_EMAIL = "bob_pulse@test.com"
        const val CHARLIE_EMAIL = "charlie_pulse@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3500L
    }
}
