package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk

@Composable
fun ComicActionPanel(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
    ) {
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .padding(
                        start = 5.dp,
                        top = 5.dp
                    )
                    .background(
                        color = ComicInk,
                        shape = ComicSlantedShape
                    )
        )

        Row(
            modifier =
                Modifier
                    .background(
                        color = backgroundColor,
                        shape = ComicSlantedShape
                    )
                    .border(
                        width = 2.dp,
                        color = ComicInk,
                        shape = ComicSlantedShape
                    )
                    .clickable(
                        enabled = enabled,
                        onClick = onClick
                    )
                    .padding(
                        horizontal = 18.dp,
                        vertical = 20.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ComicInk,
                modifier =
                    Modifier.size(30.dp)
            )

            Spacer(
                modifier =
                    Modifier.width(12.dp)
            )

            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = ComicInk
            )
        }
    }
}