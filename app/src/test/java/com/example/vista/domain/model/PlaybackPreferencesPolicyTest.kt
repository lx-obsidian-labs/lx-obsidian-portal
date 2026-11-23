package com.example.vista.domain.model

import org.junit.Assert.*
import org.junit.Test

class PlaybackPreferencesPolicyTest {
    @Test fun legacyQualityLabelsStillApplyRealHeightPreference() {
        assertEquals(1080, PlaybackPreferencesPolicy.maxHeight("High (1080p)"))
        assertEquals(480, PlaybackPreferencesPolicy.maxHeight("Data Saver (480p)"))
        assertEquals("720p", PlaybackPreferencesPolicy.canonicalQuality("Standard (720p)"))
        assertEquals(Int.MAX_VALUE, PlaybackPreferencesPolicy.maxHeight("Auto"))
    }
    @Test fun userFacingLanguageLabelsMapToMediaTrackTags() {
        assertEquals("pt", PlaybackPreferencesPolicy.language("Portuguese"))
        assertEquals("ja", PlaybackPreferencesPolicy.language("Japanese"))
        assertNull(PlaybackPreferencesPolicy.language("Original"))
        assertNull(PlaybackPreferencesPolicy.language("Off"))
    }
}
