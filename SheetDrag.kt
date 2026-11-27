package com.isai.app.features.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

val PlayerSheetSpring = spring<Float>(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)

/**
 * Drives the mini-player <-> full-player expansion (0f = mini, 1f = full) directly from the finger,
 * then settles with a spring using fling velocity. Swipe up on the mini-player, swipe down on the full player.
 * Must sit *outside* any graphicsLayer that moves with the finger, otherwise drag deltas cancel out.
 */
fun Modifier.sheetDrag(
    expand: Animatable<Float, AnimationVector1D>,
    heightPx: Float,
    scope: CoroutineScope,
): Modifier = pointerInput(heightPx) {
    val tracker = VelocityTracker()
    var y = 0f
    detectVerticalDragGestures(
        onDragStart = { tracker.resetTracking(); y = 0f },
        onDragEnd = {
            val v = tracker.calculateVelocity().y
            val target = when {
                v < -900f -> 1f
                v > 900f -> 0f
                expand.value > 0.5f -> 1f
                else -> 0f
            }
            scope.launch { expand.animateTo(target, PlayerSheetSpring) }
        },
        onDragCancel = { scope.launch { expand.animateTo(if (expand.value > 0.5f) 1f else 0f, PlayerSheetSpring) } },
    ) { change, dy ->
        change.consume()
        y += dy
        tracker.addPosition(change.uptimeMillis, Offset(0f, y))
        scope.launch { expand.snapTo((expand.value - dy / heightPx).coerceIn(0f, 1f)) }
    }
}
