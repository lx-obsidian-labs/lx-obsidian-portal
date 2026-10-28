package com.example.vista.ai

/** Pure ranking policy. Popularity/catalog ratings are not user preference signals. */
object TasteRanker {
    fun score(genres: Set<String>, explicit: Set<String>, inferred: Map<String, Int>): Int =
        genres.sumOf { (if (it in explicit) 10 else 0) + (inferred[it] ?: 0).coerceAtMost(5) }
    fun eligible(key: String, watched: Set<String>, disliked: Set<String>) = key !in watched && key !in disliked
}
