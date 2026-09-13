package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicYellow

private enum class ComicNavDestination {
    HOME,
    LIBRARY,
    BROWSE
}

private val ComicNavPaper =
    Color(0xFFFFFCF5)

private val ComicNavMuted =
    Color(0xFF686868)

private val SelectedNavShape =
    GenericShape { size, _ ->
        moveTo(
            x = size.width * 0.10f,
            y = 0f
        )

        lineTo(
            x = size.width * 0.90f,
            y = 0f
        )

        lineTo(
            x = size.width,
            y = size.height
        )

        lineTo(
            x = 0f,
            y = size.height
        )

        close()
    }

@Composable
fun ComicBottomNavigation(
    currentRoute: String?,
    onHomeClick: () -> Unit,
    onBrowseClick: () -> Unit,
    onLibraryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    color = ComicNavPaper
                )
                .drawBehind {
                    val strokeWidth =
                        3.dp.toPx()

                    drawLine(
                        color = ComicInk,
                        start =
                            Offset(
                                x = 0f,
                                y =
                                    strokeWidth /
                                            2f
                            ),
                        end =
                            Offset(
                                x = size.width,
                                y =
                                    strokeWidth /
                                            2f
                            ),
                        strokeWidth =
                            strokeWidth
                    )
                }
                .navigationBarsPadding()
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(72.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            ComicBottomNavigationItem(
                label = "Home",
                destination =
                    ComicNavDestination.HOME,
                selected =
                    currentRoute == "home",
                contentDescription =
                    "Home tab",
                onClick = onHomeClick,
                modifier =
                    Modifier.weight(1f)
            )

            ComicBottomNavigationItem(
                label = "Library",
                destination =
                    ComicNavDestination.LIBRARY,
                selected =
                    currentRoute == "library",
                contentDescription =
                    "Library tab",
                onClick = onLibraryClick,
                modifier =
                    Modifier.weight(1f)
            )

            ComicBottomNavigationItem(
                label = "Browse",
                destination =
                    ComicNavDestination.BROWSE,
                selected =
                    currentRoute == "browse",
                contentDescription =
                    "Browse tab",
                onClick = onBrowseClick,
                modifier =
                    Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ComicBottomNavigationItem(
    label: String,
    destination: ComicNavDestination,
    selected: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier =
            modifier
                .fillMaxHeight()
                .semantics {
                    this.contentDescription =
                        contentDescription
                }
                .clickable(
                    onClick = onClick
                ),
        contentAlignment =
            Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .requiredHeight(90.dp)
                        .offset(
                            y = (-8).dp
                        )
                        .zIndex(1f)
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                color = ComicYellow,
                                shape =
                                    SelectedNavShape
                            )
                            .border(
                                width = 3.dp,
                                color = ComicInk,
                                shape =
                                    SelectedNavShape
                            )
                            .padding(
                                top = 10.dp,
                                bottom = 8.dp
                            ),
                    horizontalAlignment =
                        Alignment.CenterHorizontally,
                    verticalArrangement =
                        Arrangement.spacedBy(
                            space = 4.dp,
                            alignment =
                                Alignment.CenterVertically
                        )
                ) {
                    ComicNavigationIcon(
                        destination =
                            destination,
                        tint = ComicInk,
                        modifier =
                            Modifier.size(42.dp)
                    )

                    Text(
                        text = label,
                        style =
                            MaterialTheme
                                .typography
                                .labelLarge
                                .copy(
                                    fontSize = 17.sp,
                                    lineHeight = 19.sp
                                ),
                        fontWeight =
                            FontWeight.Black,
                        color = ComicInk
                    )
                }
            }
        } else {
            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally,
                verticalArrangement =
                    Arrangement.spacedBy(
                        5.dp
                    )
            ) {
                ComicNavigationIcon(
                    destination =
                        destination,
                    tint =
                        ComicNavMuted,
                    modifier =
                        Modifier.size(40.dp)
                )

                Text(
                    text = label,
                    style =
                        MaterialTheme
                            .typography
                            .labelMedium
                            .copy(
                                fontSize = 15.sp,
                                lineHeight = 18.sp
                            ),
                    fontWeight =
                        FontWeight.Bold,
                    color =
                        ComicNavMuted
                )
            }
        }
    }
}

@Composable
private fun ComicNavigationIcon(
    destination: ComicNavDestination,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
    ) {
        when (destination) {
            ComicNavDestination.HOME -> {
                val path =
                    Path().apply {
                        moveTo(
                            x =
                                size.width *
                                        0.10f,
                            y =
                                size.height *
                                        0.47f
                        )

                        lineTo(
                            x =
                                size.width *
                                        0.50f,
                            y =
                                size.height *
                                        0.13f
                        )

                        lineTo(
                            x =
                                size.width *
                                        0.90f,
                            y =
                                size.height *
                                        0.47f
                        )

                        lineTo(
                            x =
                                size.width *
                                        0.77f,
                            y =
                                size.height *
                                        0.47f
                        )

                        lineTo(
                            x =
                                size.width *
                                        0.77f,
                            y =
                                size.height *
                                        0.84f
                        )

                        lineTo(
                            x =
                                size.width *
                                        0.58f,
                            y =
                                size.height *
                                        0.84f
                        )

                        lineTo(
                            x =
                                size.width *
                                        0.58f,
                            y =
                                size.height *
                                        0.61f
                        )

                        lineTo(
                            x =
                                size.width *
                                        0.42f,
                            y =
                                size.height *
                                        0.61f
                        )

                        lineTo(
                            x =
                                size.width *
                                        0.42f,
                            y =
                                size.height *
                                        0.84f
                        )

                        lineTo(
                            x =
                                size.width *
                                        0.23f,
                            y =
                                size.height *
                                        0.84f
                        )

                        lineTo(
                            x =
                                size.width *
                                        0.23f,
                            y =
                                size.height *
                                        0.47f
                        )

                        close()
                    }

                drawPath(
                    path = path,
                    color = tint
                )
            }

            ComicNavDestination.LIBRARY -> {
                drawRect(
                    color = tint,
                    topLeft =
                        Offset(
                            x =
                                size.width *
                                        0.18f,
                            y =
                                size.height *
                                        0.22f
                        ),
                    size =
                        Size(
                            width =
                                size.width *
                                        0.18f,
                            height =
                                size.height *
                                        0.60f
                        )
                )

                drawRect(
                    color = tint,
                    topLeft =
                        Offset(
                            x =
                                size.width *
                                        0.42f,
                            y =
                                size.height *
                                        0.16f
                        ),
                    size =
                        Size(
                            width =
                                size.width *
                                        0.18f,
                            height =
                                size.height *
                                        0.66f
                        )
                )

                rotate(
                    degrees = -10f,
                    pivot =
                        Offset(
                            x =
                                size.width *
                                        0.73f,
                            y =
                                size.height *
                                        0.52f
                        )
                ) {
                    drawRect(
                        color = tint,
                        topLeft =
                            Offset(
                                x =
                                    size.width *
                                            0.66f,
                                y =
                                    size.height *
                                            0.22f
                            ),
                        size =
                            Size(
                                width =
                                    size.width *
                                            0.17f,
                                height =
                                    size.height *
                                            0.60f
                            )
                    )
                }
            }

            ComicNavDestination.BROWSE -> {
                val strokeWidth =
                    3.6.dp.toPx()

                drawCircle(
                    color = tint,
                    radius =
                        size.minDimension *
                                0.27f,
                    center =
                        Offset(
                            x =
                                size.width *
                                        0.40f,
                            y =
                                size.height *
                                        0.40f
                        ),
                    style =
                        Stroke(
                            width =
                                strokeWidth
                        )
                )

                drawLine(
                    color = tint,
                    start =
                        Offset(
                            x =
                                size.width *
                                        0.60f,
                            y =
                                size.height *
                                        0.60f
                        ),
                    end =
                        Offset(
                            x =
                                size.width *
                                        0.86f,
                            y =
                                size.height *
                                        0.86f
                        ),
                    strokeWidth =
                        strokeWidth,
                    cap =
                        StrokeCap.Round
                )
            }
        }
    }
}