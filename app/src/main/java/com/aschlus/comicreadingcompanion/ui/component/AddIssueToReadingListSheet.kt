package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingList
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaperDeep
import com.aschlus.comicreadingcompanion.ui.theme.ComicRed
import com.aschlus.comicreadingcompanion.ui.theme.ComicYellow
import com.aschlus.comicreadingcompanion.ui.viewmodel.AddIssueToReadingListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIssueToReadingListSheet(
    viewModel: AddIssueToReadingListViewModel,
    onDismissRequest: () -> Unit
) {
    val readingLists by viewModel.userReadingLists.collectAsState()
    val isAdding by viewModel.isAdding.collectAsState()
    val addedReadingListId by viewModel.addedReadingListId.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )

    LaunchedEffect(
        addedReadingListId
    ) {
        if (addedReadingListId != null) {
            viewModel.clearResult()
            onDismissRequest()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.clearResult()
            onDismissRequest()
        },
        sheetState = sheetState,
        containerColor = ComicPaper,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    start = 18.dp,
                    end = 18.dp,
                    top = 12.dp,
                    bottom = 24.dp
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier =
                    Modifier.fillMaxWidth(),
                contentAlignment =
                    Alignment.Center
            ) {
                Box(
                    modifier =
                        Modifier
                            .height(4.dp)
                            .fillMaxWidth(0.12f)
                            .background(
                                color = ComicInk,
                                shape =
                                    RoundedCornerShape(percent = 50)
                            )
                )
            }

            ComicHomeSectionHeader(
                text = "ADD TO READING LIST"
            )

            if (isAdding) {
                LinearProgressIndicator(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .border(
                                width = 1.dp,
                                color = ComicInk,
                                shape = RoundedCornerShape(percent = 50)
                            ),
                    color = ComicYellow,
                    trackColor = ComicPaperDeep
                )
            }

            errorMessage?.let { message ->
                ComicSheetMessage(
                    message = message,
                    error = true
                )
            }

            if (readingLists.isEmpty()) {
                ComicEmptyState(
                    message = "No custom reading lists yet."
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(
                            max = 400.dp
                        ),
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = readingLists,
                        key = { readingList -> readingList.id}
                    ) { readingList ->
                        ComicAddIssueReadingListRow(
                            readingList = readingList,
                            enabled = !isAdding,
                            onClick = {
                                viewModel.addToReadingList(readingList.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComicAddIssueReadingListRow(
    readingList: ReadingList,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier =
            modifier.fillMaxWidth()
    ) {
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .offset(
                        x = 3.dp,
                        y = 4.dp
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
                    .fillMaxWidth()
                    .clip(shape)
                    .background(
                        ComicPaper.copy(
                            alpha =
                                if (enabled) {
                                    1f
                                } else {
                                    0.6f
                                }
                        )
                    )
                    .border(
                        width = 2.dp,
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
                    .clickable(
                        enabled = enabled,
                        onClick = onClick
                    )
                    .padding(
                        horizontal = 14.dp,
                        vertical = 11.dp
                    )
        ) {
            Column(
                modifier =
                    Modifier.padding(
                        end = 30.dp
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = readingList.title,
                    style =
                        MaterialTheme.typography.titleMedium
                            .copy(
                                fontSize = 16.sp,
                                lineHeight = 19.sp,
                                fontWeight = FontWeight.Bold
                            ),
                    color = ComicInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                readingList.description
                    ?.takeIf { it.isNotBlank() }
                    ?.let { description ->
                        Text(
                            text = description,
                            style =
                                MaterialTheme.typography.bodyMedium
                                    .copy(
                                        fontSize = 13.sp,
                                        lineHeight = 17.sp
                                    ),
                            color = ComicInk.copy(alpha = 0.72f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
            }

            Text(
                text = "›",
                modifier =
                    Modifier.align(
                        Alignment.CenterEnd
                    ),
                fontSize = 30.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold,
                color = ComicInk
            )
        }
    }
}

@Composable
private fun ComicSheetMessage(
    message: String,
    error: Boolean,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(9.dp)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    color =
                        if (error) {
                            ComicRed.copy(alpha = 0.12f)
                        } else {
                            ComicPaper
                        },
                    shape = shape
                )
                .border(
                    width = 2.dp,
                    color =
                        if (error) {
                            ComicRed
                        } else {
                            ComicInk
                        },
                    shape = shape
                )
                .padding(
                    horizontal = 14.dp,
                    vertical = 11.dp
                )
    ) {
        Text(
            text = message,
            style =
                MaterialTheme.typography.bodyMedium
                    .copy(
                        fontWeight = FontWeight.SemiBold
                    ),
            color = ComicInk
        )
    }
}