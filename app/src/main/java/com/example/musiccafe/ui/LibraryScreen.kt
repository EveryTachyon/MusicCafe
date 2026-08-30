package com.example.musiccafe.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musiccafe.Playlist

@Composable
fun LibraryContent(
    importedSongs: List<Uri>,
    onOpenSavedSongs: () -> Unit,
    playlists: List<Playlist>,
    onOpenCreatePlaylist: () -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Library",
                    color = Color.White,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item { LibrarySummaryRow("${importedSongs.size} songs", Icons.Outlined.LibraryMusic, onOpenSavedSongs) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Playlists",
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.AutoMirrored.Outlined.List, contentDescription = "Playlists", tint = Color.White, modifier = Modifier.height(28.dp))
            }
        }
        item {
            Row(
                modifier = Modifier.padding(horizontal = 22.dp).clickable(onClick = onOpenCreatePlaylist),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(86.dp).background(CardBackground, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", color = Color.White, fontSize = 42.sp)
                }
                Text("Create playlist", color = Color.White, fontSize = 20.sp, modifier = Modifier.padding(start = 18.dp))
            }
        }
        items(playlists) { playlist ->
            Text(playlist.name, color = Color.White, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 22.dp))
        }
    }
}

@Composable
fun LibrarySummaryRow(
    subtitle: String,
    icon: ImageVector,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(86.dp).background(CardBackground, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = SoftText, modifier = Modifier.height(34.dp))
        }
        Column(modifier = Modifier.padding(start = 18.dp)) {
            Text("Saved songs", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = SoftText, fontSize = 18.sp)
        }
    }
}
