package com.example.qlinic.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = teal,            // A main brand color, used for buttons, links, etc.
    onPrimary = white,         // Color for text/icons on top of the primary color.
    secondary = orange,        // An accent color.
    onSecondary = white,       // Color for text/icons on top of the secondary color.
    background = white,        // The screen background color.
    onBackground = darkblue,   // Color for text on the background.
    surface = lightblue,       // Color for surfaces like Cards, Sheets, Menus.
    onSurface = darkblue,      // Color for text on top of surfaces.
    error = red,               // Color for showing errors.
    onError = white,           // Color for text/icons on top of the error color.
    outline = grey             // Color for borders and dividers.

    /* You can define other colors as well */
)

// DarkColorScheme?? dk need or not
private val DarkColorScheme = darkColorScheme(
    primary = teal,
    onPrimary = darkblue,
    secondary = orange,
    onSecondary = white,
    background = darkblue,
    onBackground = white,
    surface = darkblue,
    onSurface = lightblue,
    error = red,
    onError = white,
    outline = grey
)

@Composable
fun QlinicTheme(
    darkTheme: Boolean = false,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable() () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}