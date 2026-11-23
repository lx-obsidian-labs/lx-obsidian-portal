package com.example.vista.ui.kids

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.vista.data.local.PreferencesManager
import com.example.vista.domain.model.ContentAccessPolicy
import com.example.vista.domain.model.MediaItem
import com.example.vista.domain.model.toMediaItem
import com.example.vista.domain.model.watchlistKey
import com.example.vista.domain.repository.CatalogRepository
import com.example.vista.navigation.Screen
import com.example.vista.ui.components.MediaCard
import com.example.vista.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KidsUiState(
    val loading: Boolean = true,
    val items: List<MediaItem> = emptyList(),
    val error: String? = null,
    val hasPin: Boolean = false
)

@HiltViewModel
class KidsViewModel @Inject constructor(
    private val catalog: CatalogRepository,
    private val preferences: PreferencesManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(KidsUiState())
    val uiState: StateFlow<KidsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.hasParentalPin.collect { hasPin -> _uiState.update { it.copy(hasPin = hasPin) } }
        }
        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val settings = preferences.settings.first()
                val movies = catalog.getMoviesByGenre("16", 1)
                    .filter { ContentAccessPolicy.allows(settings, it.ageRating, it.isAdult) }
                    .take(20).map { it.toMediaItem() }
                val series = catalog.getPopularSeries(1)
                    .filter { ContentAccessPolicy.allows(settings, it.ageRating, false) }
                    .take(12).map { it.toMediaItem() }
                _uiState.update { it.copy(loading = false, items = movies + series) }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = "Could not load the kids catalog. Check your connection and try again.") }
            }
        }
    }

    suspend fun verifyPin(pin: String): Boolean = preferences.verifyParentalPin(pin)

    suspend fun disableKidsMode() = preferences.setKidsMode(false)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KidsModeScreen(navController: NavController, viewModel: KidsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showExit by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    BackHandler { showExit = true }

    Box(Modifier.fillMaxSize().background(Background)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showExit = true }) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Exit Kids mode", tint = PrimaryText)
                }
                Column(Modifier.weight(1f)) {
                    Text("Vista Kids", color = PrimaryText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text("A small, safe corner of Vista", color = SecondaryText, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = { navController.navigate(Screen.AiAssistant.route) }) {
                    Icon(Icons.Outlined.AutoAwesome, "Ask Vista", tint = Accent)
                }
            }

            when {
                state.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Accent) }
                state.error != null -> Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
                    Text(state.error ?: "Something went wrong", color = SecondaryText)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = viewModel::load) { Text("Try again") }
                }
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(132.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.items, key = { it.watchlistKey() }) { item ->
                        MediaCard(item = item, onClick = {
                            val route = when (item) {
                                is MediaItem.MovieItem -> Screen.MovieDetails.createRoute(item.id)
                                is MediaItem.SeriesItem -> Screen.SeriesDetails.createRoute(item.id)
                                else -> null
                            }
                            route?.let(navController::navigate)
                        })
                    }
                }
            }
        }

        if (showExit) {
            AlertDialog(
                onDismissRequest = { showExit = false },
                title = { Text("Leave Vista Kids?") },
                text = {
                    if (state.hasPin) {
                        Column {
                            Text("Enter the parental PIN to return to Vista.", color = SecondaryText)
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = pin,
                                onValueChange = { pin = it.filter(Char::isDigit).take(6); pinError = false },
                                label = { Text("Parental PIN") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                isError = pinError,
                                shape = RoundedCornerShape(14.dp)
                            )
                            if (pinError) Text("That PIN is not correct.", color = ErrorAccent, style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        Text("No parental PIN is set. You can return to the full Vista experience.", color = SecondaryText)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (!state.hasPin) {
                            scope.launch {
                                viewModel.disableKidsMode()
                                showExit = false
                                navController.popBackStack()
                            }
                        } else {
                            scope.launch {
                                if (viewModel.verifyPin(pin)) {
                                    viewModel.disableKidsMode()
                                    showExit = false
                                    navController.popBackStack()
                                } else pinError = true
                            }
                        }
                    }) { Text("Exit", color = Accent) }
                },
                dismissButton = { TextButton(onClick = { showExit = false }) { Text("Stay") } },
                containerColor = RaisedSurface
            )
        }
    }
}
