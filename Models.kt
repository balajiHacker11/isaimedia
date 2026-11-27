package com.isai.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---- JSON (matches tracks.json) ----
@Serializable
data class CatalogDto(
    val artist: String = "",
    val albums: List<AlbumDto> = emptyList(),
    val tracks: List<TrackDto> = emptyList(),
)

@Serializable
data class AlbumDto(
    @SerialName("album_key") val key: String,
    val album: String,
    @SerialName("film_or_project") val project: String? = null,
    @SerialName("album_type") val type: String? = null,
    val language: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
)

@Serializable
data class TrackDto(
    val id: Int,
    val title: String,
    @SerialName("track_type") val trackType: String = "song",
    val album: String,
    @SerialName("film_or_project") val project: String? = null,
    val language: String? = null,
    @SerialName("album_release_date") val albumReleaseDate: String? = null,
    val composer: String? = null,
    @SerialName("credited_artists") val creditedArtists: List<String> = emptyList(),
    val notes: String? = null,
    // Optional: add "audio_url" to any track in tracks.json to stream it directly.
    @SerialName("audio_url") val audioUrl: String? = null,
)

// ---- Domain ----
enum class TrackType(val label: String) {
    SONG("Song"), VERSION("Version"), THEME("Theme"), BGM("Score");

    companion object {
        fun from(s: String) = entries.firstOrNull { it.name.equals(s, ignoreCase = true) } ?: SONG
    }
}

data class Track(
    val id: Int,
    val title: String,
    val type: TrackType,
    val albumName: String,
    val albumKey: String,
    val project: String?,
    val language: String?,
    val releaseDate: String?,
    val composer: String?,
    val artists: List<String>,
    val notes: String?,
    val audioUrl: String?,
) {
    /** Credited singers if any, otherwise the film/project, otherwise the album. */
    val artistLine: String get() = if (artists.isNotEmpty()) artists.joinToString(", ") else (project ?: albumName)
    val subtitle: String get() = "${type.label} · $artistLine"
    val searchBlob: String = listOfNotNull(title, albumName, project, language, composer, type.label, artists.joinToString(" "))
        .joinToString(" ").lowercase()
}

data class Album(
    val key: String,
    val title: String,
    val project: String?,
    val type: String?,
    val language: String?,
    val releaseDate: String?,
    val tracks: List<Track>,
) {
    val year: String? get() = releaseDate?.take(4)
    val subtitle: String get() = listOfNotNull(project, year).joinToString(" · ")
    val isScore: Boolean get() = type?.contains("score", ignoreCase = true) == true
    val isSoundtrack: Boolean get() = type?.contains("soundtrack", ignoreCase = true) == true
}

@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val trackIds: List<Int> = emptyList(),
)

// ---- Settings ----
enum class AudioQuality(val label: String, val maxBitrate: Int) {
    LOW("Low · 96 kbps", 96_000),
    NORMAL("Normal · 160 kbps", 160_000),
    HIGH("High · 320 kbps", 320_000),
}

enum class ThemeMode { DARK, LIGHT, SYSTEM }

data class AppSettings(
    val showIntro: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val accentIndex: Int = 0,
    val quality: AudioQuality = AudioQuality.NORMAL,
    val dataSaver: Boolean = false,
    val haptics: Boolean = true,
    val gestureSwipeRows: Boolean = true,
    val gestureSwipeArt: Boolean = true,
    val gestureDoubleTapLike: Boolean = true,
    val gestureDoubleTapSeek: Boolean = true,
    val privateSession: Boolean = false,
) {
    val maxBitrate: Int get() = if (dataSaver) AudioQuality.LOW.maxBitrate else quality.maxBitrate
}
