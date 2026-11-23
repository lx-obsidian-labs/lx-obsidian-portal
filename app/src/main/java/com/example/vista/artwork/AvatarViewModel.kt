package com.example.vista.artwork

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AvatarUiState(val selectedId: String = "default", val saving: Boolean = false,
    val error: String? = null, val message: String? = null)

@HiltViewModel
class AvatarViewModel @Inject constructor(private val repository: AvatarRepository) : ViewModel() {
    private val _state = MutableStateFlow(AvatarUiState())
    val state = _state.asStateFlow()
    init { viewModelScope.launch { repository.selectedId.collect { id -> _state.update { it.copy(selectedId = id, message = null, error = null) } } } }
    fun retrySync() { viewModelScope.launch { repository.retrySync() } }
    fun save(id: String) {
        if (_state.value.saving) return
        _state.update { it.copy(saving = true, error = null, message = null) }
        viewModelScope.launch {
            try {
                val message = repository.save(id)
                _state.update { it.copy(saving = false, message = message) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _state.update { it.copy(saving = false, error = "Your avatar couldn't be saved. Please try again.") } }
        }
    }
}
