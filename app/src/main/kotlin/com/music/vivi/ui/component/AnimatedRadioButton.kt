/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedRadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 1. Squash-and-stretch button scaling animation based on press state
    val buttonScale by animateFloatAsState(
        targetValue = if (enabled && isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "buttonScale"
    )

    // 2. Bouncy overshoot spring-scale transition for the inner dot
    val dotScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.55f, // Elastic overshoot
            stiffness = Spring.StiffnessLow
        ),
        label = "dotScale"
    )

    // 3. Smooth color transitions for selection state
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.onSurfaceVariant
    
    val borderColor by animateColorAsState(
        targetValue = if (selected) primaryColor else outlineColor,
        animationSpec = tween(durationMillis = 200),
        label = "borderColor"
    )
    
    val dotColor by animateColorAsState(
        targetValue = if (selected) primaryColor else Color.Transparent,
        animationSpec = tween(durationMillis = 150),
        label = "dotColor"
    )

    // Layout containing clickable bounds and canvas drawing
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(24.dp)
            .scale(buttonScale) // Apply the press squash-and-stretch scale
            .semantics { role = Role.RadioButton }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = false,
                    radius = 20.dp
                ),
                enabled = enabled && onClick != null,
                onClick = onClick ?: {}
            )
            .alpha(if (enabled) 1f else 0.38f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(20.dp)) {
            val outerRadius = size.minDimension / 2
            val strokeWidth = 2.dp.toPx()
            
            // Draw outer border circle
            drawCircle(
                color = borderColor,
                radius = outerRadius - strokeWidth / 2,
                style = Stroke(width = strokeWidth)
            )
            
            // Draw inner selected dot circle
            if (dotScale > 0f) {
                // Inner dot is 10dp diameter (5dp radius) when outer is 20dp diameter
                val dotRadius = (outerRadius / 2) * dotScale
                drawCircle(
                    color = dotColor,
                    radius = dotRadius
                )
            }
        }
    }
}
