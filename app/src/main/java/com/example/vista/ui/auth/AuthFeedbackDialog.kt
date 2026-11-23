package com.example.vista.ui.auth

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.example.vista.ui.theme.*

/** Window-level feedback remains visible above the keyboard and any form scroll position. */
@Composable
fun AuthFeedbackDialog(state: AuthUiState, onDismiss: () -> Unit, onRecover: () -> Unit) {
    val message = state.error ?: return
    val problem = state.problem ?: AuthProblem("Please check this", message)
    AlertDialog(onDismissRequest = onDismiss, containerColor = CardSurface,
        title = { Text(problem.title, color = PrimaryText, fontWeight = FontWeight.Bold) },
        text = { Text(message, color = SecondaryText) },
        confirmButton = { TextButton(onClick = onRecover) {
            Text(when (problem.recovery) {
                AuthRecovery.RETRY -> "Try again"
                AuthRecovery.SIGN_IN -> "Sign in instead"
                AuthRecovery.RESET_PASSWORD -> "Reset password"
                AuthRecovery.REVIEW -> "Review details"
            }, color = Accent)
        } },
        dismissButton = { if (problem.recovery != AuthRecovery.REVIEW) TextButton(onClick = onDismiss) { Text("Close", color = PrimaryText) } })
}
