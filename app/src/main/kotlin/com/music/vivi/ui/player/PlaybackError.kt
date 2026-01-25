package com.music.vivi.ui.player

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.PlaybackException
import com.music.vivi.R

@Composable
fun PlaybackError(error: PlaybackException, retry: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
        Modifier.pointerInput(Unit) {
            detectTapGestures(
                onTap = { retry() }
            )
        }
    ) {
        Icon(
            painter = painterResource(R.drawable.info),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error
        )

        var currentError: Throwable? = error
        var errorMessage: String? = null
        while (currentError != null) {
            currentError.message?.takeIf { it.isNotBlank() }?.let { errorMessage = it }
            currentError = currentError.cause
        }

        Text(
            text = errorMessage ?: stringResource(R.string.error_unknown),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
