package com.example.fairball.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.fairball.data.ThemePreference

private val DefaultPurple80 = Color(0xFFD0BCFF)
private val DefaultPurpleGrey80 = Color(0xFFCCC2DC)
private val DefaultPink80 = Color(0xFFEFB8C8)
private val DefaultPurple40 = Color(0xFF6650a4)
private val DefaultPurpleGrey40 = Color(0xFF625b71)
private val DefaultPink40 = Color(0xFF7D5260)

private val DarkColorScheme = darkColorScheme(
    primary = DefaultPurple80,
    secondary = DefaultPurpleGrey80,
    tertiary = DefaultPink80
)

private val LightColorScheme = lightColorScheme(
    primary = DefaultPurple40,
    secondary = DefaultPurpleGrey40,
    tertiary = DefaultPink40
)

private val CustomLightColorScheme = lightColorScheme(
    primary = Color(0xFF00695C),
    secondary = Color(0xFFFF6F00),
    tertiary = Color(0xFF4A148C),
    background = Color(0xFFF5F5F5),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
)

private val CustomDarkColorScheme = darkColorScheme(
    primary = Color(0xFFA5D6A7),
    secondary = Color(0xFFFFAB40),
    tertiary = Color(0xFFCE93D8),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
)

private val AppTypography = Typography()

@Composable
fun FairBallTheme(
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themePreference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.DARK -> true
        ThemePreference.LIGHT -> false
        ThemePreference.CUSTOM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        themePreference == ThemePreference.CUSTOM -> {
            if (darkTheme) CustomDarkColorScheme else CustomLightColorScheme
        }
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}