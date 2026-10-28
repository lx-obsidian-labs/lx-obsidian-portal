package com.example.vista.ui.cast

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vista.domain.model.PersonDetails
import com.example.vista.domain.repository.CatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CastDetailsUiState(
    val isLoading: Boolean = true,
    val person: PersonDetails? = null,
    val error: String? = null
)

@HiltViewModel
class CastDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val catalogRepository: CatalogRepository
) : ViewModel() {

    private val personId: String = savedStateHandle["personId"] ?: ""

    private val _uiState = MutableStateFlow(CastDetailsUiState())
    val uiState: StateFlow<CastDetailsUiState> = _uiState.asStateFlow()

    init {
        if (personId.isNotEmpty()) {
            loadPersonDetails()
        } else {
            _uiState.update { it.copy(isLoading = false, error = "No person ID provided") }
        }
    }

    private fun loadPersonDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val person = catalogRepository.getPersonDetails(personId)
                if (person != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            person = person,
                            error = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Person not found"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load person details"
                    )
                }
            }
        }
    }

    fun retry() {
        loadPersonDetails()
    }
}
