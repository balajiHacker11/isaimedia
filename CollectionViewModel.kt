package com.isai.app.features.collection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isai.app.core.AppContainer
import com.isai.app.data.model.Track
import kotlinx.coroutines.flow.*

data class CollectionUi(
    val title: String,
    val subtitle: String,
    val seed: String,
    val tracks: List<Track>,
    val playlistId: String? = null,
)

/** One screen for album / liked songs / recently played / user playlist, chosen by nav args. */
class CollectionViewModel(c: AppContainer, handle: SavedStateHandle) : ViewModel() {
    private val kind: String = handle.get<String>("kind") ?: "liked"
    private val ref: String = handle.get<String>("id") ?: "-"

    private fun count(n: Int) = if (n == 1) "1 track" else "$n tracks"

    private val source: Flow<CollectionUi?> = when (kind) {
        "album" -> flowOf(c.catalog.album(ref)?.let {
            CollectionUi(it.title, listOfNotNull(it.subtitle.ifBlank { null }, count(it.tracks.size)).joinToString(" · "), it.key, it.tracks)
        })
        "liked" -> c.library.likedIds.map { ids ->
            val t = c.catalog.tracks.filter { it.id in ids }
            CollectionUi("Liked songs", count(t.size), "liked", t)
        }
        "recent" -> c.library.history.map { h ->
            val t = h.mapNotNull { c.catalog.track(it) }
            CollectionUi("Recently played", count(t.size), "recent", t)
        }
        "playlist" -> c.library.playlists.map { lists ->
            lists.firstOrNull { it.id == ref }?.let { p ->
                val t = p.trackIds.mapNotNull { c.catalog.track(it) }
                CollectionUi(p.name, count(t.size), p.id, t, playlistId = p.id)
            }
        }
        else -> flowOf(null)
    }

    val ui: StateFlow<CollectionUi?> = source.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
