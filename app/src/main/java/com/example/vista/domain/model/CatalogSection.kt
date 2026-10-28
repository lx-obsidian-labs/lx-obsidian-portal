package com.example.vista.domain.model

/**
 * Every browsable catalogue row, declared once.
 *
 * Home, "See All" and "load more" previously each kept their own `when (sectionId)` map. They
 * drifted out of sync: rows were renamed on the home screen while the see-all screen kept matching
 * the old key, so tapping "See All" landed on an empty page (and load-more silently did nothing).
 * Defining the rows here means the label, the query and the row type cannot disagree.
 */
enum class CatalogSection(
    val id: String,
    val title: String,
    val type: HomeSectionType
) {
    TRENDING_MOVIES("trending_movies", "Trending Movies", HomeSectionType.TRENDING),
    TRENDING_SERIES("trending_series", "Trending Series", HomeSectionType.TRENDING),
    TVMAZE_TRENDING("tvmaze_trending", "Premiere Series", HomeSectionType.TVMAZE_TRENDING),
    NEW_RELEASES("new_releases", "New Releases", HomeSectionType.NEW_RELEASES),
    POPULAR_MOVIES("popular_movies", "Popular Movies", HomeSectionType.POPULAR_MOVIES),
    POPULAR_SERIES("popular_series", "Popular Series", HomeSectionType.POPULAR_SERIES),
    TOP_RATED_MOVIES("top_rated_movies", "Top Rated Movies", HomeSectionType.RECOMMENDED),
    ACTION("genre_action", "Action & Adventure", HomeSectionType.GENRE_BASED),
    COMEDY("genre_comedy", "Comedy Picks", HomeSectionType.GENRE_BASED),
    DRAMA_SERIES("genre_drama_series", "Drama Series", HomeSectionType.GENRE_BASED),
    HORROR("genre_horror", "Horror", HomeSectionType.GENRE_BASED),
    ANIMATION("genre_animation", "Animation", HomeSectionType.GENRE_BASED);

    companion object {
        /** Rows shown in the hero carousel are curated from the trending list. */
        const val FEATURED_ID = "featured"

        fun fromId(id: String): CatalogSection? = entries.firstOrNull { it.id == id }
    }
}
