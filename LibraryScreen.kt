package com.isai.app.features.library

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.isai.app.core.LocalPlayerVm
import com.isai.app.data.model.Playlist
import com.isai.app.shared.CoverArt

@Composable
fun LibraryScreen(onOpen: (kind: String, id: String) -> Unit) {
    val vm = LocalPlayerVm.current
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val liked by vm.likedIds.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Playlist?>(null) }
    var name by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Your library", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = { name = ""; creating = true }) { Icon(Icons.Rounded.Add, "New playlist") }
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                LibRow("Liked songs", "${liked.size} tracks", onClick = { onOpen("liked", "-") }) {
                    GradientIcon(Icons.Rounded.Favorite, Color(0xFF6C4DFF), Color(0xFFB9A8FF))
                }
            }
            item {
                LibRow("Recently played", "Your last 50 plays", onClick = { onOpen("recent", "-") }) {
                    GradientIcon(Icons.Rounded.History, Color(0xFF0E7C86), Color(0xFF7FE0D0))
                }
            }
            items(playlists, key = { it.id }) { p ->
                LibRow(p.name, "${p.trackIds.size} tracks · hold to delete", onClick = { onOpen("playlist", p.id) }, onLongClick = { deleting = p }) {
                    CoverArt(p.id, p.name, Modifier.size(56.dp), corner = 10.dp)
                }
            }
            if (playlists.isEmpty()) item {
                Text(
                    "Playlists you make show up here. Long-press any track and choose Add to playlist.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }

    if (creating) AlertDialog(
        onDismissRequest = { creating = false },
        title = { Text("New playlist") },
        text = { OutlinedTextField(name, { name = it }, singleLine = true, placeholder = { Text("Name") }) },
        confirmButton = { TextButton(onClick = { vm.createPlaylist(name); creating = false }, enabled = name.isNotBlank()) { Text("Create") } },
        dismissButton = { TextButton(onClick = { creating = false }) { Text("Cancel") } },
    )
    deleting?.let { p ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete “${p.name}”?") },
            text = { Text("The songs stay in the catalog; only the playlist is removed.") },
            confirmButton = { TextButton(onClick = { vm.deletePlaylist(p.id); deleting = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun LibRow(title: String, subtitle: String, onClick: () -> Unit, onLongClick: (() -> Unit)? = null, cover: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        cover()
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun GradientIcon(icon: ImageVector, c1: Color, c2: Color) {
    Box(
        Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(listOf(c1, c2))),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = Color.White) }
}
