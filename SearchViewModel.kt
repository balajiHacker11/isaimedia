package com.isai.app.features.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isai.app.core.AppContainer
import com.isai.app.data.model.Track
import com.isai.app.data.model.TrackType
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

@OptIn(FlowPreview::class)
class SearchViewModel(private val c: AppContainer) : ViewModel() {
    private val query = MutableStateFlow("")
    val type = MutableStateFlow<TrackType?>(null)
    val language = MutableStateFlow<String?>(null)
    val languages: List<String> = c.catalog.languages

    val results: StateFlow<List<Track>> = combine(query.debounce(120), type, language) { q, t, l ->
        if (q.isBlank() && t == null && l == null) emptyList() else c.catalog.search(q, t, l)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(q: String) { query.value = q }
    fun setType(t: TrackType?) { type.value = if (type.value == t) null else t }
    fun setLanguage(l: String?) { language.value = if (language.value == l) null else l }
}
