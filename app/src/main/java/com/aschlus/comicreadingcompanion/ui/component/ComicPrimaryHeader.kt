package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.ui.theme.ComicBlue
import com.aschlus.comicreadingcompanion.ui.theme.ComicHeaderTextTransform
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicYellow
import com.aschlus.comicreadingcompanion.ui.theme.LilitaOneFontFamily

private val ComicPrimaryHeaderContentHeight = 136.dp

@Composable
fun ComicPrimaryHeader(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(ComicBlue)
                .drawBehind {
                    val strokeWidth = 2.dp.toPx()

                    drawLine(
                        color = ComicInk,
                        start =
                            Offset(
                                x = 0f,
                                y = size.height - strokeWidth / 2f
                            ),
                        end =
                            Offset(
                                x = size.width,
                                y = size.height - strokeWidth / 2f
                            ),
                        strokeWidth = strokeWidth
                    )
                }
    ) {
        ComicHalftoneOverlay(
            modifier = Modifier.matchParentSize(),
            dotColor = ComicInk,
            spacing = 10.dp,
            radius = 1.1.dp,
            startFraction = 0f,
            minAlpha = 0.3f,
            maxAlpha = 0.14f
        )

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
        ) {

            Spacer(
                modifier =
                    Modifier.windowInsetsTopHeight(
                        WindowInsets.statusBars
                    )
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            ComicPrimaryHeaderContentHeight
                        ),
                content = content
            )
        }
    }
}

@Composable
fun ComicPrimaryHeaderTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    val outlineWidth =
        with (density) {
            10.2.dp.toPx()
        }

    val titleStyle = MaterialTheme.typography.displayLarge
        .copy(
            fontFamily = LilitaOneFontFamily,
            fontSize = 54.sp,
            lineHeight = 54.sp,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Normal,
            fontStyle = FontStyle.Normal,
            textGeometricTransform = ComicHeaderTextTransform
        )

    Box(
        modifier = modifier
    ) {
        Text(
            text = text,
            modifier =
                Modifier.clearAndSetSemantics { },
            style = titleStyle.copy(
                drawStyle =
                    Stroke(
                        width = outlineWidth,
                        join = StrokeJoin.Round
                    )
            ),
            color = ComicInk
        )

        Text(
            text = text,
            style = titleStyle,
            color = ComicYellow
        )
    }
}