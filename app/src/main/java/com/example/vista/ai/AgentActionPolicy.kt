package com.example.vista.ai

/** Local intent/reference rules supplement the model; only catalog-backed app actions are offered. */
object AgentActionPolicy {
    fun isNewsFollowup(message: String, history: List<ChatMessage>): Boolean =
        (Regex("^(tell me more|what.s confirmed|what is confirmed|is (it|that) confirmed|what about|any updates|explain (this|that)|what does (this|that) mean)\\b", RegexOption.IGNORE_CASE).containsMatchIn(message.trim()) ||
            Regex("\\b(first|second|third|fourth|fifth) (story|article|report|one)\\b", RegexOption.IGNORE_CASE).containsMatchIn(message)) &&
            history.takeLast(8).any { it.role == "user" && Regex("\\b(news|headlines|renewed|announcement|casting)\\b", RegexOption.IGNORE_CASE).containsMatchIn(it.content) }
    fun isNewsRequest(message: String, history: List<ChatMessage> = emptyList()): Boolean {
        if (Regex("^(save|remove|download|open|resume|play|compare|find|search)\\b", RegexOption.IGNORE_CASE).containsMatchIn(message.trim())) return false
        val news = Regex("\\b(news|headlines|announced|announcement|renewed|renewal|cancelled|canceled|casting|release date|trailer news)\\b", RegexOption.IGNORE_CASE)
        return news.containsMatchIn(message) || isNewsFollowup(message, history) ||
            (Regex("^(tell me more|what.s confirmed|what is confirmed|is (it|that) confirmed|what about|any updates|and in south africa)\\b", RegexOption.IGNORE_CASE).containsMatchIn(message.trim()) &&
                history.takeLast(4).any { it.role == "user" && news.containsMatchIn(it.content) })
    }
    val tools = setOf("help", "search", "recommend", "cast", "plot", "save", "remove", "navigate", "details", "download", "watchlist", "continue", "compare")
    fun localPlan(message: String): AssistantPlan {
        val text = message.trim()
        val lower = text.lowercase()
        val tool = when {
            isNewsRequest(message) -> "help"
            Regex("\\b(download|offline)\\b").containsMatchIn(lower) -> "download"
            Regex("^(save|add to (my )?(watch ?list|list))\\b").containsMatchIn(lower) -> "save"
            Regex("^(remove|unsave)\\b").containsMatchIn(lower) -> "remove"
            Regex("^(continue watching|resume( watching)?)$").matches(lower) -> "continue"
            lower in setOf("my watchlist", "my watch list", "show my watchlist", "show my watch list") -> "watchlist"
            lower.startsWith("cast") || lower.startsWith("who stars") -> "cast"
            lower.startsWith("plot") -> "plot"
            lower.startsWith("compare ") -> "compare"
            lower.startsWith("recommend") || lower.startsWith("suggest") || lower.contains("in the mood") -> "recommend"
            lower.startsWith("open ") || lower.startsWith("details ") -> "details"
            lower in setOf("hi", "hello", "hey", "help") -> "help"
            else -> "search"
        }
        val ordinal = listOf("first", "second", "third", "fourth", "fifth", "sixth", "seventh", "eighth")
            .indexOfFirst { Regex("\\b$it\\s+(one|movie|film|title)\\b").containsMatchIn(lower) ||
                Regex("\\bthe $it[.!?]?$").containsMatchIn(lower) }.takeIf { it >= 0 }?.plus(1)
        val query = text.replace(Regex("^(search( for)?|find|download|save|remove|unsave|cast( of)?|plot( of)?|details( for)?|open|compare)\\s+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+(for offline( viewing)?|to my (watch ?list|list))$", RegexOption.IGNORE_CASE), "")
        return AssistantPlan(tool, query.take(200), "all", ordinal)
    }

    fun referencesPrevious(message: String): Boolean = Regex(
        "\\b(it|that one|this one|the (first|second|third|fourth|fifth|sixth|seventh|eighth)( one| movie| film| title)?)\\b",
        RegexOption.IGNORE_CASE).containsMatchIn(message)
}
