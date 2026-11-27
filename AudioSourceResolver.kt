package com.isai.app.data.repo

import android.content.Context
import com.isai.app.data.model.Track
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

data class ResolvedAudio(val uri: String, val isDemo: Boolean)

/**
 * tracks.json only contains metadata, so this decides where the audio for each track comes from.
 * Order: file the user attached on-device -> "audio_url" in tracks.json -> assets/audio_sources.json
 * ({"<trackId>": "<https or content:// uri>"}) -> a royalty-free demo stream so playback always works.
 * HLS (.m3u8) and progressive MP3/AAC URLs both work; ExoPlayer detects the type.
 */
class AudioSourceResolver(context: Context, json: Json) {
    private val fileMap: Map<Int, String> = runCatching {
        context.assets.open("audio_sources.json").bufferedReader().use {
            json.decodeFromString<Map<String, String>>(it.readText())
        }.mapNotNull { (k, v) -> k.toIntOrNull()?.let { id -> id to v } }.toMap()
    }.getOrDefault(emptyMap())

    fun resolve(track: Track, userFile: String?): ResolvedAudio {
        userFile?.let { return ResolvedAudio(it, false) }
        track.audioUrl?.let { return ResolvedAudio(it, false) }
        fileMap[track.id]?.let { return ResolvedAudio(it, false) }
        return ResolvedAudio(DEMO[track.id % DEMO.size], true)
    }

    companion object {
        // Placeholder test streams for development only. Replace with audio you have rights to use.
        val DEMO = (1..16).map { "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-$it.mp3" }
    }
}
