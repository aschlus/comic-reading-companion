package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aschlus.comicreadingcompanion.ui.theme.ComicCardBorderWidth
import com.aschlus.comicreadingcompanion.ui.theme.ComicCardShadowOffsetX
import com.aschlus.comicreadingcompanion.ui.theme.ComicCardShadowOffsetY
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk

@Composable
fun ComicAddIssuesTrigger(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(9.dp)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(54.dp)
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .offset(
                        x = ComicCardShadowOffsetX,
                        y = ComicCardShadowOffsetY
                    )
                    .background(
                        color = ComicInk,
                        shape = shape
                    )
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(shape)
                    .background(
                        color = MaterialTheme.colorScheme.surface
                    )
                    .border(
                        width = ComicCardBorderWidth,
                        color = ComicInk,
                        shape = shape
                    )
                    .clickable(
                        role = Role.Button,
                        onClick = onClick
                    )
                    .padding(
                        horizontal = 14.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(25.dp)
            )

            Text(
                text = "Search issues or series",
                modifier = Modifier.padding(start = 10.dp),
                style = MaterialTheme.typography.bodyLarge
                    .copy(
                        fontWeight = FontWeight.Medium
                    ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}