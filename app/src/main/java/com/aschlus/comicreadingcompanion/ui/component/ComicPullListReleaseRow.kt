
package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.data.database.models.PullListScheduledReleaseResult
import com.aschlus.comicreadingcompanion.ui.theme.ComicCardBorderWidth
import com.aschlus.comicreadingcompanion.ui.theme.ComicCardShape
import com.aschlus.comicreadingcompanion.ui.theme.ComicDataRowShadowOffsetX
import com.aschlus.comicreadingcompanion.ui.theme.ComicDataRowShadowOffsetY
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ComicPullListReleaseRow(
    release: PullListScheduledReleaseResult,
    secondaryDateLabel: String,
    secondaryDate: String?,
    modifier: Modifier = Modifier
) {
    val title =
        release.issueNumber
            ?.takeIf { it.isNotBlank() }
            ?.let { "${release.seriesTitle} #$it" }
            ?: release.seriesTitle

    val priceText =
        release.price?.takeIf { it.isNotBlank() }?.let { price ->
            if (release.priceCurrency == "USD") {
                "$$price"
            } else {
                listOfNotNull(
                    price,
                    release.priceCurrency?.takeIf { it.isNotBlank() }
                ).joinToString(" ")
            }
        }

    val secondaryDateText =
        secondaryDate
            ?.takeIf { it.isNotBlank() }
            ?.let { date ->
                val formattedDate = runCatching {
                    LocalDate.parse(date).format(
                        DateTimeFormatter.ofPattern(
                            "MMM d, yyyy",
                            Locale.getDefault()
                        )
                    )
                }.getOrNull() ?: date

                "$secondaryDateLabel $formattedDate"
            }

    val shape = ComicCardShape

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(
                    x = ComicDataRowShadowOffsetX,
                    y = ComicDataRowShadowOffsetY
                )
                .background(
                    color = ComicInk,
                    shape = shape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = shape
                )
                .border(
                    width = ComicCardBorderWidth,
                    color = MaterialTheme.colorScheme.outline,
                    shape = shape
                )
                .padding(
                    horizontal = 14.dp,
                    vertical = 12.dp
                ),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        lineHeight = 19.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                priceText?.let { price ->
                    Text(
                        text = price,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            release.displayTitle
                ?.takeIf {
                    it.isNotBlank() && it != title
                }
                ?.let { displayTitle ->
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

            Text(
                text = release.publisherName,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            secondaryDateText?.let { dateText ->
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}