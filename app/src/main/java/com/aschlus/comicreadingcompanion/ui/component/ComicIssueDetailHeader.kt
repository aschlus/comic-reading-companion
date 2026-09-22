package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.ui.theme.ComicHeaderTextTransform
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper
import com.aschlus.comicreadingcompanion.ui.theme.ComicYellow
import com.aschlus.comicreadingcompanion.ui.theme.LilitaOneFontFamily

@Composable
fun ComicIssueDetailHeader(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    val outlineWidth =
        with (density) {
            8.dp.toPx()
        }

    val titleStyle =
        TextStyle(
            fontFamily = LilitaOneFontFamily,
            fontSize = 38.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.Normal,
            textGeometricTransform = ComicHeaderTextTransform
        )

    ComicPrimaryHeader(
        modifier = modifier
    ) {
        Box(
            modifier =
                Modifier
                    .align(
                        Alignment.CenterStart
                    )
                    .offset(
                        x = 18.dp
                    )
                    .size(58.dp)
        ) {
            val shape = RoundedCornerShape(10.dp)

            Box(
                modifier =
                    Modifier
                        .size(54.dp)
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
                        .size(54.dp)
                        .clip(shape)
                        .background(
                            ComicPaper
                        )
                        .border(
                            width = 2.dp,
                            color = ComicInk,
                            shape = shape
                        )
                        .clickable(
                            onClick = onBackClick
                        ),
                contentAlignment =
                    Alignment.Center
            ) {
                Icon(
                    imageVector =
                        Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = ComicInk,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Box(
            modifier =
                Modifier
                    .align(
                        Alignment.CenterStart
                    )
                    .padding(
                        start = 96.dp,
                        end = 12.dp
                    )
        ) {
            Text(
                text = "ISSUE DETAIL",
                modifier =
                    Modifier
                        .clearAndSetSemantics { },
                style =
                    titleStyle.copy(
                        drawStyle =
                            Stroke(
                                width = outlineWidth,
                                join = StrokeJoin.Round
                            )
                    ),
                color = ComicInk,
                maxLines = 1
            )

            Text(
                text = "ISSUE DETAIL",
                style = titleStyle,
                color = ComicYellow,
                maxLines = 1
            )
        }
    }
}