package com.example.vista.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.vista.navigation.Screen
import com.example.vista.ui.components.BrandAttribution
import com.example.vista.ui.components.VistaMessageKind
import com.example.vista.ui.components.rememberVistaMessenger
import kotlinx.coroutines.launch
import com.example.vista.ui.theme.*

@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val messenger = rememberVistaMessenger()
    val scope = rememberCoroutineScope()
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showPasswordResetDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.profileSaved.collect { success ->
            messenger?.show(
                if (success) "Profile updated" else "Failed to update profile",
                if (success) VistaMessageKind.SUCCESS else VistaMessageKind.ERROR
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.passwordResetResult.collect { success ->
            messenger?.show(
                if (success) "Password reset email sent" else "Failed to send reset email",
                if (success) VistaMessageKind.SUCCESS else VistaMessageKind.ERROR
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .background(
                    Brush.verticalGradient(
                        // Brand red, not the purple used on the onboarding and premium surfaces.
                        // Profile is where a returning viewer looks first, so it should carry the
                        // same accent as the rest of the app rather than a one-off colour.
                        colors = listOf(Accent.copy(alpha = 0.15f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            ProfileHeader(uiState.viewer)

            Spacer(modifier = Modifier.height(24.dp))

            StatisticsRow(
                watchlistCount = uiState.watchlistCount,
                historyCount = uiState.historyCount
            )

            Spacer(modifier = Modifier.height(32.dp))

            SubscriptionCard()

            Spacer(modifier = Modifier.height(32.dp))

            ProfileSection("PREFERENCES") {
                ProfileMenuItem(
                    icon = Icons.Outlined.Settings,
                    title = "Settings",
                    subtitle = "Playback, data, and visual style",
                    onClick = { navController.navigate(Screen.Settings.createRoute("MAIN")) }
                )
                ProfileMenuItem(
                    icon = Icons.Outlined.History,
                    title = "Watch History",
                    subtitle = "Resume where you left off",
                    onClick = { navController.navigate(Screen.WatchHistory.route) }
                )
                ProfileMenuItem(
                    icon = Icons.Outlined.Insights,
                    title = "Your Stats",
                    subtitle = "Time watched, streaks and totals",
                    onClick = { navController.navigate(Screen.Stats.route) }
                )
                ProfileMenuItem(
                    icon = Icons.Outlined.Notifications,
                    title = "Notifications",
                    subtitle = "New episodes and releases",
                    onClick = { navController.navigate(Screen.Settings.createRoute("NOTIFICATIONS")) }
                )
                ProfileMenuItem(
                    icon = Icons.Outlined.Storage,
                    title = "Data & Storage",
                    subtitle = "Usage and cache",
                    onClick = { navController.navigate(Screen.Settings.createRoute("DATA_STORAGE")) }
                )
                ProfileMenuItem(
                    icon = Icons.Outlined.Palette,
                    title = "Appearance",
                    subtitle = "Theme and visual style",
                    onClick = { navController.navigate(Screen.Settings.createRoute("APPEARANCE")) }
                )
            }

            ProfileSection("ACCOUNT") {
                ProfileMenuItem(
                    icon = Icons.Outlined.AccountCircle,
                    title = "Edit Profile",
                    onClick = { showEditProfileDialog = true }
                )
                ProfileMenuItem(
                    icon = Icons.Outlined.Lock,
                    title = "Security & Password",
                    onClick = { showPasswordResetDialog = true }
                )
                ProfileMenuItem(
                    icon = Icons.AutoMirrored.Outlined.Logout,
                    title = "Sign Out",
                    titleColor = Accent,
                    onClick = { showSignOutDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            BrandAttribution()

            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            currentAlias = uiState.viewer?.alias ?: "",
            isSaving = uiState.isSaving,
            onDismiss = { showEditProfileDialog = false },
            onSave = { alias ->
                viewModel.updateProfileAlias(alias)
                showEditProfileDialog = false
            }
        )
    }

    if (showPasswordResetDialog) {
        val email = uiState.viewer?.email ?: ""
        PasswordResetDialog(
            email = email,
            onDismiss = { showPasswordResetDialog = false },
            onConfirm = { resetEmail ->
                viewModel.sendPasswordReset(resetEmail)
                showPasswordResetDialog = false
            }
        )
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out?") },
            text = { Text("You will be signed out and returned to the login screen.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.signOut()
                    showSignOutDialog = false
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }) {
                    Text("Sign Out", color = Accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel", color = PrimaryText)
                }
            },
            containerColor = RaisedSurface
        )
    }
}

@Composable
private fun EditProfileDialog(
    currentAlias: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var alias by remember { mutableStateOf(currentAlias) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column {
                Text("Display Name", color = MutedText, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = alias,
                    onValueChange = { alias = it },
                    placeholder = { Text("Enter your name", color = MutedText) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = MutedText.copy(alpha = 0.3f),
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(alias.trim()) },
                enabled = alias.isNotBlank() && !isSaving
            ) {
                Text("Save", color = Accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = PrimaryText)
            }
        },
        containerColor = RaisedSurface
    )
}

@Composable
private fun PasswordResetDialog(
    email: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var resetEmail by remember { mutableStateOf(email) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset Password") },
        text = {
            Column {
                Text(
                    "Enter your email address and we'll send you a link to reset your password.",
                    color = MutedText,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = resetEmail,
                    onValueChange = { resetEmail = it },
                    placeholder = { Text("Email address", color = MutedText) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = MutedText.copy(alpha = 0.3f),
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(resetEmail.trim()) },
                enabled = resetEmail.isNotBlank()
            ) {
                Text("Send Reset Link", color = Accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = PrimaryText)
            }
        },
        containerColor = RaisedSurface
    )
}

@Composable
private fun ProfileHeader(viewer: com.example.vista.domain.model.Viewer?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Accent, AmbientPurple)
                        )
                    )
                    .border(3.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = "Profile",
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
            }

            Surface(
                color = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(32.dp).border(2.dp, Background, CircleShape)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Edit, contentDescription = null, tint = Background, modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = viewer?.alias ?: "Guest User",
            color = PrimaryText,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.5).sp
        )

        Text(
            text = viewer?.email ?: "Sign in to sync your library",
            color = MutedText,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun StatisticsRow(watchlistCount: Int, historyCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(label = "Watchlist", value = watchlistCount.toString())
        Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.White.copy(alpha = 0.1f)))
        StatItem(label = "Watched", value = historyCount.toString())
        Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.White.copy(alpha = 0.1f)))
        StatItem(label = "Following", value = "--")
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = PrimaryText,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black
        )
        Text(
            text = label,
            color = MutedText,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun SubscriptionCard() {
    var showAccessDialog by remember { mutableStateOf(false) }

    Surface(
        onClick = { showAccessDialog = true },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        color = Accent.copy(alpha = 0.1f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Accent.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Accent, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Star, contentDescription = null, tint = Color.White)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Your access",
                    color = PrimaryText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    // States what is actually true today. "Free access to all content" implied a
                    // plan that could be upgraded, and there is no plan and nothing to upgrade to.
                    text = "Free - no subscription",
                    color = MutedText,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MutedText,
                modifier = Modifier.size(24.dp)
            )
        }
    }

    if (showAccessDialog) {
        AlertDialog(
            onDismissRequest = { showAccessDialog = false },
            title = { Text("Your access is free", color = PrimaryText) },
            text = {
                Column {
                    AccessLine("Every movie and series in the catalogue")
                    AccessLine("No monthly fee, ever, while access stays open")
                    AccessLine("No card on file and nothing to cancel")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "If Vista ever introduces a paid plan, you will be told before anything changes.",
                        color = MutedText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAccessDialog = false }) {
                    Text("Got it", color = Accent)
                }
            },
            containerColor = RaisedSurface
        )
    }
}

@Composable
private fun AccessLine(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Check,
            contentDescription = null,
            tint = Accent,
            modifier = Modifier.size(16.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = PrimaryText,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ProfileSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = Accent,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Surface(
            color = RaisedSurface,
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    titleColor: Color = PrimaryText,
    onClick: () -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Accent.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (titleColor == Accent) Accent else MutedText,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor,
                fontWeight = FontWeight.SemiBold
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText
                )
            }
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MutedText.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}
