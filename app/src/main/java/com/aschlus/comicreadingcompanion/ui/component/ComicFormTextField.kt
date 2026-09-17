package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicMutedInk

@Composable
fun ComicFormTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    minHeight: Int = 48
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
                    .heightIn(min = minHeight.dp)
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
                    SolidColor(ComicInk),
                decorationBox = { innerTextField ->
                    Box{
                        if (
                            value.isEmpty() &&
                            placeholder != null
                        ) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = ComicMutedInk
                            )
                        }

                        innerTextField()
                    }
                }
            )
        }
    }
}