package com.example.vista.ai

/** Bounded conversation turn sent to the authenticated Cloudflare assistant. */
data class ChatMessage(val role: String, val content: String)
