package com.isai.app.core

import android.app.Application
import com.isai.app.data.repo.AudioSourceResolver
import com.isai.app.data.repo.CatalogRepository
import com.isai.app.data.repo.LibraryRepository
import com.isai.app.data.repo.SettingsRepository
import com.isai.app.playback.MediaCache
import com.isai.app.playback.PlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json

/** Tiny manual DI container: one instance per process, created in IsaiApp. */
class AppContainer(val app: Application) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val catalog by lazy { CatalogRepository(app, json) }
    val settings = SettingsRepository(app)
    val library = LibraryRepository(app, json)
    val mediaCache = MediaCache(app)
    val audioResolver by lazy { AudioSourceResolver(app, json) }
    val player by lazy { PlayerController(app, appScope, catalog, library, settings, audioResolver) }
}
