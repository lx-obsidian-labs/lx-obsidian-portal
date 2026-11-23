package com.example.vista.artwork

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.vista.ui.theme.*
import com.example.vista.util.LocalTvConfiguration
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import java.time.LocalDate
import kotlinx.coroutines.delay

@Composable
fun rememberCampaignDate(): LocalDate {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var date by remember { mutableStateOf(LocalDate.now()) }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { date = LocalDate.now(); delay(60_000) }
        }
    }
    return date
}

@Composable
fun rememberArtworkResource(artwork: VistaArtwork): Int? {
    val context = LocalContext.current
    return remember(context.resources, artwork) { VistaArtworkRegistry.resolve(context, artwork) }
}

/** Decorative image decoded at the measured size by Coil; text/actions always remain native. */
@Composable
fun VistaHeroBackground(artwork: VistaArtwork, modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center, bottomGradient: Boolean = true) {
    val resource = rememberArtworkResource(artwork)
    Box(modifier.background(Background)) {
        if (resource != null) AsyncImage(resource, contentDescription = null, contentScale = ContentScale.Crop,
            alignment = alignment, modifier = Modifier.matchParentSize())
        Box(Modifier.matchParentSize().background(if (bottomGradient) Brush.verticalGradient(
            listOf(Color.Black.copy(alpha = 0.15f), Color.Black.copy(alpha = 0.35f), Background))
            else Brush.horizontalGradient(listOf(Background.copy(alpha = 0.9f), Background.copy(alpha = 0.35f)))))
    }
}

@Composable
fun VistaArtworkBanner(artwork: VistaArtwork, heading: String, subtitle: String, actionLabel: String,
    onClick: () -> Unit, modifier: Modifier = Modifier, description: String? = null,
    contentAlignment: Alignment.Horizontal = Alignment.Start, showFallback: Boolean = false) {
    val resource = rememberArtworkResource(artwork)
    if (artwork.kind != ArtworkKind.LANDSCAPE) return
    if (resource == null && !showFallback) return
    val tv = LocalTvConfiguration.current.isTv
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(20.dp)
    BoxWithConstraints(modifier.fillMaxWidth().clip(shape).onFocusChanged { focused = it.isFocused }
        .border(if (focused) 3.dp else 0.dp, if (focused) Color.White else Color.Transparent, shape)
        .clickable(role = Role.Button, onClickLabel = actionLabel, onClick = onClick)) {
        Box(Modifier.fillMaxWidth().heightIn(min = if (tv) 260.dp else (maxWidth * 9 / 16).coerceAtLeast(190.dp))) {
            if (resource != null) AsyncImage(resource, null, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
            Box(Modifier.matchParentSize().background(Brush.horizontalGradient(
                if (contentAlignment == Alignment.End) listOf(Background.copy(alpha = 0.25f), Background.copy(alpha = 0.88f))
                else listOf(Background.copy(alpha = 0.9f), Background.copy(alpha = 0.2f)))))
            Column(Modifier.fillMaxWidth().padding(if (tv) 32.dp else 20.dp),
                horizontalAlignment = contentAlignment, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(heading, color = PrimaryText, style = if (tv) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(0.85f))
                Text(subtitle, color = PrimaryText, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth(0.85f))
                if (!description.isNullOrBlank()) Text(description, color = SecondaryText, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(0.85f))
                Surface(color = Accent, shape = RoundedCornerShape(10.dp)) {
                    Text(actionLabel, color = PrimaryText, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
                }
            }
        }
    }
}

@Composable
fun VistaProfileAvatar(id: String, size: Dp, modifier: Modifier = Modifier, description: String? = "Profile avatar") {
    val avatar = VistaArtworkRegistry.avatar(id)
    VistaAvatarImage(avatar.artwork, size, modifier, description, assistant = false)
}

@Composable
fun VistaAssistantAvatar(size: Dp = 40.dp, modifier: Modifier = Modifier) =
    VistaAvatarImage(VistaArtworkRegistry.assistant.artwork, size, modifier, "Vista AI assistant", assistant = true)

@Composable
private fun VistaAvatarImage(artwork: VistaArtwork?, size: Dp, modifier: Modifier, description: String?, assistant: Boolean) {
    val resource = artwork?.let { rememberArtworkResource(it) }
    var failed by remember(resource) { mutableStateOf(false) }
    Box(modifier.size(size).clip(CircleShape).background(CardSurface).border(1.dp, Accent.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center) {
        if (resource != null && !failed) AsyncImage(resource, description, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(), onError = { failed = true })
        else Icon(if (assistant) Icons.Outlined.SmartToy else Icons.Outlined.Person, description,
            tint = PrimaryText, modifier = Modifier.size(size * 0.55f))
    }
}

@Composable
fun CurrentUserAvatar(size: Dp = 32.dp, modifier: Modifier = Modifier, viewModel: AvatarViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    VistaProfileAvatar(state.selectedId, size, modifier)
}

/** Same picker is reachable from phone Profile/Settings and TV Home, with separate focus/selection. */
@Composable
fun VistaAvatarPicker(onDismiss: () -> Unit, viewModel: AvatarViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var draft by rememberSaveable(state.selectedId) { mutableStateOf(state.selectedId) }
    LaunchedEffect(Unit) { viewModel.retrySync() }
    Dialog(onDismissRequest = { if (!state.saving) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = CardSurface, shape = RoundedCornerShape(24.dp),
            modifier = Modifier.padding(20.dp).widthIn(max = 700.dp).fillMaxWidth().heightIn(max = 650.dp)) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Choose your avatar", color = PrimaryText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Make your profile your own.", color = SecondaryText)
                LazyVerticalGrid(columns = GridCells.Adaptive(120.dp), modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(VistaArtworkRegistry.userAvatars, key = { it.id }) { avatar ->
                        var focused by remember { mutableStateOf(false) }
                        val selected = draft == avatar.id
                        Column(Modifier.clip(RoundedCornerShape(16.dp)).background(RaisedSurface)
                            .onFocusChanged { focused = it.isFocused }
                            .border(if (focused) 3.dp else if (selected) 2.dp else 1.dp,
                                if (focused) Color.White else if (selected) Accent else Color.Transparent, RoundedCornerShape(16.dp))
                            .clickable(enabled = !state.saving, role = Role.RadioButton) { draft = avatar.id }
                            .semantics { contentDescription = avatar.label; this.selected = selected; stateDescription = if (selected) "Selected" else "Not selected" }
                            .padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            VistaProfileAvatar(avatar.id, 80.dp, description = null)
                            Text(avatar.label.substringBefore(" —"), color = PrimaryText, modifier = Modifier.padding(top = 8.dp))
                            if (selected) Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.CheckCircle, null, tint = Accent, modifier = Modifier.size(16.dp))
                                Text("Selected", color = PrimaryText, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                state.error?.let { Text(it, color = ErrorAccent) }
                state.message?.let { Text(it, color = SecondaryText) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, enabled = !state.saving) { Text("Close", color = PrimaryText) }
                    Button(onClick = { viewModel.save(draft) }, enabled = !state.saving,
                        colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
                        if (state.saving) CircularProgressIndicator(Modifier.size(18.dp), color = PrimaryText, strokeWidth = 2.dp)
                        else Text("Save avatar", color = PrimaryText)
                    }
                }
            }
        }
    }
}
