package com.example.vista.artwork

import com.example.vista.data.local.PreferencesManager
import com.example.vista.data.local.ProfileScope
import com.example.vista.data.remote.firebase.FirebaseManager
import io.mockk.*
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class AvatarRepositoryTest {
    private val preferences = mockk<PreferencesManager>()
    private val profile = mockk<ProfileScope>()
    private val firebase = mockk<FirebaseManager>()
    private val stored = mutableMapOf<String, String>()
    private fun repository(owner: String): AvatarRepository {
        every { profile.current() } returns owner
        every { profile.owners } returns emptyFlow()
        every { preferences.avatarId(any()) } answers { flowOf(stored[firstArg()]) }
        coEvery { preferences.setAvatarId(any(), any(), any()) } coAnswers { stored[firstArg()] = secondArg() }
        coEvery { preferences.acknowledgeAvatarSync(any(), any()) } just Runs
        coEvery { firebase.updateAvatar(any(), any()) } just Runs
        return AvatarRepository(preferences, profile, firebase)
    }
    @Test fun guestSelectionPersistsStableIdWithoutAnyCloudWrite() = runTest {
        val repo = repository("")
        repo.save("female_01")
        assertEquals("female_01", stored[""])
        coVerify { preferences.setAvatarId("", "female_01", false) }
        coVerify(exactly = 0) { firebase.updateAvatar(any(), any()) }
    }
    @Test fun cloudFailureKeepsLocalSelectionAndDoesNotClaimItWasSynced() = runTest {
        val repo = repository("owner-a")
        coEvery { firebase.updateAvatar(any(), any()) } throws IOException("offline")
        val message = repo.save("male_01")
        assertEquals("male_01", stored["owner-a"])
        assertTrue(message.contains("Saved on this device"))
        coVerify(exactly = 0) { preferences.acknowledgeAvatarSync(any(), any()) }
    }
    @Test fun successfulSyncUpdatesOnlyTheActiveOwnersAvatarField() = runTest {
        val repo = repository("owner-a")
        repo.save("male_01")
        coVerify { firebase.updateAvatar("owner-a", "male_01") }
        coVerify { preferences.acknowledgeAvatarSync("owner-a", "male_01") }
        assertFalse(stored.containsKey("owner-b"))
    }
    @Test fun reservedAssistantAvatarCannotBePersistedAsAUserChoice() = runTest {
        val repo = repository("")
        try { repo.save("ai_assistant"); fail("Expected reserved avatar rejection") }
        catch (_: IllegalArgumentException) { }
        coVerify(exactly = 0) { preferences.setAvatarId(any(), any(), any()) }
    }
}
