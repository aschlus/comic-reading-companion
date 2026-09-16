package com.aschlus.comicreadingcompanion.ui.component

import android.app.Dialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aschlus.comicreadingcompanion.ui.theme.ComicBlue
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper
import com.aschlus.comicreadingcompanion.ui.theme.ComicRed

@Composable
fun ComicConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false
) {
    Dialog(
        onDismissRequest = onDismiss
    ) {
        val shape = RoundedCornerShape(14.dp)

        Box(
            modifier =
                modifier.fillMaxWidth()
        ) {
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .offset(
                            x = 4.dp,
                            y = 5.dp
                        )
                        .background(
                            color = ComicInk,
                            shape = shape
                        )
            )

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            color = ComicPaper,
                            shape = shape
                        )
                        .border(
                            width = 2.dp,
                            color = ComicInk,
                            shape = shape
                        )
                        .padding(20.dp),
                verticalArrangement =
                    Arrangement.spacedBy(16.dp)
            ) {
                ComicSectionBanner(
                    text = title,
                    backgroundColor = ComicRed
                )

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge
                        .copy(
                            fontSize = 15.sp,
                            lineHeight = 21.sp
                        ),
                    color = ComicInk
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            10.dp,
                            Alignment.End
                        )
                ) {
                    ComicDialogButton(
                        text = "Cancel",
                        backgroundColor = ComicPaper,
                        onClick = onDismiss
                    )

                    ComicDialogButton(
                        text = confirmText,
                        backgroundColor =
                            if (destructive) {
                                ComicRed
                            } else {
                                ComicBlue
                            },
                        textColor =
                            if (destructive) {
                                Color.White
                            } else {
                                ComicInk
                            },
                        onClick = onConfirm
                    )
                }
            }
        }
    }
}

@Composable
internal fun ComicDialogButton(
    text: String,
    backgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = ComicInk,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(9.dp)

    Box(
        modifier =
            modifier
                .height(42.dp)
    ) {
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .offset(
                        x = 2.dp,
                        y = 3.dp
                    )
                    .background(
                        color =
                            ComicInk.copy(
                                alpha =
                                    if (enabled) {
                                        1f
                                    } else {
                                        0.35f
                                    }
                            ),
                        shape = shape
                    )
        )

        Box(
            modifier =
                Modifier
                    .height(41.dp)
                    .background(
                        color = backgroundColor.copy(
                            alpha =
                                if (enabled) {
                                    1f
                                } else {
                                    0.35f
                                }
                        ),
                        shape = shape
                    )
                    .border(
                        width = 2.dp,
                        color = ComicInk.copy(
                            alpha =
                                if (enabled) {
                                    1f
                                } else {
                                    0.35f
                                }
                        ),
                        shape = shape
                    )
                    .clickable(
                        enabled = enabled,
                        onClick = {
                            if (enabled) {
                                onClick()
                            }
                        }
                    )
                    .padding(
                        horizontal = 18.dp
                    ),
            contentAlignment =
                Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge
                    .copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                color = textColor.copy(
                    alpha =
                        if (enabled) {
                            1f
                        } else {
                            0.35f
                        }
                )
            )
        }
    }
}