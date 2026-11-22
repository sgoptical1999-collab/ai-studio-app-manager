package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Core Dark Navy Canvas & Surfaces
val NavyVoid = Color(0xFF060A14)
val NavyBackground = Color(0xFF080D1A)
val NavySurface = Color(0xFF0F172A)
val NavySurfaceElevated = Color(0xFF162038)
val NavySurfaceHighest = Color(0xFF1E2B4A)
val NavyBorder = Color(0xFF263556)
val NavyBorderSubtle = Color(0xFF1A2540)

// Primary Electric Blue & Secondary Royal Purple Accents
val ElectricBlue = Color(0xFF3B82F6)
val ElectricBlueBright = Color(0xFF60A5FA)
val ElectricBlueContainer = Color(0xFF172D57)
val RoyalPurple = Color(0xFF8B5CF6)
val RoyalPurpleBright = Color(0xFFA78BFA)
val RoyalPurpleContainer = Color(0xFF2B1D54)
val SkyCyan = Color(0xFF38BDF8)

// Text Hierarchy
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFFCBD5E1)
val TextMuted = Color(0xFF8B9DC3)

// Semantic Status Colors
val StatusSuccess = Color(0xFF10B981)
val StatusSuccessContainer = Color(0xFF0B3529)
val StatusWarning = Color(0xFFF59E0B)
val StatusWarningContainer = Color(0xFF3B2809)
val StatusError = Color(0xFFEF4444)
val StatusErrorContainer = Color(0xFF3F1219)
val StatusInfo = Color(0xFF38BDF8)
val StatusInfoContainer = Color(0xFF0C2D48)

// Reusable Brand Gradients
val PrimaryBrandGradient = Brush.linearGradient(
    colors = listOf(ElectricBlue, RoyalPurple)
)

val AccentCardGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF142244), Color(0xFF1E1945))
)

val HeroOverlayGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0x66080D1A),
        Color(0xCC080D1A),
        NavyBackground
    )
)

// Curated Icon Badge Colors for User App Folders
val FolderBadgeColors = listOf(
    "#3B82F6", // Electric Blue
    "#8B5CF6", // Royal Purple
    "#06B6D4", // Cyan
    "#10B981", // Emerald
    "#F59E0B", // Amber
    "#EC4899", // Rose
    "#6366F1", // Indigo
    "#14B8A6"  // Teal
)
