package com.example.vista.artwork

import android.content.Intent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.vista.support.*
import com.example.vista.ui.auth.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class SupportUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun authenticationFailureIsAnActionableDialogNotABottomBanner() {
        var retried = false
        val problem = AuthProblem("Check your connection", "Reconnect and try again.", AuthRecovery.RETRY)
        compose.setContent { MaterialTheme { AuthFeedbackDialog(AuthUiState(error = problem.message, problem = problem), {}, { retried = true }) } }
        compose.onNodeWithText("Check your connection").assertExists()
        compose.onNodeWithText("Try again").performClick()
        assertTrue(retried)
    }
    @Test fun emailDraftContainsTheReviewedRecipientSubjectAndBody() {
        val email = SupportReport.build(SupportCategory.PLAYBACK, "A synthetic QA report.", "1.0.12", false, null)
        val intent = SupportActions.emailIntent(email)
        assertEquals(Intent.ACTION_SENDTO, intent.action)
        assertEquals("mailto", intent.data?.scheme)
        assertTrue(intent.data!!.schemeSpecificPart.startsWith(VistaContacts.SUPPORT))
        assertEquals(email.subject, intent.getStringExtra(Intent.EXTRA_SUBJECT))
        assertEquals(email.body, intent.getStringExtra(Intent.EXTRA_TEXT))
    }
}
