package com.isai.app.features.player

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isai.app.core.AppContainer
import com.isai.app.data.model.Playlist
import com.isai.app.data.model.Track
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Activity-scoped: playback state + the actions every screen needs (play, like, playlists). */
class PlayerViewModel(private val c: AppContainer) : ViewModel() {
    val state = c.player.state
    val position = c.player.position
    val sleepEndsAt = c.player.sleepEndsAt

    val likedIds: StateFlow<Set<Int>> = c.library.likedIds.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val playlists: StateFlow<List<Playlist>> = c.library.playlists.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Playback
    fun play(tracks: List<Track>, index: Int = 0) = c.player.playQueue(tracks, index)
    fun shufflePlay(tracks: List<Track>) = c.player.playQueue(tracks, shuffle = true)
    fun playNext(t: Track) = c.player.playNext(t)
    fun addToQueue(t: Track) = c.player.addToQueue(t)
    fun togglePlay() = c.player.togglePlay()
    fun next() = c.player.next()
    fun previous() = c.player.previous()
    fun seekTo(ms: Long) = c.player.seekTo(ms)
    fun seekBy(ms: Long) = c.player.seekBy(ms)
    fun toggleShuffle() = c.player.toggleShuffle()
    fun cycleRepeat() = c.player.cycleRepeat()
    fun jumpTo(i: Int) = c.player.jumpTo(i)
    fun removeAt(i: Int) = c.player.removeAt(i)
    fun setSleepTimer(minutes: Int?) = c.player.setSleepTimer(minutes)

    // Library
    fun toggleLike(id: Int) { viewModelScope.launch { c.library.toggleLike(id) } }
    fun createPlaylist(name: String, firstTrack: Int? = null) { viewModelScope.launch { c.library.createPlaylist(name, firstTrack) } }
    fun addToPlaylist(playlistId: String, trackId: Int) { viewModelScope.launch { c.library.addToPlaylist(playlistId, trackId) } }
    fun removeFromPlaylist(playlistId: String, trackId: Int) { viewModelScope.launch { c.library.removeFromPlaylist(playlistId, trackId) } }
    fun deletePlaylist(id: String) { viewModelScope.launch { c.library.deletePlaylist(id) } }

    /** Link an audio file from the device (that you have the rights to) to a catalog track. */
    fun attachAudio(trackId: Int, uri: Uri) {
        runCatching {
            c.app.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        viewModelScope.launch {
            c.library.setAudioOverride(trackId, uri.toString())
            c.player.refreshSource(trackId)
        }
    }
}
