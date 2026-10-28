package com.example.vista.ai

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.vista.domain.model.*
import com.example.vista.navigation.Screen
import com.example.vista.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val reply: AssistantReply? = null,
    val taste: TastePreferences = TastePreferences(),
    val spoilers: Boolean = false
)

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val repository: AiRepository,
    private val tastes: TasteStore,
    savedState: SavedStateHandle,
    private val profile: com.example.vista.data.local.ProfileScope
) : ViewModel() {
    private val contextId: String? = savedState["mediaId"]
    private val contextType: String? = savedState["mediaType"]
    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState = _uiState.asStateFlow()
    private var request: Job? = null

    init {
        viewModelScope.launch {
            profile.owners.collect {
                cancel()
                _uiState.value = AiChatUiState(taste = tastes.read())
            }
        }
    }

    private suspend fun refreshTaste() {
        _uiState.update { it.copy(taste = tastes.read()) }
    }

    fun toggleGenre(genre: String) {
        viewModelScope.launch {
            tastes.toggleGenre(genre)
            refreshTaste()
        }
    }

    fun enableTaste(value: Boolean) {
        viewModelScope.launch {
            tastes.setEnabled(value)
            refreshTaste()
            _uiState.update { it.copy(reply = null) }
        }
    }

    fun resetTaste() {
        viewModelScope.launch {
            cancel()
            tastes.reset()
            refreshTaste()
            _uiState.update { AiChatUiState(taste = it.taste) }
        }
    }

    fun spoilers(value: Boolean) {
        cancel()
        _uiState.update { it.copy(spoilers = value, reply = null) }
    }

    fun dislike(pick: AssistantPick) {
        viewModelScope.launch {
            tastes.dislike(pick.item.watchlistKey())
            _uiState.update { it.copy(reply = it.reply?.copy(picks = it.reply.picks - pick)) }
        }
    }

    fun confirm(pick: AssistantPick, remove: Boolean) {
        viewModelScope.launch {
            try {
                repository.confirmWatchlist(pick.item, remove)
                _uiState.update {
                    it.copy(
                        error = null,
                        reply = AssistantReply(if (remove) "Removed from Watch List." else "Saved to Watch List.")
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun cancel() {
        request?.cancel()
        _uiState.update { it.copy(isLoading = false) }
    }

    fun sendMessage(text: String) {
        val message = text.trim()
        if (message.isEmpty() || _uiState.value.isLoading) return
        _uiState.update {
            it.copy(
                messages = (it.messages + ChatMessage("user", message)).takeLast(30),
                isLoading = true,
                error = null,
                reply = null
            )
        }
        request = viewModelScope.launch {
            try {
                val reply = repository.answer(message, contextId, contextType, _uiState.value.spoilers)
                currentCoroutineContext().ensureActive()
                _uiState.update { it.copy(reply = reply, isLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Assistant unavailable.") }
            }
        }
    }
}

@Composable
fun AiChatScreen(navController: NavController, viewModel: AiChatViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }
    var voiceState by remember { mutableStateOf("Idle") }
    var spoken by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<AssistantPick?>(null) }
    var resetDialog by remember { mutableStateOf(false) }
    var showPreferencesSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val main = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    val voice = remember {
        VoiceSession(
            context,
            { value -> main.post { voiceState = value } },
            { text -> input = text; viewModel.sendMessage(text) }
        )
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) voice.start() else voiceState = "Microphone denied. Type your request instead."
    }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current

    DisposableEffect(lifecycle, voice) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                voice.stop()
                viewModel.cancel()
            }
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose {
            lifecycle.lifecycle.removeObserver(observer)
            voice.close()
        }
    }

    LaunchedEffect(state.reply, spoken) {
        if (spoken) state.reply?.text?.let(voice::speak)
    }

    if (pending != null) {
        val pick = pending!!
        val remove = state.reply?.pendingTool == "remove"
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(if (remove) "Remove from Watch List?" else "Save to Watch List?", color = PrimaryText, fontWeight = FontWeight.Bold) },
            text = { Text(pick.item.title, color = SecondaryText) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.confirm(pick, remove)
                        pending = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Confirm", color = PrimaryText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }) { Text("Cancel", color = MutedText) }
            },
            containerColor = CardSurface
        )
    }

    if (resetDialog) {
        AlertDialog(
            onDismissRequest = { resetDialog = false },
            title = { Text("Reset Personalization?", color = PrimaryText, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Clears taste preferences and feedback. Your watch list and viewing history remain untouched.",
                    color = SecondaryText
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetTaste()
                        voice.stop()
                        resetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Reset", color = PrimaryText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { resetDialog = false }) { Text("Cancel", color = MutedText) }
            },
            containerColor = CardSurface
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp)
        ) {
            // Header Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Brush.linearGradient(listOf(Accent, AccentDark)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Vista AI Assistant",
                        style = MaterialTheme.typography.titleLarge,
                        color = PrimaryText,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Your personal movie guide & spoiler companion",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedText
                    )
                }
                IconButton(onClick = { showPreferencesSheet = !showPreferencesSheet }) {
                    Icon(
                        imageVector = if (showPreferencesSheet) Icons.Outlined.ExpandLess else Icons.Outlined.Tune,
                        contentDescription = "Preferences",
                        tint = Accent
                    )
                }
            }

            // Preferences Panel (Collapsible)
            AnimatedVisibility(visible = showPreferencesSheet) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    color = CardSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Personalization", style = MaterialTheme.typography.bodyMedium, color = PrimaryText, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Switch(
                                checked = state.taste.enabled,
                                onCheckedChange = viewModel::enableTaste,
                                colors = SwitchDefaults.colors(checkedTrackColor = Accent)
                            )
                        }
                        if (state.taste.enabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(listOf("Action", "Comedy", "Drama", "Science Fiction", "Romance", "Horror", "Animation")) { genre ->
                                    FilterChip(
                                        selected = genre in state.taste.genres,
                                        onClick = { viewModel.toggleGenre(genre) },
                                        label = { Text(genre, style = MaterialTheme.typography.labelSmall) },
                                        enabled = state.taste.enabled,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Accent,
                                            selectedLabelColor = PrimaryText,
                                            containerColor = RaisedSurface,
                                            labelColor = SecondaryText
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Allow plot spoilers", style = MaterialTheme.typography.bodyMedium, color = PrimaryText, modifier = Modifier.weight(1f))
                            Switch(
                                checked = state.spoilers,
                                onCheckedChange = viewModel::spoilers,
                                colors = SwitchDefaults.colors(checkedTrackColor = Accent)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Spoken responses", style = MaterialTheme.typography.bodyMedium, color = PrimaryText, modifier = Modifier.weight(1f))
                            Switch(
                                checked = spoken,
                                onCheckedChange = {
                                    spoken = it
                                    if (!it) voice.stop()
                                },
                                colors = SwitchDefaults.colors(checkedTrackColor = Accent)
                            )
                        }
                        TextButton(
                            onClick = { resetDialog = true },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Reset preferences", color = Accent, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Chat & Suggestions List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                // Prompt Chips
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("Recommend for my taste", "Recommend something relaxing", "Short comedy movies", "Cast details", "Plot summary")) { prompt ->
                            AssistChip(
                                onClick = {
                                    voice.stop()
                                    viewModel.sendMessage(prompt)
                                },
                                label = { Text(prompt, style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Accent, modifier = Modifier.size(14.dp))
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = CardSurface,
                                    labelColor = PrimaryText
                                ),
                                shape = RoundedCornerShape(20.dp),
                                border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = Color.White.copy(alpha = 0.1f))
                            )
                        }
                    }
                }

                // Chat Messages
                items(state.messages) { msg ->
                    val isUser = msg.role == "user"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            color = if (isUser) Accent.copy(alpha = 0.2f) else CardSurface,
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isUser) 16.dp else 4.dp,
                                bottomEnd = if (isUser) 4.dp else 16.dp
                            ),
                            border = BorderStroke(1.dp, if (isUser) Accent.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.widthIn(max = 300.dp)
                        ) {
                            Text(
                                text = msg.content,
                                color = PrimaryText,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                // AI Reply & Recommended Picks
                state.reply?.let { reply ->
                    item {
                        Surface(
                            color = CardSurface,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Accent.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Accent, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Assistant Response", style = MaterialTheme.typography.labelMedium, color = Accent, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(reply.text, color = PrimaryText, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    // Picks List
                    items(reply.picks, key = { it.item.watchlistKey() }) { pick ->
                        Surface(
                            color = RaisedSurface,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                com.example.vista.ui.components.CoilImage(
                                    url = pick.item.posterUrl.orEmpty(),
                                    contentDescription = pick.item.title,
                                    modifier = Modifier
                                        .width(60.dp)
                                        .height(90.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pick.item.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = PrimaryText,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = pick.reason,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MutedText
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                voice.stop()
                                                navController.navigate(
                                                    if (pick.item is MediaItem.SeriesItem) Screen.SeriesDetails.createRoute(pick.item.id)
                                                    else Screen.MovieDetails.createRoute(pick.item.id)
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Accent),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text("Details", style = MaterialTheme.typography.labelSmall, color = PrimaryText, fontWeight = FontWeight.Bold)
                                        }
                                        OutlinedButton(
                                            onClick = { pending = pick },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.height(32.dp),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                                        ) {
                                            Text(
                                                if (reply.pendingTool == "remove") "Remove" else "Save",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = PrimaryText
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.dislike(pick) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Outlined.ThumbDown, contentDescription = "Not for me", tint = MutedText, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Error State
                state.error?.let { err ->
                    item {
                        Surface(
                            color = ErrorAccent.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, ErrorAccent.copy(alpha = 0.3f))
                        ) {
                            Text(err, color = ErrorAccent, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                        }
                    }
                }

                // Loading State
                if (state.isLoading) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(CircleShape),
                                color = Accent
                            )
                            TextButton(onClick = viewModel::cancel) {
                                Text("Cancel request", color = MutedText, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // Voice Status Badge
            if (voiceState != "Idle") {
                Surface(
                    color = Accent.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = voiceState,
                        style = MaterialTheme.typography.labelSmall,
                        color = Accent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            // Input Dock
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp, top = 4.dp),
                color = CardSurface,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            voice.stop()
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                voice.start()
                            } else {
                                permission.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Input",
                            tint = if (voiceState.startsWith("Listening")) Accent else PrimaryText
                        )
                    }
                    if (voiceState != "Idle" || state.isLoading) {
                        IconButton(
                            onClick = {
                                voice.stop()
                                viewModel.cancel()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop",
                                tint = ErrorAccent
                            )
                        }
                    }
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask Vista Assistant...", color = MutedText, style = MaterialTheme.typography.bodyMedium) },
                        maxLines = 3,
                        enabled = !state.isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            disabledBorderColor = Color.Transparent
                        )
                    )
                    IconButton(
                        enabled = input.isNotBlank() && !state.isLoading,
                        onClick = {
                            voice.stop()
                            viewModel.sendMessage(input)
                            input = ""
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (input.isNotBlank() && !state.isLoading) Accent else MutedText
                        )
                    }
                }
            }
        }
    }
}
