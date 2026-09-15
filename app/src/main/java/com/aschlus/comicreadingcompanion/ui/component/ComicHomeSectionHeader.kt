package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicRed
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable
fun ComicHomeSectionHeader(
    text: String,
    onSeeAllClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    val outlineWidth =
        with (density) {
            1.3.dp.toPx()
        }

    val titleStyle =
        TextStyle(
            fontSize = 20.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic
        )

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(48.dp),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Box(
            modifier =
                Modifier
                    .height(42.dp)
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
                            shape = ComicSlantedShape
                        )
            )

            Box(
                modifier =
                    Modifier
                        .height(42.dp)
                        .background(
                            color = ComicRed,
                            shape = ComicSlantedShape
                        )
                        .border(
                            width = 2.dp,
                            color = ComicInk,
                            shape = ComicSlantedShape
                        )
                        .padding(
                            horizontal = 14.dp
                        ),
                contentAlignment =
                    Alignment.CenterStart
            ) {
                Text(
                    text = text,
                    modifier =
                        Modifier
                            .offset(y = 1.dp)
                            .clearAndSetSemantics { },
                    style = titleStyle.copy(
                        drawStyle =
                            Stroke(
                                width = outlineWidth
                            )
                    ),
                    color = ComicInk,
                    maxLines = 1
                )

                Text(
                    text = text,
                    modifier = Modifier.offset(y = 1.dp),
                    style = titleStyle,
                    color = Color.White,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (onSeeAllClick != null) {
            Text(
                text = "See All  ›",
                modifier =
                    Modifier
                        .semantics {
                            contentDescription =
                                "$text see all"
                        }
                        .clickable(
                            onClick = onSeeAllClick
                        )
                        .padding(
                            horizontal = 8.dp,
                            vertical = 10.dp
                        ),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ComicInk
            )
        }
    }
}