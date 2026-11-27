package com.isai.app.data.repo

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.isai.app.data.model.AppSettings
import com.isai.app.data.model.AudioQuality
import com.isai.app.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object SettingsKeys {
    val SHOW_INTRO = booleanPreferencesKey("show_intro")
    val THEME = stringPreferencesKey("theme")
    val ACCENT = intPreferencesKey("accent")
    val QUALITY = stringPreferencesKey("quality")
    val DATA_SAVER = booleanPreferencesKey("data_saver")
    val HAPTICS = booleanPreferencesKey("haptics")
    val G_SWIPE_ROWS = booleanPreferencesKey("g_swipe_rows")
    val G_SWIPE_ART = booleanPreferencesKey("g_swipe_art")
    val G_DT_LIKE = booleanPreferencesKey("g_dt_like")
    val G_DT_SEEK = booleanPreferencesKey("g_dt_seek")
    val PRIVATE = booleanPreferencesKey("private_session")
}

class SettingsRepository(private val context: Context) {
    val settings: Flow<AppSettings> = context.settingsStore.data.map { p ->
        val d = AppSettings()
        AppSettings(
            showIntro = p[SettingsKeys.SHOW_INTRO] ?: d.showIntro,
            themeMode = p[SettingsKeys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: d.themeMode,
            accentIndex = p[SettingsKeys.ACCENT] ?: d.accentIndex,
            quality = p[SettingsKeys.QUALITY]?.let { runCatching { AudioQuality.valueOf(it) }.getOrNull() } ?: d.quality,
            dataSaver = p[SettingsKeys.DATA_SAVER] ?: d.dataSaver,
            haptics = p[SettingsKeys.HAPTICS] ?: d.haptics,
            gestureSwipeRows = p[SettingsKeys.G_SWIPE_ROWS] ?: d.gestureSwipeRows,
            gestureSwipeArt = p[SettingsKeys.G_SWIPE_ART] ?: d.gestureSwipeArt,
            gestureDoubleTapLike = p[SettingsKeys.G_DT_LIKE] ?: d.gestureDoubleTapLike,
            gestureDoubleTapSeek = p[SettingsKeys.G_DT_SEEK] ?: d.gestureDoubleTapSeek,
            privateSession = p[SettingsKeys.PRIVATE] ?: d.privateSession,
        )
    }

    suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        context.settingsStore.edit { it[key] = value }
    }
}
