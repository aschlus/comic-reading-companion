package com.aschlus.comicreadingcompanion.ui.component

import android.app.Dialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aschlus.comicreadingcompanion.ui.theme.ComicBlue
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper
import com.aschlus.comicreadingcompanion.ui.theme.ComicRed

@Composable
fun ComicFormDialog(
    title: String,
    primaryLabel: String,
    primaryValue: String,
    onPrimaryValueChange: (String) -> Unit,
    secondaryLabel: String,
    secondaryValue: String,
    onSecondaryValueChange: (String) -> Unit,
    confirmText: String,
    confirmEnabled: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
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

                ComicDialogTextField(
                    label = primaryLabel,
                    value = primaryValue,
                    onValueChange = onPrimaryValueChange,
                    singleLine = true
                )

                ComicDialogTextField(
                    label = secondaryLabel,
                    value = secondaryValue,
                    onValueChange = onSecondaryValueChange,
                    singleLine = false
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
                        enabled = confirmEnabled,
                        onClick = onConfirm
                    )

                    ComicDialogButton(
                        text = confirmText,
                        backgroundColor = ComicBlue,
                        enabled = confirmEnabled,
                        onClick = {
                            if (confirmEnabled) {
                                onConfirm()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ComicDialogTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(9.dp)

    Column(
        modifier =
            modifier.fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium
                .copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
            color = ComicInk
        )

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(
                        min =
                            if (singleLine) {
                                48.dp
                            } else {
                                96.dp
                            }
                    )
                    .background(
                        color = Color.White,
                        shape = shape
                    )
                    .border(
                        width = 2.dp,
                        color = ComicInk,
                        shape = shape
                    ),
            contentAlignment =
                if (singleLine) {
                    Alignment.CenterStart
                } else {
                    Alignment.TopStart
                }
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                singleLine = singleLine,
                maxLines =
                    if (singleLine) {
                        1
                    } else {
                        4
                    },
                textStyle = MaterialTheme.typography.bodyLarge
                    .copy(
                        color = ComicInk
                    ),
                cursorBrush =
                    SolidColor(ComicInk)
            )
        }
    }
}