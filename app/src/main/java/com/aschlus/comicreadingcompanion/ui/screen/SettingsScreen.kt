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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
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
import com.aschlus.comicreadingcompanion.ui.theme.ComicGreen
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicMutedInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper
import com.aschlus.comicreadingcompanion.ui.theme.ComicRed
import com.aschlus.comicreadingcompanion.ui.viewmodel.ComicVineSettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: ComicVineSettingsViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

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
        containerColor = ComicPaper,
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

                    Spacer(Modifier.width(16.dp))

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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ComicHomeSectionHeader(
                text = "COMIC VINE API KEY"
            )

            Text(
                text = "Configure the key used by the upcoming " +
                    "Comic Vine series-import feature.",
                color = ComicMutedInk
            )

            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = ComicInk
                    )
                }

                uiState.hasSavedApiKey -> {
                    Text(
                        text = "API KEY CONFIGURED",
                        color = ComicGreen
                    )

                    Text(
                        text = "A key is saved securely on the device. " +
                            "For security, the saved key itself is never shown.",
                        color = ComicMutedInk
                    )
                }

                else -> {
                    Text(
                        text = "NO API KEY CONFIGURED",
                        color = ComicMutedInk
                    )
                }
            }

            Text(
                text = if (uiState.hasSavedApiKey) {
                    "Enter a new key below to replace the saved one."
                } else {
                    "Enter your Comic Vine API key below."
                },
                color = ComicInk
            )

            val fieldShape = RoundedCornerShape(9.dp)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clip(fieldShape)
                    .background(Color.White)
                    .border(
                        width = 2.dp,
                        color = ComicInk,
                        shape = fieldShape
                    )
                    .padding(start = 12.dp, end = 4.dp),
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
                                contentDescription = "Comic Vine API key"
                            },
                        singleLine = true,
                        textStyle = androidx.compose.material3.MaterialTheme
                            .typography.bodyLarge
                            .copy(color = ComicInk),
                        cursorBrush = SolidColor(ComicInk),
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
                                        color = ComicMutedInk
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
                            tint = ComicInk
                        )
                    }
                }
            }

            Button(
                onClick = {
                    viewModel.saveApiKey(apiKeyInput)
                },
                enabled = !uiState.isLoading && !uiState.isSaving
            ) {
                Text(
                    text = if (uiState.isSaving) {
                        "CHECKING API KEY..."
                    } else if (uiState.hasSavedApiKey) {
                        "REPLACE API KEY"
                    } else {
                        "SAVE API KEY"
                    }
                )
            }

            if (uiState.hasSavedApiKey) {
                TextButton(
                    onClick = {
                        viewModel.clearFeedback()
                        showRemoveConfirmation = true
                    },
                    enabled = !uiState.isSaving
                ) {
                    Text("REMOVE API KEY")
                }
            }

            if (uiState.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = ComicInk
                )
            }

            uiState.message?.let { message ->
                Text(
                    text = message,
                    color = ComicGreen
                )
            }

            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = ComicRed
                )
            }
        }
    }

    if (showRemoveConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showRemoveConfirmation = false
            },
            title = {
                Text("Remove Comic Vine API key?")
            },
            text = {
                Text(
                    "You'll need to enter your key again before " +
                        "using Comic Vine features."
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