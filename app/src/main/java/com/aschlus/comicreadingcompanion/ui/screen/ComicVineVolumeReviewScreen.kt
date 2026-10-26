package com.aschlus.comicreadingcompanion.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.data.database.entities.IssueType
import com.aschlus.comicreadingcompanion.data.remote.ComicVineIssueHit
import com.aschlus.comicreadingcompanion.ui.component.ComicCoverImage
import com.aschlus.comicreadingcompanion.ui.component.ComicFormDialog
import com.aschlus.comicreadingcompanion.ui.component.ComicFormDropdownField
import com.aschlus.comicreadingcompanion.ui.component.ComicFormDropdownOption
import com.aschlus.comicreadingcompanion.ui.component.ComicFormDropdownOptions
import com.aschlus.comicreadingcompanion.ui.component.ComicHomeSectionHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicPrimaryHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicPrimaryHeaderBackButton
import com.aschlus.comicreadingcompanion.ui.component.ComicPrimaryHeaderTitle
import com.aschlus.comicreadingcompanion.ui.component.ComicWideActionButton
import com.aschlus.comicreadingcompanion.ui.theme.ComicBlueLight
import com.aschlus.comicreadingcompanion.ui.theme.ComicCardBorderWidth
import com.aschlus.comicreadingcompanion.ui.theme.ComicCardShadowOffsetX
import com.aschlus.comicreadingcompanion.ui.theme.ComicCardShadowOffsetY
import com.aschlus.comicreadingcompanion.ui.theme.ComicCardShape
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicMutedInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper
import com.aschlus.comicreadingcompanion.ui.theme.ComicRed
import com.aschlus.comicreadingcompanion.ui.theme.ComicSpacingLg
import com.aschlus.comicreadingcompanion.ui.theme.ComicSpacingMd
import com.aschlus.comicreadingcompanion.ui.theme.ComicSpacingSm
import com.aschlus.comicreadingcompanion.ui.theme.ComicSpacingXs
import com.aschlus.comicreadingcompanion.ui.viewmodel.ComicVineIssueTypeInference
import com.aschlus.comicreadingcompanion.ui.viewmodel.ComicVineVolumeReviewViewModel
import com.aschlus.comicreadingcompanion.util.formatPublicationDate

@Composable
fun ComicVineVolumeReviewScreen(
    viewModel: ComicVineVolumeReviewViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var showCreateUniverseDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var newUniverseName by rememberSaveable {
        mutableStateOf("")
    }

    var newUniverseDesignation by rememberSaveable {
        mutableStateOf("")
    }

    var universeMenuExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ComicPrimaryHeader {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ComicPrimaryHeaderBackButton(
                        onClick = onBackClick
                    )

                    Spacer(Modifier.width(ComicSpacingLg))

                    ComicPrimaryHeaderTitle(
                        text = "SERIES REVIEW"
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding),
            contentPadding = PaddingValues(ComicSpacingLg),
            verticalArrangement = Arrangement.spacedBy(ComicSpacingMd)
        ) {
            if (uiState.isLoading && uiState.volume == null) {
                item {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            uiState.error?.let { error ->
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(ComicRed.copy(alpha = 0.12f))
                            .border(
                                width = ComicCardBorderWidth,
                                color = ComicRed,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = ComicSpacingMd, vertical = 10.dp)
                    ) {
                        Text(
                            text = error,
                            color = ComicRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            uiState.volume?.let { volume ->
                item {
                    ComicHomeSectionHeader(
                        text = "SERIES DETAILS"
                    )
                }

                item {
                    ReviewInfoCard {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "SERIES TITLE",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            val titleFieldShape = RoundedCornerShape(6.dp)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(titleFieldShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(
                                        width = ComicCardBorderWidth,
                                        color = ComicInk,
                                        shape = titleFieldShape
                                    )
                                    .padding(horizontal = ComicSpacingMd, vertical = ComicSpacingMd)
                            ) {
                                BasicTextField(
                                    value = uiState.editedTitle,
                                    onValueChange = viewModel::updateEditedTitle,
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(ComicSpacingMd)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(ComicSpacingMd)
                            ) {
                                ReviewEditableMetadataField(
                                    label = "PUBLISHER",
                                    value = uiState.editedPublisher,
                                    onValueChange = viewModel::updateEditedPublisher,
                                    modifier = Modifier.weight(1f)
                                )

                                ReviewEditableMetadataField(
                                    label = "VOLUME NUMBER",
                                    value = uiState.editedVolume,
                                    onValueChange = viewModel::updateEditedVolume,
                                    modifier = Modifier.weight(1f),
                                    keyboardType = KeyboardType.Number
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(ComicSpacingMd)
                            ) {
                                ReviewEditableMetadataField(
                                    label = "START YEAR",
                                    value = uiState.editedStartYear,
                                    onValueChange = viewModel::updateEditedStartYear,
                                    modifier = Modifier.weight(1f),
                                    keyboardType = KeyboardType.Number
                                )

                                ReviewEditableMetadataField(
                                    label = "END YEAR",
                                    value = uiState.editedEndYear,
                                    onValueChange = viewModel::updateEditedEndYear,
                                    modifier = Modifier.weight(1f),
                                    keyboardType = KeyboardType.Number
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(ComicSpacingMd)
                            ) {
                                ReviewMetadataItem(
                                    label = "REPORTED ISSUES",
                                    value = volume.countOfIssues?.toString() ?: "Unknown",
                                    modifier = Modifier.weight(1f)
                                )

                                ReviewMetadataItem(
                                    label = "SOURCE ID",
                                    value = volume.id.toString(),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "UNIVERSE / CONTINUITY",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            val selectedUniverse = uiState.availableUniverses
                                .firstOrNull { it.id == uiState.selectedUniverseId }

                            ComicFormDropdownField(
                                text = selectedUniverse?.let { universe ->
                                    universe.designation?.let { designation ->
                                        "${universe.name} ($designation)"
                                    } ?: universe.name
                                } ?: if (uiState.selectedUniverseId == null) {
                                    "No universe"
                                } else {
                                    "Select a universe"
                                },
                                expanded = universeMenuExpanded,
                                onExpandedChange = {
                                    universeMenuExpanded = it
                                }
                            ) {
                                ComicFormDropdownOption(
                                    text = "NO UNIVERSE",
                                    onClick = {
                                        viewModel.selectUniverse(null)
                                        universeMenuExpanded = false
                                    }
                                )
                                ComicFormDropdownOption(
                                    text = "CREATE NEW UNIVERSE...",
                                    onClick = {
                                        universeMenuExpanded = false
                                        newUniverseName = ""
                                        newUniverseDesignation = ""
                                        showCreateUniverseDialog = true
                                    }
                                )

                                uiState.availableUniverses.forEach { universe ->
                                    ComicFormDropdownOption(
                                        text = universe.designation?.let { designation ->
                                            "${universe.name} ($designation)"
                                        } ?: universe.name,
                                        onClick = {
                                            viewModel.selectUniverse(universe.id)
                                            universeMenuExpanded = false
                                        }
                                    )
                                }
                            }

                            if (uiState.isCreatingUniverse) {
                                Text(
                                    text = "Creating universe...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }

                            uiState.universeCreationError?.let { error ->
                                Text(
                                    text = error,
                                    color = ComicRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(ComicSpacingSm)
                        ) {
                            ComicWideActionButton(
                                text = if (uiState.isImporting) {
                                    "IMPORTING..."
                                } else {
                                    "IMPORT TO CATALOG"
                                },
                                backgroundColor = ComicBlueLight,
                                onClick = viewModel::importReviewedSeries,
                                enabled = !uiState.isLoading &&
                                    !uiState.isImporting &&
                                    !uiState.isCreatingUniverse &&
                                    uiState.editedTitle.isNotBlank() &&
                                    uiState.editedPublisher.isNotBlank() &&
                                    (
                                        uiState.selectedUniverseId == null ||
                                            uiState.availableUniverses.any { universe ->
                                                universe.id == uiState.selectedUniverseId &&
                                                    !universe.designation.isNullOrBlank()
                                            }
                                    )
                            )

                            if (uiState.isImporting) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(ComicSpacingSm)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Text(
                                        text = "Importing series and issues...",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            uiState.importError?.let { error ->
                                Text(
                                    text = error,
                                    color = ComicRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            uiState.importSuccessMessage?.let { message ->
                                Text(
                                    text = message,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                item {
                    ComicHomeSectionHeader(
                        text = "ISSUES"
                    )
                }

                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(ComicSpacingXs)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(ComicBlueLight)
                                .border(
                                    width = ComicCardBorderWidth,
                                    color = ComicInk,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = ComicSpacingSm)
                        ) {
                            Text(
                                text = "Loaded ${uiState.issues.size} of ${
                                    uiState.totalIssueRecords
                                        ?: "an unknown number of"
                                } source issue records",
                                color = ComicInk,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (uiState.isLoading) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Text(
                                    text = "Loading issues...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                items(
                    items = uiState.issues,
                    key = { issue -> issue.id }
                ) { issue ->
                    val issueType =
                        uiState.issueTypeOverrides[issue.id]
                            ?: IssueType.valueOf(
                                ComicVineIssueTypeInference.infer(issue)
                            )

                    ReviewIssueRow(
                        issue = issue,
                        issueType = issueType,
                        enabled = !uiState.isImporting,
                        onIssueTypeSelected = { selectedType ->
                            viewModel.updateIssueType(
                                issueId = issue.id,
                                issueType = selectedType
                            )
                        }
                    )
                }

                if (!uiState.isLoading && uiState.issues.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(
                                    width = ComicCardBorderWidth,
                                    color = ComicInk,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = ComicSpacingMd, vertical = ComicSpacingMd)
                        ) {
                            Text(
                                text = "No issues records were retrieved",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateUniverseDialog) {
        ComicFormDialog(
            title = "CREATE UNIVERSE",
            primaryLabel = "UNIVERSE NAME",
            primaryValue = newUniverseName,
            onPrimaryValueChange = {
                newUniverseName = it
            },
            secondaryLabel = "CONTINUITY DESIGNATION",
            secondaryValue = newUniverseDesignation,
            onSecondaryValueChange = {
                newUniverseDesignation = it
            },
            confirmText = "CREATE",
            confirmEnabled = newUniverseName.isNotBlank() &&
                newUniverseDesignation.isNotBlank(),
            onConfirm = {
                viewModel.createUniverse(
                    name = newUniverseName,
                    designation = newUniverseDesignation
                )
                showCreateUniverseDialog = false
            },
            onDismiss = {
                showCreateUniverseDialog = false
            }
        )
    }
}

@Composable
private fun ReviewInfoCard(
    content: @Composable () -> Unit
) {
    val shape = ComicCardShape

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(
                    ComicCardShadowOffsetX,
                    ComicCardShadowOffsetY
                )
                .background(
                    color = ComicInk,
                    shape = shape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = ComicCardBorderWidth,
                    color = ComicInk,
                    shape = shape
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(ComicSpacingSm)
        ) {
            content()
        }
    }
}

@Composable
private fun ReviewEditableMetadataField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(ComicSpacingXs)
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        val fieldShape = RoundedCornerShape(6.dp)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(fieldShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = ComicCardBorderWidth,
                    color = ComicInk,
                    shape = fieldShape
                )
                .padding(
                    horizontal = 10.dp,
                    vertical = 11.dp
                )
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType
                ),
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ReviewMetadataItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ReviewIssueRow(
    issue: ComicVineIssueHit,
    issueType: IssueType,
    enabled: Boolean,
    onIssueTypeSelected: (IssueType) -> Unit
) {
    val shape = ComicCardShape

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(
                    ComicCardShadowOffsetX,
                    ComicCardShadowOffsetY
                )
                .background(
                    color = ComicInk,
                    shape = shape
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = ComicCardBorderWidth,
                    color = ComicInk,
                    shape = shape
                )
                .padding(
                    horizontal = 10.dp,
                    vertical = 9.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ComicCoverImage(
                coverUrl = issue.imageUrl,
                contentDescription = issue.issueNumber?.let {
                    "Issue #$it cover"
                } ?: "${issue.name ?: "Issue"} cover",
                modifier = Modifier
                    .width(52.dp)
                    .aspectRatio(2f / 3f),
                placeholderText = issue.issueNumber?.let {
                    "#$it"
                } ?: "No Cover",
                shape = RoundedCornerShape(4.dp),
                borderColor = ComicInk
            )

            Spacer(Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = issue.name
                        ?.takeIf { it.isNotBlank() }
                        ?: "Untitled issue",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = androidx.compose.material3.MaterialTheme
                        .typography.titleMedium.copy(
                            fontSize = 15.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Bold
                        ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                (issue.coverDate?.takeIf { it.isNotBlank() }
                    ?: issue.storeDate?.takeIf { it.isNotBlank() }
                )?.let { date ->
                    Text(
                        text = "Publication date: ${formatPublicationDate(date)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme
                            .typography.bodySmall.copy(
                                fontSize = 12.sp,
                                lineHeight = 15.sp
                            ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "Source ID: ${issue.id}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme
                        .typography.bodySmall.copy(
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                ReviewIssueTypeSelector(
                    issueId = issue.id,
                    issueType = issueType,
                    enabled = enabled,
                    onIssueTypeSelected = onIssueTypeSelected
                )
            }
        }
    }
}

@Composable
private fun ReviewIssueTypeSelector(
    issueId: Long,
    issueType: IssueType,
    enabled: Boolean,
    onIssueTypeSelected: (IssueType) -> Unit
) {
    var expanded by rememberSaveable(issueId) {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = "ISSUE TYPE",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clickable(
                    enabled = enabled,
                    role = Role.Button
                ) {
                    expanded = !expanded
                }
                .testTag("issue-type-selector-$issueId"),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = ComicCardBorderWidth,
                        color = ComicInk,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = ComicSpacingSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = issueType.displayLabel(),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        if (expanded) {
            ComicFormDropdownOptions(
                modifier = Modifier.padding(top = 2.dp)
            ) {
                IssueType.values().forEach { option ->
                    ComicFormDropdownOption(
                        text = option.displayLabel(),
                        onClick = {
                            onIssueTypeSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

private fun IssueType.displayLabel(): String {
    return when (this) {
        IssueType.REGULAR -> "Regular"
        IssueType.ANNUAL -> "Annual"
        IssueType.SPECIAL -> "Special"
        IssueType.ONE_SHOT -> "One-shot"
        IssueType.GIANT_SIZE -> "Giant-size"
        IssueType.PREVIEW -> "Preview"
    }
}