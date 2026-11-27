package com.isai.app.features.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.isai.app.core.rememberHaptic
import com.isai.app.shared.CoverArt
import com.isai.app.shared.artColors

/** Persistent mini-player, tinted from the album colour. Tap or swipe up to expand. */
@Composable
fun MiniPlayer(vm: PlayerViewModel, onExpand: () -> Unit, dragModifier: Modifier) {
    val s by vm.state.collectAsStateWithLifecycle()
    val liked by vm.likedIds.collectAsStateWithLifecycle()
    val t = s.currentTrack ?: return
    val haptic = rememberHaptic()
    val (_, deep) = remember(t.albumKey) { artColors(t.albumKey) }
    val tint by animateColorAsState(deep, tween(500), label = "mini-tint")
    val isLiked = t.id in liked

    CompositionLocalProvider(LocalContentColor provides Color.White) {
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(14.dp)).background(tint)
                .then(dragModifier)
                .clickable(onClick = onExpand),
        ) {
            Row(Modifier.padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                CoverArt(t.albumKey, t.albumName, Modifier.size(44.dp), corner = 8.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(t.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(t.artistLine, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = { haptic(); vm.toggleLike(t.id) }) {
                    Icon(if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Like")
                }
                IconButton(onClick = { haptic(); vm.togglePlay() }) {
                    Icon(if (s.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (s.isPlaying) "Pause" else "Play", Modifier.size(30.dp))
                }
            }
            MiniProgress(vm, s.durationMs, Modifier.align(Alignment.BottomCenter).padding(horizontal = 10.dp))
        }
    }
}

@Composable
private fun MiniProgress(vm: PlayerViewModel, duration: Long, modifier: Modifier) {
    val pos by vm.position.collectAsStateWithLifecycle()
    LinearProgressIndicator(
        progress = { if (duration > 0) (pos.toFloat() / duration).coerceIn(0f, 1f) else 0f },
        modifier = modifier.fillMaxWidth().height(2.dp),
        color = Color.White, trackColor = Color.White.copy(alpha = 0.22f),
    )
}
