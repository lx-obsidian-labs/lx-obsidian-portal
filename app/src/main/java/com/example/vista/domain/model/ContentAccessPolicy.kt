package com.example.vista.domain.model

object ContentAccessPolicy {
    private val adultRatings = setOf("NC-17", "18", "18+", "TV-MA", "MA15+")
    private val kidsAllowedRatings = setOf("G", "PG", "TV-Y", "TV-Y7", "TV-G", "TV-PG", "7", "U", "E")
    private val teenAllowedRatings = kidsAllowedRatings + setOf("PG-13", "TV-14", "13", "13+", "T", "E10+")

    private fun normalized(rating: String?) = rating?.trim()?.uppercase().orEmpty()

    fun isMature(ageRating: String?, isAdult: Boolean = false): Boolean =
        isAdult || normalized(ageRating) in adultRatings

    fun isKidsSafe(ageRating: String?, isAdult: Boolean = false): Boolean {
        if (isAdult) return false
        val rating = normalized(ageRating)
        return rating.isEmpty() || rating in kidsAllowedRatings
    }

    fun allows(settings: AppSettings, ageRating: String?, isAdult: Boolean): Boolean {
        if (settings.kidsMode || settings.ageBand == AgeBand.CHILD) return isKidsSafe(ageRating, isAdult)
        if (settings.ageBand == AgeBand.TEEN) {
            val rating = normalized(ageRating)
            return !isAdult && (rating.isEmpty() || rating in teenAllowedRatings)
        }
        return settings.showAdultContent || !isMature(ageRating, isAdult)
    }

    fun allows(settings: AppSettings, item: MediaItem): Boolean = when (item) {
        is MediaItem.MovieItem -> allows(settings, item.ageRating, item.isAdult)
        is MediaItem.SeriesItem -> allows(settings, item.ageRating, false)
        else -> true
    }
}
