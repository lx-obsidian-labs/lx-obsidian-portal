package com.example.vista.ai

import org.junit.Assert.*
import org.junit.Test

class TasteRankerTest {
    @Test fun explicitPreferenceOutranksRepeatedInferredGenre() {
        assertTrue(TasteRanker.score(setOf("Comedy"), setOf("Comedy"), emptyMap()) > TasteRanker.score(setOf("Drama"), emptySet(), mapOf("Drama" to 100)))
    }
    @Test fun watchedAndDislikedTitlesAreExcludedWithoutMixingMovieAndSeriesIds() {
        assertFalse(TasteRanker.eligible("movie:1", setOf("movie:1"), emptySet()))
        assertTrue(TasteRanker.eligible("series:1", setOf("movie:1"), emptySet()))
        assertFalse(TasteRanker.eligible("series:1", emptySet(), setOf("series:1")))
    }
}
