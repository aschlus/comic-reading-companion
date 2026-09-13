package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk

enum class ComicLibraryActionIcon {
    CREATE,
    IMPORT
}

private val ComicLibraryActionShape =
    GenericShape { size, _ ->
        moveTo(
            x = size.width * 0.04f,
            y = 0f
        )

        lineTo(
            x = size.width,
            y = 0f
        )

        lineTo(
            x = size.width * 0.94f,
            y = size.height
        )

        lineTo(
            x = 0f,
            y = size.height
        )

        close()
    }

@Composable
fun ComicLibraryActionCard(
    title: String,
    icon: ComicLibraryActionIcon,
    backgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val upperTitle =
        title.uppercase()

    val firstLine =
        upperTitle.substringBefore(
            " READING LIST"
        )

    val secondLine =
        if (
            upperTitle.contains(
                " READING LIST"
            )
        ) {
            upperTitle.substringAfter(
                "$firstLine "
            )
        } else {
            null
        }

    Box(
        modifier =
            modifier
                .height(72.dp)
                .graphicsLayer {
                    alpha =
                        if (enabled) {
                            1f
                        } else {
                            0.55f
                        }
                }
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .offset(
                        x = 4.dp,
                        y = 5.dp
                    )
                    .background(
                        color = ComicInk,
                        shape =
                            ComicLibraryActionShape
                    )
        )

        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        color = backgroundColor,
                        shape =
                            ComicLibraryActionShape
                    )
                    .border(
                        width = 3.dp,
                        color = ComicInk,
                        shape =
                            ComicLibraryActionShape
                    )
                    .clickable(
                        enabled = enabled,
                        onClick = onClick
                    )
                    .padding(
                        horizontal = 10.dp,
                        vertical = 8.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.spacedBy(
                    8.dp
                )
        ) {
            ComicLibraryActionIcon(
                icon = icon,
                modifier =
                    Modifier.size(46.dp)
            )

            Column(
                modifier =
                    Modifier.weight(1f),
                verticalArrangement =
                    Arrangement.spacedBy(
                        1.dp
                    )
            ) {
                Text(
                    text = firstLine,
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium
                            .copy(
                                fontSize = 17.sp,
                                lineHeight = 19.sp,
                                letterSpacing = (-0.4).sp,
                                fontStyle =
                                    FontStyle.Italic
                            ),
                    fontWeight =
                        FontWeight.Black,
                    color = ComicInk,
                    maxLines = 1
                )

                if (secondLine != null) {
                    Text(
                        text = secondLine,
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium
                                .copy(
                                    fontSize = 16.sp,
                                    lineHeight = 18.sp,
                                    letterSpacing = (-0.4).sp,
                                    fontStyle =
                                        FontStyle.Italic
                                ),
                        fontWeight =
                            FontWeight.Black,
                        color = ComicInk,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun ComicLibraryActionIcon(
    icon: ComicLibraryActionIcon,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
    ) {
        when (icon) {
            ComicLibraryActionIcon.CREATE -> {
                val strokeWidth =
                    5.dp.toPx()

                drawLine(
                    color = ComicInk,
                    start =
                        Offset(
                            x =
                                size.width *
                                        0.18f,
                            y =
                                size.height *
                                        0.50f
                        ),
                    end =
                        Offset(
                            x =
                                size.width *
                                        0.82f,
                            y =
                                size.height *
                                        0.50f
                        ),
                    strokeWidth =
                        strokeWidth,
                    cap =
                        StrokeCap.Square
                )

                drawLine(
                    color = ComicInk,
                    start =
                        Offset(
                            x =
                                size.width *
                                        0.50f,
                            y =
                                size.height *
                                        0.18f
                        ),
                    end =
                        Offset(
                            x =
                                size.width *
                                        0.50f,
                            y =
                                size.height *
                                        0.82f
                        ),
                    strokeWidth =
                        strokeWidth,
                    cap =
                        StrokeCap.Square
                )
            }

            ComicLibraryActionIcon.IMPORT -> {
                val arrow =
                    Path().apply {
                        moveTo(
                            x =
                                size.width *
                                        0.50f,
                            y =
                                size.height *
                                        0.10f
                        )

                        lineTo(
                            x =
                                size.width *
                                        0.78f,
                            y =
                                size.height *
                                        0.38f
                        )

                        lineTo(
                            x =
                                size.width *
                                        0.63f,
                            y =
                                size.height *
                                        0.38f
                        )

                        lineTo(
                            x =
                                size.width *
                                        0.63f,
                            y =
                                size.height *
                                        0.62f
                        )

                        lineTo(
                            x =
                                size.width *
                                        0.37f,
                            y =
                                size.height *
                                        0.62f
                        )

                        lineTo(
                            x =
                                size.width *
                                        0.37f,
                            y =
                                size.height *
                                        0.38f
                        )

                        lineTo(
                            x =
                                size.width *
                                        0.22f,
                            y =
                                size.height *
                                        0.38f
                        )

                        close()
                    }

                drawPath(
                    path = arrow,
                    color = ComicInk
                )

                val tray =
                    Path().apply {
                        moveTo(
                            x =
                                size.width *
                                        0.18f,
                            y =
                                size.height *
                                        0.68f
                        )

                        lineTo(
                            x =
                                size.width *
                                        0.18f,
                            y =
                                size.height *
                                        0.85f
                        )

                        lineTo(
                            x =
                                size.width *
                                        0.82f,
                            y =
                                size.height *
                                        0.85f
                        )

                        lineTo(
                            x =
                                size.width *
                                        0.82f,
                            y =
                                size.height *
                                        0.68f
                        )
                    }

                drawPath(
                    path = tray,
                    color = ComicInk,
                    style =
                        Stroke(
                            width =
                                4.5.dp.toPx(),
                            cap =
                                StrokeCap.Square
                        )
                )
            }
        }
    }
}