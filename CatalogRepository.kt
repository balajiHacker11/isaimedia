package com.isai.app.data.repo

import android.content.Context
import com.isai.app.data.model.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlin.random.Random

/** Reads tracks.json from assets once; everything else queries memory. */
class CatalogRepository(context: Context, json: Json) {
    val artist: String
    val albums: List<Album>
    val tracks: List<Track>
    val languages: List<String>
    private val byId: Map<Int, Track>

    init {
        val dto = context.assets.open("tracks.json").bufferedReader().use {
            json.decodeFromString<CatalogDto>(it.readText())
        }
        artist = dto.artist
        val albumByName = dto.albums.associateBy { it.album }
        tracks = dto.tracks.map { t ->
            Track(
                id = t.id, title = t.title, type = TrackType.from(t.trackType),
                albumName = t.album, albumKey = albumByName[t.album]?.key ?: t.album,
                project = t.project, language = t.language, releaseDate = t.albumReleaseDate,
                composer = t.composer, artists = t.creditedArtists, notes = t.notes, audioUrl = t.audioUrl,
            )
        }
        byId = tracks.associateBy { it.id }
        albums = dto.albums.map { a ->
            Album(a.key, a.album, a.project, a.type, a.language, a.releaseDate, tracks.filter { it.albumKey == a.key })
        }.filter { it.tracks.isNotEmpty() }
        languages = tracks.mapNotNull { it.language }.distinct().sorted()
    }

    fun track(id: Int): Track? = byId[id]
    fun album(key: String): Album? = albums.firstOrNull { it.key == key }

    fun search(query: String, type: TrackType?, language: String?): List<Track> {
        val tokens = query.lowercase().split(' ').filter { it.isNotBlank() }
        return tracks.asSequence()
            .filter { type == null || it.type == type }
            .filter { language == null || it.language.equals(language, ignoreCase = true) }
            .filter { t -> tokens.all { tok -> t.searchBlob.contains(tok) } }
            .sortedBy { t -> if (tokens.isNotEmpty() && t.title.lowercase().startsWith(tokens[0])) 0 else 1 }
            .toList()
    }

    /** Rule-based "made for you": a seeded shuffle of vocal tracks so it is stable per day. */
    fun dailyMix(seed: Long, size: Int = 20): List<Track> =
        tracks.filter { it.type == TrackType.SONG || it.type == TrackType.THEME }.shuffled(Random(seed)).take(size)
}
