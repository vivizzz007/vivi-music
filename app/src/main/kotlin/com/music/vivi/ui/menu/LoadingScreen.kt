package com.music.vivi.ui.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.music.vivi.R

/**
 * A simple loading screen overlay showing a percentage value.
 * Used during long running operations like song downloading or importing.
 */
@Composable
fun LoadingScreen(isVisible: Boolean, value: Int) {
    if (isVisible) {
        Dialog(
            onDismissRequest = {}
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.progress_percentage, value.toString()),
                    color = Color.White,
                    fontSize = 26.sp
                )
            }
        }
    }
}
