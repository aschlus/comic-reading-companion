package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable
fun ComicBottomNavigation(
    currentRoute: String?,
    onHomeClick: () -> Unit,
    onBrowseClick: () -> Unit,
    onLibraryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val itemColors =
        NavigationBarItemDefaults.colors(
            selectedIconColor = ComicInk,
            selectedTextColor = ComicInk,
            indicatorColor = MaterialTheme.colorScheme.secondary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        )

    NavigationBar(
        modifier =
            modifier.border(
                width = 2.dp,
                color = ComicInk,
                shape = RectangleShape
            ),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            modifier =
                Modifier.semantics {
                    contentDescription = "Home tab"
                },
            selected = currentRoute == "home",
            onClick = onHomeClick,
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null
                )
            },
            label = {
                Text("Home")
            },
            colors = itemColors
        )

        NavigationBarItem(
            modifier =
                Modifier.semantics {
                    contentDescription = "Browse tab"
                },
            selected = currentRoute == "browse",
            onClick = onBrowseClick,
            icon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null
                )
            },
            label = {
                Text("Browse")
            },
            colors = itemColors
        )

        NavigationBarItem(
            modifier =
                Modifier.semantics {
                    contentDescription = "Library tab"
                },
            selected = currentRoute == "library",
            onClick = onLibraryClick,
            icon = {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = null
                )
            },
            label = {
                Text("Library")
            },
            colors = itemColors
        )
    }
}