package com.example.vista.ai

import org.junit.Assert.*
import org.junit.Test

class RecommendationConstraintsTest {
    @Test fun hardDurationAndYearFiltersNeverTreatUnknownsAsMatches() {
        val filters = RecommendationConstraints.parse("comedy under 90 minutes from 2020")
        assertTrue(filters.accepts(89, 2020))
        assertFalse(filters.accepts(90, 2020))
        assertFalse(filters.accepts(null, 2020))
        assertFalse(filters.accepts(80, null))
        assertFalse(filters.accepts(80, 2019))
    }
    @Test fun hoursAndInclusiveYearRangesAreSupported() {
        val filters = RecommendationConstraints.parse("up to 2 hours, 1990–2000")
        assertTrue(filters.accepts(120, 1990))
        assertTrue(filters.accepts(90, 2000))
        assertFalse(filters.accepts(121, 1995))
        assertFalse(filters.accepts(90, 2001))
    }
}
