package com.isai.app.features.settings

import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isai.app.core.AppContainer
import com.isai.app.data.model.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(private val c: AppContainer) : ViewModel() {
    val settings: StateFlow<AppSettings> = c.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _cacheBytes = MutableStateFlow(0L)
    val cacheBytes: StateFlow<Long> = _cacheBytes.asStateFlow()

    init { refreshCache() }

    fun <T> set(key: Preferences.Key<T>, value: T) { viewModelScope.launch { c.settings.set(key, value) } }

    fun refreshCache() {
        viewModelScope.launch(Dispatchers.IO) { _cacheBytes.value = runCatching { c.mediaCache.usedBytes() }.getOrDefault(0L) }
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { c.mediaCache.clear() }
            _cacheBytes.value = runCatching { c.mediaCache.usedBytes() }.getOrDefault(0L)
        }
    }
}
