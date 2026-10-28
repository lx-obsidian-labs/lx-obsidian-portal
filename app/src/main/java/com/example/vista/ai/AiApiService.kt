package com.example.vista.ai

/** UI conversation item. Backend request/tool contracts are defined in milestone 1. */
data class ChatMessage(val role: String, val content: String)
