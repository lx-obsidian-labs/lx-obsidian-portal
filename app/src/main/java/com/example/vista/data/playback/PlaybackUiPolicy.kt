package com.example.vista.data.playback

object PlaybackUiPolicy {
    fun canAutoplay(ended: Boolean, enabled: Boolean, hasNext: Boolean, resumed: Boolean,
        guestBlocked: Boolean, needsDataConsent: Boolean, hasError: Boolean, recovering: Boolean): Boolean =
        ended && enabled && hasNext && resumed && !guestBlocked && !needsDataConsent && !hasError && !recovering

    fun opensTvOptions(keyCode: Int, keyDown: Boolean, longPress: Boolean): Boolean =
        keyDown && (keyCode == 82 || ((keyCode == 23 || keyCode == 66) && longPress))
}
