package com.isai.app.core

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.ViewModelProvider
import com.isai.app.data.model.AppSettings
import com.isai.app.data.model.Track
import com.isai.app.features.player.PlayerViewModel

val LocalSettings = staticCompositionLocalOf { AppSettings() }
val LocalVmFactory = staticCompositionLocalOf<ViewModelProvider.Factory> { error("ViewModel factory not provided") }
val LocalPlayerVm = staticCompositionLocalOf<PlayerViewModel> { error("PlayerViewModel not provided") }

/** Opens the track options sheet. Second arg, when non-null, adds "Remove from this playlist". */
val LocalTrackMenu = staticCompositionLocalOf<(Track, (() -> Unit)?) -> Unit> { { _, _ -> } }

/** Returns a function that fires haptic feedback if the user hasn't turned haptics off in Settings. */
@Composable
fun rememberHaptic(): () -> Unit {
    val h = LocalHapticFeedback.current
    val enabled = LocalSettings.current.haptics
    return remember(h, enabled) { { if (enabled) h.performHapticFeedback(HapticFeedbackType.LongPress) } }
}
