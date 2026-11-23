package com.example.vista.artwork

import android.content.Context
import androidx.annotation.DrawableRes

enum class ArtworkKind { PORTRAIT, LANDSCAPE, AVATAR }

enum class VistaArtwork(val resourceName: String, val kind: ArtworkKind) {
    ONBOARDING("vista_onboarding_portrait", ArtworkKind.PORTRAIT),
    ONBOARDING_ALT("vista_onboarding_portrait_alt", ArtworkKind.PORTRAIT),
    MOVIE_NIGHT("vista_banner_movie_night", ArtworkKind.LANDSCAPE),
    RELAX("vista_banner_relax", ArtworkKind.LANDSCAPE),
    DISCOVER("vista_banner_discover", ArtworkKind.LANDSCAPE),
    CHRISTMAS("vista_season_christmas", ArtworkKind.LANDSCAPE),
    CHRISTMAS_ALT("vista_season_christmas_alt", ArtworkKind.LANDSCAPE),
    PREMIUM("vista_premium_background", ArtworkKind.LANDSCAPE),
    AI_DISCOVERY("vista_ai_discovery", ArtworkKind.LANDSCAPE),
    AI_AVATAR("vista_avatar_ai_assistant", ArtworkKind.AVATAR),
    MALE_AVATAR("vista_avatar_male_01", ArtworkKind.AVATAR),
    FEMALE_AVATAR("vista_avatar_female_01", ArtworkKind.AVATAR),
    FOX("vista_avatar_fox_01", ArtworkKind.AVATAR),
    OWL("vista_avatar_owl_01", ArtworkKind.AVATAR),
    ORBIT("vista_avatar_orbit_01", ArtworkKind.AVATAR),
    EMBER("vista_avatar_ember_01", ArtworkKind.AVATAR),
    SUMMIT("vista_avatar_summit_01", ArtworkKind.AVATAR),
    MOON("vista_avatar_moon_01", ArtworkKind.AVATAR)
}

data class VistaAvatar(val id: String, val label: String, val artwork: VistaArtwork?)

/** Stable identifiers belong in persistence; resource integers are resolved only for display. */
object VistaArtworkRegistry {
    val userAvatars = listOf(
        VistaAvatar("default", "Classic — default profile icon", null),
        VistaAvatar("male_01", "Cinematic — portrait in a black hoodie", VistaArtwork.MALE_AVATAR),
        VistaAvatar("female_01", "Dreamer — portrait with braided hair", VistaArtwork.FEMALE_AVATAR),
        VistaAvatar("fox_01", "Explorer — original geometric fox", VistaArtwork.FOX),
        VistaAvatar("owl_01", "Night Owl — original owl illustration", VistaArtwork.OWL),
        VistaAvatar("orbit_01", "Orbit — red planet and orbit", VistaArtwork.ORBIT),
        VistaAvatar("ember_01", "Ember — red and white flame", VistaArtwork.EMBER),
        VistaAvatar("summit_01", "Summit — mountain landscape", VistaArtwork.SUMMIT),
        VistaAvatar("moon_01", "Moonlight — crescent over red mountains", VistaArtwork.MOON)
    )
    val assistant = VistaAvatar("ai_assistant", "Vista AI assistant", VistaArtwork.AI_AVATAR)
    fun avatar(id: String): VistaAvatar = userAvatars.firstOrNull { it.id == id } ?: userAvatars.first()
    fun selectable(id: String): Boolean = userAvatars.any { it.id == id }

    @DrawableRes
    fun resolve(context: Context, artwork: VistaArtwork): Int? {
        @Suppress("DiscouragedApi")
        val id = context.resources.getIdentifier(artwork.resourceName, "drawable", context.packageName)
        return id.takeIf { it != 0 }
    }

    fun movieNight(day: Long, available: Set<VistaArtwork>): VistaArtwork =
        if (day % 2L != 0L && VistaArtwork.RELAX in available) VistaArtwork.RELAX else VistaArtwork.MOVIE_NIGHT
}
