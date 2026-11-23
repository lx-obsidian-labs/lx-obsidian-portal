package com.example.vista.ui.theme

import androidx.compose.ui.graphics.Color

// Core colors matching Vista Watch branding
val Background = Color(0xFF0A0A0A)
val RaisedSurface = Color(0xFF141414)
val CardSurface = Color(0xFF1A1A1A)

// Text hierarchy
val PrimaryText = Color(0xFFFFFFFF)
val SecondaryText = Color(0xCCFFFFFF) // 80% white
val MutedText = Color(0x80FFFFFF) // 50% white

// Accent - Red (matching Vista Watch branding)
val Accent = Color(0xFFE50914) // Netflix-style red
val AccentDark = Color(0xFFB8070F)
val AccentLight = Color(0xFFFF1A25)

// Error - deliberately NOT the brand red.
//
// Every semantic slot in the theme is the same `Accent` (#E50914), which meant a failure state was
// pixel-identical to a filled primary button - a Play button and an error were told apart by
// nothing but context. Deep orange is far enough from the brand red to be unambiguous while still
// reading as "something went wrong", and far enough from `RatingGold` (#FFC107) to not be mistaken
// for an age rating. Pair it with dark text: white on this lands at 2.8:1, dark lands at 6.2:1.
val ErrorAccent = Color(0xFFFF6D00)

// Rating badge - Gold/Yellow
val RatingGold = Color(0xFFFFC107)
val RatingBadgeBackground = Color(0xFF1A1A1A)

// Age rating badge
val AgeBadgeBackground = Color(0xFF333333)

// 2026 Liquid Glass & Cinematic Colors
val DeepCinema = Color(0xFF050505)

// Ambient Gloom (for cinematic backgrounds)
val AmbientRed = Color(0x1AEE0914)
val AmbientBlue = Color(0x1A2196F3)
val AmbientPurple = Color(0x1A9C27B0)

// Progress bar
val ProgressTrack = Color(0xFF333333)
val ProgressIndicator = Accent
