@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.example.vista.ui.onboarding

import com.example.vista.data.local.PreferencesManager
import com.example.vista.data.remote.firebase.FirebaseManager
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

class OnboardingViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val preferences = mockk<PreferencesManager>(relaxed = true)
    private val firebase = mockk<FirebaseManager>(relaxed = true)
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        every { preferences.onboardingSeenAt } returns flowOf(0L)
        every { preferences.displayName } returns flowOf("Nia")
    }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun navigationWaitsForDurableCompletionAndRepeatedTapsCannotDoubleSubmit() = runTest(dispatcher) {
        var marked = false
        coEvery { preferences.setDisplayName("Mia") } coAnswers { delay(100) }
        coEvery { preferences.markOnboardingSeen() } coAnswers { delay(100); marked = true }
        val viewModel = OnboardingViewModel(preferences, firebase)
        runCurrent()
        var callbacks = 0
        viewModel.finish(" Mia ") { assertTrue(marked); callbacks++ }
        viewModel.finish("Second tap") { callbacks++ }
        runCurrent()
        assertEquals(0, callbacks)
        assertTrue(viewModel.uiState.value.isFinishing)
        advanceUntilIdle()
        assertEquals(1, callbacks)
        coVerify(exactly = 1) { preferences.setDisplayName("Mia") }
        coVerify(exactly = 1) { preferences.markOnboardingSeen() }
    }

    @Test fun saveFailureLeavesTourOpenWithRetryableErrorAndDoesNotMarkCompletion() = runTest(dispatcher) {
        coEvery { preferences.setDisplayName(any()) } throws IOException("Storage unavailable")
        val viewModel = OnboardingViewModel(preferences, firebase)
        runCurrent()
        var navigated = false
        viewModel.finish("Mia") { navigated = true }
        advanceUntilIdle()
        assertFalse(navigated)
        assertNotNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isFinishing)
        coVerify(exactly = 0) { preferences.markOnboardingSeen() }
    }

    @Test fun existingCompletionIsPreservedAsReturningViewer() = runTest(dispatcher) {
        every { preferences.onboardingSeenAt } returns flowOf(System.currentTimeMillis() - 86_400_000)
        val viewModel = OnboardingViewModel(preferences, firebase)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isWelcomeBack)
        assertEquals("Nia", viewModel.uiState.value.suggestedName)
        coVerify(exactly = 0) { preferences.markOnboardingSeen() }
    }
}
