package com.aschlus.comicreadingcompanion.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val ComicLightColorScheme =
    lightColorScheme(
        primary = ComicBlue,
        onPrimary = ComicInk,
        primaryContainer = ComicBlueLight,
        onPrimaryContainer = ComicInk,

        secondary = ComicYellow,
        onSecondary = ComicInk,
        secondaryContainer = ComicYellow,
        onSecondaryContainer = ComicInk,

        tertiary = ComicRed,
        onTertiary = ComicPaper,
        tertiaryContainer = ComicRedLight,
        onTertiaryContainer = ComicInk,

        background = ComicPaper,
        onBackground = ComicInk,

        surface = ComicPaper,
        onSurface = ComicInk,

        surfaceVariant = ComicPaperDeep,
        onSurfaceVariant = ComicMutedInk,

        outline = ComicInk,
        outlineVariant = ComicMutedInk,

        error = ComicRed,
        onError = ComicPaper
    )

private val ComicDarkColorScheme =
    darkColorScheme(
        primary = ComicBlueLight,
        onPrimary = ComicInk,
        primaryContainer = ComicBlueDark,
        onPrimaryContainer = ComicInk,

        secondary = ComicYellow,
        onSecondary = ComicInk,

        tertiary = ComicRedLight,
        onTertiary = ComicInk,

        background = ComicDarkBackground,
        onBackground = ComicPaper,

        surface = ComicDarkSurface,
        onSurface = ComicPaper,

        surfaceVariant = ComicDarkSurfaceVariant,
        onSurfaceVariant = ComicPaperDeep,

        outline = ComicPaperDeep,
        outlineVariant = ComicMutedInk,

        error = ComicRedLight,
        onError = ComicInk
    )

@Composable
fun ComicReadingCompanionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme =
        when {
            dynamicColor &&
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.S -> {

                val context = LocalContext.current

                if (darkTheme) {
                    dynamicDarkColorScheme(context)
                } else {
                    dynamicLightColorScheme(context)
                }
            }

            darkTheme ->
                ComicDarkColorScheme

            else ->
                ComicLightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = ComicShapes,
        content = content
    )
}