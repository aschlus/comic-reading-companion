package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ComicHalftoneOverlay(
    modifier: Modifier = Modifier,
    dotColor: Color,
    spacing: Dp = 10.dp,
    radius: Dp = 1.2.dp,
    startFraction: Float = 0f,
    minAlpha: Float = 0f,
    maxAlpha: Float = 0.18f
) {
    Canvas(
        modifier = modifier
    ) {
        val spacingPx = spacing.toPx()
        val radiusPx = radius.toPx()

        val safeStartFraction =
            startFraction.coerceIn(
                0f,
                0.95f
            )

        var row = 0
        var y = spacingPx / 2f

        while (y < size.height) {
            var x =
                if (row % 2 == 0) {
                    spacingPx / 2f
                } else {
                    spacingPx
                }

            while (x < size.width) {
                val horizontalFraction =
                    if (size.width == 0f) {
                        0f
                    } else {
                        x / size.width
                    }

                val fadeProgress =
                    ((horizontalFraction - safeStartFraction) / (1f - safeStartFraction))
                        .coerceIn(
                            0f,
                            1f
                        )

                if (fadeProgress > 0f) {
                    drawCircle(
                        color = dotColor.copy(
                            alpha = minAlpha + (maxAlpha - minAlpha) * fadeProgress
                        ),
                        radius = radiusPx,
                        center = Offset(
                            x = x,
                            y = y
                        )
                    )
                }

                x += spacingPx
            }

            row += 1
            y += spacingPx
        }
    }
}