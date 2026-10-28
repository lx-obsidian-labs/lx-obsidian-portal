package com.example.vista.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

/**
 * Custom navigation transitions for Vista Cinema app.
 * 
 * Transitions:
 * - Main tabs (Home, Discover, Saved, Search): Cross-fade with slight scale
 * - Detail screens: Slide from right
 * - Settings: Slide up from bottom
 * - Player: Fade (full screen experience)
 */

// Duration constants
private const val FAST_TRANSITION_DURATION = 200
private const val NORMAL_TRANSITION_DURATION = 300
private const val SLOW_TRANSITION_DURATION = 400

/**
 * Fade transition for main tab navigation.
 * Provides a smooth cross-fade effect when switching between main tabs.
 */
fun NavGraphBuilder.composableWithFadeTransition(
    route: String,
    arguments: List<androidx.navigation.NamedNavArgument> = emptyList(),
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {
    composable(
        route = route,
        arguments = arguments,
        enterTransition = {
            fadeIn(animationSpec = tween(NORMAL_TRANSITION_DURATION)) +
                scaleIn(
                    initialScale = 0.95f,
                    animationSpec = tween(NORMAL_TRANSITION_DURATION)
                )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(NORMAL_TRANSITION_DURATION))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(NORMAL_TRANSITION_DURATION)) +
                scaleIn(
                    initialScale = 0.95f,
                    animationSpec = tween(NORMAL_TRANSITION_DURATION)
                )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(NORMAL_TRANSITION_DURATION))
        },
        content = content
    )
}

/**
 * Slide transition for detail screens (Movie, Series details).
 * Provides a slide-from-right effect when navigating to detail screens.
 */
fun NavGraphBuilder.composableWithSlideTransition(
    route: String,
    arguments: List<androidx.navigation.NamedNavArgument> = emptyList(),
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {
    composable(
        route = route,
        arguments = arguments,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth / 3 },
                animationSpec = tween(NORMAL_TRANSITION_DURATION)
            ) + fadeIn(
                animationSpec = tween(NORMAL_TRANSITION_DURATION)
            )
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth / 3 },
                animationSpec = tween(NORMAL_TRANSITION_DURATION)
            ) + fadeOut(
                animationSpec = tween(FAST_TRANSITION_DURATION)
            )
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth / 3 },
                animationSpec = tween(NORMAL_TRANSITION_DURATION)
            ) + fadeIn(
                animationSpec = tween(NORMAL_TRANSITION_DURATION)
            )
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth / 3 },
                animationSpec = tween(NORMAL_TRANSITION_DURATION)
            ) + fadeOut(
                animationSpec = tween(FAST_TRANSITION_DURATION)
            )
        },
        content = content
    )
}

/**
 * Slide-up transition for settings and modal screens.
 * Provides a slide-up-from-bottom effect for overlay screens.
 */
fun NavGraphBuilder.composableWithSlideUpTransition(
    route: String,
    arguments: List<androidx.navigation.NamedNavArgument> = emptyList(),
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {
    composable(
        route = route,
        arguments = arguments,
        enterTransition = {
            slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight / 4 },
                animationSpec = tween(NORMAL_TRANSITION_DURATION)
            ) + fadeIn(
                animationSpec = tween(NORMAL_TRANSITION_DURATION)
            )
        },
        exitTransition = {
            slideOutVertically(
                targetOffsetY = { fullHeight -> fullHeight / 4 },
                animationSpec = tween(NORMAL_TRANSITION_DURATION)
            ) + fadeOut(
                animationSpec = tween(FAST_TRANSITION_DURATION)
            )
        },
        popEnterTransition = {
            slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight / 4 },
                animationSpec = tween(NORMAL_TRANSITION_DURATION)
            ) + fadeIn(
                animationSpec = tween(NORMAL_TRANSITION_DURATION)
            )
        },
        popExitTransition = {
            slideOutVertically(
                targetOffsetY = { fullHeight -> fullHeight / 4 },
                animationSpec = tween(NORMAL_TRANSITION_DURATION)
            ) + fadeOut(
                animationSpec = tween(FAST_TRANSITION_DURATION)
            )
        },
        content = content
    )
}

/**
 * Fade-only transition for player and full-screen experiences.
 * Provides a simple fade effect for immersive screens.
 */
fun NavGraphBuilder.composableWithFadeOnlyTransition(
    route: String,
    arguments: List<androidx.navigation.NamedNavArgument> = emptyList(),
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {
    composable(
        route = route,
        arguments = arguments,
        enterTransition = {
            fadeIn(animationSpec = tween(SLOW_TRANSITION_DURATION))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(NORMAL_TRANSITION_DURATION))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(SLOW_TRANSITION_DURATION))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(NORMAL_TRANSITION_DURATION))
        },
        content = content
    )
}

/**
 * Search transition - combines slide from top with fade.
 * Provides a unique transition for the search screen.
 */
fun NavGraphBuilder.composableWithSearchTransition(
    route: String,
    arguments: List<androidx.navigation.NamedNavArgument> = emptyList(),
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {
    composable(
        route = route,
        arguments = arguments,
        enterTransition = {
            slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(NORMAL_TRANSITION_DURATION)
            ) + fadeIn(
                animationSpec = tween(NORMAL_TRANSITION_DURATION)
            )
        },
        exitTransition = {
            slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(NORMAL_TRANSITION_DURATION)
            ) + fadeOut(
                animationSpec = tween(FAST_TRANSITION_DURATION)
            )
        },
        popEnterTransition = {
            slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(NORMAL_TRANSITION_DURATION)
            ) + fadeIn(
                animationSpec = tween(NORMAL_TRANSITION_DURATION)
            )
        },
        popExitTransition = {
            slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(NORMAL_TRANSITION_DURATION)
            ) + fadeOut(
                animationSpec = tween(FAST_TRANSITION_DURATION)
            )
        },
        content = content
    )
}
