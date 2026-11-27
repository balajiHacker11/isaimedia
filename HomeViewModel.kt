package com.isai.app.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isai.app.core.AppContainer
import com.isai.app.data.model.Album
import com.isai.app.data.model.Track
import com.isai.app.data.model.TrackType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HomeUi(
    val recent: List<Track>,
    val mix: List<Track>,
    val soundtracks: List<Album>,
    val scores: List<Album>,
    val singles: List<Album>,
    val themesAndVersions: List<Track>,
)

class HomeViewModel(private val c: AppContainer) : ViewModel() {
    private val tick = MutableStateFlow(0)
    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private fun build(history: List<Int>, t: Int): HomeUi {
        val cat = c.catalog
        return HomeUi(
            recent = history.mapNotNull { cat.track(it) }.take(12),
            mix = cat.dailyMix(LocalDate.now().toEpochDay() + t),
            soundtracks = cat.albums.filter { it.isSoundtrack },
            scores = cat.albums.filter { it.isScore },
            singles = cat.albums.filter { !it.isSoundtrack && !it.isScore },
            themesAndVersions = cat.tracks.filter { it.type == TrackType.THEME || it.type == TrackType.VERSION },
        )
    }

    val ui: StateFlow<HomeUi> = combine(c.library.history, tick) { h, t -> build(h, t) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), build(emptyList(), 0))

    /** Pull-to-refresh reshuffles "Made for you". */
    fun refresh() {
        viewModelScope.launch {
            _refreshing.value = true
            delay(500)
            tick.update { it + 1 }
            _refreshing.value = false
        }
    }
}
