package com.music.vivi.ui.utils

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.unit.dp

/**
 * Modifies a CornerBasedShape to only have rounded corners at the top.
 */
fun CornerBasedShape.top(): CornerBasedShape = copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp))
