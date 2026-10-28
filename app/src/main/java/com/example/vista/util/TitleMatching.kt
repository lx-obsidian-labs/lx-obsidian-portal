package com.example.vista.util

/** Normalised comparison key for a release title. */
fun String.titleKey(): String = lowercase().filter { it.isLetterOrDigit() }

/**
 * Whether two catalogue entries refer to the same release.
 *
 * The app merges catalogues that have entirely unrelated id spaces (TMDB numeric ids and local
 * slugs), so the same film arrives twice with two different ids. Matching on the normalised title
 * plus the release year collapses those duplicates; an unknown year on either side counts as a
 * match so an entry with missing metadata still deduplicates.
 */
fun sameTitle(aTitle: String, aYear: Int?, bTitle: String, bYear: Int?): Boolean {
    if (aTitle.titleKey().isEmpty()) return false
    if (aTitle.titleKey() != bTitle.titleKey()) return false
    return aYear == null || bYear == null || aYear == bYear
}

/**
 * Removes duplicate titles, keeping the first occurrence. Callers pass their list in preference
 * order, so the surviving entry is the one from the preferred source.
 */
fun <T> List<T>.dedupeByTitle(title: (T) -> String, year: (T) -> Int?): List<T> {
    val result = mutableListOf<T>()
    for (item in this) {
        val duplicate = result.any { sameTitle(title(it), year(it), title(item), year(item)) }
        if (!duplicate) result.add(item)
    }
    return result
}
