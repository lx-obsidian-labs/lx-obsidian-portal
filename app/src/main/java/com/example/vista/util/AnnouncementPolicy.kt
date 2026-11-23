package com.example.vista.util

object AnnouncementPolicy {
    fun obsoleteRelease(title: String, body: String, installedVersion: String): Boolean {
        val text = "$title $body"
        val version = Regex("(?<![\\d.])1\\.0\\.(\\d+)(?![\\d.])").find(text)?.groupValues?.get(1)?.toIntOrNull() ?: return false
        val installed = installedVersion.removePrefix("1.0.").toIntOrNull() ?: return false
        val releaseLanguage = listOf("coming", "update", "release", "install", "launch", "available")
            .any { text.contains(it, ignoreCase = true) }
        return releaseLanguage && version < installed
    }
}
