package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.ui.theme.ComicAccentTextTransform
import com.aschlus.comicreadingcompanion.ui.theme.ComicCardBorderWidth
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicRed
import com.aschlus.comicreadingcompanion.ui.theme.LilitaOneFontFamily

@Composable
fun ComicDataSectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    val outlineWidth = with(LocalDensity.current) { 6.dp.toPx() }

    val titleStyle = TextStyle(
        fontFamily = LilitaOneFontFamily,
        fontSize = 17.sp,
        lineHeight = 19.sp,
        textGeometricTransform = ComicAccentTextTransform
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.height(34.dp)) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = 2.dp, y = 2.dp)
                    .background(ComicInk, ComicSlantedShape)
            )

            Box(
                modifier = Modifier
                    .height(34.dp)
                    .background(ComicRed, ComicSlantedShape)
                    .border(
                        width = ComicCardBorderWidth,
                        color = ComicInk,
                        shape = ComicSlantedShape
                    )
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = text,
                    modifier = Modifier
                        .offset(y = 1.dp)
                        .clearAndSetSemantics { },
                    style = titleStyle.copy(
                        drawStyle = Stroke(
                            width = outlineWidth,
                            join = StrokeJoin.Round
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
    }
}