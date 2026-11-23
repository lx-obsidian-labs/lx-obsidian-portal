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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.saveable.rememberSaveable
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
    val sessionId: String = java.util.UUID.randomUUID().toString(),
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val reply: AssistantReply? = null,
    val taste: TastePreferences = TastePreferences(),
    val spoilers: Boolean = false,
    val progress: String = "",
    val actionBusy: Boolean = false,
    val actionStatus: String? = null,
    val recent: List<AiConversation> = emptyList(),
    val brief: AssistantReply? = null,
    val briefsLoading: Boolean = false,
    val briefsError: String? = null
)

data class AiConversation(val id: String, val title: String, val messages: List<ChatMessage>,
    val reply: AssistantReply?, val picks: List<AssistantPick>, val actionStatus: String?,
    val newsSources: List<AssistantSource>, val newsFeed: String)

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
    private var actionJob: Job? = null
    private var briefJob: Job? = null
    private var briefGeneration = 0
    private var generation = 0
    private var lastPicks = emptyList<AssistantPick>()
    private var lastNewsSources = emptyList<AssistantSource>()
    private var lastNewsFeed = "global"
    private var requestNewsSources = emptyList<AssistantSource>()
    private var requestNewsFeed = "global"

    init {
        viewModelScope.launch {
            profile.owners.collect {
                cancel()
                briefJob?.cancel()
                lastPicks = emptyList()
                lastNewsSources = emptyList()
                lastNewsFeed = "global"
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
        if (_uiState.value.actionBusy) return
        val owner = profile.current()
        val currentGeneration = generation
        _uiState.update { it.copy(actionBusy = true, actionStatus = null) }
        actionJob = viewModelScope.launch {
            try {
                check(profile.current() == owner) { "Your profile changed. Choose the title again." }
                repository.confirmWatchlist(pick.item, remove)
                if (generation != currentGeneration || profile.current() != owner) return@launch
                _uiState.update {
                    it.copy(
                        error = null,
                        actionStatus = if (remove) "Removed ${pick.item.title} from Watch List." else "Saved ${pick.item.title} to Watch List."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                if (generation == currentGeneration) _uiState.update { it.copy(actionBusy = false) }
            }
        }
    }

    fun cancel() {
        generation++
        briefGeneration++
        briefJob?.cancel()
        request?.cancel()
        actionJob?.cancel()
        _uiState.update { it.copy(isLoading = false, actionBusy = false, briefsLoading = false, progress = "") }
    }

    fun newConversation() {
        val previous = snapshot()
        cancel()
        lastPicks = emptyList()
        lastNewsSources = emptyList()
        lastNewsFeed = "global"
        _uiState.update { AiChatUiState(taste = it.taste, spoilers = it.spoilers,
            recent = (listOfNotNull(previous) + it.recent).take(8), brief = it.brief,
            briefsLoading = it.briefsLoading, briefsError = it.briefsError) }
    }

    private fun snapshot(): AiConversation? = _uiState.value.takeIf { it.messages.isNotEmpty() }?.let {
        AiConversation(java.util.UUID.randomUUID().toString(), it.messages.firstOrNull { turn -> turn.role == "user" }?.content?.take(64) ?: "Conversation",
            it.messages, it.reply, lastPicks, it.actionStatus, lastNewsSources, lastNewsFeed)
    }

    fun openConversation(id: String) {
        val target = _uiState.value.recent.firstOrNull { it.id == id } ?: return
        val previous = snapshot()
        cancel()
        lastPicks = target.picks
        lastNewsSources = target.newsSources
        lastNewsFeed = target.newsFeed
        _uiState.update { it.copy(messages = target.messages, reply = target.reply, error = null, actionStatus = target.actionStatus,
            recent = (listOfNotNull(previous) + it.recent.filterNot { chat -> chat.id == id }).take(8)) }
    }

    fun loadBrief() {
        if (_uiState.value.briefsLoading) return
        val owner = profile.current()
        val briefTurn = ++briefGeneration
        _uiState.update { it.copy(briefsLoading = true, briefsError = null) }
        briefJob = viewModelScope.launch {
            try {
                val brief = repository.answer("Latest entertainment news", spoilers = _uiState.value.spoilers, sourcePage = "Entertainment Brief")
                if (profile.current() == owner && briefGeneration == briefTurn) _uiState.update { it.copy(brief = brief) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (profile.current() == owner && briefGeneration == briefTurn) _uiState.update { it.copy(briefsError = e.message ?: "Brief unavailable. Try again.") } }
            finally { if (profile.current() == owner && briefGeneration == briefTurn) _uiState.update { it.copy(briefsLoading = false) } }
        }
    }

    fun discussBrief() {
        val brief = _uiState.value.brief ?: return
        newConversation()
        lastNewsSources = brief.sources
        lastNewsFeed = brief.newsFeed
        _uiState.update { it.copy(messages = listOf(ChatMessage("user", "Latest entertainment news")), reply = brief) }
    }
    fun retryLast() {
        val last = _uiState.value.messages.lastOrNull()?.takeIf { it.role == "user" } ?: return
        _uiState.update { it.copy(messages = it.messages.dropLast(1)) }
        sendMessage(last.content, newsSources = requestNewsSources, newsFeed = requestNewsFeed)
    }

    fun sendMessage(text: String, sourcePage: String = "AI assistant", newsSources: List<AssistantSource>? = null, newsFeed: String? = null) {
        val message = text.trim()
        if (message.isEmpty() || _uiState.value.isLoading) return
        if (message.length > 1000) {
            _uiState.update { it.copy(error = "Keep your message under 1,000 characters.") }
            return
        }
        val history = _uiState.value.messages.toMutableList().apply {
            _uiState.value.reply?.let { add(ChatMessage("assistant", it.text)) }
            _uiState.value.actionStatus?.let { add(ChatMessage("assistant", "Action receipt from Vista: $it")) }
        }.takeLast(20)
        val evidence = newsSources ?: if (AgentActionPolicy.isNewsFollowup(message, history)) lastNewsSources else emptyList()
        val feed = newsFeed ?: lastNewsFeed
        requestNewsSources = evidence
        requestNewsFeed = feed
        val requestGeneration = ++generation
        _uiState.update {
            it.copy(
                messages = (history + ChatMessage("user", message)).takeLast(30),
                isLoading = true,
                error = null,
                reply = null,
                actionStatus = null, progress = "Understanding your request"
            )
        }
        request = viewModelScope.launch {
            try {
                val reply = repository.answer(message, contextId, contextType, _uiState.value.spoilers, history, sourcePage, lastPicks,
                    newsSourceIds = evidence.map { it.id }, newsFeed = feed) { phase ->
                    if (generation == requestGeneration) _uiState.update { it.copy(progress = phase) }
                }
                currentCoroutineContext().ensureActive()
                if (generation != requestGeneration) return@launch
                if (reply.picks.isNotEmpty()) lastPicks = reply.picks.take(8)
                if (reply.sources.isNotEmpty()) { lastNewsSources = reply.sources; lastNewsFeed = reply.newsFeed }
                else if (!AgentActionPolicy.isNewsRequest(message, history) && evidence.isEmpty()) lastNewsSources = emptyList()
                _uiState.update { it.copy(reply = reply, isLoading = false) }
            } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
                if (generation == requestGeneration) _uiState.update { it.copy(isLoading = false, error = "This request took too long. Try a more specific title or retry.") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (generation == requestGeneration) _uiState.update { it.copy(isLoading = false, error = e.message ?: "Assistant unavailable.") }
            } finally {
                if (generation == requestGeneration) _uiState.update { it.copy(isLoading = false, progress = "") }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
fun AiChatScreen(navController: NavController, viewModel: AiChatViewModel = hiltViewModel(), floating: Boolean = false, onDismiss: (() -> Unit)? = null) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf("") }
    var section by rememberSaveable { mutableIntStateOf(0) }
    var showHistory by remember { mutableStateOf(false) }
    var followLatest by remember { mutableStateOf(true) }
    var voiceState by remember { mutableStateOf("Idle") }
    var spoken by remember { mutableStateOf(false) }
    var lastSpokenTurn by remember { mutableStateOf("") }
    var cloudVoice by rememberSaveable { mutableStateOf(VoiceSession.cloudVoiceEnabled(context)) }
    var pending by remember { mutableStateOf<AssistantPick?>(null) }
    var resetDialog by remember { mutableStateOf(false) }
    var showPreferencesSheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress to listState.canScrollForward }.collect { (scrolling, forward) ->
            if (scrolling) followLatest = !forward
        }
    }
    LaunchedEffect(state.messages.size, state.reply, state.isLoading, state.error, section) {
        if (followLatest && section == 0 && listState.layoutInfo.totalItemsCount > 0) listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
    }

    val scope = rememberCoroutineScope()
    val main = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    val voice = remember {
        VoiceSession(
            context,
            { value -> main.post { voiceState = value } },
            { text -> input = ""; viewModel.sendMessage(text, if (floating) "Home (floating chat)" else "AI assistant") },
            onInterrupt = viewModel::cancel
        )
    }
    fun spokenTurnKey(text: String) = "${state.sessionId}:${state.messages.size}:$text"
    fun speakReply(text: String) {
        lastSpokenTurn = spokenTurnKey(text)
        voice.speak(text)
    }
    fun beginVoice() {
        state.reply?.let { lastSpokenTurn = spokenTurnKey(it.text) }
        spoken = true
        voice.interrupt()
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) beginVoice() else voiceState = "Microphone denied. Type your request instead."
    }
    LaunchedEffect(cloudVoice) { voice.setCloudVoice(cloudVoice) }
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

    LaunchedEffect(state.reply, spoken, state.sessionId, state.messages.size) {
        val reply = state.reply ?: return@LaunchedEffect
        if (!spoken && !voice.isHandsFree) return@LaunchedEffect
        val key = spokenTurnKey(reply.text)
        if (key == lastSpokenTurn) return@LaunchedEffect
        lastSpokenTurn = key
        voice.speak(reply.text)
    }

    LaunchedEffect(state.error) {
        if (state.error != null) voice.stop()
    }
    LaunchedEffect(state.sessionId) { voice.stop(); lastSpokenTurn = ""; input = "" }

    if (showHistory) ModalBottomSheet(onDismissRequest = { showHistory = false }, containerColor = CardSurface) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Recent conversations", style = MaterialTheme.typography.titleLarge, color = PrimaryText)
            Text("Kept for this session only. Starting a new chat adds the current conversation here.", color = SecondaryText)
            if (state.recent.isEmpty()) Text("No previous conversations yet.", color = SecondaryText)
            state.recent.forEach { chat ->
                TextButton(onClick = { voice.stop(); viewModel.openConversation(chat.id); section = 0; followLatest = true; showHistory = false }) {
                    Text(chat.title, color = PrimaryText, maxLines = 2)
                }
            }
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
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
                 .then(if (floating) Modifier else Modifier.statusBarsPadding())
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
                        text = "Vista AI",
                        style = MaterialTheme.typography.titleLarge,
                        color = PrimaryText,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Your cinema companion",
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
                if (!floating) TextButton(onClick = { showHistory = true }) { Text("Recent", color = Accent) }
                TextButton(onClick = { voice.stop(); pending = null; viewModel.newConversation(); section = 0; followLatest = true }) { Text("New chat", color = Accent) }
                if (onDismiss != null) {
                    TextButton(onClick = { voice.stop(); viewModel.cancel(); onDismiss() }) { Text("Close", color = SecondaryText) }
                }
            }

            // Preferences Panel (Collapsible)
            if (!floating) TabRow(selectedTabIndex = section, containerColor = Background, contentColor = Accent) {
                Tab(selected = section == 0, onClick = { section = 0 }, text = { Text("Chat") })
                Tab(selected = section == 1, onClick = {
                    voice.stop(); section = 1
                    if (state.brief == null && !state.briefsLoading) viewModel.loadBrief()
                }, text = { Text("Entertainment Brief") })
            }
            if (showPreferencesSheet) ModalBottomSheet(onDismissRequest = { showPreferencesSheet = false }, containerColor = CardSurface) {
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
                            Text("Read responses aloud", style = MaterialTheme.typography.bodyMedium, color = PrimaryText, modifier = Modifier.weight(1f))
                            Switch(
                                checked = spoken,
                                onCheckedChange = {
                                    spoken = it
                                    if (!it) voice.stop()
                                },
                                colors = SwitchDefaults.colors(checkedTrackColor = Accent)
                            )
                        }
                        Text("Spoken responses", style = MaterialTheme.typography.titleSmall, color = PrimaryText)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = cloudVoice, onClick = { cloudVoice = true })
                            TextButton(onClick = { cloudVoice = true }) { Text("ElevenLabs · natural cloud voice", color = PrimaryText) }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = !cloudVoice, onClick = { cloudVoice = false })
                            TextButton(onClick = { cloudVoice = false }) { Text("Device voice · Android speech engine", color = PrimaryText) }
                        }
                        Text("Talk over a reply to interrupt it, or tap Interrupt to ask something else. Headphones help prevent speaker echo; voice interruption depends on your device’s speech recognizer. ElevenLabs requires sign-in and a connection. Tap Stop voice to end.",
                            color = SecondaryText, style = MaterialTheme.typography.bodySmall)
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
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                if (section == 1) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Entertainment, with sources.", style = MaterialTheme.typography.headlineSmall, color = PrimaryText)
                            Text("Recent reporting, not a guarantee of playback availability. Publisher dates appear below.", color = SecondaryText)
                            OutlinedButton(onClick = viewModel::loadBrief, enabled = !state.briefsLoading) { Text("Refresh brief") }
                            if (state.briefsLoading) { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Accent); Text("Retrieving entertainment reporting…", color = SecondaryText) }
                            state.briefsError?.let { Text(it, color = ErrorAccent) }
                            state.brief?.let { brief ->
                                AiAnswerContent(brief, onRead = { speakReply(brief.text) }, onFollowup = null, onSource = { source ->
                                    voice.stop(); viewModel.discussBrief(); section = 0; followLatest = true
                                    viewModel.sendMessage("Explain this news report: ${source.title}", newsSources = listOf(source), newsFeed = brief.newsFeed)
                                })
                                Button(onClick = { voice.stop(); viewModel.discussBrief(); section = 0; followLatest = true }) { Text("Discuss this brief with AI") }
                            }
                        }
                    }
                } else {
                // Prompt Chips
                if (state.messages.isEmpty()) {
                    item {
                        Surface(color = CardSurface, shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.padding(20.dp)) {
                                Text("What’s the plan tonight?", style = MaterialTheme.typography.titleLarge, color = PrimaryText, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                Text("Find your next watch, compare titles or ask about entertainment news. Spoilers stay off unless you enable them.", color = SecondaryText, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                if (state.messages.isEmpty()) item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("Something to watch tonight", "Latest entertainment news", "Recommend a comedy", "Show my watchlist", "Resume watching")) { prompt ->
                            AssistChip(
                                onClick = {
                                     voice.stop()
                                    viewModel.sendMessage(prompt, if (floating) "Home (floating chat)" else "AI assistant")
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
                        if (!isUser) {
                            com.example.vista.artwork.VistaAssistantAvatar(size = 40.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Surface(
                            color = if (isUser) Accent.copy(alpha = 0.2f) else CardSurface,
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isUser) 16.dp else 4.dp,
                                bottomEnd = if (isUser) 4.dp else 16.dp
                            ),
                            border = BorderStroke(1.dp, if (isUser) Accent.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f)),
                             modifier = Modifier.widthIn(max = 600.dp).weight(1f, fill = false)
                        ) {
                            Text(
                                text = msg.content,
                                color = PrimaryText,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        if (isUser) {
                            Spacer(Modifier.width(8.dp))
                            com.example.vista.artwork.CurrentUserAvatar(size = 32.dp)
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
                                    com.example.vista.artwork.VistaAssistantAvatar(size = 40.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Assistant Response", style = MaterialTheme.typography.labelMedium, color = Accent, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                AiAnswerContent(reply, onRead = { speakReply(reply.text) }, onFollowup = { prompt ->
                                    voice.stop(); followLatest = true; viewModel.sendMessage(prompt)
                                }, onSource = { source ->
                                    voice.stop(); followLatest = true
                                    viewModel.sendMessage("Explain this news report: ${source.title}", newsSources = listOf(source), newsFeed = reply.newsFeed)
                                })
                                reply.destination?.let { route ->
                                    Spacer(Modifier.height(12.dp))
                                    Button(onClick = {
                                        voice.stop()
                                        onDismiss?.invoke()
                                        navController.navigate(route) { launchSingleTop = true }
                                    }, colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
                                        Text(reply.destinationLabel ?: "Open page")
                                    }
                                }
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
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                             onClick = {
                                                 voice.stop()
                                                 onDismiss?.invoke()
                                                navController.navigate(
                                                    if (pick.item is MediaItem.SeriesItem) Screen.SeriesDetails.createRoute(pick.item.id)
                                                    else Screen.MovieDetails.createRoute(pick.item.id)
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Accent),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                             modifier = Modifier.heightIn(min = 48.dp)
                                        ) {
                                            Text("Details", style = MaterialTheme.typography.labelSmall, color = PrimaryText, fontWeight = FontWeight.Bold)
                                        }
                                        OutlinedButton(
                                             onClick = { pending = pick },
                                             enabled = !state.actionBusy,
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                             modifier = Modifier.heightIn(min = 48.dp),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                                        ) {
                                            Text(
                                                if (reply.pendingTool == "remove") "Remove" else "Save",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = PrimaryText
                                            )
                                         }
                                         TextButton(onClick = {
                                             voice.stop(); onDismiss?.invoke()
                                             navController.navigate(if (pick.item is MediaItem.SeriesItem) Screen.SeriesDetails.createRoute(pick.item.id)
                                                 else Screen.Downloads.createRoute(pick.item.id, pick.item.title, MediaType.MOVIE))
                                         }) { Text(if (pick.item is MediaItem.SeriesItem) "Choose episode" else "Review download", style = MaterialTheme.typography.labelSmall) }
                                        IconButton(
                                            onClick = { viewModel.dislike(pick) },
                                             modifier = Modifier.size(48.dp)
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
                            Column(Modifier.padding(12.dp)) {
                                Text(err, color = ErrorAccent, style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = viewModel::retryLast, enabled = !state.isLoading) { Text("Retry last request") }
                            }
                        }
                    }
                }
                state.actionStatus?.let { status -> item { Text(status, color = Accent, modifier = Modifier.padding(12.dp)) } }

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
                             Text(state.progress, color = SecondaryText, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                }
            }

            if (section == 0 && listState.canScrollForward) TextButton(onClick = {
                followLatest = true
                scope.launch { listState.animateScrollToItem((listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)) }
            }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Latest messages ↓", color = Accent) }

            // Voice Status Badge
            if (voiceState != "Idle") {
                Surface(
                    color = Accent.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Column(Modifier.padding(8.dp)) {
                    Text(
                        text = voiceState,
                        style = MaterialTheme.typography.labelSmall,
                        color = Accent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                    Row {
                        TextButton(onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                beginVoice()
                            } else {
                                voice.stop(); viewModel.cancel()
                                permission.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }) { Text("Interrupt", color = Accent) }
                        TextButton(onClick = { voice.stop(); viewModel.cancel() }) { Text("Stop voice", color = Accent) }
                    }
                    }
                }
            }

            // Input Dock
            if (section == 0) Surface(
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
                               if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                   beginVoice()
                             } else {
                                 permission.launch(Manifest.permission.RECORD_AUDIO)
                             }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                              contentDescription = "Interrupt and speak",
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
                        onValueChange = { input = it.take(1000) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask about movies, series or news…", color = MutedText, style = MaterialTheme.typography.bodyMedium) },
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
                             viewModel.sendMessage(input, if (floating) "Home (floating chat)" else "AI assistant")
                            followLatest = true
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
