package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.ui.theme.ComicCardBorderWidth

@Composable
fun ComicDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 4.dp,
        border =
            BorderStroke(
                width = ComicCardBorderWidth,
                color = MaterialTheme.colorScheme.outline
            ),
        content = content
    )
}

@Composable
fun ComicDropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false
) {
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge
                    .copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                color =
                    when {
                        !enabled ->
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)

                        destructive ->
                            MaterialTheme.colorScheme.error

                        else ->
                            MaterialTheme.colorScheme.onSurface
                    }
            )
        },
        onClick = onClick,
        modifier =
            modifier.heightIn(
                min = 48.dp
            ),
        enabled = enabled,
        contentPadding =
            PaddingValues(
                horizontal = 14.dp
            )
    )
}