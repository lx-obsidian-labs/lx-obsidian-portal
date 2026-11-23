package com.example.vista.ui.settings

data class SettingsDestination(val page: SettingsPage, val title: String, val description: String, val keywords: String)

object SettingsDirectory {
    val destinations = listOf(
        SettingsDestination(SettingsPage.PLAYBACK, "Playback", "Quality, autoplay, speed and seek controls", "video streaming resume data saver"),
        SettingsDestination(SettingsPage.AUDIO_SUBTITLES, "Audio & subtitles", "Language, captions, size and colour", "sound text accessibility"),
        SettingsDestination(SettingsPage.DOWNLOADS, "Downloads", "Offline quality, storage and download library", "offline pause resume delete wifi unmetered"),
        SettingsDestination(SettingsPage.AI_VOICE, "AI & voice", "Assistant, personalization, genres and device voices", "agent actions taste recommendations microphone tts"),
        SettingsDestination(SettingsPage.NOTIFICATIONS, "Notifications", "Device permission, announcements and daily reminders", "push fcm alerts"),
        SettingsDestination(SettingsPage.DATA_STORAGE, "Data & storage", "Mobile-data policy and temporary-cache maintenance", "cellular internet wifi cache repair space"),
        SettingsDestination(SettingsPage.FAMILY, "Family & age", "Age band, parental PIN and kids-only mode", "child children kids teen age rating parental pin mature restriction"),
        SettingsDestination(SettingsPage.APPEARANCE, "Appearance", "Theme and festive collections", "dark light oled system christmas seasonal"),
        SettingsDestination(SettingsPage.PRIVACY_HISTORY, "Privacy & history", "Diagnostics and local viewing/search records", "clear delete recent watched"),
        SettingsDestination(SettingsPage.ACCOUNT, "Account", "Profile and sign-in", "email password sign out guest"),
        SettingsDestination(SettingsPage.SUPPORT, "Help & Support", "Bug reports, account help and official contacts", "email contact playback installation privacy legal faq support"),
        SettingsDestination(SettingsPage.ABOUT, "About Vista Watch", "Version, update check and attribution", "release install build help")
    )
    fun search(query: String): List<SettingsDestination> {
        val words = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        return destinations.filter { entry -> words.all { it in "${entry.title} ${entry.description} ${entry.keywords}".lowercase() } }
    }
}
