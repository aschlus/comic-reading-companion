package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicSpacingMd
import com.aschlus.comicreadingcompanion.ui.theme.ComicAccentTextTransform
import com.aschlus.comicreadingcompanion.ui.theme.ComicCardBorderWidth
import com.aschlus.comicreadingcompanion.ui.theme.ComicCardShadowOffsetX
import com.aschlus.comicreadingcompanion.ui.theme.ComicCardShadowOffsetY
import com.aschlus.comicreadingcompanion.ui.theme.LilitaOneFontFamily

@Composable
fun ComicLibraryActionButton(
    text: String,
    icon: ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val shape =
        RoundedCornerShape(8.dp)

    Box(
        modifier =
            modifier
                .height(56.dp)
                .alpha(
                    if (enabled) {
                        1f
                    } else {
                        0.55f
                    }
                )
    ) {
        // Small printed offset shadow.
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .offset(
                        x = ComicCardShadowOffsetX,
                        y = ComicCardShadowOffsetY
                    )
                    .background(
                        color = ComicInk,
                        shape = shape
                    )
        )

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(shape)
                    .background(backgroundColor)
                    .border(
                        width = ComicCardBorderWidth,
                        color = ComicInk,
                        shape = shape
                    )
                    .clickable(
                        enabled = enabled,
                        role = Role.Button,
                        onClick = onClick
                    )
        ) {
            ComicHalftoneOverlay(
                modifier = Modifier.fillMaxSize(),
                dotColor = ComicInk,
                spacing = 8.dp,
                radius = 0.70.dp,
                startFraction = 0.58f,
                minAlpha = 0.1f,
                maxAlpha = 0.17f
            )

            Row(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = ComicSpacingMd
                        ),
                verticalAlignment =
                    Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ComicInk,
                    modifier =
                        Modifier.size(30.dp)
                )

                Text(
                    text = text,
                    modifier =
                        Modifier.padding(
                            start = 10.dp
                        ),
                    style = MaterialTheme.typography.titleMedium
                        .copy(
                            fontFamily = LilitaOneFontFamily,
                            fontSize = 16.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Normal,
                            fontStyle = FontStyle.Normal,
                            textGeometricTransform = ComicAccentTextTransform
                        ),
                    color = ComicInk
                )
            }
        }
    }
}