package com.example.vista.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vista.ui.theme.ErrorAccent
import com.example.vista.ui.theme.ThemeColors
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** How a [VistaMessenger] message should be coloured. */
enum class VistaMessageKind { SUCCESS, ERROR, INFO }

/**
 * App-wide surface for transient confirmations and failures.
 *
 * Replaces `Toast.makeText`, which rendered a system popup that ignored the theme entirely: white
 * system text on the platform's own grey, dropped over a black cinematic UI. Failures also had
 * nowhere to use the theme's `error` colour, because nothing in the app referenced
 * `colorScheme.error` at all.
 *
 * A single instance is shared so the host can live in the root `Scaffold` while any screen raises a
 * message - adding a `SnackbarHost` per screen would let two overlap at once, which reads as a bug.
 *
 * Assumes one message at a time. The kind is read from [kind] when the host renders, so a burst of
 * concurrent calls would colour them all by the most recent kind. Every call site is a user action
 * that replaces any message already on screen, so this does not come up in practice.
 */
@Singleton
class VistaMessenger @Inject constructor() {
    internal val hostState = SnackbarHostState()
    internal val kind = MutableStateFlow(VistaMessageKind.INFO)

    suspend fun show(message: String, kind: VistaMessageKind = VistaMessageKind.INFO) {
        this.kind.value = kind
        hostState.showSnackbar(message, duration = SnackbarDuration.Short)
    }
}

/**
 * Renders [messenger]'s queue. Place once, in the root `Scaffold`.
 *
 * `containerColor` is pulled from [ThemeColors] rather than a fixed value so the neutral variant
 * stays legible in the light and OLED themes.
 */@Composable
fun VistaSnackbarHost(messenger: VistaMessenger, modifier: Modifier = Modifier) {
    val kind by messenger.kind.collectAsStateWithLifecycle()

    SnackbarHost(
        hostState = messenger.hostState,
        modifier = modifier,
    ) { data ->
        // Error and success carry a saturated fill, so they take dark text; the neutral variant
        // follows the surface colour and needs the opposite.
        val onFill = Color(0xFF1A1A1A)
        val containerColor = when (kind) {
            VistaMessageKind.ERROR -> ErrorAccent
            VistaMessageKind.SUCCESS -> Color(0xFF2E7D32)
            VistaMessageKind.INFO -> ThemeColors.raisedSurface
        }
        val contentColor = when (kind) {
            VistaMessageKind.ERROR, VistaMessageKind.SUCCESS -> onFill
            VistaMessageKind.INFO -> ThemeColors.primaryText
        }

        Snackbar(
            containerColor = containerColor,
            contentColor = contentColor,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = data.visuals.message, color = contentColor)
        }
    }
}

/**
 * Hilt entry point for reaching the singleton from a composable.
 *
 * `hiltViewModel()` only works for ViewModels, and threading the messenger through every screen as
 * a parameter would touch a lot of call sites for a UI concern. This keeps it a one-liner at the
 * point of use.
 */
val LocalVistaMessenger = staticCompositionLocalOf<VistaMessenger?> { null }

/**
 * The app-wide messenger, or null where none was provided (TV activities run their own screens and
 * never raise messages).
 *
 * Nullable on purpose: a non-null local with a throwing default would crash any preview or TV
 * surface that forgot to provide one, and a missing snackbar is never worth a crash.
 */
@Composable
fun rememberVistaMessenger(): VistaMessenger? = LocalVistaMessenger.current
