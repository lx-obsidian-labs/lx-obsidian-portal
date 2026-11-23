package com.example.vista.ai

import com.google.gson.JsonObject
import java.net.URI
import java.time.Instant

/** Optional v2 evidence must not break a usable v1 text/action response. */
object AiReplyDecoder {
    private fun JsonObject.text(key: String): String? = get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString

    fun enrich(reply: AssistantReply, response: JsonObject): AssistantReply {
        val sources = response.get("sources")?.takeIf { it.isJsonArray }?.asJsonArray?.take(5)?.mapNotNull { element ->
            runCatching {
                val source = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@runCatching null
                val url = source.text("url")?.takeIf { it.length <= 2000 } ?: return@runCatching null
                val uri = URI(url)
                val host = uri.host ?: return@runCatching null
                if (uri.scheme != "https" || uri.userInfo != null || uri.port != -1 ||
                    !Regex("[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}").matches(host) ||
                    Regex("(^|\\.)(localhost|local|internal|test|example|invalid)$", RegexOption.IGNORE_CASE).containsMatchIn(host)) return@runCatching null
                val title = source.text("title")?.trim()?.take(240)?.takeIf { it.isNotBlank() } ?: return@runCatching null
                val published = source.text("publishedAt")?.let { Instant.parse(it).toString() } ?: return@runCatching null
                AssistantSource(title, url, source.text("publisher")?.take(100) ?: host, published,
                    source.text("id")?.take(2000)?.takeIf { it.isNotBlank() } ?: url)
            }.getOrNull()
        }.orEmpty()
        val followups = response.get("followups")?.takeIf { it.isJsonArray }?.asJsonArray?.take(3)?.mapNotNull {
            it.takeIf { item -> item.isJsonPrimitive && item.asJsonPrimitive.isString }?.asString?.trim()?.take(160)?.takeIf(String::isNotBlank)
        }.orEmpty()
        return reply.copy(sources = sources, followups = followups,
            newsFeed = response.text("newsFeed")?.takeIf { it in setOf("global", "za") } ?: "global",
            retrievedAt = runCatching { response.text("retrievedAt")?.let { Instant.parse(it).toString() } }.getOrNull())
    }
}
