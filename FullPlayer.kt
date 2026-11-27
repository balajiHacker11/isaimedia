package com.isai.app.features.player

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.isai.app.core.LocalSettings
import com.isai.app.core.LocalTrackMenu
import com.isai.app.core.rememberHaptic
import com.isai.app.shared.CoverArt
import com.isai.app.shared.EqualizerBars
import com.isai.app.shared.artColors
import com.isai.app.playback.PlayerUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Ink = Color(0xFF0B0B10)

@Composable
fun FullPlayer(vm: PlayerViewModel, onCollapse: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val liked by vm.likedIds.collectAsStateWithLifecycle()
    val sleepEnds by vm.sleepEndsAt.collectAsStateWithLifecycle()
    val t = s.currentTrack ?: return
    val settings = LocalSettings.current
    val haptic = rememberHaptic()
    val menu = LocalTrackMenu.current
    val scope = rememberCoroutineScope()
    val accent = MaterialTheme.colorScheme.primary
    val (_, deep) = remember(t.albumKey) { artColors(t.albumKey) }
    val top by animateColorAsState(deep, tween(700), label = "player-top")
    val isLiked = t.id in liked

    var showQueue by remember { mutableStateOf(false) }
    var showSleep by remember { mutableStateOf(false) }

    // Cover feedback (heart / ±10s) + swipe offset
    val artOffset = remember { Animatable(0f) }
    var burstTick by remember { mutableIntStateOf(0) }
    var burstLabel by remember { mutableStateOf("") }
    var showBurst by remember { mutableStateOf(false) }
    LaunchedEffect(burstTick) {
        if (burstTick > 0) { showBurst = true; delay(650); showBurst = false }
    }
    fun fire(label: String) { burstLabel = label; burstTick++ }

    CompositionLocalProvider(LocalContentColor provides Color.White) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to top, 0.6f to Ink, 1f to Ink))) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp)) {
                // Top bar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onCollapse) { Icon(Icons.Rounded.KeyboardArrowDown, "Minimize", Modifier.size(32.dp)) }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Playing from", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                        Text(t.albumName, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    }
                    IconButton(onClick = { menu(t, null) }) { Icon(Icons.Rounded.MoreVert, "More") }
                }

                // Cover art: swipe to change track, double-tap centre to like, double-tap edges to seek
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    val side = minOf(maxWidth, maxHeight)
                    val playScale by animateFloatAsState(if (s.isPlaying) 1f else 0.88f, spring(dampingRatio = 0.6f, stiffness = 250f), label = "art-scale")
                    Box(
                        Modifier.size(side)
                            .pointerInput(settings.gestureDoubleTapLike, settings.gestureDoubleTapSeek, isLiked, t.id) {
                                detectTapGestures(onDoubleTap = { o ->
                                    val zone = o.x / size.width
                                    when {
                                        zone < 0.3f && settings.gestureDoubleTapSeek -> { vm.seekBy(-10_000); fire("−10s"); haptic() }
                                        zone > 0.7f && settings.gestureDoubleTapSeek -> { vm.seekBy(10_000); fire("+10s"); haptic() }
                                        settings.gestureDoubleTapLike -> { if (!isLiked) vm.toggleLike(t.id); fire("heart"); haptic() }
                                        else -> Unit
                                    }
                                })
                            }
                            .pointerInput(settings.gestureSwipeArt) {
                                if (!settings.gestureSwipeArt) return@pointerInput
                                var total = 0f
                                detectHorizontalDragGestures(
                                    onDragStart = { total = 0f },
                                    onDragEnd = {
                                        scope.launch {
                                            when {
                                                total < -size.width * 0.25f -> { haptic(); vm.next() }
                                                total > size.width * 0.25f -> { haptic(); vm.previous() }
                                                else -> Unit
                                            }
                                            artOffset.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 300f))
                                        }
                                    },
                                    onDragCancel = { scope.launch { artOffset.animateTo(0f) } },
                                ) { change, dx ->
                                    change.consume()
                                    total += dx
                                    scope.launch { artOffset.snapTo(total * 0.7f) }
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier.fillMaxSize()
                                .graphicsLayer { translationX = artOffset.value; rotationZ = artOffset.value / 45f; scaleX = playScale; scaleY = playScale }
                                .shadow(24.dp, RoundedCornerShape(20.dp)),
                        ) { CoverArt(t.albumKey, t.albumName, Modifier.fillMaxSize(), corner = 20.dp) }

                        AnimatedVisibility(
                            showBurst,
                            enter = scaleIn(spring(dampingRatio = 0.45f)) + fadeIn(tween(100)),
                            exit = fadeOut(tween(300)),
                        ) {
                            Box(Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.38f)).padding(26.dp), contentAlignment = Alignment.Center) {
                                if (burstLabel == "heart") Icon(Icons.Rounded.Favorite, null, Modifier.size(84.dp), tint = Color.White)
                                else Text(burstLabel, fontSize = 32.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                // Title + like
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(t.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
                        Text(t.artistLine, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.bodyMedium)
                    }
                    IconButton(onClick = { haptic(); vm.toggleLike(t.id) }) {
                        Icon(if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Like", tint = if (isLiked) accent else Color.White, modifier = Modifier.size(28.dp))
                    }
                }
                s.error?.let { Text(it, color = Color(0xFFFF8A80), style = MaterialTheme.typography.bodySmall) }

                Scrubber(vm, s.durationMs)

                // Transport
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { haptic(); vm.toggleShuffle() }) {
                        Icon(Icons.Rounded.Shuffle, "Shuffle", tint = if (s.shuffle) accent else Color.White.copy(alpha = 0.8f))
                    }
                    IconButton(onClick = { haptic(); vm.previous() }, Modifier.size(56.dp)) { Icon(Icons.Rounded.SkipPrevious, "Previous", Modifier.size(40.dp)) }
                    Box(
                        Modifier.size(72.dp).clip(CircleShape).background(Color.White).clickable { haptic(); vm.togglePlay() },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (s.isBuffering) CircularProgressIndicator(Modifier.size(34.dp), color = Ink, strokeWidth = 3.dp)
                        else AnimatedContent(s.isPlaying, transitionSpec = { scaleIn(spring(0.6f)) + fadeIn() togetherWith scaleOut() + fadeOut() }, label = "play") { playing ->
                            Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (playing) "Pause" else "Play", Modifier.size(42.dp), tint = Ink)
                        }
                    }
                    IconButton(onClick = { haptic(); vm.next() }, Modifier.size(56.dp)) { Icon(Icons.Rounded.SkipNext, "Next", Modifier.size(40.dp)) }
                    IconButton(onClick = { haptic(); vm.cycleRepeat() }) {
                        val on = s.repeatMode != Player.REPEAT_MODE_OFF
                        Icon(if (s.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, "Repeat", tint = if (on) accent else Color.White.copy(alpha = 0.8f))
                    }
                }

                // Footer: sleep timer, source chip, queue
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showSleep = true }) {
                        Icon(Icons.Rounded.Bedtime, "Sleep timer", tint = if (sleepEnds != null) accent else Color.White.copy(alpha = 0.8f))
                    }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (s.isDemoAudio) {
                            Text(
                                "Demo audio", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.75f),
                                modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                    IconButton(onClick = { showQueue = true }) { Icon(Icons.AutoMirrored.Rounded.QueueMusic, "Queue") }
                }
            }
        }
    }

    if (showQueue) QueueSheet(vm) { showQueue = false }
    if (showSleep) SleepDialog(active = sleepEnds != null, onPick = { vm.setSleepTimer(it); showSleep = false }, onDismiss = { showSleep = false })
}

@Composable
private fun Scrubber(vm: PlayerViewModel, duration: Long) {
    val pos by vm.position.collectAsStateWithLifecycle()
    var dragging by remember { mutableStateOf<Float?>(null) }
    val frac = if (duration > 0) (pos.toFloat() / duration).coerceIn(0f, 1f) else 0f
    val shown = dragging ?: frac
    Column {
        Slider(
            value = shown,
            onValueChange = { dragging = it },
            onValueChangeFinished = { dragging?.let { vm.seekTo((it * duration).toLong()) }; dragging = null },
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = 0.25f)),
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(fmt((shown * duration).toLong()), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
            Text(fmt(duration), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
        }
    }
}

private fun fmt(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

@Composable
private fun SleepDialog(active: Boolean, onPick: (Int?) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep timer") },
        text = {
            Column {
                listOf(15, 30, 45, 60).forEach { m ->
                    TextButton(onClick = { onPick(m) }, Modifier.fillMaxWidth()) { Text("$m minutes") }
                }
                if (active) TextButton(onClick = { onPick(null) }, Modifier.fillMaxWidth()) { Text("Turn off timer") }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun QueueSheet(vm: PlayerViewModel, onDismiss: () -> Unit) {
    val s: PlayerUiState by vm.state.collectAsStateWithLifecycle()
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val list = rememberLazyListState(initialFirstVisibleItemIndex = (s.queueIndex - 1).coerceAtLeast(0))
    val accent = MaterialTheme.colorScheme.primary
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Text("Queue", Modifier.padding(horizontal = 20.dp, vertical = 8.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        LazyColumn(state = list, modifier = Modifier.heightIn(max = 480.dp)) {
            itemsIndexed(s.queue) { i, q ->
                val current = i == s.queueIndex
                Row(
                    Modifier.fillMaxWidth().clickable { vm.jumpTo(i) }.padding(start = 20.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CoverArt(q.albumKey, q.albumName, Modifier.size(44.dp), corner = 8.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(q.title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (current) accent else MaterialTheme.colorScheme.onSurface, fontWeight = if (current) FontWeight.Bold else FontWeight.Medium)
                        Text(q.subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    if (current) EqualizerBars(s.isPlaying, accent)
                    IconButton(onClick = { vm.removeAt(i) }) { Icon(Icons.Rounded.Close, "Remove from queue") }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
