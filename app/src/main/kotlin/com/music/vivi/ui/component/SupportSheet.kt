/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.vivi.GoogleSansFlex
import com.music.vivi.R
import com.music.vivi.ui.utils.safeOpenUri

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun SupportSheet(
    onDismiss: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.support_vivi),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Support ViviMusic",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                    fontFamily = GoogleSansFlex
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Start
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "ViviMusic is built with passion and shared for free. If you love the experience, consider supporting the developer to help keep the project alive and growing with new features.",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = GoogleSansFlex,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Donation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // UPI Button
            SupportExpressiveButton(
                text = "UPI (India)",
                icon = R.drawable.currency_rupee_upi,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.weight(1f),
                onClick = {
                    uriHandler.safeOpenUri(context, "upi://pay?pa=vividhpashokan@axl&pn=Vividh P Ashokan")
                    onDismiss()
                }
            )

            // Ko-fi Button
            SupportExpressiveButton(
                text = "Coffee",
                icon = R.drawable.buymeacoffee,
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
                modifier = Modifier.weight(1f),
                onClick = {
                    uriHandler.safeOpenUri(context, "https://ko-fi.com/vividhpashokan")
                    onDismiss()
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // GitHub Star Button
        SupportExpressiveButton(
            text = "Star on GitHub",
            icon = R.drawable.github,
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                uriHandler.safeOpenUri(context, "https://github.com/vividhpashokan/ViviMusic")
                onDismiss()
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Maybe Later Button
        SupportExpressiveButton(
            text = "Maybe Later",
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            onClick = onDismiss
        )
    }
}

@Composable
fun SupportExpressiveButton(
    text: String,
    icon: Int? = null,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    isOutlined: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        label = "buttonScale"
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .height(64.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        color = if (isOutlined) Color.Transparent else containerColor,
        contentColor = contentColor,
        border = if (isOutlined) androidx.compose.foundation.BorderStroke(2.dp, containerColor.copy(alpha = 0.5f)) else null,
        shape = CircleShape,
        interactionSource = interactionSource,
        tonalElevation = if (isOutlined) 0.dp else 4.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = GoogleSansFlex,
                    fontSize = 16.sp
                ),
                maxLines = 1
            )
        }
    }
}
