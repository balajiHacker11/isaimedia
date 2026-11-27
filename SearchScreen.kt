package com.isai.app.features.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.isai.app.core.LocalTrackMenu
import com.isai.app.core.LocalVmFactory
import com.isai.app.data.model.TrackType
import com.isai.app.shared.TrackRow
import com.isai.app.shared.artColors

private data class Tile(val label: String, val seed: String, val onClick: () -> Unit)

@Composable
fun SearchScreen() {
    val vm: SearchViewModel = viewModel(factory = LocalVmFactory.current)
    val player = LocalPlayerVm.current
    val menu = LocalTrackMenu.current
    val results by vm.results.collectAsStateWithLifecycle()
    val type by vm.type.collectAsStateWithLifecycle()
    val lang by vm.language.collectAsStateWithLifecycle()
    val ps by player.state.collectAsStateWithLifecycle()
    val liked by player.likedIds.collectAsStateWithLifecycle()
    var text by rememberSaveable { mutableStateOf("") }
    val active = text.isNotBlank() || type != null || lang != null

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text("Search", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it; vm.setQuery(it) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            placeholder = { Text("Songs, albums, films, singers") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = {
                if (text.isNotEmpty()) IconButton(onClick = { text = ""; vm.setQuery("") }) { Icon(Icons.Rounded.Close, "Clear") }
            },
        )
        LazyRow(contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TrackType.entries.forEach { t ->
                item(key = t.name) { FilterChip(type == t, { vm.setType(t) }, label = { Text(t.label + "s") }) }
            }
            vm.languages.forEach { l ->
                item(key = "lang-$l") { FilterChip(lang == l, { vm.setLanguage(l) }, label = { Text(l) }) }
            }
        }

        when {
            !active -> {
                val tiles = TrackType.entries.map { t -> Tile(t.label + "s", t.name) { vm.setType(t) } } +
                    vm.languages.map { l -> Tile(l, l) { vm.setLanguage(l) } }
                LazyVerticalGrid(
                    GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(tiles) { tile ->
                        val (c1, c2) = artColors(tile.seed)
                        Box(
                            Modifier.fillMaxWidth().height(92.dp).clip(RoundedCornerShape(16.dp))
                                .background(Brush.linearGradient(listOf(c1, c2)))
                                .clickable(onClick = tile.onClick).padding(14.dp),
                        ) {
                            Text(tile.label, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
            results.isEmpty() -> Text(
                "Nothing matches that. Try a film name or a language.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(24.dp),
            )
            else -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                itemsIndexed(results, key = { _, t -> t.id }) { i, t ->
                    TrackRow(
                        track = t,
                        isCurrent = ps.currentTrack?.id == t.id, isPlaying = ps.isPlaying, liked = t.id in liked,
                        onClick = { player.play(results, i) },
                        onLike = { player.toggleLike(t.id) },
                        onQueue = { player.addToQueue(t) },
                        onMenu = { menu(t, null) },
                    )
                }
            }
        }
    }
}
