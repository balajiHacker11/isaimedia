package com.isai.app.features.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.isai.app.core.AccentColors
import com.isai.app.core.AccentNames
import com.isai.app.core.LocalVmFactory
import com.isai.app.data.model.AudioQuality
import com.isai.app.data.model.ThemeMode
import com.isai.app.data.repo.SettingsKeys

@Composable
fun SettingsScreen() {
    val vm: SettingsViewModel = viewModel(factory = LocalVmFactory.current)
    val s by vm.settings.collectAsStateWithLifecycle()
    val cache by vm.cacheBytes.collectAsStateWithLifecycle()

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item { Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp)) }

        item { Section("Playback") }
        item {
            Text("Streaming quality", Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodyLarge)
            LazyRow(contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AudioQuality.entries.forEach { q ->
                    item { FilterChip(s.quality == q, { vm.set(SettingsKeys.QUALITY, q.name) }, label = { Text(q.label) }) }
                }
            }
        }
        item { Toggle("Data saver", "Caps streaming at 96 kbps", s.dataSaver) { vm.set(SettingsKeys.DATA_SAVER, it) } }
        item { Toggle("Private session", "Don't record plays in Recently played", s.privateSession) { vm.set(SettingsKeys.PRIVATE, it) } }

        item { Section("Appearance") }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { m ->
                    item { FilterChip(s.themeMode == m, { vm.set(SettingsKeys.THEME, m.name) }, label = { Text(m.name.lowercase().replaceFirstChar { it.uppercase() }) }) }
                }
            }
        }
        item {
            Text("Accent colour", Modifier.padding(start = 16.dp, top = 12.dp), style = MaterialTheme.typography.bodyLarge)
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                AccentColors.forEachIndexed { i, c ->
                    Box(
                        Modifier.size(38.dp).clip(CircleShape).background(c)
                            .border(if (i == s.accentIndex) BorderStroke(3.dp, Color.White) else BorderStroke(0.dp, Color.Transparent), CircleShape)
                            .clickable { vm.set(SettingsKeys.ACCENT, i) },
                    )
                }
            }
            Text(AccentNames.getOrElse(s.accentIndex) { "" }, Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        item { Toggle("Show intro on launch", "The photo deck at startup", s.showIntro) { vm.set(SettingsKeys.SHOW_INTRO, it) } }

        item { Section("Gestures") }
        item { Toggle("Haptic feedback", "Likes, skips, intro swipes", s.haptics) { vm.set(SettingsKeys.HAPTICS, it) } }
        item { Toggle("Swipe track rows", "Right to like, left to queue", s.gestureSwipeRows) { vm.set(SettingsKeys.G_SWIPE_ROWS, it) } }
        item { Toggle("Swipe album art", "Left or right to change track", s.gestureSwipeArt) { vm.set(SettingsKeys.G_SWIPE_ART, it) } }
        item { Toggle("Double-tap cover to like", "Centre of the art", s.gestureDoubleTapLike) { vm.set(SettingsKeys.G_DT_LIKE, it) } }
        item { Toggle("Double-tap sides to seek", "Left or right edge, 10 seconds", s.gestureDoubleTapSeek) { vm.set(SettingsKeys.G_DT_SEEK, it) } }

        item { Section("Storage") }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Audio cache", style = MaterialTheme.typography.bodyLarge)
                    Text("%.1f MB used (max 512 MB)".format(cache / 1_048_576.0), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(onClick = vm::clearCache) { Text("Clear") }
            }
        }
        item {
            Text(
                "Tracks without an audio source play royalty-free demo streams. See README to plug in your own.",
                Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(title, Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
}

@Composable
private fun Toggle(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked, onChange)
    }
}
