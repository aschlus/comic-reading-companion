package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicRed

@Composable
fun ComicLibrarySectionHeader(
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    val outlineWidth =
        with (density) {
            1.3.dp.toPx()
        }

    val titleStyle =
        TextStyle(
            fontSize = 22.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic
        )

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(50.dp),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Box(
            modifier =
                Modifier
                    .width(170.dp)
                    .height(44.dp)
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .offset(
                            x = 3.dp,
                            y = 4.dp
                        )
                        .background(
                            color = ComicInk,
                            shape = ComicSlantedShape
                        )
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(
                            color = ComicRed,
                            shape = ComicSlantedShape
                        )
                        .border(
                            width = 2.dp,
                            color = ComicInk,
                            shape = ComicSlantedShape
                        ),
                contentAlignment =
                    Alignment.Center
            ) {
                Text(
                    text = "YOUR LISTS",
                    modifier =
                        Modifier
                            .clearAndSetSemantics { },
                    style =
                        titleStyle.copy(
                            drawStyle =
                                Stroke(
                                    width = outlineWidth
                                )
                        ),

                    color = ComicInk
                )

                Text(
                    text = "YOUR LISTS",
                    style = titleStyle,
                    color = Color.White
                )
            }
        }

        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(
                        start = 12.dp
                    )
                    .height(3.dp)
                    .background(ComicInk)
        )
    }
}