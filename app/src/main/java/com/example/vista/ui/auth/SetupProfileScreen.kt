package com.example.vista.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.vista.domain.model.AgeBand
import com.example.vista.domain.model.Viewer
import com.example.vista.navigation.Screen
import com.example.vista.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupProfileScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var alias by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var ageBand by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<AgeBand?>(null) }
    var selectedAvatar by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("default") }
    var navigated by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val avatars = com.example.vista.artwork.VistaArtworkRegistry.userAvatars

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    LaunchedEffect(uiState.error) { if (uiState.error != null) keyboard?.hide() }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    LaunchedEffect(uiState.needsProfileSetup, uiState.isUserLoggedIn) {
        if (!navigated && uiState.isUserLoggedIn && !uiState.needsProfileSetup) {
            navigated = true
            navController.navigate(viewModel.postAuthRoute()) {
                popUpTo(Screen.Auth.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "One Last Step",
                style = MaterialTheme.typography.headlineMedium,
                color = PrimaryText,
                fontWeight = FontWeight.Black
            )
            
            Text(
                text = "What should we call you?",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedText
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Avatar Selection
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Accent.copy(alpha = 0.1f))
                    .border(2.dp, Accent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                com.example.vista.artwork.VistaProfileAvatar(selectedAvatar, 112.dp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(avatars) { avatar ->
                    val isSelected = selectedAvatar == avatar.id
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Accent else CardSurface)
                            .clickable(onClickLabel = "Choose ${avatar.label}") { selectedAvatar = avatar.id }
                            .border(2.dp, if (isSelected) Color.White else Color.Transparent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        com.example.vista.artwork.VistaProfileAvatar(avatar.id, 72.dp, description = avatar.label)
                        if (isSelected) Icon(Icons.Outlined.Check, "Selected avatar", tint = Color.White,
                            modifier = Modifier.align(Alignment.BottomEnd).size(22.dp).background(Accent, CircleShape))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = alias,
                onValueChange = { if (it.length <= 20) alias = it },
                label = { Text("Username / Alias") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = CardSurface,
                    cursorColor = Accent
                ),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text("Who is this profile for?", style = MaterialTheme.typography.titleSmall, color = PrimaryText, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("We use this to hide restricted titles and shape your home screen.", style = MaterialTheme.typography.bodySmall, color = MutedText)
            Spacer(modifier = Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AgeBand.values().forEach { band ->
                    FilterChip(
                        selected = ageBand == band,
                        onClick = { ageBand = band },
                        label = { Text(band.displayName) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            ageBand?.let {
                Text(it.description, color = SecondaryText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val selectedBand = ageBand
                    if (alias.isNotBlank() && selectedBand != null) {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        viewModel.saveProfile(alias = alias, avatarId = selectedAvatar, ageBand = selectedBand)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = alias.isNotBlank() && ageBand != null && !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Start Watching", fontWeight = FontWeight.Black)
                }
            }

        }
    }
    AuthFeedbackDialog(uiState, onDismiss = viewModel::clearError, onRecover = viewModel::recover)
}
