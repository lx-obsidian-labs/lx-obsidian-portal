package com.example.vista.util

import java.net.URI

object NotificationLinks {

    private val allowedHosts = setOf("lxobsidianportal.co.za", "www.lxobsidianportal.co.za", "play.google.com")

    fun resolve(raw: String?): String? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null
        val uri = runCatching { URI(value) }.getOrNull() ?: return null
        if (!uri.scheme.equals("https", ignoreCase = true)) return null
        if (uri.userInfo != null || uri.port != -1) return null
        val host = uri.host?.lowercase() ?: return null
        if (host !in allowedHosts) return null
        val path = uri.rawPath.orEmpty()
        val query = uri.rawQuery?.let { "?$it" }.orEmpty()
        val fragment = uri.rawFragment?.let { "#$it" }.orEmpty()
        return URI("https://$host$path$query$fragment").toASCIIString()
    }
}
