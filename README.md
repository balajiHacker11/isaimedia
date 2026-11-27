# Isai: Compose + ExoPlayer music app

Native Android (Kotlin, Jetpack Compose, Media3/ExoPlayer) built from `ARCHITECTURE.pdf`
and `app/src/main/assets/tracks.json` (110 tracks, 12 albums).

## Run it
1. Android Studio (Ladybug or newer), open this folder, let Gradle sync (needs internet once).
2. Run on a device or emulator, Android 8.0+ (API 26).
   The Gradle wrapper jar isn't included; Studio syncs without it. For the CLI run `gradle wrapper` once.

> This project was written without an Android SDK available, so it has not been compiled yet.
> Expect to fix a few small compile errors on first sync (Compose/Media3 APIs shift between versions).
> Versions are pinned in `app/build.gradle.kts` (Compose BOM 2024.10.01, Media3 1.5.0, Kotlin 2.0.21, AGP 8.7.3).

## Where the audio comes from
`tracks.json` is metadata only. Playback resolves a source per track in this order
(`data/repo/AudioSourceResolver.kt`):

1. A file you attached on the device: long-press a track, then **Attach audio file from device**.
2. An `"audio_url"` field added to the track inside `tracks.json`.
3. `app/src/main/assets/audio_sources.json`, a map of track id to URL, e.g. `{"1": "https://your.cdn/katchi-sera.m3u8"}`.
4. Fallback: royalty-free SoundHelix demo MP3s (marked "Demo audio" in the player). Dev placeholders only.

Use audio you own or are licensed to stream. HLS (`.m3u8`), MP3 and AAC all work.

## Intro photos
`res/drawable-nodpi/intro_01..07.jpg`, shown by `features/intro/IntroScreen.kt`
(swipe or tap the deck; background cross-fades and blurs on Android 12+). Swap or add files and
update the `IntroPhotos` list at the top of that file.

## Structure (follows the PDF's Clean Architecture split)
```
core/        theme, DI container, ViewModel factory, CompositionLocals
data/        model/ (JSON + domain), repo/ (catalog, library, settings, audio resolver)
playback/    PlaybackService (ExoPlayer + MediaSession), PlayerController, MediaCache
features/    intro, home, search, library, collection (album/liked/recent/playlist), player, settings
shared/      TrackRow, cards, CoverArt (generated gradients), options sheet
navigation/  AppRoot (intro -> shell), MainShell (nav + mini/full player)
```
UI -> ViewModel (StateFlow) -> repository / PlayerController -> (assets, DataStore, MediaSession). UI never touches ExoPlayer.

## Gestures (each toggleable in Settings)
| Gesture | Action |
|---|---|
| Swipe up on mini-player / down on full player | Expand / minimize (follows the finger, springs on release) |
| Swipe cover left/right | Next / previous track |
| Double-tap cover centre / left edge / right edge | Like / -10s / +10s |
| Swipe a track row right / left | Like / add to queue |
| Long-press a track | Options sheet |
| Pull down on Home | Reshuffle "Made for you" |

## Playback features in place
Background playback, notification + lock-screen controls, audio focus, pause on headphones unplug,
gapless playlists, 30-60s buffering, 512 MB disk cache, quality / data-saver bitrate cap,
shuffle, repeat, queue (jump, remove), sleep timer, history (with private session), likes, playlists.

## Not built yet (next phases from the PDF)
Crossfade, equalizer, synced lyrics, drag-to-reorder queue, offline downloads, artwork in the notification
(the catalog has no cover images, so covers are generated), Android Auto.
