package com.music.vivi.update.settingstyle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.music.vivi.R

// fun ModernInfoItem(
//    icon: @Composable () -> Unit,
//    title: String,
//    subtitle: String,
//    onClick: (() -> Unit)? = null,
//    showArrow: Boolean = false,
//    showSettingsIcon: Boolean = false,
//    iconBackgroundColor: Color? = null,
//    titleColor: Color? = null,
//    subtitleColor: Color? = null,
//    arrowColor: Color? = null,
//    settingsIconColor: Color? = null,
//    trailingContent: (@Composable () -> Unit)? = null
// ) {
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .then(
//                if (onClick != null) {
//                    Modifier.clickable(onClick = onClick)
//                } else Modifier
//            )
//            .padding(horizontal = 20.dp, vertical = 14.dp),
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        Box(
//            modifier = Modifier
//                .size(40.dp)
//                .background(
//                    iconBackgroundColor ?: MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
//                    CircleShape
//                ),
//            contentAlignment = Alignment.Center
//        ) {
//            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
//                icon()
//            }
//        }
//
//        Spacer(modifier = Modifier.width(16.dp))
//
//        Column(modifier = Modifier.weight(1f)) {
//            Text(
//                text = title,
//                style = MaterialTheme.typography.bodyLarge.copy(
//                    fontWeight = FontWeight.Medium
//                ),
//                color = titleColor ?: MaterialTheme.colorScheme.onSurface
//            )
//            Text(
//                text = subtitle,
//                style = MaterialTheme.typography.bodyMedium,
//                color = subtitleColor ?: MaterialTheme.colorScheme.onSurfaceVariant
//            )
//        }
//
//        if (trailingContent != null) {
//            trailingContent()
//        } else {
//            Row(
//                horizontalArrangement = Arrangement.spacedBy(8.dp),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
// //                if (showArrow) {
// //                    Icon(
// //                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
// //                        contentDescription = null,
// //                        modifier = Modifier.size(18.dp), //16
// //                        tint = arrowColor ?: if (showSettingsIcon) {
// //                            MaterialTheme.colorScheme.primary
// //                        } else {
// //                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
// //                        }
// //                    )
// //                }
// //
// //                if (showSettingsIcon) {
// //                    Icon(
// //                        painter = painterResource(R.drawable.settings),
// //                        contentDescription = null,
// //                        modifier = Modifier.size(24.dp), //20
// //                        tint = settingsIconColor ?: MaterialTheme.colorScheme.primary
// //                    )
// //                }
//
//                if (showArrow) {
//                    Icon(
//                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
//                        contentDescription = null,
//                        modifier = Modifier.size(18.dp),
//                        tint = arrowColor ?: MaterialTheme.colorScheme.primary
//                    )
//                }
//
//                if (showSettingsIcon) {
//                    Icon(
//                        painter = painterResource(R.drawable.settings),
//                        contentDescription = null,
//                        modifier = Modifier.size(24.dp),
//                        tint = settingsIconColor ?: MaterialTheme.colorScheme.primary
//                    )
//                }
//            }
//        }
//    }
// }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ModernInfoItem(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    showArrow: Boolean = false,
    showSettingsIcon: Boolean = false,
    iconBackgroundColor: Color? = null,
    titleColor: Color? = null,
    subtitleColor: Color? = null,
    arrowColor: Color? = null,
    settingsIconColor: Color? = null,
    iconContentColor: Color? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    iconShape: Shape = MaterialShapes.Ghostish.toShape(),
    iconSize: androidx.compose.ui.unit.Dp = 44.dp,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .background(
                    iconBackgroundColor ?: MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    iconShape
                )
                .then(if (iconShape != CircleShape) Modifier.clip(iconShape) else Modifier.clip(CircleShape)),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(
                LocalContentColor provides (iconContentColor ?: MaterialTheme.colorScheme.primary)
            ) {
                icon()
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = titleColor ?: MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = subtitleColor ?: MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (trailingContent != null) {
            trailingContent()
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showArrow) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = arrowColor ?: MaterialTheme.colorScheme.primary
                    )
                }

                // Add vertical divider when both icons are shown
                if (showArrow && showSettingsIcon) {
                    VerticalDivider(
                        modifier = Modifier.height(25.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)

                    )
                }

                if (showSettingsIcon) {
                    Icon(
                        painter = painterResource(R.drawable.settings),
                        contentDescription = null,
                        modifier = Modifier.size(25.dp),
                        tint = settingsIconColor ?: MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
