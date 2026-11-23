package com.example.vista.ui.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import org.junit.Assert.*
import org.junit.Test
import io.mockk.*

class AuthFeedbackTest {
    @Test fun duplicateEmailOffersSignInWithoutExposingSdkDetails() {
        val failure = mockk<FirebaseAuthException> { every { errorCode } returns "ERROR_EMAIL_ALREADY_IN_USE" }
        val problem = AuthFeedback.from(failure, AuthOperation.SIGN_UP)
        assertEquals(AuthRecovery.SIGN_IN, problem.recovery)
        assertFalse(problem.message.contains("secret"))
    }
    @Test fun profileNetworkFailureExplainsThatTheAccountAlreadyExists() {
        val problem = AuthFeedback.from(mockk<FirebaseNetworkException>(), AuthOperation.PROFILE_WRITE)
        assertTrue(problem.message.contains("don't need to create another account"))
        assertEquals(AuthRecovery.RETRY, problem.recovery)
    }
    @Test fun emailIsNormalizedButPasswordWhitespaceIsNotSilentlyChanged() {
        assertEquals("person@example.com", AuthInputPolicy.email(" person@example.com "))
        assertTrue(AuthInputPolicy.validEmail(" person@example.com "))
        assertFalse(AuthInputPolicy.validEmail("person@"))
        assertNotNull(AuthInputPolicy.signupPasswordError("short"))
        assertNull(AuthInputPolicy.signupPasswordError("  secret  "))
    }
}
