package com.isai.app.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.isai.app.core.LocalPlayerVm
import com.isai.app.core.LocalVmFactory
import com.isai.app.shared.*
import java.time.LocalTime

@Composable
fun HomeScreen(onOpenCollection: (kind: String, id: String) -> Unit) {
    val vm: HomeViewModel = viewModel(factory = LocalVmFactory.current)
    val ui by vm.ui.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val player = LocalPlayerVm.current

    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Late night listening"
    }

    PullToRefreshBox(isRefreshing = refreshing, onRefresh = vm::refresh, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Text(
                    greeting, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                    modifier = Modifier.statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 16.dp),
                )
            }
            item { MixHero(count = ui.mix.size, onPlay = { player.play(ui.mix, 0) }) }

            if (ui.recent.isNotEmpty()) item {
                Shelf("Jump back in") {
                    ShelfRow { itemsIndexed(ui.recent) { i, t -> TrackCard(t) { player.play(ui.recent, i) } } }
                }
            }
            item {
                Shelf("Made for you", subtitle = "Pull down to reshuffle") {
                    ShelfRow { itemsIndexed(ui.mix) { i, t -> TrackCard(t) { player.play(ui.mix, i) } } }
                }
            }
            item {
                Shelf("Film soundtracks") {
                    ShelfRow { itemsIndexed(ui.soundtracks) { _, a -> AlbumCard(a) { onOpenCollection("album", a.key) } } }
                }
            }
            item {
                Shelf("Scores and BGM", subtitle = "Instrumental, front to back") {
                    ShelfRow { itemsIndexed(ui.scores) { _, a -> AlbumCard(a) { onOpenCollection("album", a.key) } } }
                }
            }
            item {
                Shelf("Indie and singles") {
                    ShelfRow { itemsIndexed(ui.singles) { _, a -> AlbumCard(a) { onOpenCollection("album", a.key) } } }
                }
            }
            item {
                Shelf("Themes and versions") {
                    ShelfRow { itemsIndexed(ui.themesAndVersions) { i, t -> TrackCard(t) { player.play(ui.themesAndVersions, i) } } }
                }
            }
        }
    }
}

@Composable
private fun MixHero(count: Int, onPlay: () -> Unit) {
    val (c1, c2) = artColors("daily-mix")
    Row(
        Modifier.padding(16.dp).fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(c2, c1.copy(alpha = 0.85f))))
            .clickable(onClick = onPlay)
            .padding(22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Your daily mix", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
            Text("$count songs picked for today", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium)
        }
        Box(Modifier.size(56.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.PlayArrow, "Play daily mix", tint = Color(0xFF0B0B10), modifier = Modifier.size(32.dp))
        }
    }
}
