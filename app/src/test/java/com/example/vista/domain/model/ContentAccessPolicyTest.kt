package com.example.vista.domain.model

import org.junit.Assert.*
import org.junit.Test

class ContentAccessPolicyTest {
    @Test fun kidsModeBlocksMatureAndAdultTitles() {
        val settings = AppSettings(kidsMode = true, showAdultContent = true)
        assertFalse(ContentAccessPolicy.allows(settings, "PG-13", false))
        assertFalse(ContentAccessPolicy.allows(settings, "TV-MA", false))
        assertFalse(ContentAccessPolicy.allows(settings, null, true))
        assertTrue(ContentAccessPolicy.allows(settings, null, false))
        assertTrue(ContentAccessPolicy.allows(settings, "G", false))
        assertTrue(ContentAccessPolicy.allows(settings, "TV-Y7", false))
    }

    @Test fun childBandForcesFamilyFriendlyContent() {
        val settings = AppSettings(ageBand = AgeBand.CHILD, showAdultContent = true)
        assertFalse(ContentAccessPolicy.allows(settings, "NC-17", false))
        assertTrue(ContentAccessPolicy.allows(settings, null, false))
        assertTrue(ContentAccessPolicy.allows(settings, "PG", false))
    }

    @Test fun teenBandBlocksOnlyExplicitAdultRatings() {
        val settings = AppSettings(ageBand = AgeBand.TEEN)
        assertFalse(ContentAccessPolicy.allows(settings, "NC-17", false))
        assertFalse(ContentAccessPolicy.allows(settings, "TV-MA", false))
        assertFalse(ContentAccessPolicy.allows(settings, "NC-17", false))
        assertTrue(ContentAccessPolicy.allows(settings, null, false))
        assertTrue(ContentAccessPolicy.allows(settings, "PG-13", false))
        assertTrue(ContentAccessPolicy.allows(settings, "TV-14", false))
    }

    @Test fun adultBandRespectsMatureOptIn() {
        val off = AppSettings(ageBand = AgeBand.ADULT, showAdultContent = false)
        val on = AppSettings(ageBand = AgeBand.ADULT, showAdultContent = true)
        assertTrue(ContentAccessPolicy.allows(off, "R", false))
        assertFalse(ContentAccessPolicy.allows(off, "NC-17", false))
        assertTrue(ContentAccessPolicy.allows(on, "NC-17", false))
        assertTrue(ContentAccessPolicy.allows(on, null, true))
    }
}
