package com.soundboard.android.ui.theme

import androidx.compose.ui.graphics.Color

// Icon-inspired color scheme: Dark blue backgrounds with pink/magenta neon accents
// Based on the isometric soundboard icon design

// Dark theme colors (primary theme)
val DarkBlueBackground = Color(0xFF1A1B3A)    // Deep dark blue background
val DarkBlueSurface = Color(0xFF252749)       // Slightly lighter blue for surfaces
val DarkBlueVariant = Color(0xFF2A2B5C)       // Mid-tone blue for containers

val NeonPink = Color(0xFFFF4081)              // Primary neon pink accent
val NeonPinkLight = Color(0xFFFF79B0)         // Lighter pink for highlights
val NeonPinkDark = Color(0xFFC60055)          // Darker pink for pressed states

val CoralAccent = Color(0xFFFF6B6B)           // Coral red for secondary accents (plants)
val CoralLight = Color(0xFFFF8A80)            // Light coral for highlights
val CoralDark = Color(0xFFE53935)             // Dark coral for emphasis

// Text colors
val TextPrimary = Color(0xFFFFFFFF)           // White text
val TextSecondary = Color(0xFFB0BEC5)         // Light blue-grey text
val TextDisabled = Color(0xFF607D8B)          // Muted blue-grey

// Button and surface colors
val ButtonSurface = Color(0xFF303F5F)         // Button background
val ButtonPressed = Color(0xFF3D4E73)         // Pressed button state
val SurfaceVariant = Color(0xFF37415C)        // Alternative surface color

// Status and feedback colors
val SuccessGreen = Color(0xFF4CAF50)          // Success states
val WarningAmber = Color(0xFFFF9800)          // Warning states
val ErrorRed = Color(0xFFF44336)              // Error states

// Glow and highlight effects
val GlowPink = Color(0x33FF4081)              // Semi-transparent glow effect
val GlowCoral = Color(0x33FF6B6B)             // Semi-transparent coral glow
val Highlight = Color(0x1AFFFFFF)             // Subtle white highlight

// Legacy color names for compatibility
val Purple80 = NeonPinkLight
val PurpleGrey80 = TextSecondary
val Pink80 = CoralLight

val Purple40 = NeonPink
val PurpleGrey40 = DarkBlueVariant
val Pink40 = CoralAccent 