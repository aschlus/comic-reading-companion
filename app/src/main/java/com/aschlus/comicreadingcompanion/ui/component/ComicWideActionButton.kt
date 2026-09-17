package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

@Composable
fun ComicWideActionButton(
    text: String,
    backgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(9.dp)

    val alpha =
        if (enabled) {
            1f
        } else {
            0.35f
        }

    Box(
        modifier =
            modifier
                .height(46.dp)
                .semantics(
                    mergeDescendants = true
                ) {
                    role = Role.Button
                }
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .offset(
                        x = 2.dp,
                        y = 3.dp
                    )
                    .background(
                        color =
                            ComicInk.copy(
                                alpha = alpha
                            ),
                        shape = shape
                    )
        )

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .background(
                        color = backgroundColor.copy(alpha = alpha),
                        shape = shape
                    )
                    .border(
                        width = 2.dp,
                        color = ComicInk.copy(alpha = alpha),
                        shape = shape
                    )
                    .clickable(
                        enabled = enabled,
                        onClick = onClick
                    ),
            contentAlignment =
                Alignment.Center
        ) {
            Text(
                text = text,
                style =
                    MaterialTheme.typography
                        .labelLarge
                        .copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                color =
                    ComicInk.copy(alpha = alpha)
            )
        }
    }
}