package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.ui.theme.ComicBlue
import com.aschlus.comicreadingcompanion.ui.theme.ComicBlueDark
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicRed
import com.aschlus.comicreadingcompanion.ui.theme.ComicYellow

private val LibrarySpeechBubblePaper =
    Color(0xFFFFFEF8)

@Composable
fun ComicLibraryHeader(
    modifier: Modifier = Modifier
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(208.dp)
                .background(
                    ComicBlue
                )
                .drawBehind {
                    val strokeWidth =
                        3.dp.toPx()

                    drawLine(
                        color = ComicInk,
                        start =
                            Offset(
                                x = 0f,
                                y =
                                    size.height -
                                            strokeWidth / 2f
                            ),
                        end =
                            Offset(
                                x = size.width,
                                y =
                                    size.height -
                                            strokeWidth / 2f
                            ),
                        strokeWidth =
                            strokeWidth
                    )
                }
    ) {
        LibraryHeaderBackground()

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
        ) {
            LayeredLibraryTitle(
                modifier =
                    Modifier
                        .align(
                            Alignment.TopStart
                        )
                        .padding(
                            start = 24.dp,
                            top = 30.dp
                        )
            )

            LibrarySpeechBubble(
                modifier =
                    Modifier
                        .align(
                            Alignment.TopEnd
                        )
                        .padding(
                            top = 16.dp,
                            end = 5.dp
                        )
            )
        }
    }
}

@Composable
private fun LibraryHeaderBackground() {
    Canvas(
        modifier =
            Modifier.fillMaxSize()
    ) {
        /*
         * Main halftone field.
         *
         * The mockup has substantially denser dots than
         * the previous implementation.
         */
        val spacing =
            9.dp.toPx()

        val smallRadius =
            1.15.dp.toPx()

        var row = 0
        var y =
            spacing / 2f

        while (y < size.height) {
            var x =
                if (row % 2 == 0) {
                    spacing / 2f
                } else {
                    spacing
                }

            while (x < size.width) {
                drawCircle(
                    color =
                        ComicBlueDark.copy(
                            alpha = 0.30f
                        ),
                    radius =
                        smallRadius,
                    center =
                        Offset(
                            x = x,
                            y = y
                        )
                )

                x += spacing
            }

            row += 1
            y += spacing
        }

        /*
         * Heavier dots near the upper area reproduce
         * the denser printed texture visible in the
         * mockup without changing the whole background.
         */
        val largeSpacing =
            15.dp.toPx()

        val largeRadius =
            1.7.dp.toPx()

        var largeRow = 0
        var largeY =
            largeSpacing / 2f

        while (
            largeY <
            size.height * 0.42f
        ) {
            var largeX =
                if (
                    largeRow % 2 == 0
                ) {
                    largeSpacing / 2f
                } else {
                    largeSpacing
                }

            while (
                largeX <
                size.width
            ) {
                drawCircle(
                    color =
                        ComicInk.copy(
                            alpha = 0.13f
                        ),
                    radius =
                        largeRadius,
                    center =
                        Offset(
                            x = largeX,
                            y = largeY
                        )
                )

                largeX +=
                    largeSpacing
            }

            largeRow += 1
            largeY +=
                largeSpacing
        }

        /*
         * Angular background panels.
         *
         * These are intentionally more visible than in
         * the previous pass because they are clearly
         * readable in the mockup.
         */
        val firstPanel =
            Path().apply {
                moveTo(
                    x = size.width * 0.03f,
                    y = size.height * 0.18f
                )

                lineTo(
                    x = size.width * 0.37f,
                    y = size.height * 0.10f
                )

                lineTo(
                    x = size.width * 0.42f,
                    y = size.height * 0.53f
                )

                lineTo(
                    x = size.width * 0.08f,
                    y = size.height * 0.59f
                )

                close()
            }

        drawPath(
            path = firstPanel,
            color =
                ComicBlueDark.copy(
                    alpha = 0.14f
                )
        )

        drawPath(
            path = firstPanel,
            color =
                ComicInk.copy(
                    alpha = 0.18f
                ),
            style =
                Stroke(
                    width = 2.dp.toPx()
                )
        )

        val secondPanel =
            Path().apply {
                moveTo(
                    x = size.width * 0.34f,
                    y = size.height * 0.05f
                )

                lineTo(
                    x = size.width * 0.73f,
                    y = size.height * 0.10f
                )

                lineTo(
                    x = size.width * 0.65f,
                    y = size.height * 0.55f
                )

                lineTo(
                    x = size.width * 0.42f,
                    y = size.height * 0.51f
                )

                close()
            }

        drawPath(
            path = secondPanel,
            color =
                ComicBlueDark.copy(
                    alpha = 0.14f
                )
        )

        drawPath(
            path = secondPanel,
            color =
                ComicInk.copy(
                    alpha = 0.18f
                ),
            style =
                Stroke(
                    width = 2.dp.toPx()
                )
        )

        val thirdPanel =
            Path().apply {
                moveTo(
                    x = size.width * 0.58f,
                    y = size.height * 0.45f
                )

                lineTo(
                    x = size.width * 0.98f,
                    y = size.height * 0.35f
                )

                lineTo(
                    x = size.width,
                    y = size.height * 0.78f
                )

                lineTo(
                    x = size.width * 0.63f,
                    y = size.height * 0.84f
                )

                close()
            }

        drawPath(
            path = thirdPanel,
            color =
                ComicBlueDark.copy(
                    alpha = 0.13f
                )
        )

        drawPath(
            path = thirdPanel,
            color =
                ComicInk.copy(
                    alpha = 0.17f
                ),
            style =
                Stroke(
                    width = 2.dp.toPx()
                )
        )
    }
}

@Composable
private fun LayeredLibraryTitle(
    modifier: Modifier = Modifier
) {
    val titleStyle =
        MaterialTheme
            .typography
            .displayLarge
            .copy(
                fontSize = 61.sp,
                lineHeight = 61.sp,
                letterSpacing = (-2.5).sp,
                fontWeight =
                    FontWeight.Black,
                fontStyle =
                    FontStyle.Italic
            )

    Box(
        modifier =
            modifier.graphicsLayer {
                scaleX = 1.02f
                scaleY = 1.08f

                transformOrigin =
                    TransformOrigin(
                        pivotFractionX = 0f,
                        pivotFractionY = 0f
                    )
            }
    ) {
        /*
         * Deep black rear layer.
         */
        Text(
            text = "LIBRARY",
            modifier =
                Modifier
                    .offset(
                        x = (-7).dp,
                        y = 13.dp
                    )
                    .clearAndSetSemantics { },
            style = titleStyle,
            color = ComicInk
        )

        /*
         * Red extrusion.
         */
        Text(
            text = "LIBRARY",
            modifier =
                Modifier
                    .offset(
                        x = (-4).dp,
                        y = 9.dp
                    )
                    .clearAndSetSemantics { },
            style = titleStyle,
            color = ComicRed
        )

        /*
         * Thick black face outline.
         */
        LibraryTitleOutlineCopy(
            x = (-3).dp,
            y = 0.dp,
            style = titleStyle
        )

        LibraryTitleOutlineCopy(
            x = 3.dp,
            y = 0.dp,
            style = titleStyle
        )

        LibraryTitleOutlineCopy(
            x = 0.dp,
            y = (-3).dp,
            style = titleStyle
        )

        LibraryTitleOutlineCopy(
            x = 0.dp,
            y = 3.dp,
            style = titleStyle
        )

        LibraryTitleOutlineCopy(
            x = (-2).dp,
            y = (-2).dp,
            style = titleStyle
        )

        LibraryTitleOutlineCopy(
            x = 2.dp,
            y = (-2).dp,
            style = titleStyle
        )

        LibraryTitleOutlineCopy(
            x = (-2).dp,
            y = 2.dp,
            style = titleStyle
        )

        LibraryTitleOutlineCopy(
            x = 2.dp,
            y = 2.dp,
            style = titleStyle
        )

        /*
         * Yellow visible face.
         */
        Text(
            text = "LIBRARY",
            style = titleStyle,
            color = ComicYellow
        )
    }
}

@Composable
private fun LibraryTitleOutlineCopy(
    x: Dp,
    y: Dp,
    style: TextStyle
) {
    Text(
        text = "LIBRARY",
        modifier =
            Modifier
                .offset(
                    x = x,
                    y = y
                )
                .clearAndSetSemantics { },
        style = style,
        color = ComicInk
    )
}

@Composable
private fun LibrarySpeechBubble(
    modifier: Modifier = Modifier
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth(
                    fraction = 0.38f
                )
                .height(108.dp)
    ) {
        Canvas(
            modifier =
                Modifier.fillMaxSize()
        ) {
            val bubble =
                Path().apply {
                    moveTo(
                        x = size.width * 0.18f,
                        y = size.height * 0.05f
                    )

                    cubicTo(
                        x1 = size.width * 0.08f,
                        y1 = size.height * 0.06f,
                        x2 = size.width * 0.03f,
                        y2 = size.height * 0.16f,
                        x3 = size.width * 0.03f,
                        y3 = size.height * 0.31f
                    )

                    cubicTo(
                        x1 = size.width * 0.03f,
                        y1 = size.height * 0.51f,
                        x2 = size.width * 0.11f,
                        y2 = size.height * 0.63f,
                        x3 = size.width * 0.25f,
                        y3 = size.height * 0.67f
                    )

                    /*
                     * Longer tail, matching the mockup.
                     */
                    lineTo(
                        x = size.width * 0.10f,
                        y = size.height * 0.96f
                    )

                    lineTo(
                        x = size.width * 0.42f,
                        y = size.height * 0.69f
                    )

                    cubicTo(
                        x1 = size.width * 0.62f,
                        y1 = size.height * 0.73f,
                        x2 = size.width * 0.82f,
                        y2 = size.height * 0.71f,
                        x3 = size.width * 0.91f,
                        y3 = size.height * 0.62f
                    )

                    cubicTo(
                        x1 = size.width * 0.98f,
                        y1 = size.height * 0.54f,
                        x2 = size.width * 0.98f,
                        y2 = size.height * 0.20f,
                        x3 = size.width * 0.88f,
                        y3 = size.height * 0.10f
                    )

                    cubicTo(
                        x1 = size.width * 0.75f,
                        y1 = size.height * 0.01f,
                        x2 = size.width * 0.35f,
                        y2 = size.height * 0.01f,
                        x3 = size.width * 0.18f,
                        y3 = size.height * 0.05f
                    )

                    close()
                }

            drawPath(
                path = bubble,
                color =
                    LibrarySpeechBubblePaper
            )

            drawPath(
                path = bubble,
                color = ComicInk,
                style =
                    Stroke(
                        width =
                            3.dp.toPx()
                    )
            )
        }

        Text(
            text =
                "ALL YOUR\n" +
                        "READING LISTS\n" +
                        "IN ONE PLACE!",
            modifier =
                Modifier
                    .align(
                        Alignment.TopCenter
                    )
                    .padding(
                        start = 14.dp,
                        end = 9.dp,
                        top = 14.dp
                    ),
            style =
                MaterialTheme
                    .typography
                    .labelLarge
                    .copy(
                        fontSize = 14.sp,
                        lineHeight = 15.sp,
                        letterSpacing = (-0.25).sp,
                        fontStyle =
                            FontStyle.Italic
                    ),
            fontWeight =
                FontWeight.Black,
            textAlign =
                TextAlign.Center,
            color = ComicInk
        )
    }
}