package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicSpacingSm

@Composable
fun ComicFormDropdownField(
    text: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    menuContent: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(9.dp)

    Column(
        modifier =
            modifier.fillMaxWidth()
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .alpha(
                        if (enabled) {
                            1f
                        } else {
                            0.38f
                        }
                    )
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = shape
                    )
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = shape
                    )
                    .clickable(
                        enabled = enabled,
                        role = Role.Button
                    ) {
                        onExpandedChange(!expanded)
                    }
                    .padding(
                        start = 14.dp,
                        end = ComicSpacingSm
                    ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
                    .copy(
                        fontWeight = FontWeight.Medium
                    ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )

            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier =
                    Modifier
                        .size(26.dp)
                        .graphicsLayer {
                            rotationZ =
                                if (expanded) {
                                    180f
                                } else {
                                    0f
                                }
                        }
            )
        }

        if (expanded) {
            ComicFormDropdownOptions(
                modifier = Modifier.padding(top = 6.dp),
                content = menuContent
            )
        }
    }
}