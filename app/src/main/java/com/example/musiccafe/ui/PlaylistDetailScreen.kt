package com.example.musiccafe.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musiccafe.Playlist
import com.example.musiccafe.displayName
import androidx.compose.ui.platform.LocalContext

@Composable
fun PlaylistDetailContent(
    playlist: Playlist,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onPlaySong: (Uri, String) -> Unit,
    onRemoveSong: (Uri) -> Unit
) {
    val context = LocalContext.current
    var selectedSong by remember { mutableStateOf<Pair<Uri, String>?>(null) }
    val songNames = playlist.songs.map { uri ->
        uri to (uri.lastPathSegment?.substringAfterLast('/') ?: displayName(context.contentResolver, uri))
    }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 18.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(playlist.name, color = Color.White, fontSize = 28.sp, modifier = Modifier.weight(1f), maxLines = 1)
            IconButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, contentDescription = "Edit playlist", tint = Color.White)
            }
        }
        Text("${playlist.songs.size} songs", color = SoftText, fontSize = 16.sp, modifier = Modifier.padding(start = 14.dp, bottom = 18.dp))
        if (songNames.isEmpty()) {
            Text("This playlist is empty.", color = SoftText, fontSize = 17.sp, modifier = Modifier.padding(14.dp))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(songNames, key = { it.first.toString() }) { (uri, name) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().background(CardBackground, RoundedCornerShape(10.dp)).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f).clickable { onPlaySong(uri, name) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SongArtwork(uri, Modifier.size(58.dp), 8.dp)
                            Text(name, color = Color.White, fontSize = 17.sp, modifier = Modifier.padding(start = 14.dp), maxLines = 1)
                        }
                        IconButton(onClick = { selectedSong = uri to name }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = "Song options", tint = Color.White)
                        }
                    }
                }
            }
        }
    }

    selectedSong?.let { (uri, name) ->
        SongOptionsBottomSheet(
            title = name,
            artist = "Playlist track",
            artworkUri = uri,
            onDismiss = { selectedSong = null },
            onPlayNext = { selectedSong = null },
            onAddToQueue = { selectedSong = null },
            onRemoveFromSaved = null,
            onRemoveFromPlaylist = {
                selectedSong = null
                onRemoveSong(uri)
            },
            onViewAlbum = { selectedSong = null },
            onViewArtist = { selectedSong = null },
            onAddToPlaylist = { selectedSong = null },
            onEditTrack = { selectedSong = null },
            onShare = { selectedSong = null },
            onNotInterested = { selectedSong = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsBottomSheet(
    title: String,
    artist: String,
    artworkUri: Uri,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onRemoveFromSaved: (() -> Unit)?,
    onRemoveFromPlaylist: (() -> Unit)?,
    onViewAlbum: () -> Unit,
    onViewArtist: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onEditTrack: () -> Unit,
    onShare: () -> Unit,
    onNotInterested: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF111214)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(172.dp, 150.dp).background(CardBackground, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                SongArtwork(artworkUri, Modifier.fillMaxSize(), 12.dp)
            }
            Text(title, color = Color.White, fontSize = 34.sp, modifier = Modifier.padding(top = 14.dp), maxLines = 2)
            Text(artist, color = SoftText, fontSize = 18.sp, modifier = Modifier.padding(top = 2.dp, bottom = 18.dp))

            val actions = listOf(
                "Remove from saved" to onRemoveFromSaved,
                "Play next" to { onPlayNext() },
                "Add to queue" to { onAddToQueue() },
                "Remove from playlist" to onRemoveFromPlaylist,
                "View album" to { onViewAlbum() },
                "View artist" to { onViewArtist() },
                "Add to playlist" to { onAddToPlaylist() },
                "Edit track" to { onEditTrack() },
                "Share" to { onShare() },
                "Not interested in this artist" to { onNotInterested() }
            )

            actions.forEach { (label, action) ->
                if (action != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { action() }.padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val icon = when (label) {
                            "Remove from saved" -> "♥"
                            "Play next" -> "⇄"
                            "Add to queue" -> "≡"
                            "Remove from playlist" -> "−"
                            "View album" -> "◉"
                            "View artist" -> "◌"
                            "Add to playlist" -> "+"
                            "Edit track" -> "/"
                            "Share" -> "↑"
                            else -> "◌"
                        }
                        Text(icon, color = AccentGreen, fontSize = 20.sp, modifier = Modifier.padding(end = 16.dp))
                        Text(label, color = Color.White, fontSize = 22.sp)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 10.dp)
                    .background(Color(0xFF2A2A2D), RoundedCornerShape(28.dp))
                    .clickable { onDismiss() }
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("CANCEL", color = Color.White, fontSize = 22.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
        }
    }
}
