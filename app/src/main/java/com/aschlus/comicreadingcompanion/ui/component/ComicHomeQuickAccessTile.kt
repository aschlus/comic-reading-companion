package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk

@Composable
fun ComicHomeQuickAccessTile(
    text: String,
    icon: ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 30.dp,
    textFontSize: TextUnit = 15.sp,
    textLineHeight: TextUnit = 16.sp,
    drawCustomPlus: Boolean = false
) {
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier =
            modifier
                .height(96.dp)
    ) {
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .offset(
                        x = 3.dp,
                        y = 4.dp
                    )
                    .background(
                        color = ComicInk,
                        shape = shape
                    )
        )

        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(
                        color = backgroundColor
                    )
                    .border(
                        width = 2.dp,
                        color = ComicInk,
                        shape = shape
                    )
                    .clickable(
                        onClick = onClick
                    )
        ) {
            Canvas(
                modifier =
                    Modifier.fillMaxSize()
            ) {
                val center =
                    Offset(
                        x = size.width / 2f,
                        y = size.height / 2f
                    )

                val rayLength = size.maxDimension * 0.8f

                repeat(16) { index ->
                    rotate(
                        degrees =
                            index * 22.5f,
                        pivot = center
                    ) {
                        drawLine(
                            color =
                                ComicInk.copy(
                                    alpha = 0.055f
                                ),
                            start = center,
                            end =
                                Offset(
                                    x = center.x,
                                    y = center.y - rayLength
                                ),
                            strokeWidth = 4.dp.toPx()
                        )
                    }
                }
            }

            ComicHalftoneOverlay(
                modifier =
                    Modifier.fillMaxSize(),
                dotColor = ComicInk,
                spacing = 9.dp,
                radius = 0.65.dp,
                startFraction = 0.45f,
                minAlpha = 0.04f,
                maxAlpha = 0.11f
            )

            Column(
                modifier =
                    Modifier.fillMaxSize(),
                verticalArrangement =
                    Arrangement.Center,
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {
                Box(
                    modifier =
                        Modifier.size(38.dp),
                    contentAlignment =
                        Alignment.Center
                ) {
                    if (drawCustomPlus) {
                        Canvas(
                            modifier =
                                Modifier.size(34.dp)
                        ) {
                            val strokeWidth =
                                3.dp.toPx()

                            val halfLength =
                                11.dp.toPx()

                            val centerX =
                                size.width / 2f

                            val centerY =
                                size.height / 2f

                            drawLine(
                                color = ComicInk,
                                start =
                                    Offset(
                                        centerX - halfLength,
                                        centerY
                                    ),
                                end =
                                    Offset(
                                        centerX + halfLength,
                                        centerY
                                    ),
                                strokeWidth = strokeWidth
                            )

                            drawLine(
                                color = ComicInk,
                                start =
                                    Offset(
                                        centerX,
                                        centerY - halfLength
                                    ),
                                end =
                                    Offset(
                                        centerX,
                                        centerY + halfLength
                                    ),
                                strokeWidth = strokeWidth
                            )
                        }
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = ComicInk,
                            modifier =
                                Modifier.size(iconSize)
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(3.dp)
                )

                Box(
                    modifier =
                        Modifier.height(34.dp),
                    contentAlignment =
                        Alignment.Center
                ) {
                    Text(
                        text = text,
                        fontSize = textFontSize,
                        lineHeight = textLineHeight,
                        fontWeight = FontWeight.ExtraBold,
                        fontStyle = FontStyle.Italic,
                        color = ComicInk,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }
        }
    }
}