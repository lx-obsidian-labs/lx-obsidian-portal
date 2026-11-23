package com.example.vista.ui.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.TimeoutCancellationException
import java.io.IOException

enum class AuthOperation { SIGN_IN, SIGN_UP, RESET_PASSWORD, PROFILE_LOOKUP, PROFILE_WRITE }
enum class AuthRecovery { REVIEW, RETRY, SIGN_IN, RESET_PASSWORD }
data class AuthProblem(val title: String, val message: String, val recovery: AuthRecovery = AuthRecovery.REVIEW)
data class AuthFormDraft(val email: String = "", val password: String = "", val confirmation: String = "")

object AuthInputPolicy {
    const val MIN_PASSWORD_LENGTH = 6
    fun email(value: String) = value.trim()
    fun validEmail(value: String): Boolean = email(value).let {
        it.length <= 254 && it.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
    }
    fun signupPasswordError(password: String): String? = when {
        password.length < MIN_PASSWORD_LENGTH -> "Use at least $MIN_PASSWORD_LENGTH characters."
        password.isBlank() -> "Your password cannot contain only spaces."
        else -> null
    }
}

/** UI-safe explanations; never render raw SDK exception messages, request URLs or credentials. */
object AuthFeedback {
    fun fromCode(code: String?, operation: AuthOperation): AuthProblem = when (code) {
        "ERROR_EMAIL_ALREADY_IN_USE", "EMAIL_EXISTS" -> AuthProblem("An account already uses this email",
            "You can sign in with this email, or use Forgot password to recover access. Your email is still filled in.", AuthRecovery.SIGN_IN)
        "ERROR_INVALID_EMAIL", "INVALID_EMAIL" -> AuthProblem("Check your email address", "Enter a complete email address, such as name@example.com.")
        "ERROR_WEAK_PASSWORD", "WEAK_PASSWORD" -> AuthProblem("Choose a stronger password",
            "Use at least 6 characters. If it is still rejected, try a longer password with a mix of letters, numbers and a symbol.")
        "ERROR_INVALID_CREDENTIAL" -> if (operation == AuthOperation.SIGN_UP) AuthProblem("Account verification couldn't finish",
            "Check your connection and retry. If this keeps happening, update Vista Watch or contact technical support.", AuthRecovery.RETRY)
            else AuthProblem("We couldn't sign you in", "Check your email and password, or reset your password.", AuthRecovery.RESET_PASSWORD)
        "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND", "INVALID_LOGIN_CREDENTIALS" ->
            AuthProblem("We couldn't sign you in", "Check your email and password. If you're unsure of your password, you can reset it.", AuthRecovery.RESET_PASSWORD)
        "ERROR_USER_DISABLED" -> AuthProblem("This account is unavailable", "Contact Vista Watch support at ${com.example.vista.support.VistaContacts.SUPPORT} for help with this account.")
        "ERROR_OPERATION_NOT_ALLOWED", "OPERATION_NOT_ALLOWED" -> AuthProblem("Email sign-in is temporarily unavailable",
            "This sign-in method is unavailable for the app. Please try again later or contact Vista Watch support.")
        "ERROR_APP_NOT_AUTHORIZED", "ERROR_INVALID_API_KEY" -> AuthProblem("This build couldn't access sign-in",
            "Please install the latest official update or contact Vista Watch technical support.")
        "ERROR_TOO_MANY_REQUESTS", "TOO_MANY_ATTEMPTS_TRY_LATER" -> AuthProblem("Please wait before trying again",
            "There have been too many attempts. Wait a few minutes, then try again.")
        else -> when (operation) {
            AuthOperation.PROFILE_LOOKUP -> AuthProblem("You're signed in, but your profile couldn't load",
                "Please check your connection and retry. You don't need to create another account.", AuthRecovery.RETRY)
            AuthOperation.PROFILE_WRITE -> AuthProblem("Your profile couldn't be saved",
                "Your account already exists. Check your connection and retry this step; don't create another account.", AuthRecovery.RETRY)
            AuthOperation.RESET_PASSWORD -> AuthProblem("The reset request couldn't be completed", "Please check your connection and try again.", AuthRecovery.RETRY)
            AuthOperation.SIGN_UP -> AuthProblem("Your account request couldn't be completed",
                "Please try again. If you already created this account, sign in instead.", AuthRecovery.RETRY)
            else -> AuthProblem("Sign-in couldn't be completed", "Please check your connection and try again.", AuthRecovery.RETRY)
        }
    }

    fun from(error: Throwable, operation: AuthOperation): AuthProblem {
        if (error is TimeoutCancellationException && operation == AuthOperation.SIGN_UP) return AuthProblem(
            "We couldn't confirm account creation", "The request timed out. Try signing in with the same email and password before creating another account.", AuthRecovery.SIGN_IN)
        if (error is FirebaseNetworkException || error is IOException || error is TimeoutCancellationException) {
            val profile = operation == AuthOperation.PROFILE_LOOKUP || operation == AuthOperation.PROFILE_WRITE
            return AuthProblem(if (profile) "Your account is ready; this step needs a connection" else "Check your connection",
                if (profile) "Please reconnect and retry. You don't need to create another account."
                else "We couldn't reach the sign-in service. Reconnect to the internet and try again.", AuthRecovery.RETRY)
        }
        if (error is FirebaseTooManyRequestsException) return fromCode("ERROR_TOO_MANY_REQUESTS", operation)
        if (error is FirebaseFirestoreException && error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) return AuthProblem(
            "Your account exists, but profile access was denied",
            "Please contact Vista Watch support at ${com.example.vista.support.VistaContacts.SUPPORT}. Creating another account won't fix this profile-access problem.")
        return fromCode((error as? FirebaseAuthException)?.errorCode, operation)
    }
}
