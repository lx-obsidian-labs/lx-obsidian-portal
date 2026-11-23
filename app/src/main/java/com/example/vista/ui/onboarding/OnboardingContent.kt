package com.example.vista.ui.onboarding

data class OnboardingCapability(val id: String, val title: String, val detail: String)

/** Kept as plain data so onboarding promises can be checked alongside release capabilities. */
object OnboardingContent {
    val capabilities = listOf(
        OnboardingCapability("catalog", "Discover your next story", "Browse movies, series, cast and trailers. Catalog information does not guarantee a playable source."),
        OnboardingCapability("recommendations", "Suggestions with a reason", "Choose favourite genres, use your viewing and saved titles, and tell Vista ‘Not for me’. Personalization is optional."),
        OnboardingCapability("assistant", "An assistant that helps you act", "Signed-in cloud chat can help find and compare titles, review downloads and prepare library actions. Saving or removing a title needs your confirmation."),
        OnboardingCapability("downloads", "Movie nights offline", "Download eligible media sources over an unmetered connection. Completed titles play from Library → Downloads. Some sources support streaming only."),
        OnboardingCapability("library", "Your library on this device", "Keep saved titles, history and supported playback progress locally. Cross-device library sync is not available yet."),
        OnboardingCapability("controls", "Make Vista work for you", "Settings has search, data controls, native audio/subtitle preferences, AI & Voice and storage tools. Guest browsing has a three-day trial; cloud AI requires sign-in.")
    )
}
