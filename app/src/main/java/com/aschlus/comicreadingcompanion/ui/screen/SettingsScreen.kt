package com.aschlus.comicreadingcompanion.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.aschlus.comicreadingcompanion.ui.component.ComicHomeSectionHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicPrimaryHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicPrimaryHeaderBackButton
import com.aschlus.comicreadingcompanion.ui.component.ComicPrimaryHeaderTitle
import com.aschlus.comicreadingcompanion.ui.component.ComicWideActionButton
import com.aschlus.comicreadingcompanion.ui.theme.ComicBlue
import com.aschlus.comicreadingcompanion.ui.theme.ComicDarkBackground
import com.aschlus.comicreadingcompanion.ui.theme.ComicGreen
import com.aschlus.comicreadingcompanion.ui.theme.ComicGreenDark
import com.aschlus.comicreadingcompanion.ui.theme.ComicRed
import com.aschlus.comicreadingcompanion.ui.theme.ComicRedDark
import com.aschlus.comicreadingcompanion.ui.theme.ComicRedLight
import com.aschlus.comicreadingcompanion.ui.theme.ComicSpacingLg
import com.aschlus.comicreadingcompanion.ui.theme.ComicSpacingMd
import com.aschlus.comicreadingcompanion.ui.theme.ComicSpacingXs
import com.aschlus.comicreadingcompanion.ui.viewmodel.ComicVineSettingsViewModel
import com.aschlus.comicreadingcompanion.ui.viewmodel.MetronSettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: ComicVineSettingsViewModel,
    metronViewModel: MetronSettingsViewModel? = null,
    onBackClick: () -> Unit,
    onSearchSeriesClick: () -> Unit = {},
    onExportBackupClick: () -> Unit = {},
    onValidateBackupClick: () -> Unit = {},
    onRestoreBackupClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    val isDarkTheme = MaterialTheme.colorScheme.background == ComicDarkBackground
    val statusSuccessColor = if (isDarkTheme) {
        ComicGreen
    } else {
        ComicGreenDark
    }

    val statusErrorColor = if (isDarkTheme) {
        ComicRedLight
    } else {
        ComicRedDark
    }

    var apiKeyInput by remember { mutableStateOf("") }
    var keyVisible by remember { mutableStateOf(false) }
    var showRemoveConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.message) {
        if (uiState.message == "Comic Vine API key validated and saved securely.") {
            apiKeyInput = ""
            keyVisible = false
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ComicPrimaryHeader {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ComicPrimaryHeaderBackButton(
                        onClick = onBackClick
                    )

                    Spacer(Modifier.width(ComicSpacingLg))

                    ComicPrimaryHeaderTitle(
                        text = "SETTINGS"
                    )
                }
            }
        }
    ) { innerPadding: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(ComicSpacingLg),
            verticalArrangement = Arrangement.spacedBy(ComicSpacingLg)
        ) {
            ComicHomeSectionHeader(
                text = "API KEY"
            )

            Text(
                text = "Configure the key used by series search and import features.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                uiState.hasSavedApiKey -> {
                    Text(
                        text = "API KEY CONFIGURED",
                        color = statusSuccessColor
                    )

                    Text(
                        text = "A key is saved securely on the device. " +
                            "For security, the saved key itself is never shown.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> {
                    Text(
                        text = "NO API KEY CONFIGURED",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            ComicWideActionButton(
                text = "SEARCH SERIES",
                backgroundColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                onClick = onSearchSeriesClick,
                enabled = !uiState.isLoading && !uiState.isSaving,
                modifier = Modifier.width(180.dp)
            )

            Text(
                text = if (uiState.hasSavedApiKey) {
                    "Enter a new key below to replace the saved one."
                } else {
                    "Enter your API key below."
                },
                color = MaterialTheme.colorScheme.onBackground
            )

            val fieldShape = RoundedCornerShape(9.dp)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clip(fieldShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = fieldShape
                    )
                    .padding(start = ComicSpacingMd, end = ComicSpacingXs),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = apiKeyInput,
                        onValueChange = {
                            apiKeyInput = it
                            viewModel.clearFeedback()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .semantics {
                                contentDescription = "API key"
                            },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge
                            .copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            autoCorrectEnabled = false
                        ),
                        visualTransformation = if (keyVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        decorationBox = { innerTextField ->
                            Box {
                                if (apiKeyInput.isEmpty()) {
                                    Text(
                                        text = "Enter API key",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                innerTextField()
                            }
                        }
                    )

                    IconButton(
                        onClick = { keyVisible = !keyVisible},
                        enabled = apiKeyInput.isNotEmpty(),
                        modifier = Modifier.semantics {
                            contentDescription = if (keyVisible) {
                                "Hide API key"
                            } else {
                                "Show API key"
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (keyVisible) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            ComicWideActionButton(
                text = when {
                    uiState.isSaving -> "CHECKING API KEY..."
                    uiState.hasSavedApiKey -> "REPLACE API KEY"
                    else ->"SAVE API KEY"
                },
                backgroundColor = ComicBlue,
                onClick = {
                    viewModel.saveApiKey(apiKeyInput)
                },
                enabled = !uiState.isLoading && !uiState.isSaving,
                modifier = Modifier.fillMaxWidth()
            )

            if (uiState.hasSavedApiKey) {
                ComicWideActionButton(
                    text = "REMOVE API KEY",
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    onClick = {
                        viewModel.clearFeedback()
                        showRemoveConfirmation = true
                    },
                    enabled = !uiState.isSaving,
                    modifier = Modifier.width(180.dp)
                )
            }

            if (uiState.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            uiState.message?.let { message ->
                Text(
                    text = message,
                    color = statusSuccessColor
                )
            }

            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = statusErrorColor
                )
            }

            metronViewModel?.let { releaseViewModel ->
                MetronSettingsSection(
                    viewModel = releaseViewModel
                )
            }

            ComicHomeSectionHeader(
                text = "DATA BACKUP"
            )

            Text(
                text = "Save a portable JSON backup of your comic catalog, " +
                    "reading lists, reading progress, and collection ownership. " +
                    "Your Comic Vine API key is not included.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ComicWideActionButton(
                text = "EXPORT FULL BACKUP",
                backgroundColor = ComicBlue,
                onClick = onExportBackupClick,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Check a saved backup before restoring it. " +
                    "Validation does not modify your catalog.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ComicWideActionButton(
                text = "VALIDATE BACKUP FILE",
                backgroundColor = ComicBlue,
                onClick = onValidateBackupClick,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Restore a saved backup by replacing your current " +
                    "catalog, reading lists, reading progress, and ownership. " +
                    "You'll be asked to confirm before anything is changed.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ComicWideActionButton(
                text = "RESTORE FROM BACKUP",
                backgroundColor = ComicRed,
                onClick = onRestoreBackupClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showRemoveConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showRemoveConfirmation = false
            },
            title = {
                Text("Remove API key?")
            },
            text = {
                Text(
                    "You'll need to enter your key again before " +
                        "using series search and import features."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveConfirmation = false
                        viewModel.clearApiKey()
                    }
                ) {
                    Text("Remove key")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRemoveConfirmation = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MetronSettingsSection(
    viewModel: MetronSettingsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    val uriHandler = LocalUriHandler.current
    val attribution = viewModel.sourceAttribution

    var tokenInput by remember { mutableStateOf("") }
    var tokenVisible by remember { mutableStateOf(false) }
    var showRemoveConfirmation by remember {
        mutableStateOf(false)
    }

    val isDarkTheme =
        MaterialTheme.colorScheme.background == ComicDarkBackground

    val successColor =
        if (isDarkTheme) ComicGreen else ComicGreenDark

    val errorColor =
        if (isDarkTheme) ComicRedLight else ComicRedDark

    LaunchedEffect(uiState.message) {
        if (
            uiState.message ==
            "Metron API token validated and saved securely."
        ) {
            tokenInput = ""
            tokenVisible = false
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(ComicSpacingMd)
    ) {
        ComicHomeSectionHeader(
            text = "RELEASE DATA"
        )

        Text(
            text = "Connect your own Metron account to retrieve " +
                    "scheduled comic releases and FOC dates. " +
                    "Create an API token in your Metron account's " +
                    "API Tokens section. The token is checked before " +
                    "saving and is never included in app backups.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp)
                )
            }

            uiState.hasSavedToken -> {
                Text(
                    text = "METRON TOKEN CONFIGURED",
                    color = successColor
                )

                Text(
                    text = "A token is saved securely on this device. " +
                            "For security, the saved token is never shown.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            else -> {
                Text(
                    text = "NO METRON TOKEN CONFIGURED",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        val fieldShape = RoundedCornerShape(9.dp)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(fieldShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = fieldShape
                )
                .padding(
                    start = ComicSpacingMd,
                    end = ComicSpacingXs
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = tokenInput,
                    onValueChange = {
                        tokenInput = it
                        viewModel.clearFeedback()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            contentDescription = "Metron API token"
                        },
                    singleLine = true,
                    enabled = !uiState.isLoading && !uiState.isSaving,
                    textStyle = MaterialTheme.typography.bodyLarge
                        .copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                    cursorBrush = SolidColor(
                        MaterialTheme.colorScheme.onSurface
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        autoCorrectEnabled = false
                    ),
                    visualTransformation =
                        if (tokenVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                    decorationBox = { innerTextField ->
                        Box {
                            if (tokenInput.isEmpty()) {
                                Text(
                                    text = "Paste Metron API token",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            innerTextField()
                        }
                    }
                )

                IconButton(
                    onClick = {
                        tokenVisible = !tokenVisible
                    },
                    enabled = tokenInput.isNotEmpty(),
                    modifier = Modifier.semantics {
                        contentDescription =
                            if (tokenVisible) {
                                "Hide Metron token"
                            } else {
                                "Show Metron token"
                            }
                    }
                ) {
                    Icon(
                        imageVector =
                            if (tokenVisible) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        ComicWideActionButton(
            text = when {
                uiState.isSaving -> "CHECKING METRON TOKEN..."
                uiState.hasSavedToken -> "REPLACE METRON TOKEN"
                else -> "SAVE METRON TOKEN"
            },
            backgroundColor = ComicBlue,
            onClick = {
                viewModel.saveToken(tokenInput)
            },
            enabled = !uiState.isLoading && !uiState.isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        if (uiState.hasSavedToken) {
            ComicWideActionButton(
                text = if (uiState.isCheckingReleases) {
                    "FETCHING RELEASES..."
                } else {
                    "PREVIEW NEXT 14 DAYS"
                },
                backgroundColor = ComicBlue,
                onClick = {
                    viewModel.previewUpcomingReleases()
                },
                enabled = !uiState.isLoading &&
                    !uiState.isSaving &&
                    !uiState.isCheckingReleases,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (uiState.hasSavedToken) {
            ComicWideActionButton(
                text = "REMOVE METRON TOKEN",
                backgroundColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                onClick = {
                    viewModel.clearFeedback()
                    showRemoveConfirmation = true
                },
                enabled = !uiState.isSaving,
                modifier = Modifier.width(220.dp)
            )
        }

        if (uiState.isSaving) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp)
            )
        }

        uiState.message?.let { message ->
            Text(
                text = message,
                color = successColor
            )
        }

        uiState.error?.let { error ->
            Text(
                text = error,
                color = errorColor
            )
        }

        if (uiState.isCheckingReleases) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp)
            )
        }

        uiState.releasePreviewError?.let { error ->
            Text(
                text = error,
                color = errorColor
            )
        }

        uiState.releasePreviewCount?.let { count ->
            Text(
                text = "Found $count scheduled issues.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            uiState.releasePreviewRange?.let { range ->
                Text(
                    text = "Store-date range: $range",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = if (count == 0) {
                    "No scheduled releases were returned for this period."
                } else {
                    "Showing ${uiState.releasePreview.size} sample " +
                        "release(s), ordered by store date."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            uiState.releasePreview.forEach { release ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "${release.seriesTitle} " +
                            "#${release.issueNumber ?: "?"}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Store date: " +
                                (release.storeReleaseDate ?: "Not provided") +
                                " · Cover date: " +
                                (release.coverDate ?: "Not provided"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Metron ID: ${release.externalIssueId}" +
                                " · Comic Vine ID: " +
                                (release.comicVineId ?: "Not linked"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    TextButton(
                        onClick = {
                            viewModel.inspectReleaseDetails(
                                release.externalIssueId
                            )
                        },
                        enabled = !uiState.isLoadingReleaseDetail
                    ) {
                        Text(
                            text = if (
                                uiState.isLoadingReleaseDetail &&
                                uiState.releaseDetailRequestId == release.externalIssueId
                            ) {
                                "LOADING DETAILS..."
                            } else {
                                "LOAD FULL DETAILS"
                            }
                        )
                    }

                    val inspected = uiState.inspectedReleaseDetail

                    if (
                        inspected != null &&
                        inspected.externalIssueId == release.externalIssueId
                    ) {
                        Text(
                            text = "FOC: ${inspected.focDate ?: "Not provided"}" +
                                " · Price: ${inspected.price ?: "Not provided"}" +
                                " ${inspected.priceCurrency.orEmpty()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "SKU: ${inspected.distributorSku ?: "Not provided"}" +
                                " · UPC: ${inspected.upc ?: "Not provided"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "Comic Vine ID: ${inspected.comicVineId ?: "Not linked"}" +
                                " · GCD ID: ${inspected.gcdId ?: "Not linked"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (
                        uiState.releaseDetailRequestId == release.externalIssueId &&
                        uiState.releaseDetailError != null
                    ) {
                        Text(
                            text = uiState.releaseDetailError!!,
                            color = errorColor
                        )
                    }
                }
            }
        }

        ComicHomeSectionHeader(
            text = "DATA ATTRIBUTION"
        )

        Text(
            text = "Comic release data from ${attribution.providerName}, " +
                "licensed under ${attribution.licenseName}.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = attribution.changesNotice,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        attribution.additionalNotice?.let { notice ->
            Text(
                text = notice,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(ComicSpacingMd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = {
                    uriHandler.openUri(attribution.sourceUrl)
                }
            ) {
                Text("${attribution.providerName.uppercase()} SOURCE")
            }

            TextButton(
                onClick = {
                    uriHandler.openUri(attribution.licenseUrl)
                }
            ) {
                Text("CC BY-SA 4.0 LICENSE")
            }
        }
    }

    if (showRemoveConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showRemoveConfirmation = false
            },
            title = {
                Text("Remove Metron token?")
            },
            text = {
                Text(
                    "The saved token will be removed from this device. " +
                    "You can enter a new token later."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveConfirmation = false
                        viewModel.clearToken()
                    }
                ) {
                    Text("Remove token")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRemoveConfirmation = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}