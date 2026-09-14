package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicMutedInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper
import com.aschlus.comicreadingcompanion.ui.theme.ComicYellow
import androidx.compose.foundation.layout.offset

@Composable
fun ComicBottomNavigation(
    currentRoute: String?,
    onHomeClick: () -> Unit,
    onBrowseClick: () -> Unit,
    onLibraryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier =
            modifier
                .drawWithContent {
                    drawContent()

                    val strokeWidth = 2.dp.toPx()

                    drawLine(
                        color = ComicInk,
                        start = Offset(
                            x = 0f,
                            y = strokeWidth / 2f
                        ),
                        end = Offset(
                            x = size.width,
                            y = strokeWidth / 2f
                        ),
                        strokeWidth = strokeWidth
                    )
                },
        containerColor = ComicPaper,
        tonalElevation = 0.dp
    ) {
        ComicBottomNavDestination(
            label = "Home",
            icon = Icons.Default.Home,
            selected = currentRoute == "home",
            contentDescription = "Home tab",
            onClick = onHomeClick
        )

        ComicBottomNavDestination(
            label = "Library",
            icon = Icons.Default.List,
            selected = currentRoute == "library",
            contentDescription = "Library tab",
            onClick = onLibraryClick
        )

        ComicBottomNavDestination(
            label = "Browse",
            icon = Icons.Default.Search,
            selected = currentRoute == "browse",
            contentDescription = "Browse tab",
            onClick = onBrowseClick
        )
    }
}

@Composable
private fun RowScope.ComicBottomNavDestination(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    contentDescription: String,
    onClick: () -> Unit
) {
    val contentColor =
        if (selected) {
            ComicInk
        } else {
            ComicMutedInk
        }

    Box(
        modifier =
            Modifier
                .weight(1f)
                .semantics {
                    this.contentDescription = contentDescription
                }
                .selectable(
                    selected = selected,
                    onClick = onClick,
                    role = Role.Tab
                ),
        contentAlignment =
            Alignment.Center
    ) {
        Column(
            modifier =
                if (selected) {
                    Modifier
                        .width(104.dp)
                        .height(58.dp)
                        .background(
                            color = ComicYellow,
                            shape = RoundedCornerShape(22.dp)
                        )
                } else {
                    Modifier
                },
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier =
                    Modifier.size(
                        if (selected) {
                            27.dp
                        } else {
                            25.dp
                        }
                    )
            )

            Text(
                text = label,
                modifier =
                    Modifier.offset(
                        y = (-2).dp
                    ),
                color = contentColor,
                fontSize = 13.sp,
                fontWeight =
                    if (selected) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    }
            )
        }
    }
}