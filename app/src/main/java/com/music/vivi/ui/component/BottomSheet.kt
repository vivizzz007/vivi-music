package com.music.vivi.ui.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.music.vivi.constants.NavigationBarAnimationSpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Bottom Sheet
 * Modified from [ViMusic](https://github.com/vfsfitvnm/ViMusic)
 */
@Composable
fun BottomSheet(
    state: BottomSheetState,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    collapsedBackgroundColor: Color = Color.Transparent,
    onDismiss: (() -> Unit)? = null,
    collapsedContent: @Composable BoxScope.() -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .offset {
                val y = (state.expandedBound - state.value)
                    .roundToPx()
                    .coerceAtLeast(0)
                IntOffset(x = 0, y = y)
            }
            .pointerInput(state) {
                val velocityTracker = VelocityTracker()

                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        velocityTracker.addPointerInputChange(change)
                        state.dispatchRawDelta(dragAmount)
                    },
                    onDragCancel = {
                        velocityTracker.resetTracking()
                        state.snapTo(state.collapsedBound)
                    },
                    onDragEnd = {
                        val velocity = -velocityTracker.calculateVelocity().y
                        velocityTracker.resetTracking()
                        state.performFling(velocity, onDismiss)
                    }
                )
            }
            .clip(
                RoundedCornerShape(
                    topStart = if (!state.isExpanded) 16.dp else 0.dp,
                    topEnd = if (!state.isExpanded) 16.dp else 0.dp
                )
            )
    ) {
        if (!state.isCollapsed && !state.isDismissed) {
            BackHandler(onBack = state::collapseSoft)
        }

        if (!state.isCollapsed) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = ((state.progress - 0.25f) * 4).coerceIn(0f, 1f)
                    }
                    .background(backgroundColor),
                content = content
            )
        }

        if (!state.isExpanded && (onDismiss == null || !state.isDismissed)) {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = 1f - (state.progress * 4).coerceAtMost(1f)
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = state::expandSoft
                    )
                    .fillMaxWidth()
                    .height(state.collapsedBound)
                    .background(collapsedBackgroundColor),
                content = collapsedContent
            )
        }
    }
}

@Stable
class BottomSheetState(
    draggableState: DraggableState,
    private val coroutineScope: CoroutineScope,
    private val animation: Animatable<Dp, AnimationVector1D>,
    private val onAnchorChanged: (Int) -> Unit,
    val collapsedBound: Dp,
) : DraggableState by draggableState {
    private val dismissedBound: Dp
        get() = animation.lowerBound!!

    val expandedBound: Dp
        get() = animation.upperBound!!

    val value by animation.asState()

    val isDismissed by derivedStateOf {
        value == animation.lowerBound!!
    }

    val isCollapsed by derivedStateOf {
        value == collapsedBound
    }

    val isExpanded by derivedStateOf {
        value == animation.upperBound
    }

    val progress by derivedStateOf {
        1f - (animation.upperBound!! - animation.value) / (animation.upperBound!! - collapsedBound)
    }

    private fun collapse(animationSpec: AnimationSpec<Dp>) {
        onAnchorChanged(collapsedAnchor)
        coroutineScope.launch {
            animation.animateTo(collapsedBound, animationSpec)
        }
    }

    private fun expand(animationSpec: AnimationSpec<Dp>) {
        onAnchorChanged(expandedAnchor)
        coroutineScope.launch {
            animation.animateTo(animation.upperBound!!, animationSpec)
        }
    }

    private fun collapse() {
        collapse(SpringSpec())
    }

    private fun expand() {
        expand(SpringSpec())
    }

    fun collapseSoft() {
        collapse(spring(stiffness = Spring.StiffnessMediumLow))
    }

    fun expandSoft() {
        expand(spring(stiffness = Spring.StiffnessMediumLow))
    }

    fun dismiss() {
        onAnchorChanged(dismissedAnchor)
        coroutineScope.launch {
            animation.animateTo(animation.lowerBound!!)
        }
    }

    fun snapTo(value: Dp) {
        coroutineScope.launch {
            animation.snapTo(value)
        }
    }

    fun performFling(velocity: Float, onDismiss: (() -> Unit)?) {
        if (velocity > 250) {
            expand()
        } else if (velocity < -250) {
            if (value < collapsedBound && onDismiss != null) {
                dismiss()
                onDismiss.invoke()
            } else {
                collapse()
            }
        } else {
            val l0 = dismissedBound
            val l1 = (collapsedBound - dismissedBound) / 2
            val l2 = (expandedBound - collapsedBound) / 2
            val l3 = expandedBound

            when (value) {
                in l0..l1 -> {
                    if (onDismiss != null) {
                        dismiss()
                        onDismiss.invoke()
                    } else {
                        collapse()
                    }
                }

                in l1..l2 -> collapse()
                in l2..l3 -> expand()
                else -> Unit
            }
        }
    }

    val preUpPostDownNestedScrollConnection
        get() = object : NestedScrollConnection {
            var isTopReached = false

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (isExpanded && available.y < 0) {
                    isTopReached = false
                }

                return if (isTopReached && available.y < 0 && source == NestedScrollSource.UserInput) {
                    dispatchRawDelta(available.y)
                    available
                } else {
                    Offset.Zero
                }
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (!isTopReached) {
                    isTopReached = consumed.y == 0f && available.y > 0
                }

                return if (isTopReached && source == NestedScrollSource.UserInput) {
                    dispatchRawDelta(available.y)
                    available
                } else {
                    Offset.Zero
                }
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                return if (isTopReached) {
                    val velocity = -available.y
                    performFling(velocity, null)

                    available
                } else {
                    Velocity.Zero
                }
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                isTopReached = false
                return Velocity.Zero
            }
        }
}

const val expandedAnchor = 2
const val collapsedAnchor = 1
const val dismissedAnchor = 0

@Composable
fun rememberBottomSheetState(
    dismissedBound: Dp,
    expandedBound: Dp,
    collapsedBound: Dp = dismissedBound,
    initialAnchor: Int = dismissedAnchor,
): BottomSheetState {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    var previousAnchor by rememberSaveable {
        mutableIntStateOf(initialAnchor)
    }
    val animation = remember {
        Animatable(0.dp, Dp.VectorConverter)
    }

    return remember(dismissedBound, expandedBound, collapsedBound, coroutineScope) {
        val initialValue = when (previousAnchor) {
            expandedAnchor -> expandedBound
            collapsedAnchor -> collapsedBound
            dismissedAnchor -> dismissedBound
            else -> error("Unknown BottomSheet anchor")
        }

        animation.updateBounds(dismissedBound.coerceAtMost(expandedBound), expandedBound)
        coroutineScope.launch {
            animation.animateTo(initialValue, NavigationBarAnimationSpec)
        }

        BottomSheetState(
            draggableState = DraggableState { delta ->
                coroutineScope.launch {
                    animation.snapTo(animation.value - with(density) { delta.toDp() })
                }
            },
            onAnchorChanged = { previousAnchor = it },
            coroutineScope = coroutineScope,
            animation = animation,
            collapsedBound = collapsedBound
        )
    }
}
