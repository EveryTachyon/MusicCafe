package com.example.musiccafe.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    onPlaySong: (Uri, String) -> Unit
) {
    val context = LocalContext.current
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
                        modifier = Modifier.fillMaxWidth().background(CardBackground, RoundedCornerShape(10.dp)).clickable { onPlaySong(uri, name) }.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SongArtwork(uri, Modifier.size(58.dp), 8.dp)
                        Text(name, color = Color.White, fontSize = 17.sp, modifier = Modifier.padding(start = 14.dp), maxLines = 1)
                    }
                }
            }
        }
    }
}
