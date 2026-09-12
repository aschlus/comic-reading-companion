package com.aschlus.comicreadingcompanion.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography =
    Typography(
        displayLarge =
            TextStyle(
                fontFamily =
                    FontFamily.SansSerif,
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
                    FontFamily.SansSerif,
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
                    FontFamily.SansSerif,
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
                    FontFamily.SansSerif,
                fontWeight =
                    FontWeight.ExtraBold,
                fontSize = 28.sp,
                lineHeight = 32.sp
            ),
        titleLarge =
            TextStyle(
                fontFamily =
                    FontFamily.SansSerif,
                fontWeight =
                    FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 28.sp
            ),
        titleMedium =
            TextStyle(
                fontFamily =
                    FontFamily.SansSerif,
                fontWeight =
                    FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 24.sp
            ),
        bodyLarge =
            TextStyle(
                fontFamily =
                    FontFamily.SansSerif,
                fontWeight =
                    FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp
            ),
        bodyMedium =
            TextStyle(
                fontFamily =
                    FontFamily.SansSerif,
                fontWeight =
                    FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp
            ),
        bodySmall =
            TextStyle(
                fontFamily =
                    FontFamily.SansSerif,
                fontWeight =
                    FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp
            ),
        labelLarge =
            TextStyle(
                fontFamily =
                    FontFamily.SansSerif,
                fontWeight =
                    FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 20.sp
            ),
        labelMedium =
            TextStyle(
                fontFamily =
                    FontFamily.SansSerif,
                fontWeight =
                    FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
    )