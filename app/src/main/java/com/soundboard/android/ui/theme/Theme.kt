package com.soundboard.android.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Icon-inspired theme with dark blue backgrounds and neon pink accents
private val SoundboardDarkTheme = darkColorScheme(
    primary = NeonPink,                    // Primary neon pink for buttons and accents
    onPrimary = TextPrimary,               // White text on primary elements
    primaryContainer = DarkBlueVariant,     // Dark blue containers
    onPrimaryContainer = NeonPinkLight,     // Light pink text in containers
    
    secondary = CoralAccent,               // Coral for secondary elements
    onSecondary = TextPrimary,             // White text on secondary
    secondaryContainer = ButtonSurface,     // Button surface color
    onSecondaryContainer = CoralLight,      // Light coral text
    
    tertiary = NeonPinkLight,              // Light pink for tertiary elements
    onTertiary = DarkBlueBackground,        // Dark blue text on tertiary
    tertiaryContainer = SurfaceVariant,     // Alternative surface
    onTertiaryContainer = TextPrimary,      // White text in tertiary containers
    
    background = DarkBlueBackground,        // Deep dark blue background
    onBackground = TextPrimary,             // White text on background
    surface = DarkBlueSurface,             // Slightly lighter blue surfaces
    onSurface = TextPrimary,               // White text on surfaces
    surfaceVariant = SurfaceVariant,       // Alternative surface color
    onSurfaceVariant = TextSecondary,      // Light blue-grey text
    
    outline = TextDisabled,                // Muted outline color
    outlineVariant = DarkBlueVariant,      // Variant outline
    
    scrim = Color(0x80000000),             // Semi-transparent black scrim
    
    inverseSurface = TextPrimary,          // White inverse surface
    inverseOnSurface = DarkBlueBackground, // Dark blue text on inverse
    inversePrimary = DarkBlueVariant       // Dark blue inverse primary
)

// Light theme for compatibility (though app focuses on dark theme)
private val SoundboardLightTheme = lightColorScheme(
    primary = NeonPinkDark,
    onPrimary = TextPrimary,
    primaryContainer = NeonPinkLight,
    onPrimaryContainer = DarkBlueBackground,
    
    secondary = CoralDark,
    onSecondary = TextPrimary,
    secondaryContainer = CoralLight,
    onSecondaryContainer = DarkBlueBackground,
    
    tertiary = NeonPink,
    onTertiary = TextPrimary,
    tertiaryContainer = NeonPinkLight,
    onTertiaryContainer = DarkBlueBackground,
    
    background = Color(0xFFF5F5F5),
    onBackground = DarkBlueBackground,
    surface = Color(0xFFFFFFFF),
    onSurface = DarkBlueBackground
)

@Composable
fun AndroidSoundboardTheme(
    darkTheme: Boolean = true, // Default to dark theme for soundboard aesthetic
    // Disable dynamic color to maintain consistent branding
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> SoundboardDarkTheme
        else -> SoundboardLightTheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Use dark blue background for status bar
            window.statusBarColor = DarkBlueBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
} 