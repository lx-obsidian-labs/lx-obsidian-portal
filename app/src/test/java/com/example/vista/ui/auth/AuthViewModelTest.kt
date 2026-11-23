@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
package com.example.vista.ui.auth

import com.example.vista.data.local.PreferencesManager
import com.example.vista.data.remote.firebase.FirebaseManager
import com.example.vista.domain.model.Viewer
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

class AuthViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val firebase = mockk<FirebaseManager>()
    private val preferences = mockk<PreferencesManager>()
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        every { firebase.isUserLoggedIn() } returns false
        every { firebase.signOut() } just Runs
        coEvery { preferences.setGuestMode(any()) } just Runs
    }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun normalizedSignupAndDuplicateSubmissionsUseOneRequestAndRetainDraftOnFailure() = runTest(dispatcher) {
        coEvery { firebase.createUserWithEmail(any(), any()) } coAnswers { delay(500); Result.failure(IOException("private secret")) }
        val vm = AuthViewModel(firebase, mockk(), preferences)
        advanceUntilIdle()
        vm.signUpWithEmail(" person@example.com ", "  secret  ")
        vm.signUpWithEmail(" person@example.com ", "  secret  ")
        advanceUntilIdle()
        coVerify(exactly = 1) { firebase.createUserWithEmail("person@example.com", "  secret  ") }
        assertEquals("person@example.com", vm.draft.value.email)
        assertEquals("  secret  ", vm.draft.value.password)
        assertFalse(vm.uiState.value.error!!.contains("secret"))
        assertFalse(vm.uiState.value.isLoading)
    }
    @Test fun successfulAccountWithFailedProfileLookupRetriesOnlyTheProfile() = runTest(dispatcher) {
        var loggedIn = false
        every { firebase.isUserLoggedIn() } answers { loggedIn }
        coEvery { firebase.createUserWithEmail(any(), any()) } coAnswers { loggedIn = true; Result.success(Unit) }
        coEvery { firebase.getViewerProfileOrThrow() } throws IOException("offline")
        val vm = AuthViewModel(firebase, mockk(), preferences)
        advanceUntilIdle()
        vm.signUpWithEmail("person@example.com", "password123")
        advanceUntilIdle()
        assertEquals(AuthOperation.PROFILE_LOOKUP, vm.uiState.value.failedOperation)
        assertFalse(vm.uiState.value.needsProfileSetup)
        coEvery { firebase.getViewerProfileOrThrow() } returns Viewer(uid = "owner", alias = "Viewer")
        vm.retryLast()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isUserLoggedIn)
        coVerify(exactly = 1) { firebase.createUserWithEmail(any(), any()) }
    }
    @Test fun invalidInputsNeverReachFirebaseAndSignOutDoesNotLeaveASpinner() = runTest(dispatcher) {
        val vm = AuthViewModel(firebase, mockk(), preferences)
        advanceUntilIdle()
        vm.signUpWithEmail("invalid", "short")
        assertNotNull(vm.uiState.value.problem)
        coVerify(exactly = 0) { firebase.createUserWithEmail(any(), any()) }
        vm.updatePassword("temporary password")
        vm.signOut()
        assertFalse(vm.uiState.value.isCheckingStatus)
        assertEquals("", vm.draft.value.password)
    }
}
