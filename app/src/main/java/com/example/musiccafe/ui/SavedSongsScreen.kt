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
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.musiccafe.loadTrackSummary
import com.example.musiccafe.loadAlbumArt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.yield
import kotlinx.coroutines.withContext

@Composable
fun SavedSongsContent(
    importedSongs: List<Uri>,
    downloadedSongs: Set<Uri>,
    onBack: () -> Unit,
    onPlaySong: (Uri, String) -> Unit,
    onOpenImportSongs: () -> Unit,
    onDeleteSong: (Uri) -> Unit,
    onDeleteAllDownloads: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var trackData by remember { mutableStateOf<List<com.example.musiccafe.TrackMetadata>>(emptyList()) }
    LaunchedEffect(importedSongs) {
        trackData = importedSongs.map { uri ->
            com.example.musiccafe.TrackMetadata(
                uri = uri,
                title = uri.lastPathSegment?.substringAfterLast('/') ?: "Audio file",
                artist = "Loading...",
                artwork = null
            )
        }
        importedSongs.chunked(6).forEach { batch ->
            val summaries = withContext(Dispatchers.IO) {
                batch.map { uri -> loadTrackSummary(context, uri) }
            }
            val summariesByUri = summaries.associateBy { it.uri }
            trackData = trackData.map { track -> summariesByUri[track.uri] ?: track }
            yield()
        }
    }
    val visibleSongs = trackData.filter { track ->
        searchQuery.isBlank() || track.title.contains(searchQuery, ignoreCase = true) || track.artist.contains(searchQuery, ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(ContentBackground).padding(horizontal = 24.dp),
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
                        modifier = Modifier.fillMaxSize().background(Color(0xFF121212), RoundedCornerShape(32.dp)).padding(horizontal = 18.dp),
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
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Saved songs", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                    Text("${importedSongs.size} songs", color = SoftText, fontSize = 15.sp)
                }
                if (trackData.size < importedSongs.size) {
                    CircularProgressIndicator(
                        color = AccentGreen,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier
                        .border(2.dp, Color(0xFF2B2B2E), RoundedCornerShape(32.dp))
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .clickable(onClick = onOpenImportSongs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.CloudDownload, contentDescription = null, tint = Color.White, modifier = Modifier.height(26.dp))
                    Text("IMPORT SONGS", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(start = 12.dp))
                }
                if (downloadedSongs.isNotEmpty()) {
                    Text(
                        "DELETE DOWNLOADED FILES (${downloadedSongs.size})",
                        color = Color(0xFFFF8A80),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .clickable { showDeleteDialog = true }
                            .padding(8.dp)
                    )
                }
            }
        }
        if (importedSongs.isEmpty()) {
            item {
                Text("No songs yet. Import audio or download a YouTube video.", color = SoftText, fontSize = 17.sp)
            }
        }
        items(visibleSongs, key = { track -> track.uri.toString() }) { track ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onPlaySong(track.uri, track.title) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                SongArtwork(
                    uri = track.uri,
                    modifier = Modifier.size(74.dp),
                    cornerRadius = 12.dp
                )

                Column(
                    modifier = Modifier.weight(1f).padding(start = 14.dp)
                ) {
                    Text(track.title, color = Color.White, fontSize = 18.sp, maxLines = 1, fontWeight = FontWeight.SemiBold)
                    Text(track.artist, color = SoftText, fontSize = 15.sp, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
                }

                IconButton(onClick = { onDeleteSong(track.uri) }) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = if (track.uri in downloadedSongs) {
                            "Delete downloaded song"
                        } else {
                            "Remove song from library"
                        },
                        tint = SoftText
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete downloaded files?") },
            text = { Text("This removes ${downloadedSongs.size} files stored by MusicCafe. Imported files outside MusicCafe will not be touched.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeleteAllDownloads()
                }) { Text("DELETE") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("CANCEL") }
            }
        )
    }
}

@Composable
fun SongArtwork(uri: Uri, modifier: Modifier = Modifier, cornerRadius: androidx.compose.ui.unit.Dp = 12.dp) {
    val context = LocalContext.current
    var artwork by remember(uri) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(uri) {
        artwork = withContext(Dispatchers.IO) { loadAlbumArt(context, uri) }
    }
    Box(
        modifier = modifier.background(Color(0xFF1A1A1C), RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        if (artwork != null) {
            Image(
                bitmap = artwork!!.asImageBitmap(),
                contentDescription = "Album art",
                modifier = Modifier.fillMaxSize().background(Color(0xFF3C3C3E), RoundedCornerShape(cornerRadius))
            )
        } else {
            Icon(Icons.Outlined.LibraryMusic, contentDescription = "Music artwork", tint = SoftText)
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
    var track by remember(songUri) { mutableStateOf<com.example.musiccafe.TrackMetadata?>(null) }
    LaunchedEffect(songUri) {
        track = withContext(Dispatchers.IO) { loadTrackMetadata(context, songUri) }
    }
    val displayTitle = if (songTitle.isNotBlank()) songTitle else track?.title ?: "Loading..."
    val displayArtist = track?.artist ?: ""
    val albumArt = track?.artwork

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 5.dp)
            .background(Color(0xFF121212), RoundedCornerShape(18.dp))
            .clickable(onClick = onOpen)
            .padding(start = 6.dp, end = 9.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (albumArt != null) {
            Image(
                bitmap = albumArt.asImageBitmap(),
                contentDescription = "Album artwork",
                modifier = Modifier.size(48.dp).background(Color(0xFF3C3C3E), RoundedCornerShape(12.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF3C3C3E), RoundedCornerShape(12.dp))
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
