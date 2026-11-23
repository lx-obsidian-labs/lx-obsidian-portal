package com.example.vista.ui.tv

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.*
import com.example.vista.domain.model.toMediaItem
import com.example.vista.domain.model.watchlistKey
import com.example.vista.ui.search.SearchViewModel
import com.example.vista.ui.theme.Background
import com.example.vista.ui.theme.MutedText
import com.example.vista.ui.theme.PrimaryText
import com.example.vista.ui.theme.RaisedSurface

/**
 * TV search: D-pad friendly, voice-first. A remote keyboard is painful, so the microphone is the
 * primary path and the text field sits next to it for when a viewer already knows the title.
 * Recent/trending terms render as focusable chips rather than the phone's swipeable rows.
 */
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun TvSearchScreen(
    onBackClick: () -> Unit,
    onMediaClick: (com.example.vista.domain.model.MediaItem) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var typedQuery by rememberSaveable { mutableStateOf(uiState.query) }
    val resultGridState = rememberLazyGridState()
    var lastFocusedResult by rememberSaveable { mutableStateOf<String?>(null) }
    var lastGridQuery by rememberSaveable { mutableStateOf(typedQuery) }
    LaunchedEffect(typedQuery) {
        if (lastGridQuery != typedQuery) {
            lastFocusedResult = null
            resultGridState.scrollToItem(0)
            lastGridQuery = typedQuery
        }
    }
    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!spoken.isNullOrBlank()) {
            typedQuery = spoken
            viewModel.search(spoken)
        }
    }

    fun launchVoiceSearch() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Search Vista Watch")
        }
        runCatching { speechLauncher.launch(intent) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 64.dp, vertical = 40.dp)
    ) {
        // Header: back, title, voice
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onBackClick) {
                    Text("Back")
                }
                Spacer(Modifier.width(32.dp))
                Text(
                    text = "Search",
                    style = MaterialTheme.typography.displaySmall,
                    color = PrimaryText,
                    fontWeight = FontWeight.Black
                )
            }

            Button(onClick = { launchVoiceSearch() }) {
                Text("Voice Search")
            }
        }

        Spacer(Modifier.height(32.dp))

        // Typed query field (IME via remote/Bluetooth keyboard). The phone material3 field is reused —
        // tv-material3 has no OutlinedTextField.
        androidx.compose.material3.OutlinedTextField(
            value = typedQuery,
            onValueChange = {
                typedQuery = it
                viewModel.onQueryChange(it)
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Type a title, actor, or genre...", color = MutedText) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = com.example.vista.ui.theme.Accent,
                unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                focusedContainerColor = RaisedSurface,
                unfocusedContainerColor = RaisedSurface,
                focusedTextColor = PrimaryText,
                unfocusedTextColor = PrimaryText
            )
        )

        if (typedQuery.isBlank()) {
            Spacer(Modifier.height(32.dp))

            if (uiState.recentSearches.isNotEmpty()) {
                Text(
                    text = "Recent",
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                LazyRow(
                    state = rememberLazyListState(),
                    modifier = Modifier.focusRestorer().focusGroup(),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(uiState.recentSearches, key = { _, term -> term }) { index, term ->
                        Surface(
                            onClick = {
                                typedQuery = term
                                viewModel.search(term)
                            },
                            modifier = Modifier
                                .padding(bottom = 4.dp)
                        ) {
                            Text(
                                text = term,
                                color = PrimaryText,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(28.dp))
            }

            if (uiState.trendingSearches.isNotEmpty()) {
                Text(
                    text = "Trending Now",
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                LazyRow(
                    state = rememberLazyListState(),
                    modifier = Modifier.focusRestorer().focusGroup(),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(uiState.trendingSearches, key = { _, term -> term }) { index, term ->
                        Surface(
                            onClick = {
                                typedQuery = term
                                viewModel.search(term)
                            },
                            modifier = Modifier
                                .padding(bottom = 4.dp)
                        ) {
                            Text(
                                text = term,
                                color = PrimaryText,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            }
        } else {
            Spacer(Modifier.height(32.dp))

            val results = uiState.movies.map { it.toMediaItem() } +
                uiState.series.map { it.toMediaItem() }

            when {
                uiState.isSearching -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = com.example.vista.ui.theme.Accent
                        )
                    }
                }
                uiState.error != null -> {
                    Text(
                        text = "Search failed. Check your connection and try again.",
                        color = MutedText,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                results.isEmpty() -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Spacer(Modifier.height(48.dp))
                        Text(
                            text = "No results",
                            color = PrimaryText,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Try a different title or a voice search.",
                            color = MutedText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        state = resultGridState,
                        columns = GridCells.Adaptive(160.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.fillMaxSize().focusRestorer().focusGroup(),
                        // Cards scale to 1.08x on focus, so a focused cell in the last row or
                        // last column is clipped without this. Lazy grids scroll a focused item
                        // into view, but they do not account for its scaled bounds.
                        contentPadding = PaddingValues(
                            start = 24.dp,
                            end = 24.dp,
                            top = 24.dp,
                            bottom = 40.dp
                        )
                    ) {
                        items(results, key = { "${it.watchlistKey()}" }) { item ->
                            val focus = remember { FocusRequester() }
                            LaunchedEffect(Unit) { if (lastFocusedResult == item.watchlistKey()) focus.requestFocus() }
                            TvMediaCard(
                                item = item,
                                onClick = { onMediaClick(item) },
                                modifier = Modifier.focusRequester(focus),
                                onFocused = { lastFocusedResult = item.watchlistKey() }
                            )
                        }
                    }
                }
            }
        }
    }
}
