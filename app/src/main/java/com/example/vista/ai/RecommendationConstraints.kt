package com.example.vista.ai

data class RecommendationConstraints(val maxRuntimeMinutes: Int? = null, val yearFrom: Int? = null, val yearTo: Int? = null) {
    fun accepts(runtime: Int?, year: Int?): Boolean =
        (maxRuntimeMinutes == null || (runtime != null && runtime in 1..maxRuntimeMinutes)) &&
            (yearFrom == null || (year != null && year >= yearFrom)) &&
            (yearTo == null || (year != null && year <= yearTo))

    companion object {
        fun parse(hint: String): RecommendationConstraints {
            val duration = Regex("\\b(under|less than|below|up to|at most)\\s+(\\d{1,3})\\s*(minutes?|mins?|hours?|hrs?)\\b", RegexOption.IGNORE_CASE).find(hint)
            val maximum = duration?.let {
                val count = it.groupValues[2].toInt()
                val minutes = count * if (it.groupValues[3].lowercase().startsWith("h")) 60 else 1
                (minutes - if (it.groupValues[1].lowercase() in setOf("under", "less than", "below")) 1 else 0).coerceIn(1, 600)
            } ?: if (hint.contains("short", true)) 100 else null
            val range = Regex("\\b((?:19|20)\\d{2})\\s*(?:-|–|to)\\s*((?:19|20)\\d{2})\\b").find(hint)
            val year = Regex("\\b(?:from|released in|made in)\\s+((?:19|20)\\d{2})\\b", RegexOption.IGNORE_CASE).find(hint)?.groupValues?.get(1)?.toInt()
            val years = range?.let { listOf(it.groupValues[1].toInt(), it.groupValues[2].toInt()).sorted() }
            return RecommendationConstraints(maximum, years?.first() ?: year, years?.last() ?: year)
        }
    }
}
