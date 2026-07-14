package com.music.vivi.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.content.ContextCompat
import com.music.vivi.vivimusic.updater.extractUrls
@Composable
fun AnimatedActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isOutlined: Boolean = false,
    enabled: Boolean = true,
    buttonHeight: androidx.compose.ui.unit.Dp = 56.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cornerPercent by animateIntAsState(
        targetValue = if (isPressed) 15 else 50,
        animationSpec = tween(durationMillis = 200),
        label = "btnMorph"
    )

    if (isOutlined) {
        androidx.compose.material3.OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(buttonHeight),
            shape = RoundedCornerShape(cornerPercent),
            enabled = enabled,
            interactionSource = interactionSource,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }
    } else {
        Button(
            onClick = onClick,
            modifier = modifier.height(buttonHeight),
            shape = RoundedCornerShape(cornerPercent),
            enabled = enabled,
            interactionSource = interactionSource,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun ExpressiveIconButton(
    onClick: () -> Unit,
    painter: androidx.compose.ui.graphics.painter.Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = Color.Transparent,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cornerPercent by animateIntAsState(
        targetValue = if (isPressed) 20 else 50,
        animationSpec = tween(durationMillis = 200),
        label = "corner"
    )

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(44.dp),
        shape = RoundedCornerShape(cornerPercent),
        color = containerColor,
        contentColor = contentColor,
        interactionSource = interactionSource
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painter,
                contentDescription = contentDescription,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun ErrorSnackbar(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier
    ) { data ->
        Snackbar(
            snackbarData = data,
            containerColor = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            actionColor = MaterialTheme.colorScheme.inversePrimary,
            actionContentColor = MaterialTheme.colorScheme.inversePrimary,
            dismissActionContentColor = MaterialTheme.colorScheme.inverseOnSurface
        )
    }
}

// Shape utilities for grouped list items
private const val ConnectedCornerRadius = 4
private const val EndCornerRadius = 16

fun leadingItemShape(): RoundedCornerShape = RoundedCornerShape(
    topStart = EndCornerRadius.dp,
    topEnd = EndCornerRadius.dp,
    bottomStart = ConnectedCornerRadius.dp,
    bottomEnd = ConnectedCornerRadius.dp
)

fun middleItemShape(): RoundedCornerShape = RoundedCornerShape(ConnectedCornerRadius.dp)

fun endItemShape(): RoundedCornerShape = RoundedCornerShape(
    topStart = ConnectedCornerRadius.dp,
    topEnd = ConnectedCornerRadius.dp,
    bottomStart = EndCornerRadius.dp,
    bottomEnd = EndCornerRadius.dp
)

fun detachedItemShape(): RoundedCornerShape = RoundedCornerShape(EndCornerRadius.dp)

@Composable
fun ChangelogItem(
    text: String,
    shape: androidx.compose.ui.graphics.Shape,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        val context = LocalContext.current
        val urls = text.extractUrls()
        val annotatedText = buildAnnotatedString {
            append(text.trim())
            urls.forEach { (range, url) ->
                addStringAnnotation("URL", url, range.first, range.last + 1)
                addStyle(
                    SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    ),
                    range.first,
                    range.last + 1
                )
            }
        }

        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(16.dp))
            ClickableText(
                text = annotatedText,
                onClick = { offset ->
                    annotatedText.getStringAnnotations("URL", offset, offset).firstOrNull()?.let {
                        ContextCompat.startActivity(context, Intent(Intent.ACTION_VIEW, Uri.parse(it.item)), null)
                    }
                },
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
            )
        }
    }
}

@Composable
fun UpdaterBlobCluster(
    modifier: Modifier = Modifier,
    rotate: Boolean = true,
    isError: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "blobClusterRotation")

    val rotation1State = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "r1"
    )

    val rotation2State = infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "r2"
    )

    val rotation3State = infiniteTransition.animateFloat(
        initialValue = 180f,
        targetValue = 540f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "r3"
    )

    // Smoothly follow the rotation state when rotating, and spring back to static starting angle when stopped!
    val rotation1 by animateFloatAsState(
        targetValue = if (rotate) rotation1State.value else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "smoothR1"
    )
    val rotation2 by animateFloatAsState(
        targetValue = if (rotate) rotation2State.value else 45f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "smoothR2"
    )
    val rotation3 by animateFloatAsState(
        targetValue = if (rotate) rotation3State.value else 90f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "smoothR3"
    )

    val primaryColorTarget = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
    val secondaryColorTarget = if (isError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.secondaryContainer
    val tertiaryColorTarget = if (isError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.tertiaryContainer

    val primaryContainer by animateColorAsState(
        targetValue = primaryColorTarget,
        animationSpec = tween(500),
        label = "c1"
    )
    val secondaryContainer by animateColorAsState(
        targetValue = secondaryColorTarget,
        animationSpec = tween(500),
        label = "c2"
    )
    val tertiaryContainer by animateColorAsState(
        targetValue = tertiaryColorTarget,
        animationSpec = tween(500),
        label = "c3"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Blob 1 (large)
        Icon(
            painter = painterResource(id = com.music.vivi.R.drawable.ic_ten_sided_cookie),
            contentDescription = null,
            tint = primaryContainer,
            modifier = Modifier
                .size(140.dp)
                .rotate(rotation1)
        )
        // Blob 2 (medium)
        Icon(
            painter = painterResource(id = com.music.vivi.R.drawable.ic_ten_sided_cookie),
            contentDescription = null,
            tint = secondaryContainer,
            modifier = Modifier
                .size(100.dp)
                .rotate(rotation2)
                .align(Alignment.TopStart)
                .padding(top = 16.dp, start = 16.dp)
        )
        // Blob 3 (small)
        Icon(
            painter = painterResource(id = com.music.vivi.R.drawable.ic_ten_sided_cookie),
            contentDescription = null,
            tint = tertiaryContainer,
            modifier = Modifier
                .size(80.dp)
                .rotate(rotation3)
                .align(Alignment.BottomEnd)
                .padding(bottom = 8.dp, end = 8.dp)
        )
    }
}
