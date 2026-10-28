package com.example.vista.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks the limits applied to guest (signed-out) viewing.
 *
 * A guest gets a fixed amount of playback **per app session**. The counter lives in memory, so it
 * resets when the process is recreated — which is the intended meaning of "session limit": a guest
 * can sample the catalogue, but sustained viewing requires an account.
 *
 * Ads are also shown more aggressively to guests; [adIntervalMs] and [adSessionLimit] are read by
 * [AdManager] so the two policies stay in one place.
 */
@Singleton
class GuestSessionManager @Inject constructor() {

    companion object {
        /** Playback a guest may watch before being asked to sign up. */
        const val GUEST_PLAYBACK_CAP_SECONDS = 45 * 60

        /** Interstitial cadence for guests: more ads, more often. */
        const val GUEST_AD_INTERVAL_MS = 2 * 60 * 1000L
        const val GUEST_AD_SESSION_LIMIT = 8

        /** Interstitial cadence for signed-in viewers. */
        const val MEMBER_AD_INTERVAL_MS = 5 * 60 * 1000L
        const val MEMBER_AD_SESSION_LIMIT = 5
    }

    private val _watchedSeconds = MutableStateFlow(0)
    val watchedSeconds: StateFlow<Int> = _watchedSeconds.asStateFlow()

    private val _limitReached = MutableStateFlow(false)
    val limitReached: StateFlow<Boolean> = _limitReached.asStateFlow()

    val capSeconds: Int get() = GUEST_PLAYBACK_CAP_SECONDS

    val remainingSeconds: Int
        get() = (GUEST_PLAYBACK_CAP_SECONDS - _watchedSeconds.value).coerceAtLeast(0)

    val remainingMinutes: Int
        get() = (remainingSeconds + 59) / 60

    /**
     * Adds watched time. Returns true once the session cap has been reached, so callers can react
     * immediately instead of waiting for the next state emission.
     */
    fun recordPlayback(seconds: Int): Boolean {
        if (_limitReached.value || seconds <= 0) return _limitReached.value
        _watchedSeconds.value = _watchedSeconds.value + seconds
        if (_watchedSeconds.value >= GUEST_PLAYBACK_CAP_SECONDS) {
            _limitReached.value = true
        }
        return _limitReached.value
    }

    fun adIntervalMs(guest: Boolean): Long =
        if (guest) GUEST_AD_INTERVAL_MS else MEMBER_AD_INTERVAL_MS

    fun adSessionLimit(guest: Boolean): Int =
        if (guest) GUEST_AD_SESSION_LIMIT else MEMBER_AD_SESSION_LIMIT

    /** Starts a fresh session. Called when a guest signs up so their new account is unrestricted. */
    fun resetSession() {
        _watchedSeconds.value = 0
        _limitReached.value = false
        AdManager.resetSession()
    }
}
