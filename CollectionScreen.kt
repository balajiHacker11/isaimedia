package com.isai.app.features.collection

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.isai.app.core.LocalPlayerVm
import com.isai.app.core.LocalTrackMenu
import com.isai.app.core.LocalVmFactory
import com.isai.app.shared.CoverArt
import com.isai.app.shared.TrackRow
import com.isai.app.shared.artColors

/** Album / liked / recent / playlist page with a parallax header that collapses into a title bar. */
@Composable
fun CollectionScreen(onBack: () -> Unit) {
    val vm: CollectionViewModel = viewModel(factory = LocalVmFactory.current)
    val player = LocalPlayerVm.current
    val menu = LocalTrackMenu.current
    val ui by vm.ui.collectAsStateWithLifecycle()
    val ps by player.state.collectAsStateWithLifecycle()
    val liked by player.likedIds.collectAsStateWithLifecycle()
    val data = ui

    if (data == null) {
        Box(Modifier.fillMaxSize().statusBarsPadding()) { BackButton(onBack) }
        return
    }

    val listState = rememberLazyListState()
    val showBar by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 460 } }
    val (_, deep) = artColors(data.seed)
    val bg = MaterialTheme.colorScheme.background

    Box(Modifier.fillMaxSize()) {
        LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 24.dp)) {
            item(key = "header") {
                Column(
                    Modifier.fillMaxWidth().height(380.dp)
                        .graphicsLayer {
                            val off = if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset.toFloat() else 2000f
                            translationY = off * 0.5f
                            alpha = (1f - off / 760f).coerceIn(0f, 1f)
                        }
                        .background(Brush.verticalGradient(listOf(deep, bg)))
                        .statusBarsPadding().padding(top = 56.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CoverArt(data.seed, data.title, Modifier.size(172.dp), corner = 16.dp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        data.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp), maxLines = 2,
                    )
                    Text(data.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
            item(key = "actions") {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { player.play(data.tracks, 0) }, enabled = data.tracks.isNotEmpty()) {
                        Icon(Icons.Rounded.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Play")
                    }
                    OutlinedButton(onClick = { player.shufflePlay(data.tracks) }, enabled = data.tracks.isNotEmpty()) {
                        Icon(Icons.Rounded.Shuffle, null); Spacer(Modifier.width(6.dp)); Text("Shuffle")
                    }
                }
            }
            if (data.tracks.isEmpty()) item {
                Text(
                    "Nothing here yet. Swipe a track right to like it, or long-press for more.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp),
                )
            }
            itemsIndexed(data.tracks, key = { _, t -> t.id }) { i, t ->
                val remove: (() -> Unit)? = data.playlistId?.let { pid -> { player.removeFromPlaylist(pid, t.id) } }
                TrackRow(
                    track = t,
                    isCurrent = ps.currentTrack?.id == t.id, isPlaying = ps.isPlaying, liked = t.id in liked,
                    onClick = { player.play(data.tracks, i) },
                    onLike = { player.toggleLike(t.id) },
                    onQueue = { player.addToQueue(t) },
                    onMenu = { menu(t, remove) },
                )
            }
        }

        AnimatedVisibility(showBar, Modifier.align(Alignment.TopStart), enter = fadeIn(), exit = fadeOut()) {
            Row(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).statusBarsPadding().height(56.dp).padding(start = 56.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) { Text(data.title, fontWeight = FontWeight.Bold, maxLines = 1, style = MaterialTheme.typography.titleMedium) }
        }
        Box(Modifier.statusBarsPadding()) { BackButton(onBack) }
    }
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack, modifier = Modifier.padding(4.dp)) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
}
