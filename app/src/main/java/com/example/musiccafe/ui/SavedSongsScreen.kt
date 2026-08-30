package com.example.musiccafe.ui

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musiccafe.loadTrackMetadata

@Composable
fun SavedSongsContent(
    importedSongs: List<Uri>,
    onBack: () -> Unit,
    onPlaySong: (Uri, String) -> Unit,
    onOpenImportSongs: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val trackData = remember(importedSongs) {
        importedSongs.map { uri -> loadTrackMetadata(context, uri) }
    }
    val visibleSongs = trackData.filter { track ->
        searchQuery.isBlank() || track.title.contains(searchQuery, ignoreCase = true) || track.artist.contains(searchQuery, ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0F0E13)).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Outlined.List, contentDescription = "Song list", tint = Color.White, modifier = Modifier.height(34.dp))
            }
        }
        item {
            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 20.sp),
                modifier = Modifier.fillMaxWidth().height(58.dp),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier.fillMaxSize().background(Color(0xFF39373E), RoundedCornerShape(32.dp)).padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Search, contentDescription = "Search songs", tint = SoftText, modifier = Modifier.height(26.dp))
                        Box(modifier = Modifier.padding(start = 18.dp)) {
                            if (searchQuery.isBlank()) Text("Search", color = SoftText, fontSize = 20.sp)
                            innerTextField()
                        }
                    }
                }
            )
        }
        item {
            Text("Saved songs", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
        }
        item {
            Row(
                modifier = Modifier
                    .border(2.dp, Color(0xFF47444C), RoundedCornerShape(32.dp))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .clickable(onClick = onOpenImportSongs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.CloudDownload, contentDescription = null, tint = Color.White, modifier = Modifier.height(26.dp))
                Text("IMPORT SONGS", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(start = 12.dp))
            }
        }
        items(visibleSongs) { track ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onPlaySong(track.uri, track.title) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(74.dp).background(Color(0xFF3C3A40), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (track.artwork != null) {
                        Image(
                            bitmap = track.artwork.asImageBitmap(),
                            contentDescription = "Album art",
                            modifier = Modifier.fillMaxSize().background(Color(0xFFB9B4B8), RoundedCornerShape(12.dp))
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFB9B4B8), RoundedCornerShape(12.dp)))
                    }
                }

                Column(
                    modifier = Modifier.weight(1f).padding(start = 14.dp)
                ) {
                    Text(track.title, color = Color.White, fontSize = 18.sp, maxLines = 1, fontWeight = FontWeight.SemiBold)
                    Text(track.artist, color = SoftText, fontSize = 15.sp, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
                }

                Text("••", color = SoftText, fontSize = 22.sp, modifier = Modifier.padding(start = 12.dp))
            }
        }
    }
}

@Composable
fun MiniPlayer(
    songUri: Uri,
    songTitle: String,
    isPlaying: Boolean,
    onOpen: () -> Unit,
    onTogglePlaying: () -> Unit
) {
    val context = LocalContext.current
    val track = remember(songUri) { loadTrackMetadata(context, songUri) }
    val displayTitle = if (songTitle.isNotBlank()) songTitle else track.title
    val displayArtist = track.artist
    val albumArt = track.artwork

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 5.dp)
            .background(Color(0xFF4D4A50), RoundedCornerShape(18.dp))
            .clickable(onClick = onOpen)
            .padding(start = 6.dp, end = 9.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (albumArt != null) {
            Image(
                bitmap = albumArt.asImageBitmap(),
                contentDescription = "Album artwork",
                modifier = Modifier.size(48.dp).background(Color(0xFFB9B4B8), RoundedCornerShape(12.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFFB9B4B8), RoundedCornerShape(12.dp))
            )
        }

        Column(
            modifier = Modifier.weight(1f).padding(start = 9.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = displayTitle,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Text(
                text = displayArtist,
                color = SoftText,
                fontSize = 13.sp,
                maxLines = 1
            )
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .border(2.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onTogglePlaying, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
