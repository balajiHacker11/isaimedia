package com.isai.app.features.intro

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isai.app.R
import com.isai.app.core.rememberHaptic
import com.isai.app.shared.EqualizerBars
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private val IntroPhotos = listOf(
    R.drawable.intro_01, R.drawable.intro_02, R.drawable.intro_03, R.drawable.intro_04,
    R.drawable.intro_05, R.drawable.intro_06, R.drawable.intro_07,
)

/**
 * Interactive intro: a swipeable stack of your photos over a blurred, cross-fading backdrop.
 * Drag a card sideways (or tap it) to send it to the back; flick for a quicker throw.
 */
@Composable
fun IntroScreen(artist: String, trackCount: Int, onEnter: () -> Unit) {
    val scope = rememberCoroutineScope()
    val haptic = rememberHaptic()
    var order by remember { mutableStateOf(IntroPhotos.indices.toList()) }
    val drag = remember { Animatable(0f) }
    val entrance = remember { Animatable(0f) }
    val accent = MaterialTheme.colorScheme.primary
    val ink = Color(0xFF0B0B10)

    LaunchedEffect(Unit) { entrance.animateTo(1f, tween(1000, easing = FastOutSlowInEasing)) }

    BoxWithConstraints(Modifier.fillMaxSize().background(ink)) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val threshold = widthPx * 0.28f

        fun flingAway(dir: Float) {
            scope.launch {
                haptic()
                drag.animateTo(dir * widthPx * 1.3f, tween(240, easing = FastOutLinearInEasing))
                order = order.drop(1) + order.first()
                drag.snapTo(0f)
            }
        }

        // Backdrop: current photo, blurred (blur needs Android 12+; older versions just show it dimmed).
        Crossfade(order.first(), animationSpec = tween(700), label = "backdrop") { i ->
            Image(
                painterResource(IntroPhotos[i]), null,
                Modifier.fillMaxSize().blur(40.dp).graphicsLayer { alpha = 0.55f; scaleX = 1.25f; scaleY = 1.25f },
                contentScale = ContentScale.Crop,
            )
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xD90B0B10), Color(0x330B0B10), Color(0xF20B0B10)))))

        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(20.dp))
            Wordmark("Isai")
            Text(
                "$trackCount songs, themes and scores by $artist",
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.graphicsLayer { alpha = entrance.value },
            )
            Spacer(Modifier.height(16.dp))

            // The deck
            Box(
                Modifier.weight(1f).fillMaxWidth()
                    .pointerInput(Unit) {
                        val tracker = VelocityTracker()
                        var x = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { tracker.resetTracking(); x = 0f },
                            onDragEnd = {
                                val v = tracker.calculateVelocity().x
                                when {
                                    drag.value > threshold || v > 1400f -> flingAway(1f)
                                    drag.value < -threshold || v < -1400f -> flingAway(-1f)
                                    else -> scope.launch { drag.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 300f)) }
                                }
                            },
                            onDragCancel = { scope.launch { drag.animateTo(0f) } },
                        ) { change, dx ->
                            change.consume()
                            x += dx
                            tracker.addPosition(change.uptimeMillis, Offset(x, 0f))
                            scope.launch { drag.snapTo(drag.value + dx) }
                        }
                    }
                    .pointerInput(Unit) { detectTapGestures(onTap = { flingAway(1f) }) },
                contentAlignment = Alignment.Center,
            ) {
                order.take(3).reversed().forEach { photo ->
                    key(photo) {
                        val depth = order.indexOf(photo)
                        PhotoCard(
                            IntroPhotos[photo],
                            Modifier.fillMaxHeight(0.94f).aspectRatio(0.78f, matchHeightConstraintsFirst = true)
                                .graphicsLayer {
                                    val p = (abs(drag.value) / threshold).coerceIn(0f, 1f)
                                    val eff = if (depth == 0) 0f else depth - p
                                    scaleX = 1f - 0.07f * eff
                                    scaleY = 1f - 0.07f * eff
                                    translationY = 26.dp.toPx() * eff + (1f - entrance.value) * 320.dp.toPx()
                                    alpha = entrance.value * (1f - 0.2f * eff)
                                    if (depth == 0) {
                                        translationX = drag.value
                                        rotationZ = drag.value / widthPx * 22f
                                    }
                                },
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Dots(count = IntroPhotos.size, active = order.first(), accent = accent)
            Spacer(Modifier.height(18.dp))

            val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
                0f, 1f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse",
            )
            Button(
                onClick = { haptic(); onEnter() },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = ink),
                modifier = Modifier.fillMaxWidth().height(58.dp)
                    .graphicsLayer { val s = 1f + pulse * 0.025f; scaleX = s; scaleY = s; alpha = entrance.value },
            ) {
                EqualizerBars(true, ink, Modifier.size(18.dp))
                Spacer(Modifier.width(12.dp))
                Text("Start listening", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Swipe or tap the photos",
                color = Color.White.copy(alpha = 0.55f),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.graphicsLayer { alpha = entrance.value },
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PhotoCard(res: Int, modifier: Modifier) {
    val shape = RoundedCornerShape(28.dp)
    Box(modifier.shadow(20.dp, shape).clip(shape)) {
        Image(painterResource(res), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = Alignment.TopCenter)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.65f to Color.Transparent, 1f to Color(0x99000000))))
    }
}

/** Letters spring in one after another: the one orchestrated moment on this screen. */
@Composable
private fun Wordmark(text: String) {
    Row {
        text.forEachIndexed { i, ch ->
            key(i) {
                val a = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    delay(250L + i * 110L)
                    a.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 180f))
                }
                Text(
                    ch.toString(), color = Color.White, fontSize = 68.sp, fontWeight = FontWeight.Black,
                    modifier = Modifier.graphicsLayer { alpha = a.value.coerceIn(0f, 1f); translationY = (1f - a.value) * 70f },
                )
            }
        }
    }
}

@Composable
private fun Dots(count: Int, active: Int, accent: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            val w by animateDpAsState(if (i == active) 22.dp else 6.dp, spring(), label = "dot")
            Box(Modifier.height(6.dp).width(w).clip(CircleShape).background(if (i == active) accent else Color.White.copy(alpha = 0.35f)))
        }
    }
}
