package uz.nodirbek.flashcardsapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import uz.nodirbek.flashcardsapp.ui.theme.OutfitFamily

// ── Standard App Bar Height (UNIFIED) ──────────────────────────────────
const val APP_BAR_HEIGHT_DP = 54

// ── Dynamic Color Helper ───────────────────────────────────────────────
@Composable
private fun getAppBarColors(): Pair<Color, Color> {
    return MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.onSurface
}

@Composable
private fun getDividerColor(): Color {
    return MaterialTheme.colorScheme.outline
}

// ── Main App Bar Component ────────────────────────────────────────────
@Composable
fun UnifiedAppBar(
    title: String,
    onBackClick: (() -> Unit)? = null,
    showBackButton: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    showDivider: Boolean = true,
    showProgressBar: Boolean = false,
    progressValue: Float = 0f
) {
    val (bgColor, textColor) = getAppBarColors()
    val dividerColor = getDividerColor()

    Surface(
        color = bgColor,
        shadowElevation = 0.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(APP_BAR_HEIGHT_DP.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back/Close Button
                if (showBackButton && onBackClick != null) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textColor
                        )
                    }
                } else if (showBackButton) {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                // Title
                Text(
                    title,
                    fontFamily = OutfitFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = textColor,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )

                // Actions
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(APP_BAR_HEIGHT_DP.dp)
                ) {
                    actions()
                }
            }

            // Divider
            if (showDivider) {
                HorizontalDivider(
                    color = dividerColor,
                    thickness = 1.5.dp
                )
            }

            // Progress Bar (optional)
            if (showProgressBar) {
                LinearProgressIndicator(
                    progress = { progressValue },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = dividerColor
                )
            }
        }
    }
}

// ── Simple Back Bar (for modals/sheets) ─────────────────────────────────
@Composable
fun SimpleAppBar(
    title: String,
    onBackClick: () -> Unit,
    showDivider: Boolean = true
) {
    UnifiedAppBar(
        title = title,
        onBackClick = onBackClick,
        showBackButton = true,
        showDivider = showDivider
    )
}

// ── Close Button Bar (for full-screen sheets) ───────────────────────────
@Composable
fun CloseableAppBar(
    title: String,
    onCloseClick: () -> Unit,
    showDivider: Boolean = true
) {
    val (bgColor, textColor) = getAppBarColors()
    val dividerColor = getDividerColor()

    Surface(
        color = bgColor,
        shadowElevation = 0.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(APP_BAR_HEIGHT_DP.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Close Button
                IconButton(onClick = onCloseClick) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = textColor
                    )
                }

                // Title
                Text(
                    title,
                    fontFamily = OutfitFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = textColor,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )
            }

            // Divider
            if (showDivider) {
                HorizontalDivider(
                    color = dividerColor,
                    thickness = 1.5.dp
                )
            }
        }
    }
}

// ── Progress App Bar ────────────────────────────────────────────────────
@Composable
fun ProgressAppBar(
    title: String,
    progress: Float,
    onBackClick: () -> Unit,
    currentIndex: Int = 0,
    total: Int = 0
) {
    val (bgColor, textColor) = getAppBarColors()

    Surface(
        color = bgColor,
        shadowElevation = 0.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(APP_BAR_HEIGHT_DP.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = textColor
                    )
                }

                // Progress indicator
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    repeat(total) { idx ->
                        Box(
                            Modifier
                                .weight(1f)
                                .height(6.dp)
                                .background(
                                    when {
                                        idx < currentIndex -> MaterialTheme.colorScheme.primary
                                        idx == currentIndex -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                        else -> getDividerColor()
                                    }
                                )
                        )
                    }
                }

                if (total > 0) {
                    Text(
                        "$currentIndex/$total",
                        fontFamily = OutfitFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            HorizontalDivider(
                color = getDividerColor(),
                thickness = 1.5.dp
            )
        }
    }
}
