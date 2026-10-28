package com.example.vista.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.vista.domain.model.MediaType

sealed class Screen(val route: String) {
    // Bottom navigation
    object Home : Screen("home")
    object AiAssistant : Screen("assistant") {
        const val PATTERN = "assistant?mediaId={mediaId}&mediaType={mediaType}"
        fun companion(id: String, type: String) = "assistant?mediaId=${android.net.Uri.encode(id)}&mediaType=$type"
    }
    object Discover : Screen("discover") {
        /** Route pattern accepting an optional pre-selected [DiscoverTab][com.example.vista.ui.discover.DiscoverTab] name. */
        const val ROUTE_WITH_TAB = "discover?tab={tab}"

        /** Routes to Discover with a tab pre-selected from the home category chips. */
        fun createRoute(tabName: String) = "discover?tab=$tabName"
    }
    object Schedule : Screen("schedule")
    object Saved : Screen("saved")
    object Profile : Screen("profile")

    // Other screens
    object Splash : Screen("splash")
    object Auth : Screen("auth")
    object SetupProfile : Screen("setup_profile")
    object Search : Screen("search")
    object MovieDetails : Screen("movie_details/{movieId}") {
        fun createRoute(movieId: String) = "movie_details/$movieId"
    }
    object SeriesDetails : Screen("series_details/{seriesId}") {
        fun createRoute(seriesId: String) = "series_details/$seriesId"
    }
    object Player : Screen("player/{mediaId}/{title}?type={type}&season={season}&episode={episode}&poster={poster}") {
        /**
         * [mediaType] is required on purpose. The player must never infer the type from the
         * presence of a season number: TMDB movie and TV ids share one numeric namespace, so a
         * guess resolves to an unrelated title (this is what made series play random movies).
         */
        fun createRoute(
            mediaId: String,
            title: String,
            mediaType: MediaType,
            season: Int? = null,
            episode: Int? = null,
            poster: String? = null
        ) : String {
            val params = mutableListOf("type=${mediaType.name.lowercase()}")
            if (season != null) params.add("season=$season")
            if (episode != null) params.add("episode=$episode")
            if (poster != null) params.add("poster=${android.net.Uri.encode(poster)}")

            return "player/${android.net.Uri.encode(mediaId)}/" +
                "${android.net.Uri.encode(title)}?${params.joinToString("&")}"
        }
    }
    object Settings : Screen("settings?page={page}") {
        /**
         * Route pattern accepting an optional deep-linked [page] value. Opening a specific page
         * (e.g. from the notification bell or the profile menu) skips straight past the main list.
         */
        const val ROUTE_WITH_PAGE = "settings?page={page}"

        fun createRoute(page: String? = null) =
            if (page.isNullOrBlank()) "settings?page=" else "settings?page=$page"
    }
    object Onboarding : Screen("onboarding")
    object WatchHistory : Screen("watch_history")
    object Trailer : Screen("trailer/{videoKey}/{title}") {
        fun createRoute(videoKey: String, title: String) =
            "trailer/$videoKey/${android.net.Uri.encode(title)}"
    }
    object CastDetails : Screen("cast_details/{personId}") {
        fun createRoute(personId: String) = "cast_details/$personId"
    }
    object Library : Screen("library")
    object Stats : Screen("stats")

    /**
     * Where every milestone share prompt lands, from either a notification tap or an in-app open.
     *
     * Its own route rather than a dialog over the current screen so the notification always
     * delivers the same dedicated page, and so the viewer is never left guessing where to share.
     */
    object ShareMilestone : Screen("share_milestone/{milestoneId}") {
        fun createRoute(milestoneId: String) =
            "share_milestone/${android.net.Uri.encode(milestoneId)}"
    }
    object TvSearch : Screen("tv_search")
    object SeeAll : Screen("see_all/{sectionId}/{title}") {
        fun createRoute(sectionId: String, title: String) =
            "see_all/$sectionId/${android.net.Uri.encode(title)}"
    }
}

data class BottomNavItem(
    val screen: Screen,
    val title: String,
    val icon: ImageVector,
    /**
     * Route actually navigated to when the tab is tapped.
     *
     * Usually [Screen.route], but not always: `Screen.Settings.route` is the *pattern*
     * `settings?page={page}`, and navigating to it verbatim would send the literal text `{page}`
     * as the argument rather than matching the destination. Tabs whose route is a pattern must
     * override this with a concrete route.
     */
    val rootRoute: String = screen.route
) {
    companion object {
        /**
         * Ordered for one-handed reach: content tabs first, search next to them, then the personal
         * tab (Library) with Settings last as the least-used destination. Profile is omitted because
         * Settings is already a reachable tab.
         */
        val items = listOf(
            BottomNavItem(Screen.Home, "Home", Icons.Outlined.Home),
            BottomNavItem(Screen.Discover, "Discover", Icons.Outlined.Explore),
            BottomNavItem(Screen.AiAssistant, "AI Assistant", Icons.Outlined.AutoAwesome),
            BottomNavItem(Screen.Library, "Watch List", Icons.Outlined.VideoLibrary),
            BottomNavItem(Screen.Profile, "Profile", Icons.Outlined.Person)
        )
    }
}
