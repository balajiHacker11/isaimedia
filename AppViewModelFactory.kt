package com.isai.app.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.isai.app.features.collection.CollectionViewModel
import com.isai.app.features.home.HomeViewModel
import com.isai.app.features.player.PlayerViewModel
import com.isai.app.features.search.SearchViewModel
import com.isai.app.features.settings.SettingsViewModel

class AppViewModelFactory(private val c: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val vm: ViewModel = when {
            modelClass.isAssignableFrom(PlayerViewModel::class.java) -> PlayerViewModel(c)
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(c)
            modelClass.isAssignableFrom(SearchViewModel::class.java) -> SearchViewModel(c)
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(c)
            modelClass.isAssignableFrom(CollectionViewModel::class.java) -> CollectionViewModel(c, extras.createSavedStateHandle())
            else -> error("Unknown ViewModel: ${modelClass.name}")
        }
        return vm as T
    }
}
