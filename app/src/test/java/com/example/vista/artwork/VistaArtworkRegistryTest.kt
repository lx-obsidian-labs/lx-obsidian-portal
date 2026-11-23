package com.example.vista.artwork

import org.junit.Assert.*
import org.junit.Test

class VistaArtworkRegistryTest {
    @Test fun assistantIsReservedAndUnknownSelectionsUseTheExistingDefault() {
        assertFalse(VistaArtworkRegistry.selectable("ai_assistant"))
        assertEquals("default", VistaArtworkRegistry.avatar("ai_assistant").id)
        assertEquals("default", VistaArtworkRegistry.avatar("missing_future_avatar").id)
        assertEquals("male_01", VistaArtworkRegistry.avatar("male_01").id)
        assertEquals("female_01", VistaArtworkRegistry.avatar("female_01").id)
    }
    @Test fun mappingsAreUniqueAndPortraitsAreNeverRegisteredAsLandscapeBanners() {
        assertEquals(VistaArtwork.entries.size, VistaArtwork.entries.map { it.resourceName }.distinct().size)
        VistaArtwork.entries.forEach { assertTrue(it.resourceName.matches(Regex("[a-z_0-9]+"))) }
        assertEquals(ArtworkKind.PORTRAIT, VistaArtwork.ONBOARDING.kind)
        assertEquals(ArtworkKind.AVATAR, VistaArtwork.AI_AVATAR.kind)
        assertEquals(ArtworkKind.LANDSCAPE, VistaArtwork.MOVIE_NIGHT.kind)
    }
    @Test fun rotationUsesOneCampaignAndNeverSelectsAnUnavailableAlternative() {
        assertEquals(VistaArtwork.MOVIE_NIGHT, VistaArtworkRegistry.movieNight(1, setOf(VistaArtwork.MOVIE_NIGHT)))
        assertEquals(VistaArtwork.RELAX, VistaArtworkRegistry.movieNight(1, setOf(VistaArtwork.MOVIE_NIGHT, VistaArtwork.RELAX)))
        assertEquals(VistaArtwork.MOVIE_NIGHT, VistaArtworkRegistry.movieNight(2, setOf(VistaArtwork.MOVIE_NIGHT, VistaArtwork.RELAX)))
    }
}
