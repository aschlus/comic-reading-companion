package com.aschlus.comicreadingcompanion.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.R

val BarlowFontFamily =
    FontFamily(
        Font(
            resId = R.font.barlow_regular,
            weight = FontWeight.Normal
        ),
        Font(
            resId = R.font.barlow_medium,
            weight = FontWeight.Medium
        ),
        Font(
            resId = R.font.barlow_semibold,
            weight = FontWeight.SemiBold
        ),
        Font(
            resId = R.font.barlow_bold,
            weight = FontWeight.Bold
        ),
        Font(
            resId = R.font.barlow_extrabold,
            weight = FontWeight.ExtraBold
        ),
        Font(
            resId = R.font.barlow_black,
            weight = FontWeight.Black
        ),
        Font(
            resId = R.font.barlow_semibold_italic,
            weight = FontWeight.SemiBold,
            style = FontStyle.Italic
        ),
        Font(
            resId = R.font.barlow_extrabold_italic,
            weight = FontWeight.ExtraBold,
            style = FontStyle.Italic
        ),
        Font(
            resId = R.font.barlow_black_italic,
            weight = FontWeight.Black,
            style = FontStyle.Italic
        )
    )

val LilitaOneFontFamily =
    FontFamily(
        Font(
            resId = R.font.lilita_one_regular,
            weight = FontWeight.Normal
        )
    )

val ComicHeaderTextTransform =
    TextGeometricTransform(
        scaleX = 1.00f,
        skewX = -0.1763f
    )

val ComicAccentTextTransform =
    TextGeometricTransform(
        scaleX = 1.00f,
        skewX = -0.1405f
    )

val Typography =
    Typography(
        displayLarge =
            TextStyle(
                fontFamily =
                    BarlowFontFamily,
                fontWeight =
                    FontWeight.Black,
                fontStyle =
                    FontStyle.Italic,
                fontSize = 48.sp,
                lineHeight = 52.sp,
                letterSpacing = (-1).sp
            ),
        displayMedium =
            TextStyle(
                fontFamily =
                    BarlowFontFamily,
                fontWeight =
                    FontWeight.Black,
                fontStyle =
                    FontStyle.Italic,
                fontSize = 40.sp,
                lineHeight = 44.sp,
                letterSpacing = (-0.5).sp
            ),
        headlineLarge =
            TextStyle(
                fontFamily =
                    BarlowFontFamily,
                fontWeight =
                    FontWeight.ExtraBold,
                fontStyle =
                    FontStyle.Italic,
                fontSize = 32.sp,
                lineHeight = 36.sp
            ),
        headlineMedium =
            TextStyle(
                fontFamily =
                    BarlowFontFamily,
                fontWeight =
                    FontWeight.ExtraBold,
                fontSize = 28.sp,
                lineHeight = 32.sp
            ),
        titleLarge =
            TextStyle(
                fontFamily =
                    BarlowFontFamily,
                fontWeight =
                    FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 28.sp
            ),
        titleMedium =
            TextStyle(
                fontFamily =
                    BarlowFontFamily,
                fontWeight =
                    FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 24.sp
            ),
        bodyLarge =
            TextStyle(
                fontFamily =
                    BarlowFontFamily,
                fontWeight =
                    FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp
            ),
        bodyMedium =
            TextStyle(
                fontFamily =
                    BarlowFontFamily,
                fontWeight =
                    FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp
            ),
        bodySmall =
            TextStyle(
                fontFamily =
                    BarlowFontFamily,
                fontWeight =
                    FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp
            ),
        labelLarge =
            TextStyle(
                fontFamily =
                    BarlowFontFamily,
                fontWeight =
                    FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 20.sp
            ),
        labelMedium =
            TextStyle(
                fontFamily =
                    BarlowFontFamily,
                fontWeight =
                    FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
    )