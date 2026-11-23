package com.example.vista.feedback

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

data class FeedbackState(
    val answers: Map<String, String> = emptyMap(), val comment: String = "",
    val quoteAllowed: Boolean = false, val sending: Boolean = false,
    val sent: Boolean = false, val hidden: Boolean = false, val error: String? = null
)

@HiltViewModel
class FeedbackViewModel @Inject constructor(
    private val repository: FeedbackRepository,
    private val auth: FirebaseAuth,
    @ApplicationContext context: Context
) : ViewModel() {
    private val preferences = context.getSharedPreferences("vista_feedback", Context.MODE_PRIVATE)
    private fun statusKey() = "${FeedbackPoll.ID}:${auth.currentUser?.uid ?: "guest"}"
    private fun initialState() = FeedbackState(hidden = preferences.getBoolean(statusKey(), false),
        sent = preferences.getBoolean("${statusKey()}:sent", false))
    private val _state = MutableStateFlow(initialState())
    val state = _state.asStateFlow()
    private val listener = FirebaseAuth.AuthStateListener {
        _state.value = initialState()
    }
    init { auth.addAuthStateListener(listener) }

    fun answer(question: String, option: String) {
        if (!_state.value.sending) _state.update { it.copy(answers = it.answers + (question to option), error = null) }
    }
    fun comment(value: String) { if (!_state.value.sending) _state.update { it.copy(comment = value.take(FeedbackPoll.MAX_COMMENT)) } }
    fun quoteAllowed(value: Boolean) { if (!_state.value.sending) _state.update { it.copy(quoteAllowed = value) } }
    fun dismiss() {
        if (_state.value.sending) return
        preferences.edit().putBoolean(statusKey(), true).apply()
        _state.update { it.copy(hidden = true) }
    }
    fun submit() {
        val snapshot = _state.value
        if (snapshot.sending || snapshot.sent) return
        if (!FeedbackPoll.valid(snapshot.answers, snapshot.comment)) {
            _state.update { it.copy(error = "Please choose one answer for each question.") }; return
        }
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _state.update { it.copy(error = "Sign in to send your feedback. Your answers are still here.") }; return
        }
        viewModelScope.launch {
            _state.update { it.copy(sending = true, error = null) }
            try {
                withTimeout(20_000) { repository.submit(snapshot.answers, snapshot.comment, snapshot.quoteAllowed) }
                if (auth.currentUser?.uid != uid) return@launch
                preferences.edit().putBoolean(statusKey(), true).putBoolean("${statusKey()}:sent", true).apply()
                _state.update { it.copy(sending = false, sent = true) }
            } catch (_: TimeoutCancellationException) {
                if (auth.currentUser?.uid == uid) _state.update { it.copy(sending = false, error = "We couldn't confirm delivery. Check your connection and try again; you won't be counted twice.") }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                if (auth.currentUser?.uid == uid) _state.update { it.copy(sending = false, error = "Your feedback couldn't be sent. Check your connection and try again. Your answers are still here.") }
            }
        }
    }
    override fun onCleared() { auth.removeAuthStateListener(listener); super.onCleared() }
}
