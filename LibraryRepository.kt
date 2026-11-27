package com.isai.app.data.repo

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.isai.app.data.model.Playlist
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

private val Context.libraryStore: DataStore<Preferences> by preferencesDataStore(name = "library")

/** Likes, playlists, play history and user-attached audio files. Stored locally with DataStore. */
class LibraryRepository(private val context: Context, private val json: Json) {
    private val LIKED = stringSetPreferencesKey("liked")
    private val HISTORY = stringPreferencesKey("history")
    private val PLAYLISTS = stringPreferencesKey("playlists")
    private val OVERRIDES = stringPreferencesKey("audio_overrides")
    private val store get() = context.libraryStore

    val likedIds: Flow<Set<Int>> = store.data.map { p -> p[LIKED].orEmpty().mapNotNull { it.toIntOrNull() }.toSet() }
    val history: Flow<List<Int>> = store.data.map { p -> parseIds(p[HISTORY]) }
    val playlists: Flow<List<Playlist>> = store.data.map { p -> decodePlaylists(p[PLAYLISTS]) }
    val audioOverrides: Flow<Map<Int, String>> = store.data.map { p ->
        runCatching { json.decodeFromString<Map<String, String>>(p[OVERRIDES] ?: "{}") }.getOrDefault(emptyMap())
            .mapNotNull { (k, v) -> k.toIntOrNull()?.let { it to v } }.toMap()
    }

    private fun parseIds(s: String?) = s?.split(',')?.mapNotNull { it.toIntOrNull() }.orEmpty()
    private fun decodePlaylists(s: String?): List<Playlist> =
        if (s.isNullOrBlank()) emptyList() else runCatching { json.decodeFromString<List<Playlist>>(s) }.getOrDefault(emptyList())

    suspend fun toggleLike(id: Int) {
        store.edit { p ->
            val cur = p[LIKED].orEmpty()
            p[LIKED] = if (id.toString() in cur) cur - id.toString() else cur + id.toString()
        }
    }

    suspend fun pushHistory(id: Int) {
        store.edit { p ->
            val cur = parseIds(p[HISTORY])
            p[HISTORY] = (listOf(id) + cur.filter { it != id }).take(50).joinToString(",")
        }
    }

    private suspend fun editPlaylists(f: (List<Playlist>) -> List<Playlist>) {
        store.edit { p -> p[PLAYLISTS] = json.encodeToString(f(decodePlaylists(p[PLAYLISTS]))) }
    }

    suspend fun createPlaylist(name: String, firstTrack: Int? = null): String {
        val id = UUID.randomUUID().toString().take(8)
        editPlaylists { it + Playlist(id, name.trim().ifBlank { "New playlist" }, listOfNotNull(firstTrack)) }
        return id
    }

    suspend fun addToPlaylist(playlistId: String, trackId: Int) = editPlaylists { list ->
        list.map { if (it.id == playlistId && trackId !in it.trackIds) it.copy(trackIds = it.trackIds + trackId) else it }
    }

    suspend fun removeFromPlaylist(playlistId: String, trackId: Int) = editPlaylists { list ->
        list.map { if (it.id == playlistId) it.copy(trackIds = it.trackIds - trackId) else it }
    }

    suspend fun deletePlaylist(playlistId: String) = editPlaylists { list -> list.filter { it.id != playlistId } }

    suspend fun setAudioOverride(trackId: Int, uri: String) {
        store.edit { p ->
            val cur = runCatching { json.decodeFromString<Map<String, String>>(p[OVERRIDES] ?: "{}") }.getOrDefault(emptyMap())
            p[OVERRIDES] = json.encodeToString(cur + (trackId.toString() to uri))
        }
    }
}
