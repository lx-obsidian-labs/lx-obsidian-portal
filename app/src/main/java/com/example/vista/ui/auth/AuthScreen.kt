package com.example.vista.ui.auth

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.vista.navigation.Screen
import com.example.vista.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = (context as? Activity)

    LaunchedEffect(uiState.isUserLoggedIn, uiState.needsProfileSetup, uiState.isCheckingStatus, uiState.isGuest) {
        if (uiState.isCheckingStatus) return@LaunchedEffect

        if (uiState.isUserLoggedIn) {
            if (uiState.needsProfileSetup) {
                navController.navigate(Screen.SetupProfile.route) {
                    popUpTo(Screen.Auth.route) { inclusive = true }
                }
            } else {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Auth.route) { inclusive = true }
                }
            }
            return@LaunchedEffect
        }

        if (uiState.isGuest) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Auth.route) { inclusive = true }
            }
        }
    }

    if (uiState.isCheckingStatus) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Accent)
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        // Ambient Cinematic Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Background,
                        0.5f to AmbientRed.copy(alpha = 0.1f),
                        1f to Background
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated Logo - Premium Feel
            Surface(
                modifier = Modifier.size(120.dp),
                color = Color.White,
                shape = RoundedCornerShape(28.dp),
                shadowElevation = 12.dp
            ) {
                Box(
                    modifier = Modifier.background(
                        Brush.linearGradient(
                            colors = listOf(Color.White, Color(0xFFEEEEEE))
                        )
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "V",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-2).sp
                        ),
                        color = Accent
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "VISTA CINEMA",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp
                ),
                color = PrimaryText,
                textAlign = TextAlign.Center
            )

            Text(
                text = "PRO CINEMA SUITE",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 8.sp
                ),
                color = Accent,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CardSurface.copy(alpha = 0.6f),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    when (uiState.authMode) {
                        is AuthMode.SignIn -> SignInForm(
                            isLoading = uiState.isLoading,
                            onSignIn = { email, password ->
                                viewModel.signInWithEmail(email, password)
                            },
                            onSignUpClick = { viewModel.switchToSignUp() },
                            onForgotPasswordClick = { viewModel.switchToForgotPassword() }
                        )

                        is AuthMode.SignUp -> SignUpForm(
                            isLoading = uiState.isLoading,
                            onSignUp = { email, password ->
                                viewModel.signUpWithEmail(email, password)
                            },
                            onSignInClick = { viewModel.switchToSignIn() }
                        )

                        is AuthMode.ForgotPassword -> ForgotPasswordForm(
                            isLoading = uiState.isLoading,
                            onResetPassword = { email ->
                                viewModel.sendPasswordReset(email)
                            },
                            onBackClick = { viewModel.backFromForgotPassword() }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Guest access.
            TextButton(
                onClick = { viewModel.continueAsGuest() },
                enabled = !uiState.isLoading
            ) {
                Text(
                    text = "START FREE 3-DAY TRIAL",
                    color = Accent,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }

            Text(
                text = "A one-time 3-day free trial, no account needed. Sessions are limited to ${com.example.vista.util.GuestSessionManager.GUEST_PLAYBACK_CAP_SECONDS / 60} minutes and include ads.",
                color = MutedText.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )

            // Feedback sits directly under the form and is dismissible, rather than a small line of
            // coloured text the viewer has to notice.
            uiState.error?.let { message ->
                AuthBanner(
                    message = message,
                    isError = true,
                    onDismiss = { viewModel.clearError() }
                )
            }

            uiState.info?.let { message ->
                AuthBanner(
                    message = message,
                    isError = false,
                    onDismiss = { viewModel.clearInfo() }
                )
            }
        }
    }
}

@Composable
private fun SignInForm(
    isLoading: Boolean,
    onSignIn: (email: String, password: String) -> Unit,
    onSignUpClick: () -> Unit,
    onForgotPasswordClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val emailError = email.isNotBlank() && !isValidEmail(email)

    Column {
        AuthField(
            value = email,
            onValueChange = { email = it },
            label = "Email",
            keyboardType = KeyboardType.Email,
            imeAction = androidx.compose.ui.text.input.ImeAction.Next,
            isError = emailError,
            supportingText = if (emailError) "Enter a valid email address" else null
        )

        Spacer(modifier = Modifier.height(16.dp))

        AuthField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            keyboardType = KeyboardType.Password,
            imeAction = androidx.compose.ui.text.input.ImeAction.Done,
            isPassword = true,
            passwordVisible = passwordVisible,
            onPasswordVisibilityChange = { passwordVisible = it },
            onImeAction = { if (canSubmit(email, password)) onSignIn(email, password) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onForgotPasswordClick,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Forgot password?", color = Accent, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(24.dp))

        AuthSubmitButton(
            label = "Sign In",
            isLoading = isLoading,
            enabled = canSubmit(email, password),
            onClick = { onSignIn(email, password) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onSignUpClick, enabled = !isLoading) {
            Text(
                text = "Don't have an account? Sign Up",
                color = PrimaryText,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SignUpForm(
    isLoading: Boolean,
    onSignUp: (email: String, password: String) -> Unit,
    onSignInClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val emailInvalid = email.isNotBlank() && !isValidEmail(email)
    val passwordTooShort = password.isNotBlank() && password.length < MIN_PASSWORD_LENGTH
    val mismatch = confirmPassword.isNotBlank() && password != confirmPassword
    val canSubmit = isValidEmail(email) && !passwordTooShort && password == confirmPassword &&
        password.isNotEmpty() && !isLoading

    Column {
        AuthField(
            value = email,
            onValueChange = { email = it },
            label = "Email",
            keyboardType = KeyboardType.Email,
            imeAction = androidx.compose.ui.text.input.ImeAction.Next,
            isError = emailInvalid,
            supportingText = if (emailInvalid) "Enter a valid email address" else null
        )

        Spacer(modifier = Modifier.height(16.dp))

        AuthField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            keyboardType = KeyboardType.Password,
            imeAction = androidx.compose.ui.text.input.ImeAction.Next,
            isPassword = true,
            passwordVisible = passwordVisible,
            onPasswordVisibilityChange = { passwordVisible = it },
            isError = passwordTooShort,
            supportingText = if (passwordTooShort) {
                "Use at least $MIN_PASSWORD_LENGTH characters"
            } else {
                null
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        AuthField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = "Confirm Password",
            keyboardType = KeyboardType.Password,
            imeAction = androidx.compose.ui.text.input.ImeAction.Done,
            isPassword = true,
            passwordVisible = passwordVisible,
            onPasswordVisibilityChange = { passwordVisible = it },
            isError = mismatch,
            supportingText = if (mismatch) "Passwords do not match" else null,
            onImeAction = { if (canSubmit) onSignUp(email, password) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        AuthSubmitButton(
            label = "Create Account",
            isLoading = isLoading,
            enabled = canSubmit,
            onClick = { onSignUp(email, password) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onSignInClick, enabled = !isLoading) {
            Text(
                text = "Already have an account? Sign In",
                color = PrimaryText,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ForgotPasswordForm(
    isLoading: Boolean,
    onResetPassword: (email: String) -> Unit,
    onBackClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    val emailInvalid = email.isNotBlank() && !isValidEmail(email)

    Column {
        Text(
            text = "Enter your email and we'll send a reset link",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedText
        )

        Spacer(modifier = Modifier.height(24.dp))

        AuthField(
            value = email,
            onValueChange = { email = it },
            label = "Email",
            keyboardType = KeyboardType.Email,
            imeAction = androidx.compose.ui.text.input.ImeAction.Done,
            isError = emailInvalid,
            supportingText = if (emailInvalid) "Enter a valid email address" else null,
            onImeAction = { if (isValidEmail(email) && !isLoading) onResetPassword(email) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        AuthSubmitButton(
            label = "Send Reset Link",
            isLoading = isLoading,
            enabled = isValidEmail(email),
            onClick = { onResetPassword(email) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onBackClick, enabled = !isLoading) {
            Text(
                text = "Back to Sign In",
                color = PrimaryText,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private const val MIN_PASSWORD_LENGTH = 6

private fun isValidEmail(value: String): Boolean =
    android.util.Patterns.EMAIL_ADDRESS.matcher(value.trim()).matches()

private fun canSubmit(email: String, password: String): Boolean =
    isValidEmail(email) && password.length >= MIN_PASSWORD_LENGTH

/** Shared text field with validation, an optional password reveal, and an IME action. */
@Composable
private fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    imeAction: androidx.compose.ui.text.input.ImeAction,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordVisibilityChange: (Boolean) -> Unit = {},
    isError: Boolean = false,
    supportingText: String? = null,
    onImeAction: () -> Unit = {}
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        isError = isError,
        visualTransformation = if (isPassword && !passwordVisible) {
            PasswordVisualTransformation()
        } else {
            androidx.compose.ui.text.input.VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
            onDone = { onImeAction() },
            onNext = { onImeAction() }
        ),
        supportingText = supportingText?.let {
            { Text(it, color = Accent, style = MaterialTheme.typography.labelSmall) }
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { onPasswordVisibilityChange(!passwordVisible) }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = MutedText
                    )
                }
            }
        } else {
            null
        },
        shape = RoundedCornerShape(16.dp)
    )
}

/** Primary call to action with an inline progress indicator while a request is in flight. */
@Composable
private fun AuthSubmitButton(
    label: String,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        enabled = enabled && !isLoading,
        colors = ButtonDefaults.buttonColors(containerColor = Accent),
        shape = RoundedCornerShape(16.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = PrimaryText,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(label, fontWeight = FontWeight.Black)
    }
}

/** Dismissible message banner used for both errors and confirmations. */
@Composable
private fun AuthBanner(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        color = if (isError) Accent.copy(alpha = 0.14f) else RaisedSurface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isError) Icons.Outlined.ErrorOutline else Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = if (isError) Accent else SecondaryText,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                color = PrimaryText,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Dismiss message",
                    tint = MutedText,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

